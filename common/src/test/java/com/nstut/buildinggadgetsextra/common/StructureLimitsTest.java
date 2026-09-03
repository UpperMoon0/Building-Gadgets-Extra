package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
