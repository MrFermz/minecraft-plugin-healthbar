# minecraft-plugin-healthbar

Plugin แสดง **หลอดเลือด (health bar)** ลอยอยู่เหนือหัว entity แต่ละตัว บน Paper 26.2

ส่วนหนึ่งของ multi-module ecosystem ที่อธิบายไว้ใน [CLAUDE.md ของ root repo](../CLAUDE.md) — depend on `minecraft-plugin-core` แบบ `compileOnly` + `depend: [Core]` (ชื่อ plugin ใน `/pl` = `Healthbar`)

## Concept

- หลอดเลือดจะ **โผล่เฉพาะตอน entity โดน damage จากผู้เล่น** เท่านั้น (ตีตรง ๆ หรือยิงด้วย projectile ก็นับ) — damage จากแหล่งอื่น (ไฟ, ตกเหว, mob ตีกันเอง) ไม่โชว์
- **ถ้าเลือดเพิ่มไม่ว่าด้วยสาเหตุใด (regen, golden apple, potion ฯลฯ) หลอดที่กำลังโชว์อยู่จะอัปเดตเพิ่มตามด้วย** — แต่ heal จะไม่ทำให้หลอดเด้งขึ้นมาใหม่กับ entity ที่ไม่เคยโดนผู้เล่นตี
- แสดงผ่าน **custom name ของ entity** เลย → **ผู้เล่นทุกคนที่อยู่ใกล้เห็นหลอดเดียวกัน** โดยไม่ต้องส่ง packet แยกรายคน
- หลอดค้างอยู่ช่วงสั้น ๆ (`duration-seconds`, default 6 วิ) หลังโดนตีครั้งล่าสุด แล้วค่อย **คืนชื่อเดิม** ของ entity กลับไป
- **ต่อผู้เล่นปรับได้ผ่าน `/menu`** — เปิด/ปิดหลอด + เลือกรูปแบบ (bar / number) ดู [Per-player settings](#per-player-settings-หมวด-health-bar-ใน-menu)
- **ยังไม่มี command ของตัวเอง** (ตั้งค่าผ่าน `/menu` ของ plugin Menu)

## หลอดเป็นยังไง

เป็นแถบแนวนอนที่วาดด้วย **icon** ต่อกัน (default `█` U+2588) ส่วนที่เลือดยังเหลือ = icon ติดสีตาม state, ส่วนที่หายไป = **icon ตัวเดิม** แต่เป็นสีเทาเข้ม (`0x555555`) ที่กลืนพื้นหลังแต่ยังเห็นอยู่ → หลอดคงรูปทรงเดิมเสมอ ไม่หดหายตอนเลือดลด (ไม่มีตัวเลขกำกับ)

**เลือก icon ของหลอดได้ต่อผู้เล่นใน `/menu`** (`healthbar.icon`) — 5 แบบ:

| key | icon | ตัวอย่างหลอด (เหลือ 3/5) |
|-----|:----:|--------------------------|
| `block` (default) | `█` | `███`<span style="color:#555">`██`</span> |
| `heart` | `❤` | `❤❤❤`<span style="color:#555">`❤❤`</span> |
| `star` | `★` | `★★★`<span style="color:#555">`★★`</span> |
| `circle` | `●` | `●●●`<span style="color:#555">`●●`</span> |
| `square` | `■` | `■■■`<span style="color:#555">`■■`</span> |

**ความยาวหลอดคิดจาก max HP จริงของ entity** ไม่ใช่ค่าคงที่ — `blocks = round(maxHP / hp-per-block)` แล้ว clamp อยู่ในช่วง **1–`max-blocks` block** (default cap 10) → mob ยิ่งอึดหลอดยิ่งยาว แต่ไม่เกิน 10

| Entity | max HP | hp-per-block | block ที่ใช้ |
|--------|-------:|-------------:|------------:|
| ไก่ | 4 | 2.0 | 2 |
| ซอมบี้ | 20 | 2.0 | 10 (ชน cap) |
| Iron Golem | 100 | 2.0 | 10 (ชน cap) |

```
██████████   (เต็ม, เขียว — ซอมบี้ 20 HP = 10 block)
██████░░░░    (ส้ม — ░ = icon เดิมแต่สีเทาเข้ม ไม่ใช่คนละตัว)
██░░░░░░░░    (แดง)
██           (ไก่ 4 HP = 2 block เต็ม)
```

**สีคำนวณเองจากสัดส่วนเลือดที่เหลือ** (`HealthBarRenderer.colorFor`) ไม่ต้องไปบอกมัน — 3 state:

| สัดส่วนเลือด | สี | ความหมาย |
|--------------|----|----------|
| `>= green-above` (default `0.5`) | 🟢 เขียว | เต็ม / เกินครึ่ง |
| `red-below <= ratio < green-above` | 🟠 ส้ม | ราว ๆ ครึ่ง |
| `< red-below` (default `0.25`) | 🔴 แดง | เกือบหมด |

> ถ้ายังมีเลือดเหลือ (current > 0) หลอดจะติดอย่างน้อย 1 ขีดเสมอ ไม่โชว์ว่างเปล่าทั้งที่ยังไม่ตาย

## Per-player settings (หมวด `Health bar` ใน `/menu`)

ผู้เล่นปรับเองได้ผ่าน `/menu` (healthbar register เข้า `MenuRegistry` ของ core ตอน `onEnable`, อ่านค่าผ่าน `PlayerPreferenceService` แบบ realtime ทุกครั้งที่ตี — plugin `Menu` เป็นคน render UI, คุยผ่าน core ตาม convention ไม่ reference ข้าม plugin):

| setting key | ชนิด | ค่า | ความหมาย |
|-------------|------|-----|----------|
| `healthbar.enabled` | toggle (checkbox) | default เปิด | ปิดแล้ว = ตอน**คนนี้**ตี entity จะไม่ขึ้นหลอด |
| `healthbar.display` | choice | `bar` (default) / `number` | `bar` = หลอดบล็อกสี; `number` = ตัวเลข `current/total` ติดสีตาม state เช่น `15/20` |
| `healthbar.icon` | choice | `block` (default) / `heart` / `star` / `circle` / `square` | icon ที่ใช้วาดหลอด (เฉพาะสไตล์ `bar`) — ส่วนเลือดที่หายไปใช้ icon เดิมแต่สีเทาเข้ม |

> **ข้อจำกัด:** หลอดเขียนลง custom name ของ entity ซึ่ง **เป็นชื่อเดียวที่ทุกคนเห็นร่วมกัน** — รูปแบบที่โชว์จึงเป็นของ **คนที่ตีครั้งล่าสุด** (ถ้าจะให้แต่ละคนเห็นคนละแบบจริง ๆ ต้องส่ง name packet แยกรายผู้ชม ซึ่งเป็นงานใหญ่กว่านี้) อ่านค่า setting แบบ realtime ทุกครั้งที่ตี → เปลี่ยนใน `/menu` แล้วมีผลกับการตีครั้งถัดไป

## โครงสร้างโค้ด

| Class | หน้าที่ |
|-------|---------|
| `HealthBarPlugin` | entry point — โหลด config ผ่าน `EcosystemData`, สร้าง manager + renderer, register listener |
| `HealthBarSettings` | record อ่านค่าจาก `healthbar.yml` |
| `listener/HealthListener` | ฟัง `EntityDamageByEntityEvent` (กรองเฉพาะ damage จากผู้เล่น → `show` หลอดในสไตล์ของคนตี) + `EntityRegainHealthEvent` (heal ทุกสาเหตุ → `refresh` เฉพาะตัวที่หลอดโชว์อยู่) อ่านเลือดจริง 1 tick ถัดไปบน region thread ของ entity; อ่าน `healthbar.display` + `healthbar.icon` ของคนตีผ่าน `PlayerPreferenceService` |
| `display/HealthBarManager` | จำชื่อเดิม + สไตล์ + icon ของ entity, set custom name เป็นหลอด (`show`/`refresh`), มี sweep ทุก 1 วิ คืนชื่อเดิมเมื่อหมดเวลา |
| `render/HealthBarRenderer` | แปลง `current/max` เป็น Adventure `Component` ตาม `DisplayStyle` (bar = หลอดสีวาดด้วย icon, ส่วนหายใช้ icon เดิมสีเทาเข้ม / number = `current/total`) + เลือกสีตาม state |
| `render/DisplayStyle` | enum `BAR`/`NUMBER` + parse จากค่า preference |
| `render/BarIcon` | enum icon 5 แบบ (`BLOCK`/`HEART`/`STAR`/`CIRCLE`/`SQUARE`) — key + อักขระ + label สำหรับ dropdown ใน `/menu` |

## Config (`plugins/antitle/healthbar.yml`)

> config เป็นไฟล์แบนในโฟลเดอร์รวมของ ecosystem ที่ `plugins/antitle/healthbar.yml` (ไม่ใช่ `plugins/Healthbar/`) — resolve ผ่าน `EcosystemData` ของ core ดู [CLAUDE.md → Config directory บน server](../CLAUDE.md#config-directory-บน-server)

```yaml
display:
  duration-seconds: 6     # หลอดค้างกี่วินาทีหลังโดนตีครั้งล่าสุด
  hp-per-block: 2.0       # 1 block = กี่ HP (blocks = round(maxHP / ค่านี้))
  max-blocks: 10          # เพดานความยาวหลอด (พื้นอย่างน้อย 1 block เสมอ)
  bar-icon: "█"           # icon fallback ก่อนผู้เล่นเลือกใน /menu (default บล็อกทึบ U+2588)
  show-on-players: false  # โชว์เหนือหัวผู้เล่นด้วยไหม (default เฉพาะ mob)
colors:
  green-above: 0.5        # ratio >= ค่านี้ = เขียว
  red-below: 0.25         # ratio < ค่านี้ = แดง (ระหว่างกลาง = ส้ม)
```

## ต้องใช้ core plugin ไหม?

**ต้อง — ใช้ config/logging + per-player settings ของ core แต่ไม่เปิด DB pool ของตัวเอง**

- depend on core เพราะใช้ `EcosystemData` (วาง config ในโฟลเดอร์รวม `plugins/antitle/`) + `PluginLog` (format log ให้เหมือนทั้ง ecosystem)
- register `MenuItem` (`healthbar.enabled`, `healthbar.display`, `healthbar.icon`) เข้า `MenuRegistry` ของ core และอ่านค่าผู้เล่นผ่าน `PlayerPreferenceService` (`CoreApi.menu(...)` / `CoreApi.preferences(...)`) — ค่าเก็บในตาราง `setting_values` ของ **core** ไม่ใช่ของ healthbar
- **ไม่** register service ของตัวเองเข้า `ServicesManager`, **ไม่** ขอ `DatabaseService`/เปิด pool, **ไม่** มีตารางของตัวเอง — state ของหลอด (ตัวที่กำลังโชว์) ยังอยู่ในเมมโมรี หายตอน restart ได้ไม่เป็นไร
- ทั้ง setting registry + preference เป็น optional (`ifPresent`/null-check) ถ้า core DB ไม่พร้อม หลอดยังทำงานปกติด้วย default = bar
- ยังต้องลง `minecraft-plugin-core.jar` บน server และตั้ง `depend: [Core]` ใน `plugin.yml` เพื่อให้ load ลำดับถูก

## Build

```
./gradlew :minecraft-plugin-healthbar:build
# ได้ jar/minecraft-plugin-healthbar.jar (shadow jar, ไม่ bundle core)
```
