package com.nstut.buildinggadgetsextra.common;

/** Version-neutral pacing copied from Building Gadgets 2's server cut queue. */
public final class CutAnimationPolicy {
    private CutAnimationPolicy() {
    }

    public static int blocksPerTick(int originalSize) {
        int minimum = originalSize < 60 ? 1 : 5;
        return Math.min(Math.max(originalSize / 300, minimum), 50);
    }
}
