package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultitoolModeTest {
    @Test
    void modesHaveStableRadialOrder() {
        assertEquals(MultitoolMode.BUILD, MultitoolMode.values()[0]);
        assertEquals(MultitoolMode.EXCHANGING, MultitoolMode.BUILD.next());
        assertEquals(MultitoolMode.COPY_PASTE, MultitoolMode.EXCHANGING.next());
        assertEquals(MultitoolMode.CUT_PASTE, MultitoolMode.COPY_PASTE.next());
        assertEquals(MultitoolMode.DESTRUCTION, MultitoolMode.CUT_PASTE.next());
        assertEquals(MultitoolMode.BUILD, MultitoolMode.DESTRUCTION.next());
    }

    @Test
    void persistedNamesAreDefensive() {
        assertEquals(MultitoolMode.COPY_PASTE, MultitoolMode.parse("copy_paste"));
        assertEquals(MultitoolMode.EXCHANGING, MultitoolMode.parse("exchanging"));
        assertEquals(MultitoolMode.CUT_PASTE, MultitoolMode.parse("CUT_PASTE"));
        assertEquals(MultitoolMode.DEFAULT, MultitoolMode.parse("future_mode"));
        assertEquals(MultitoolMode.DEFAULT, MultitoolMode.parse(null));
    }

    @Test
    void everyModeHasUiMetadata() {
        for (MultitoolMode mode : MultitoolMode.values()) {
            assertEquals("buildinggadgetsextra.multitool.mode." + mode.serializedName(), mode.translationKey());
        }
    }
}
