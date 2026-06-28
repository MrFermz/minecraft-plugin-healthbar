# minecraft-plugin-healthbar

Plugin แสดง **หลอดเลือด (health bar)** ลอยอยู่เหนือหัว entity แต่ละตัว บน Paper 26.2

ส่วนหนึ่งของ multi-module ecosystem ที่อธิบายไว้ใน [CLAUDE.md ของ root repo](../CLAUDE.md) — depend on `minecraft-plugin-core` แบบ `compileOnly` + `depend: [MinecraftPluginCore]`

## Concept

- หลอดเลือดจะ **โผล่เฉพาะตอน entity โดน damage จากผู้เล่น** เท่านั้น (ตีตรง ๆ หรือยิงด้วย projectile ก็นับ) — damage จากแหล่งอื่น (ไฟ, ตกเหว, mob ตีกันเอง) ไม่โชว์
- **ถ้าเลือดเพิ่มไม่ว่าด้วยสาเหตุใด (regen, golden apple, potion ฯลฯ) หลอดที่กำลังโชว์อยู่จะอัปเดตเพิ่มตามด้วย** — แต่ heal จะไม่ทำให้หลอดเด้งขึ้นมาใหม่กับ entity ที่ไม่เคยโดนผู้เล่นตี
- แสดงผ่าน **custom name ของ entity** เลย → **ผู้เล่นทุกคนที่อยู่ใกล้เห็นหลอดเดียวกัน** โดยไม่ต้องส่ง packet แยกรายคน
- หลอดค้างอยู่ช่วงสั้น ๆ (`duration-seconds`, default 6 วิ) หลังโดนตีครั้งล่าสุด แล้วค่อย **คืนชื่อเดิม** ของ entity กลับไป
- **ยังไม่มี command** (ตามที่ตั้งใจไว้ตอนนี้)

## หลอดเป็นยังไง

เป็นแถบทึบแนวนอน ใช้อักขระบล็อกทึบ `█` (U+2588) ต่อกัน ส่วนที่เลือดยังเหลือ = ติดสีตาม state, ส่วนที่หายไป = สีเทาเข้ม (ไม่มีตัวเลขกำกับ)

**ความยาวหลอดคิดจาก max HP จริงของ entity** ไม่ใช่ค่าคงที่ — `blocks = round(maxHP / hp-per-block)` แล้ว clamp อยู่ในช่วง **1–`max-blocks` block** (default cap 10) → mob ยิ่งอึดหลอดยิ่งยาว แต่ไม่เกิน 10

| Entity | max HP | hp-per-block | block ที่ใช้ |
|--------|-------:|-------------:|------------:|
| ไก่ | 4 | 2.0 | 2 |
| ซอมบี้ | 20 | 2.0 | 10 (ชน cap) |
| Iron Golem | 100 | 2.0 | 10 (ชน cap) |

```
██████████   (เต็ม, เขียว — ซอมบี้ 20 HP = 10 block)
██████░░░░    (ส้ม — █ ติดสี, ░ บล็อกสีเทา)
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

## โครงสร้างโค้ด

| Class | หน้าที่ |
|-------|---------|
| `HealthBarPlugin` | entry point — โหลด config ผ่าน `EcosystemData`, สร้าง manager + renderer, register listener |
| `HealthBarSettings` | record อ่านค่าจาก `healthbar.yml` |
| `listener/HealthListener` | ฟัง `EntityDamageByEntityEvent` (กรองเฉพาะ damage จากผู้เล่น → `show` หลอด) + `EntityRegainHealthEvent` (heal ทุกสาเหตุ → `refresh` เฉพาะตัวที่หลอดโชว์อยู่) อ่านเลือดจริง 1 tick ถัดไปบน region thread ของ entity |
| `display/HealthBarManager` | จำชื่อเดิมของ entity, set custom name เป็นหลอด (`show`/`refresh`), มี sweep ทุก 1 วิ คืนชื่อเดิมเมื่อหมดเวลา |
| `render/HealthBarRenderer` | แปลง `current/max` เป็น Adventure `Component` หลอดสี + เลือกสีตาม state |

## Config (`plugins/antitle/healthbar.yml`)

> config เป็นไฟล์แบนในโฟลเดอร์รวมของ ecosystem ที่ `plugins/antitle/healthbar.yml` (ไม่ใช่ `plugins/HealthBarPlugin/`) — resolve ผ่าน `EcosystemData` ของ core ดู [CLAUDE.md → Config directory บน server](../CLAUDE.md#config-directory-บน-server)

```yaml
display:
  duration-seconds: 6     # หลอดค้างกี่วินาทีหลังโดนตีครั้งล่าสุด
  hp-per-block: 2.0       # 1 block = กี่ HP (blocks = round(maxHP / ค่านี้))
  max-blocks: 10          # เพดานความยาวหลอด (พื้นอย่างน้อย 1 block เสมอ)
  filled-char: "█"        # อักขระส่วนเลือดที่เหลือ (default บล็อกทึบ U+2588)
  empty-char: "█"         # อักขระส่วนเลือดที่หาย
  show-on-players: false  # โชว์เหนือหัวผู้เล่นด้วยไหม (default เฉพาะ mob)
colors:
  green-above: 0.5        # ratio >= ค่านี้ = เขียว
  red-below: 0.25         # ratio < ค่านี้ = แดง (ระหว่างกลาง = ส้ม)
```

## ต้องใช้ core plugin ไหม?

**ต้อง — แต่ใช้แค่ระดับ config/logging ไม่แตะ database หรือ service ใด ๆ**

- depend on core เพราะใช้ `EcosystemData` (วาง config ในโฟลเดอร์รวม `plugins/antitle/`) + `PluginLog` (format log ให้เหมือนทั้ง ecosystem) — สองตัวนี้อยู่ใน core ตาม convention
- **ไม่** register service เข้า `ServicesManager`, **ไม่** ขอ `DatabaseService`/`CoreApi.database(...)`, **ไม่** เปิด pool — เป็น plugin ที่ไม่มี persistent state (state อยู่ในเมมโมรี = หลอดที่กำลังโชว์ หายตอน restart ได้ไม่เป็นไร)
- ยังต้องลง `minecraft-plugin-core.jar` บน server และตั้ง `depend: [MinecraftPluginCore]` ใน `plugin.yml` เพื่อให้ load ลำดับถูก

## Build

```
./gradlew :minecraft-plugin-healthbar:build
# ได้ jar/minecraft-plugin-healthbar.jar (shadow jar, ไม่ bundle core)
```
