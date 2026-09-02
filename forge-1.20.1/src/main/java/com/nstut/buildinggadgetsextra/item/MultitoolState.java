package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.UUID;

public final class MultitoolState {
    private static final String ACTIVE_MODE = "BGEActiveTool";
    private static final String PROFILE_PREFIX = "BGEProfileMode_";
    private static final String TEMPLATE_PREFIX = "BGETemplateProfile_";
    private static final String STATE_PREFIX = "BGEStateProfile_";
    private static final String UNDO_PREFIX = "BGEUndoProfile_";
    private static final String UUID_PREFIX = "BGEGadgetProfile_";

    /**
     * BG2 1.0.8 keeps gadget behavior in the stack's item NBT. These keys are the
     * mutable upstream gadget state that would naturally live on five separate stacks.
     * BGE metadata, enchantments/display data, and Forge capability state are deliberately
     * excluded so the physical multitool still has one battery and one cosmetic identity.
     */
    private static final String[] GENERAL_KEYS = {
            "bound", "binddirection", "anchor", "anchorList", "anchorside", "rendertype",
            "blockstate", "range", "templatename",
            "raytracefluid", "placeontop", "affecttiles", "pastereplace",
            "bind", "fuzzy", "connected_area",
            "depth", "right", "left", "up", "down"
    };

    private MultitoolState() {}

    public static MultitoolMode getActiveMode(ItemStack stack) {
        return MultitoolMode.parse(stack.getOrCreateTag().getString(ACTIVE_MODE));
    }

    public static void setActiveMode(ItemStack stack, MultitoolMode mode) {
        stack.getOrCreateTag().putString(ACTIVE_MODE, mode.serializedName());
    }

    public static ResourceLocation getProfileMode(ItemStack stack, MultitoolMode mode) {
        return ResourceLocation.tryParse(stack.getOrCreateTag().getString(PROFILE_PREFIX + mode.serializedName()));
    }

    public static void setProfileMode(ItemStack stack, MultitoolMode mode, ResourceLocation value) {
        stack.getOrCreateTag().putString(PROFILE_PREFIX + mode.serializedName(), value.toString());
    }

    public static void saveTemplateProfile(ItemStack stack, MultitoolMode mode) {
        saveGeneralProfile(stack, mode);
        saveGadgetUuidProfile(stack, mode);
        saveUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag profile = new CompoundTag();
        profile.putUUID("GadgetId", GadgetNBT.getUUID(stack));
        if (GadgetNBT.hasCopyUUID(stack)) profile.putUUID("CopyId", GadgetNBT.getCopyUUID(stack));
        profile.putLong("Start", GadgetNBT.getCopyStartPos(stack).asLong());
        profile.putLong("End", GadgetNBT.getCopyEndPos(stack).asLong());
        profile.putLong("Relative", GadgetNBT.getRelativePaste(stack).asLong());
        stack.getOrCreateTag().put(TEMPLATE_PREFIX + mode.serializedName(), profile);
    }

    public static void restoreTemplateProfile(ItemStack stack, MultitoolMode mode) {
        restoreGeneralProfile(stack, mode);
        restoreGadgetUuidProfile(stack, mode);
        restoreUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) {
            clearCopyState(stack);
            return;
        }
        CompoundTag profile = stack.getOrCreateTag().getCompound(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            clearCopyState(stack);
            return;
        }
        if (profile.hasUUID("CopyId")) GadgetNBT.setCopyUUID(stack, profile.getUUID("CopyId"));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("Start")));
        GadgetNBT.setCopyEndPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("End")));
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.of(profile.getLong("Relative")));
    }

    private static void saveGeneralProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag profile = new CompoundTag();
        profile.putBoolean("Initialized", true);
        for (String key : GENERAL_KEYS) {
            Tag value = root.get(key);
            if (value != null) profile.put(key, value.copy());
        }
        root.put(STATE_PREFIX + mode.serializedName(), profile);
    }

    private static void restoreGeneralProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag profile = root.getCompound(STATE_PREFIX + mode.serializedName());
        for (String key : GENERAL_KEYS) root.remove(key);
        if (profile.getBoolean("Initialized")) {
            for (String key : GENERAL_KEYS) {
                Tag value = profile.get(key);
                if (value != null) root.put(key, value.copy());
            }
        } else if (mode == MultitoolMode.CUT_PASTE) {
            // Native GadgetCutPaste defaults Paste Replace to true on first access.
            root.putBoolean("pastereplace", true);
        }
    }

    private static void saveGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        stack.getOrCreateTag().putUUID(UUID_PREFIX + mode.serializedName(), GadgetNBT.getUUID(stack));
    }

    private static void restoreGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrCreateTag();
        String key = UUID_PREFIX + mode.serializedName();
        UUID uuid = null;
        if (root.hasUUID(key)) uuid = root.getUUID(key);
        if (uuid == null && (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE)) {
            CompoundTag legacy = root.getCompound(TEMPLATE_PREFIX + mode.serializedName());
            if (legacy.hasUUID("GadgetId")) uuid = legacy.getUUID("GadgetId");
        }
        if (uuid == null) uuid = UUID.randomUUID();
        root.putUUID("uuid", uuid);
        root.putUUID(key, uuid);
    }

    private static void saveUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag saved = new CompoundTag();
        LinkedList<UUID> undo = GadgetNBT.getUndoList(stack);
        saved.putInt("Size", undo.size());
        for (int i = 0; i < undo.size(); i++) saved.putUUID("Entry" + i, undo.get(i));
        stack.getOrCreateTag().put(UNDO_PREFIX + mode.serializedName(), saved);
    }

    private static void restoreUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag saved = stack.getOrCreateTag().getCompound(UNDO_PREFIX + mode.serializedName());
        LinkedList<UUID> undo = new LinkedList<>();
        int size = saved.getInt("Size");
        for (int i = 0; i < size; i++) {
            String key = "Entry" + i;
            if (saved.hasUUID(key)) undo.add(saved.getUUID(key));
        }
        GadgetNBT.setUndoList(stack, undo);
    }

    private static void clearCopyState(ItemStack stack) {
        GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.ZERO);
    }
}
