package com.mrfermz.mcplugins.healthbar;

import com.mrfermz.mcplugins.core.CoreApi;
import com.mrfermz.mcplugins.core.EcosystemData;
import com.mrfermz.mcplugins.core.log.PluginLog;
import com.mrfermz.mcplugins.core.menu.MenuItem;
import com.mrfermz.mcplugins.core.menu.PlayerPreferenceService;
import com.mrfermz.mcplugins.healthbar.display.HealthBarManager;
import com.mrfermz.mcplugins.healthbar.listener.HealthListener;
import com.mrfermz.mcplugins.healthbar.render.DisplayStyle;
import com.mrfermz.mcplugins.healthbar.render.HealthBarRenderer;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Floating health bar plugin. Whenever a player damages a living entity, a
 * horizontal coloured bar appears above it (visible to every nearby player) and
 * fades back to the entity's real name after a short delay.
 *
 * <p>Depends on {@code minecraft-plugin-core} only for the shared config dir
 * ({@link EcosystemData}) and logging convention — it registers no service and
 * uses no database (see the module README).
 */
public final class HealthBarPlugin extends JavaPlugin {

    /** Short module name → plugins/antitle/healthbar.yml. */
    private static final String MODULE = "healthbar";

    /** Per-player setting key (registered on core): bar vs. number display. */
    public static final String DISPLAY_KEY = "healthbar.display";

    private PluginLog log;
    private HealthBarManager manager;

    @Override
    public void onEnable() {
        this.log = PluginLog.of(this);

        // Config lives in the shared ecosystem dir via core, not getConfig()
        // (CLAUDE.md → Config directory บน server).
        FileConfiguration config = EcosystemData.config(this, MODULE);
        HealthBarSettings settings = HealthBarSettings.from(config);

        HealthBarRenderer renderer = new HealthBarRenderer(settings);
        this.manager = new HealthBarManager(this, settings, renderer);
        manager.start();

        // Offer a per-player "bar vs number" choice via the shared in-game menu,
        // and read each hitter's choice live (CLAUDE.md → plugins talk through
        // core). Both are optional — health bars still work without them.
        CoreApi.menu(getServer()).ifPresent(registry -> registry.register(
                MenuItem.choice(DISPLAY_KEY, "Healthbar",
                        "Health bar display", "How damaged entities' health shows to you",
                        List.of(
                                new MenuItem.Option(DisplayStyle.BAR.key(), "Bar"),
                                new MenuItem.Option(DisplayStyle.NUMBER.key(), "Number (current/total)")),
                        DisplayStyle.BAR.key())));
        PlayerPreferenceService prefs = CoreApi.preferences(getServer()).orElse(null);

        getServer().getPluginManager().registerEvents(
                new HealthListener(this, settings, manager, prefs), this);

        log.info("Health bars ready (≤{} blocks, {} hp/block, {}s on screen, players: {}).",
                settings.maxBlocks(),
                settings.hpPerBlock(),
                settings.durationMillis() / 1000.0,
                settings.showOnPlayers());
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            // Put every entity's real name back before we go.
            manager.restoreAll();
        }
        if (log != null) {
            log.info("Health bars disabled, entity names restored.");
        }
    }
}
