package com.mrfermz.mcplugins.healthbar.render;

import java.util.Locale;

/**
 * The selectable character a health bar is drawn with. Chosen per player via the
 * in-game {@code /menu} (key {@code healthbar.icon}); the default is {@link #BLOCK}.
 *
 * <p>The same icon is used for both the remaining-health segments (in the state
 * colour) and the lost-health segments (in a dim grey) — so the bar keeps one
 * consistent shape and the lost part fades into the background without vanishing.
 */
public enum BarIcon {

    /** Solid full block {@code █} (U+2588) — the original, reads as one strip. */
    BLOCK("█", "Block"),

    /** Heart {@code ❤} (U+2764) — a classic life bar. */
    HEART("❤", "Heart"),

    /** Star {@code ★} (U+2605). */
    STAR("★", "Star"),

    /** Filled circle {@code ●} (U+25CF) — a row of pips. */
    CIRCLE("●", "Circle"),

    /** Filled square {@code ■} (U+25A0). */
    SQUARE("■", "Square");

    private final String icon;
    private final String label;

    BarIcon(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    /** The character drawn for one bar segment. */
    public String icon() {
        return icon;
    }

    /** Human-readable label shown in the {@code /menu} dropdown. */
    public String label() {
        return label;
    }

    /** The value stored in preferences for this icon (e.g. {@code "heart"}). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a stored preference value; anything unrecognised falls back to {@link #BLOCK}. */
    public static BarIcon fromKey(String value) {
        if (value != null) {
            String trimmed = value.trim();
            for (BarIcon icon : values()) {
                if (icon.key().equalsIgnoreCase(trimmed)) {
                    return icon;
                }
            }
        }
        return BLOCK;
    }
}
