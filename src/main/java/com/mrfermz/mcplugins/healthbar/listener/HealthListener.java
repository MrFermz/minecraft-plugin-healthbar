package com.mrfermz.mcplugins.healthbar.listener;

import com.mrfermz.mcplugins.healthbar.HealthBarSettings;
import com.mrfermz.mcplugins.healthbar.display.HealthBarManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Keeps the floating health bar in sync with an entity's health:
 *
 * <ul>
 *   <li><b>Damage</b> from a player (directly or via their projectile) makes the
 *       bar appear. Damage from any other source is ignored.</li>
 *   <li><b>Healing</b> from any source updates an <em>already-visible</em> bar so
 *       it grows back — it never makes a bar pop up on an entity a player never
 *       hit (see {@link HealthBarManager#refresh}).</li>
 * </ul>
 */
public final class HealthListener implements Listener {

    private final Plugin plugin;
    private final HealthBarSettings settings;
    private final HealthBarManager manager;

    public HealthListener(Plugin plugin, HealthBarSettings settings, HealthBarManager manager) {
        this.plugin = plugin;
        this.settings = settings;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (victim instanceof Player && !settings.showOnPlayers()) {
            return;
        }
        if (!isPlayerSource(event.getDamager())) {
            return;
        }
        // Read the post-hit health one tick later (see syncBar).
        syncBar(victim, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        // Heal of any cause, but only if a bar is already showing on this entity.
        syncBar(entity, true);
    }

    /**
     * Re-renders the bar from the entity's actual health one tick later, on the
     * entity's own region thread — {@code getHealth()} is still the pre-event
     * value while the event is being handled. When {@code refreshOnly} is set the
     * bar is only updated if one is already visible (used for healing).
     */
    private void syncBar(LivingEntity entity, boolean refreshOnly) {
        entity.getScheduler().run(plugin, task -> {
            if (!entity.isValid() || entity.isDead()) {
                return;
            }
            double current = entity.getHealth();
            if (current <= 0) {
                return;
            }
            double max = entity.getMaxHealth();
            if (refreshOnly) {
                manager.refresh(entity, current, max);
            } else {
                manager.show(entity, current, max);
            }
        }, null);
    }

    /** True when the damage originates from a player, including their projectiles. */
    private static boolean isPlayerSource(Entity damager) {
        if (damager instanceof Player) {
            return true;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player;
        }
        return false;
    }
}
