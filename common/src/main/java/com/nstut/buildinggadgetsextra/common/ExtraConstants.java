package com.nstut.buildinggadgetsextra.common;

public final class ExtraConstants {
    public static final String MOD_ID = "buildinggadgetsextra";
    public static final String MIRROR_HORIZONTAL = MOD_ID + ".radialmenu.mirror_horizontal";
    public static final String MIRROR_VERTICAL = MOD_ID + ".radialmenu.mirror_vertical";
    public static final String NO_TEMPLATE = MOD_ID + ".message.no_template";
    public static final String BUSY = MOD_ID + ".message.busy";
    public static final String MIRRORED_HORIZONTAL = MOD_ID + ".message.mirrored_horizontal";
    public static final String MIRRORED_VERTICAL = MOD_ID + ".message.mirrored_vertical";
    public static final String SAVE_STRUCTURE = MOD_ID + ".radialmenu.save_structure";
    public static final String LOAD_STRUCTURE = MOD_ID + ".radialmenu.load_structure";
    public static final String CUT_SELECTION = MOD_ID + ".radialmenu.cut_selection";
    public static final String STRUCTURE_SAVED = MOD_ID + ".message.structure_saved";
    public static final String STRUCTURE_LOADED = MOD_ID + ".message.structure_loaded";
    public static final String INVALID_STRUCTURE_NAME = MOD_ID + ".message.invalid_structure_name";
    public static final String STRUCTURE_SAVE_FAILED = MOD_ID + ".message.structure_save_failed";
    public static final String STRUCTURE_TOO_LARGE = MOD_ID + ".message.structure_too_large";
    public static final String STRUCTURE_LOAD_FAILED = MOD_ID + ".message.structure_load_failed";
    public static final String STRUCTURE_IMPORT_REQUIRES_COPY = MOD_ID + ".message.structure_import_requires_copy";
    public static final String STRUCTURE_BLOCK_ENTITY_STRIPPED = MOD_ID + ".message.structure_block_entity_stripped";
    public static final String CUT_NO_SELECTION = MOD_ID + ".message.cut_no_selection";
    public static final String CUT_TEMPLATE_MISMATCH = MOD_ID + ".message.cut_template_mismatch";
    public static final String CUT_NOT_ALLOWED = MOD_ID + ".message.cut_not_allowed";
    public static final String CUT_NOT_ENOUGH_ENERGY = MOD_ID + ".message.cut_not_enough_energy";
    public static final String CUT_COMPLETE = MOD_ID + ".message.cut_complete";
    public static final String CUT_NO_VALID_BLOCKS = MOD_ID + ".message.cut_no_valid_blocks";

    // Keep imported structures in the same rough envelope as native BG/BG2 copy operations.
    public static final long MAX_STRUCTURE_BLOCKS = 100_000L;
    public static final long MAX_STRUCTURE_NBT_BYTES = 64L * 1024L * 1024L;
    public static final int STRUCTURE_CHUNK_SIZE = 32 * 1024;
    public static final int MAX_STRUCTURE_FILE_BYTES = 8 * 1024 * 1024;
    public static final int MAX_STRUCTURE_TRANSFERS_PER_PLAYER = 2;

    public static final String DIALOG_SAVE_STRUCTURE = MOD_ID + ".dialog.save_structure";
    public static final String DIALOG_OPEN_STRUCTURE = MOD_ID + ".dialog.open_structure";
    public static final String DIALOG_NBT_FILES = MOD_ID + ".dialog.nbt_files";

    private ExtraConstants() {
    }
}
