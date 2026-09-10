package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingSaveTargetTest {
    @Test
    void freshDestinationIsRetained() {
        assertFalse(new PendingSaveTarget(Paths.get("structure.nbt"), System.currentTimeMillis()).isExpired());
    }

    @Test
    void abandonedDestinationExpires() {
        assertTrue(new PendingSaveTarget(Paths.get("structure.nbt"),
                System.currentTimeMillis() - 31_000L).isExpired());
    }
}
