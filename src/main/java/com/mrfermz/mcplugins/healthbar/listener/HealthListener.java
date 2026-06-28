package com.mrfermz.mcplugins.healthbar.listener;

import com.mrfermz.mcplugins.core.settings.PlayerPreferenceService;
import com.mrfermz.mcplugins.healthbar.HealthBarPlugin;
import com.mrfermz.mcplugins.healthbar.HealthBarSettings;
import com.mrfermz.mcplugins.healthbar.display.HealthBarManager;
import com.mrfermz.mcplugins.healthbar.render.DisplayStyle;
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
    /** Per-player preferences from core; {@code null} if the service isn't available. */
    private final PlayerPreferenceService prefs;

    public HealthListener(Plugin plugin, HealthBarSettings settings, HealthBarManager manager,
                          PlayerPreferenceService prefs) {
        this.plugin = plugin;
        this.settings = settings;
        this.manager = manager;
        this.prefs = prefs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (victim instanceof Player && !settings.showOnPlayers()) {
            return;
        }
        Player damager = playerSource(event.getDamager());
        if (damager == null) {
            return;
        }
        // The bar is drawn in the hitter's chosen style (read live, so changing it
        // in /setting takes effect on the next hit). Read the post-hit health one
        // tick later (see syncBar).
        syncBar(victim, false, styleFor(damager));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        // Heal of any cause, but only if a bar is already showing on this entity.
        // Refresh keeps whatever style the bar already has, so style is irrelevant.
        syncBar(entity, true, DisplayStyle.BAR);
    }

    /**
     * Re-renders the bar from the entity's actual health one tick later, on the
     * entity's own region thread — {@code getHealth()} is still the pre-event
     * value while the event is being handled. When {@code refreshOnly} is set the
     * bar is only updated if one is already visible (used for healing).
     */
    private void syncBar(LivingEntity entity, boolean refreshOnly, DisplayStyle style) {
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
                manager.show(entity, current, max, style);
            }
        }, null);
    }

    /** The display style the damager picked in {@code /setting}, defaulting to a bar. */
    private DisplayStyle styleFor(Player damager) {
        if (prefs == null) {
            return DisplayStyle.BAR;
        }
        return DisplayStyle.fromKey(prefs.get(damager.getUniqueId(),
                HealthBarPlugin.DISPLAY_KEY, DisplayStyle.BAR.key()));
    }

    /** The player behind the damage (direct or via their projectile), or {@code null}. */
    private static Player playerSource(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
