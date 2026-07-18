package com.nstut.buildinggadgetsextra.common;

/** Version-neutral visibility rules for Copy Paste and Cut Paste radial-menu additions. */
public final class RadialButtonPolicy {
    private RadialButtonPolicy() {
    }

    public static boolean showMirrorButtons(String mode) {
        return "paste".equals(mode);
    }

    public static FileAction fileAction(boolean cutPasteTool, String mode) {
        if (cutPasteTool) {
            return "cut".equals(mode) ? FileAction.SAVE : FileAction.NONE;
        }
        if ("copy".equals(mode)) return FileAction.SAVE;
        if ("paste".equals(mode)) return FileAction.LOAD;
        return FileAction.NONE;
    }

    /** The legacy 1.16 gadget has no native Cut Paste item, so Cut augments Copy mode only. */
    public static boolean showLegacyCutAction(boolean cutPasteTool, String mode) {
        return !cutPasteTool && "copy".equals(mode);
    }

    public enum FileAction {
        NONE,
        SAVE,
        LOAD
    }
}
