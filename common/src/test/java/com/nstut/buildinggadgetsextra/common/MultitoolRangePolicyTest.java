package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultitoolRangePolicyTest {
    @Test
    void defaultsAndBoundsStayEndgameButFinite() {
        assertEquals(32, MultitoolRangePolicy.DEFAULT_MAX_RANGE);
        assertEquals(1, MultitoolRangePolicy.clamp(-100, 32));
        assertEquals(32, MultitoolRangePolicy.clamp(999, 32));
        assertEquals(64, MultitoolRangePolicy.clamp(999, 999));
        assertEquals(1, MultitoolRangePolicy.clamp(20, 0));
    }

    @Test
    void onlyBuildAndExchangeOwnRangeSettings() {
        assertTrue(MultitoolRangePolicy.supportsRange(MultitoolMode.BUILD));
        assertTrue(MultitoolRangePolicy.supportsRange(MultitoolMode.EXCHANGING));
        assertFalse(MultitoolRangePolicy.supportsRange(MultitoolMode.COPY_PASTE));
        assertFalse(MultitoolRangePolicy.supportsRange(MultitoolMode.CUT_PASTE));
        assertFalse(MultitoolRangePolicy.supportsRange(MultitoolMode.DESTRUCTION));

        assertFalse(MultitoolRangePolicy.resolve(MultitoolMode.COPY_PASTE, 7, 32).isPresent());
        assertEquals(7, MultitoolRangePolicy.resolve(MultitoolMode.BUILD, 7, 32).getAsInt());
    }

    @Test
    void hotkeyCyclingUsesTheSameConfiguredBounds() {
        assertEquals(OptionalInt.of(8), MultitoolRangePolicy.next(MultitoolMode.BUILD, 7, 32));
        assertEquals(OptionalInt.of(1), MultitoolRangePolicy.next(MultitoolMode.BUILD, 32, 32));
        assertEquals(OptionalInt.of(1), MultitoolRangePolicy.next(MultitoolMode.EXCHANGING, 64, 32));
        assertFalse(MultitoolRangePolicy.next(MultitoolMode.DESTRUCTION, 7, 32).isPresent());
    }
}
