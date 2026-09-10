package com.nstut.buildinggadgetsextra.common;

import java.util.OptionalInt;

/** Shared bounds and mode policy for the endgame Builder's Multitool range. */
public final class MultitoolRangePolicy {
    public static final int MIN_RANGE = 1;
    /** Zero selects the native range multiplied by the configured multiplier. */
    public static final int DEFAULT_MAX_RANGE = 0;
    public static final double DEFAULT_RANGE_MULTIPLIER = 2.0;
    public static final int HARD_MAX_RANGE = 64;

    private MultitoolRangePolicy() {}

    /** Scale distances, keeping configuration mistakes and old item data bounded. */
    public static double scaledReach(double nativeRange, double multiplier) {
        return Math.max(0, nativeRange) * Math.max(1, Math.min(4, multiplier));
    }

    public static int scaledLimit(int nativeRange, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(scaledReach(nativeRange, multiplier)));
    }

    public static boolean validDestruction(int left, int right, int up, int down, int depth,
                                           int sideMax, int spanMax) {
        return left >= 0 && right >= 0 && up >= 0 && down >= 0 && depth >= 0
                && left <= sideMax && right <= sideMax && up <= sideMax && down <= sideMax
                && depth <= sideMax && (long) left + right <= spanMax && (long) up + down <= spanMax;
    }

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
