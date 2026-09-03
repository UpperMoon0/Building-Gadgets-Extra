package com.nstut.buildinggadgetsextra.common;

/** Shared validation for dimensions supplied by untrusted structure files. */
public final class StructureLimits {
    private StructureLimits() {}

    /**
     * Returns the structure volume when all dimensions are positive and within the configured limit.
     * Returns -1 when the dimensions are invalid, too large, or overflow a signed long.
     */
    public static long checkedVolume(int x, int y, int z) {
        if (x <= 0 || y <= 0 || z <= 0) return -1L;
        try {
            long xy = Math.multiplyExact((long) x, (long) y);
            long volume = Math.multiplyExact(xy, (long) z);
            return volume <= ExtraConstants.MAX_STRUCTURE_BLOCKS ? volume : -1L;
        } catch (ArithmeticException overflow) {
            return -1L;
        }
    }
}
