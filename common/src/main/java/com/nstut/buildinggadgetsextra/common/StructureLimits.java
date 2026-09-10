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

    /** Prevent a crafted palette from allocating more entries than its declared structure can contain. */
    public static boolean validPaletteEntryCount(int entries, long volume) {
        return entries >= 0 && volume >= 0
                && entries <= volume && entries <= ExtraConstants.MAX_STRUCTURE_BLOCKS;
    }

    /** Validate a palette position before retaining it in an import-side lookup table. */
    public static boolean isWithinBounds(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
        return sizeX > 0 && sizeY > 0 && sizeZ > 0
                && x >= 0 && y >= 0 && z >= 0
                && x < sizeX && y < sizeY && z < sizeZ;
    }
}
