package com.mrfermz.mcplugins.healthbar.render;

import java.util.Locale;

/**
 * How a health bar is drawn above an entity. Chosen per player via the in-game
 * {@code /menu} (key {@code healthbar.display}); the default is
 * {@link #BAR}.
 *
 * <p>Because the bar is written into the entity's (shared) custom name, the style
 * used is that of the player who landed the hit — see the module README.
 */
public enum DisplayStyle {

    /** A coloured segmented bar (the original look). */
    BAR,

    /** Plain {@code current/total} numbers, coloured by remaining health. */
    NUMBER;

    /** The value stored in preferences for this style ({@code "bar"} / {@code "number"}). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a stored preference value; anything unrecognised falls back to {@link #BAR}. */
    public static DisplayStyle fromKey(String value) {
        if (value != null && value.trim().equalsIgnoreCase("number")) {
            return NUMBER;
        }
        return BAR;
    }
}
