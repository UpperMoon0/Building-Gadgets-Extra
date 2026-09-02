package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultitoolRangePolicyTest {
    @Test
    void defaultsAndBoundsStayEndgameButFinite() {
        assertEquals(32, MultitoolRangePolicy.DEFAULT_MAX_RANGE);
        assertEquals(1, MultitoolRangePolicy.clamp(-100, 32));
        assertEquals(32, MultitoolRangePolicy.clamp(999, 32));
        assertEquals(64, MultitoolRangePolicy.clamp(999, 999));
        assertEquals(1, MultitoolRangePolicy.clamp(20, 0));
    }
}
