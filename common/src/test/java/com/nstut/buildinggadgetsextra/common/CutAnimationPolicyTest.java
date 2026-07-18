package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CutAnimationPolicyTest {
    @Test
    void matchesModernCutQueuePacing() {
        assertEquals(1, CutAnimationPolicy.blocksPerTick(1));
        assertEquals(1, CutAnimationPolicy.blocksPerTick(59));
        assertEquals(5, CutAnimationPolicy.blocksPerTick(60));
        assertEquals(10, CutAnimationPolicy.blocksPerTick(3_000));
        assertEquals(50, CutAnimationPolicy.blocksPerTick(100_000));
    }
}
