package com.nstut.buildinggadgetsextra.common;

/** Version-neutral visibility rules for Copy Paste and Cut Paste radial-menu additions. */
public final class RadialButtonPolicy {
    private RadialButtonPolicy() {
    }

    public static boolean showMirrorButtons(String mode) {
        return "paste".equals(mode);
    }

    /** Rotation affects the stored template, so it is meaningful only while pasting. */
    public static boolean showRotateButton(String mode) {
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

    public enum FileAction {
        NONE,
        SAVE,
        LOAD
    }
}
