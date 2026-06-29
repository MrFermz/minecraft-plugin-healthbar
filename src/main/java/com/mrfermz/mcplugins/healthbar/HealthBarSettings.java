package com.mrfermz.mcplugins.healthbar;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable view of the {@code healthbar.yml} settings (see resources/config.yml).
 *
 * <p>The bar's total length is not fixed: it is computed per entity from that
 * entity's real max health ({@code round(maxHealth / hpPerBlock)}), then clamped
 * to {@code [1, maxBlocks]} — so a tougher mob gets a longer bar, up to the cap.
 *
 * @param hpPerBlock    how much max health one block represents
 * @param maxBlocks     hard cap on the bar length (and the floor is always 1)
 * @param barIcon       fallback bar character when a player hasn't picked an icon
 * @param showOnPlayers also render the bar above players, not just mobs
 * @param greenAbove    ratio at/above which the bar is green
 * @param redBelow      ratio below which the bar is red (orange in between)
 * @param durationMillis how long the bar stays visible after the last hit
 */
public record HealthBarSettings(
        double hpPerBlock,
        int maxBlocks,
        String barIcon,
        boolean showOnPlayers,
        double greenAbove,
        double redBelow,
        long durationMillis) {

    /** Reads the settings from a loaded {@code healthbar.yml}, with defaults. */
    public static HealthBarSettings from(FileConfiguration cfg) {
        return new HealthBarSettings(
                Math.max(0.1, cfg.getDouble("display.hp-per-block", 2.0)),
                Math.max(1, cfg.getInt("display.max-blocks", 10)),
                cfg.getString("display.bar-icon", "█"),
                cfg.getBoolean("display.show-on-players", false),
                cfg.getDouble("colors.green-above", 0.5),
                cfg.getDouble("colors.red-below", 0.25),
                Math.round(cfg.getDouble("display.duration-seconds", 6.0) * 1000.0));
    }
}
