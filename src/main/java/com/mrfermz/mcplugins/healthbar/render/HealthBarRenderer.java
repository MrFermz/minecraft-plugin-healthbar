package com.mrfermz.mcplugins.healthbar.render;

import com.mrfermz.mcplugins.healthbar.HealthBarSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Turns an entity's {@code current/max} health into a horizontal coloured bar
 * {@link Component} that can be set as the entity's custom name.
 *
 * <p>The colour is computed from the remaining-health ratio alone (the caller
 * does not pick it): three states — green when full-ish, orange around half, red
 * when almost empty — with the thresholds coming from {@link HealthBarSettings}.
 *
 * <p>The bar's length is also computed from the entity's real max health
 * ({@code round(max / hpPerBlock)}), clamped to {@code [1, maxBlocks]} — so a
 * tougher entity shows a longer bar, up to the cap.
 */
public final class HealthBarRenderer {

    // Three bar states. Chosen to read clearly above an entity.
    private static final TextColor GREEN = TextColor.color(0x55FF55);
    private static final TextColor ORANGE = TextColor.color(0xFFAA00);
    private static final TextColor RED = TextColor.color(0xFF5555);
    /**
     * Colour of the lost-health segments: a dim grey that blends into the
     * background yet stays readable, so the bar's full length is always visible.
     */
    private static final TextColor EMPTY = TextColor.color(0x555555);

    private final HealthBarSettings settings;

    public HealthBarRenderer(HealthBarSettings settings) {
        this.settings = settings;
    }

    /**
     * Builds the display for the given health values in the requested
     * {@link DisplayStyle}, drawing a bar out of {@code icon} (the per-player
     * choice from {@code /menu}; blank → the configured {@code bar-icon}). The same
     * icon draws both the remaining segments (state colour) and the lost segments
     * (dim grey), so the bar keeps one shape. {@code max <= 0} renders as empty/zero.
     */
    public Component render(double current, double max, DisplayStyle style, String icon) {
        double ratio = max <= 0 ? 0.0 : clamp(current / max, 0.0, 1.0);
        if (style == DisplayStyle.NUMBER) {
            return Component.text(number(current) + "/" + number(max), colorFor(ratio));
        }

        // Bar length scales with the entity's real max health, capped at maxBlocks.
        int blocks = blocksFor(max);
        int filled = (int) Math.round(ratio * blocks);
        // Keep at least one lit segment while the entity is still alive, so a
        // sliver of health never looks like an empty bar.
        if (current > 0 && filled == 0) {
            filled = 1;
        }
        int empty = blocks - filled;

        TextColor color = colorFor(ratio);
        // Per-player icon; blank (no preference) falls back to config (█).
        String glyph = (icon == null || icon.isBlank()) ? settings.barIcon() : icon;

        return Component.text(glyph.repeat(filled), color)
                .append(Component.text(glyph.repeat(empty), EMPTY));
    }

    /**
     * Number of blocks the whole bar uses for an entity with {@code max} health:
     * {@code round(max / hpPerBlock)} clamped to {@code [1, maxBlocks]}.
     */
    private int blocksFor(double max) {
        long n = Math.round(max / settings.hpPerBlock());
        return (int) Math.max(1, Math.min(settings.maxBlocks(), n));
    }

    /** Picks the bar colour purely from the remaining-health ratio. */
    private TextColor colorFor(double ratio) {
        if (ratio >= settings.greenAbove()) {
            return GREEN;
        }
        if (ratio < settings.redBelow()) {
            return RED;
        }
        return ORANGE;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Formats a health value as a whole number, or one decimal when fractional. */
    private static String number(double value) {
        double rounded = Math.max(0, value);
        if (rounded == Math.rint(rounded)) {
            return Long.toString((long) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }
}
