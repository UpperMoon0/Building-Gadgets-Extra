package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static com.nstut.buildinggadgetsextra.common.RadialButtonPolicy.FileAction.LOAD;
import static com.nstut.buildinggadgetsextra.common.RadialButtonPolicy.FileAction.NONE;
import static com.nstut.buildinggadgetsextra.common.RadialButtonPolicy.FileAction.SAVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadialButtonPolicyTest {
    @Test
    void mirrorsOnlyAppearInPasteMode() {
        assertTrue(RadialButtonPolicy.showMirrorButtons("paste"));
        assertFalse(RadialButtonPolicy.showMirrorButtons("copy"));
        assertFalse(RadialButtonPolicy.showMirrorButtons("cut"));
        assertFalse(RadialButtonPolicy.showMirrorButtons(null));
    }

    @Test
    void copyPasteToolSavesInCopyAndLoadsInPaste() {
        assertEquals(SAVE, RadialButtonPolicy.fileAction(false, "copy"));
        assertEquals(LOAD, RadialButtonPolicy.fileAction(false, "paste"));
        assertEquals(NONE, RadialButtonPolicy.fileAction(false, "cut"));
    }

    @Test
    void cutPasteToolNeverLoadsExternalStructures() {
        assertEquals(SAVE, RadialButtonPolicy.fileAction(true, "cut"));
        assertEquals(NONE, RadialButtonPolicy.fileAction(true, "paste"));
        assertEquals(NONE, RadialButtonPolicy.fileAction(true, "copy"));
    }

    @Test
    void legacyCutActionOnlyAugmentsCopyMode() {
        assertTrue(RadialButtonPolicy.showLegacyCutAction(false, "copy"));
        assertFalse(RadialButtonPolicy.showLegacyCutAction(false, "paste"));
        assertFalse(RadialButtonPolicy.showLegacyCutAction(true, "copy"));
        assertFalse(RadialButtonPolicy.showLegacyCutAction(false, null));
    }
}
