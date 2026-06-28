package com.mrfermz.mcplugins.healthbar;

import com.mrfermz.mcplugins.core.EcosystemData;
import com.mrfermz.mcplugins.core.log.PluginLog;
import com.mrfermz.mcplugins.healthbar.display.HealthBarManager;
import com.mrfermz.mcplugins.healthbar.listener.HealthListener;
import com.mrfermz.mcplugins.healthbar.render.HealthBarRenderer;
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

        getServer().getPluginManager().registerEvents(
                new HealthListener(this, settings, manager), this);

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
