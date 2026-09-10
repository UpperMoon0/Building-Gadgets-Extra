package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultitoolRangePolicyTest {
    @Test
    void multiplierFollowsNativeConfigurationAndBoundsDestructionWithoutOverflow() {
        assertEquals(30, MultitoolRangePolicy.scaledLimit(15, 2));
        assertEquals(64, MultitoolRangePolicy.scaledReach(32, 2));
        assertEquals(96, MultitoolRangePolicy.scaledReach(48, 2));
        assertEquals(48, MultitoolRangePolicy.scaledLimit(16, 3));
        assertTrue(MultitoolRangePolicy.validDestruction(20, 12, 16, 16, 32, 32, 32));
        assertFalse(MultitoolRangePolicy.validDestruction(20, 13, 0, 0, 32, 32, 32));
        assertTrue(MultitoolRangePolicy.validDestruction(32, 32, 32, 32, 32, 32, 64));
        assertFalse(MultitoolRangePolicy.validDestruction(-1, 0, 0, 0, 0, 32, 64));
        assertFalse(MultitoolRangePolicy.validDestruction(Integer.MAX_VALUE, Integer.MAX_VALUE,
                0, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void defaultsAndBoundsStayEndgameButFinite() {
        assertEquals(0, MultitoolRangePolicy.DEFAULT_MAX_RANGE);
        assertEquals(2.0, MultitoolRangePolicy.DEFAULT_RANGE_MULTIPLIER);
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
