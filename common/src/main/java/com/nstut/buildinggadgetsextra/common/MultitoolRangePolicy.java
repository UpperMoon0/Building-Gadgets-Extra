package com.nstut.buildinggadgetsextra.common;

import java.util.OptionalInt;

/** Shared bounds and mode policy for the endgame Builder's Multitool range. */
public final class MultitoolRangePolicy {
    public static final int MIN_RANGE = 1;
    public static final int DEFAULT_MAX_RANGE = 32;
    public static final int HARD_MAX_RANGE = 64;

    private MultitoolRangePolicy() {}

    public static boolean supportsRange(MultitoolMode mode) {
        return mode == MultitoolMode.BUILD || mode == MultitoolMode.EXCHANGING;
    }

    public static OptionalInt resolve(MultitoolMode mode, int requested, int configuredMax) {
        if (!supportsRange(mode)) return OptionalInt.empty();
        return OptionalInt.of(clamp(requested, configuredMax));
    }

    public static OptionalInt next(MultitoolMode mode, int current, int configuredMax) {
        if (!supportsRange(mode)) return OptionalInt.empty();
        int max = normalizedMax(configuredMax);
        return OptionalInt.of(current >= max ? MIN_RANGE : clamp(current + 1, max));
    }

    public static int clamp(int requested, int configuredMax) {
        int max = normalizedMax(configuredMax);
        return Math.max(MIN_RANGE, Math.min(max, requested));
    }

    private static int normalizedMax(int configuredMax) {
        return Math.max(MIN_RANGE, Math.min(HARD_MAX_RANGE, configuredMax));
    }
}
