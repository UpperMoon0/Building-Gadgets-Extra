package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureLimitsTest {
    @Test
    void acceptsVolumeAtConfiguredLimit() {
        assertEquals(100_000L, StructureLimits.checkedVolume(100, 100, 10));
    }

    @Test
    void rejectsVolumeAboveConfiguredLimit() {
        assertEquals(-1L, StructureLimits.checkedVolume(100_001, 1, 1));
    }

    @Test
    void rejectsOverflowingVolume() {
        assertEquals(-1L, StructureLimits.checkedVolume(2_097_152, 2_097_152, 2_097_152));
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertEquals(-1L, StructureLimits.checkedVolume(0, 10, 10));
        assertEquals(-1L, StructureLimits.checkedVolume(10, -1, 10));
    }

    @Test
    void boundsPaletteEntriesByDeclaredVolume() {
        assertTrue(StructureLimits.validPaletteEntryCount(100_000, 100_000L));
        assertTrue(StructureLimits.validPaletteEntryCount(10, 100_000L));
        assertFalse(StructureLimits.validPaletteEntryCount(11, 10L));
        assertFalse(StructureLimits.validPaletteEntryCount(100_001, 100_001L));
        assertFalse(StructureLimits.validPaletteEntryCount(-1, 10L));
    }

    @Test
    void supportsLargerDenseTemplatesOnlyWithinTheServerLimit() {
        assertEquals(110_000L, StructureLimits.checkedVolume(110, 100, 10, 110_000));
        assertEquals(-1L, StructureLimits.checkedVolume(110, 100, 10, 109_999));
        assertEquals(1_000_000L, StructureLimits.checkedVolume(100, 100, 100, 1_000_000));
        assertEquals(-1L, StructureLimits.checkedVolume(100, 100, 101, 1_000_000));
        assertEquals(-1L, StructureLimits.checkedVolume(1, 1, 1, 1_000_001));
        assertEquals(-1L, StructureLimits.checkedVolume(1, 1, 1, 0));
        assertEquals(-1L, StructureLimits.checkedVolume(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1_000_000));
        assertTrue(StructureLimits.validPaletteEntryCount(110_000, 110_000L, 110_000));
        assertFalse(StructureLimits.validPaletteEntryCount(1, 110_000L, 100_000));
        assertFalse(StructureLimits.validPaletteEntryCount(110_001, 110_000L, 1_000_000));
    }

    @Test
    void validatesPalettePositionsAgainstDeclaredDimensions() {
        assertTrue(StructureLimits.isWithinBounds(0, 0, 0, 2, 3, 4));
        assertTrue(StructureLimits.isWithinBounds(1, 2, 3, 2, 3, 4));
        assertFalse(StructureLimits.isWithinBounds(-1, 0, 0, 2, 3, 4));
        assertFalse(StructureLimits.isWithinBounds(2, 0, 0, 2, 3, 4));
        assertFalse(StructureLimits.isWithinBounds(0, 3, 0, 2, 3, 4));
        assertFalse(StructureLimits.isWithinBounds(0, 0, 4, 2, 3, 4));
    }
}
