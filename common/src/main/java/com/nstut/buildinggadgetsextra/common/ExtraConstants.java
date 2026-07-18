package com.nstut.buildinggadgetsextra.common;

public final class ExtraConstants {
    public static final String MOD_ID = "buildinggadgetsextra";
    public static final String MIRROR_HORIZONTAL = MOD_ID + ".radialmenu.mirror_horizontal";
    public static final String MIRROR_VERTICAL = MOD_ID + ".radialmenu.mirror_vertical";
    public static final String NO_TEMPLATE = MOD_ID + ".message.no_template";
    public static final String BUSY = MOD_ID + ".message.busy";
    public static final String MIRRORED_HORIZONTAL = MOD_ID + ".message.mirrored_horizontal";
    public static final String MIRRORED_VERTICAL = MOD_ID + ".message.mirrored_vertical";
    public static final String STRUCTURE_LIBRARY = MOD_ID + ".radialmenu.structure_library";
    public static final String STRUCTURE_NAME = MOD_ID + ".screen.structure_name";
    public static final String SAVE_STRUCTURE = MOD_ID + ".screen.save";
    public static final String LOAD_STRUCTURE = MOD_ID + ".screen.load";
    public static final String STRUCTURE_SAVED = MOD_ID + ".message.structure_saved";
    public static final String STRUCTURE_LOADED = MOD_ID + ".message.structure_loaded";
    public static final String STRUCTURE_NOT_FOUND = MOD_ID + ".message.structure_not_found";
    public static final String INVALID_STRUCTURE_NAME = MOD_ID + ".message.invalid_structure_name";
    public static final String STRUCTURE_SAVE_FAILED = MOD_ID + ".message.structure_save_failed";
    public static final String STRUCTURE_TOO_LARGE = MOD_ID + ".message.structure_too_large";
    public static final String STRUCTURE_LOAD_FAILED = MOD_ID + ".message.structure_load_failed";
    public static final long MAX_STRUCTURE_BLOCKS = 4_000_000L;
    public static final int STRUCTURE_CHUNK_SIZE = 32 * 1024;
    public static final int MAX_STRUCTURE_FILE_BYTES = 8 * 1024 * 1024;

    private ExtraConstants() {
    }
}
