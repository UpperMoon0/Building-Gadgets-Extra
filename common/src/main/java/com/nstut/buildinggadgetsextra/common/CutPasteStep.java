package com.nstut.buildinggadgetsextra.common;

/** The next action follows the authoritative selection and stored cut contents. */
public enum CutPasteStep {
    SELECT, CUT, PASTE;

    public static CutPasteStep of(boolean hasBuffer, boolean hasStart, boolean hasEnd) {
        return hasBuffer ? PASTE : hasStart && hasEnd ? CUT : SELECT;
    }
}
