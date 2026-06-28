package com.mrfermz.mcplugins.healthbar.display;

import com.mrfermz.mcplugins.healthbar.HealthBarSettings;
import com.mrfermz.mcplugins.healthbar.render.DisplayStyle;
import com.mrfermz.mcplugins.healthbar.render.HealthBarRenderer;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * Owns the floating health bars currently shown above entities.
 *
 * <p>The bar is rendered into the entity's custom name (so every nearby player
 * sees the same thing, with no per-player packet tracking). The entity's
 * original name and visibility are remembered the first time a bar appears and
 * restored once the bar expires — {@value} after the last hit.
 *
 * <p>A single repeating sweep restores expired bars; everything runs on the
 * server's main/region thread, so the maps are only touched from there (the
 * {@link ConcurrentHashMap} is belt-and-braces).
 */
public final class HealthBarManager {

    /** Remembered state so the entity's real name can be put back. */
    private record Tracked(Component originalName, boolean originalVisible, long expiresAt,
                           DisplayStyle style) {
    }

    private final Plugin plugin;
    private final HealthBarSettings settings;
    private final HealthBarRenderer renderer;
    private final Map<UUID, Tracked> active = new ConcurrentHashMap<>();

    public HealthBarManager(Plugin plugin, HealthBarSettings settings, HealthBarRenderer renderer) {
        this.plugin = plugin;
        this.settings = settings;
        this.renderer = renderer;
    }

    /** Starts the periodic sweep that hides expired bars (every second). */
    public void start() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin, task -> sweep(), 20L, 20L);
    }

    /**
     * Shows (or refreshes) the bar above {@code entity} for the configured
     * duration, drawn in {@code style} (the hitter's chosen display). Stores the
     * entity's real name on first appearance.
     */
    public void show(LivingEntity entity, double current, double max, DisplayStyle style) {
        UUID id = entity.getUniqueId();
        active.compute(id, (key, existing) -> {
            Component original = existing != null ? existing.originalName() : entity.customName();
            boolean visible = existing != null ? existing.originalVisible() : entity.isCustomNameVisible();
            return new Tracked(original, visible,
                    System.currentTimeMillis() + settings.durationMillis(), style);
        });

        entity.customName(renderer.render(current, max, style));
        entity.setCustomNameVisible(true);
    }

    /**
     * Updates an already-visible bar to new health values (e.g. the entity
     * healed). No-op — returns {@code false} — if the entity has no active bar,
     * so healing never makes a bar pop up on an entity a player never hit.
     * Refreshes the visibility timer so the bar stays up while health changes.
     */
    public boolean refresh(LivingEntity entity, double current, double max) {
        UUID id = entity.getUniqueId();
        Tracked existing = active.get(id);
        if (existing == null) {
            return false;
        }
        active.put(id, new Tracked(existing.originalName(), existing.originalVisible(),
                System.currentTimeMillis() + settings.durationMillis(), existing.style()));
        entity.customName(renderer.render(current, max, existing.style()));
        entity.setCustomNameVisible(true);
        return true;
    }

    /** Restores every still-tracked entity (called on disable). */
    public void restoreAll() {
        for (UUID id : active.keySet()) {
            restore(id);
        }
        active.clear();
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Tracked>> it = active.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Tracked> entry = it.next();
            if (now >= entry.getValue().expiresAt()) {
                restoreEntity(entry.getKey(), entry.getValue());
                it.remove();
            }
        }
    }

    private void restore(UUID id) {
        Tracked tracked = active.get(id);
        if (tracked != null) {
            restoreEntity(id, tracked);
        }
    }

    private void restoreEntity(UUID id, Tracked tracked) {
        Server server = plugin.getServer();
        Entity entity = server.getEntity(id);
        // If the entity is gone (dead/unloaded) there is nothing to restore.
        if (entity instanceof LivingEntity living) {
            living.customName(tracked.originalName());
            living.setCustomNameVisible(tracked.originalVisible());
        }
    }
}
