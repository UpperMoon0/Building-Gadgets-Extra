package com.nstut.buildinggadgetsextra.common;

/** Shared bounds for the endgame Builder's Multitool range. */
public final class MultitoolRangePolicy {
    public static final int MIN_RANGE = 1;
    public static final int DEFAULT_MAX_RANGE = 32;
    public static final int HARD_MAX_RANGE = 64;

    private MultitoolRangePolicy() {}

    public static int clamp(int requested, int configuredMax) {
        int max = Math.max(MIN_RANGE, Math.min(HARD_MAX_RANGE, configuredMax));
        return Math.max(MIN_RANGE, Math.min(max, requested));
    }
}
