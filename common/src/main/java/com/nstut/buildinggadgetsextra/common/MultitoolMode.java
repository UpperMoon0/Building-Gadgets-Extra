package com.nstut.buildinggadgetsextra.common;

import java.util.Locale;

/** Loader-independent identity and ordering for the Builder's Multitool tools. */
public enum MultitoolMode {
    BUILD("build", 0x55C96B),
    EXCHANGING("exchanging", 0xB06CD8),
    COPY_PASTE("copy_paste", 0x4D9DE0),
    CUT_PASTE("cut_paste", 0xF29E4C),
    DESTRUCTION("destruction", 0xE05A5A);

    public static final MultitoolMode DEFAULT = BUILD;

    private final String serializedName;
    private final int accentColor;

    MultitoolMode(String serializedName, int accentColor) {
        this.serializedName = serializedName;
        this.accentColor = accentColor;
    }

    public String serializedName() {
        return serializedName;
    }

    public int accentColor() {
        return accentColor;
    }

    public String translationKey() {
        return "buildinggadgetsextra.multitool.mode." + serializedName;
    }

    public MultitoolMode next() {
        MultitoolMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public static MultitoolMode parse(String value) {
        if (value == null || value.trim().isEmpty()) return DEFAULT;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MultitoolMode mode : values()) {
            if (mode.serializedName.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
