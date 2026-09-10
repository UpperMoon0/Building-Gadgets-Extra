package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugInstrumentationTest {
    @AfterEach
    void reset() {
        DebugInstrumentation.resetForTests();
    }

    @Test
    void burstThenSuppressesUntilRefill() {
        long now = 1_000_000_000L;
        for (int i = 0; i < DebugInstrumentation.BURST; i++) {
            assertTrue(DebugInstrumentation.acquire("range", now).allowed);
        }
        assertFalse(DebugInstrumentation.acquire("range", now).allowed);
        assertFalse(DebugInstrumentation.acquire("range", now).allowed);

        DebugInstrumentation.Permit refilled = DebugInstrumentation.acquire("range", now + 500_000_000L);
        assertTrue(refilled.allowed);
        assertEquals(2, refilled.suppressed);
    }

    @Test
    void categoriesHaveIndependentBuckets() {
        long now = 2_000_000_000L;
        for (int i = 0; i < DebugInstrumentation.BURST; i++) {
            assertTrue(DebugInstrumentation.acquire("range-apply", now).allowed);
        }
        assertFalse(DebugInstrumentation.acquire("range-apply", now).allowed);
        assertTrue(DebugInstrumentation.acquire("range-reject", now).allowed);
    }
}
