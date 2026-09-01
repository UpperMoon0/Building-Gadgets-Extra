package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedList;
import java.util.UUID;

public final class MultitoolState {
    private static final String ACTIVE_MODE = "BGEActiveTool";
    private static final String PROFILE_PREFIX = "BGEProfileMode_";
    private static final String TEMPLATE_PREFIX = "BGETemplateProfile_";
    private static final String UNDO_PREFIX = "BGEUndoProfile_";
    private static final String UUID_PREFIX = "BGEGadgetProfile_";

    private MultitoolState() {
    }

    public static MultitoolMode getActiveMode(ItemStack stack) {
        return MultitoolMode.parse(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(ACTIVE_MODE, ""));
    }

    public static void setActiveMode(ItemStack stack, MultitoolMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(ACTIVE_MODE, mode.serializedName()));
    }

    public static Identifier getProfileMode(ItemStack stack, MultitoolMode mode) {
        String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(PROFILE_PREFIX + mode.serializedName(), "");
        try { return value.isEmpty() ? null : Identifier.parse(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static void setProfileMode(ItemStack stack, MultitoolMode mode, Identifier value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(PROFILE_PREFIX + mode.serializedName(), value.toString()));
    }

    public static void saveTemplateProfile(ItemStack stack, MultitoolMode mode) {
        saveGadgetUuidProfile(stack, mode);
        saveUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag profile = new CompoundTag();
        profile.putString("GadgetId", GadgetNBT.getUUID(stack).toString());
        if (GadgetNBT.hasCopyUUID(stack)) profile.putString("CopyId", GadgetNBT.getCopyUUID(stack).toString());
        profile.putLong("Start", GadgetNBT.getCopyStartPos(stack).asLong());
        profile.putLong("End", GadgetNBT.getCopyEndPos(stack).asLong());
        profile.putLong("Relative", GadgetNBT.getRelativePaste(stack).asLong());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(TEMPLATE_PREFIX + mode.serializedName(), profile));
    }

    public static void restoreTemplateProfile(ItemStack stack, MultitoolMode mode) {
        restoreGadgetUuidProfile(stack, mode);
        restoreUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag profile = root.getCompoundOrEmpty(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.ZERO);
            return;
        }
        String copyId = profile.getStringOr("CopyId", "");
        if (!copyId.isEmpty()) stack.set(BG2DataComponents.COPY_UUID, UUID.fromString(copyId));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("Start", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setCopyEndPos(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("End", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("Relative", 0L)));
    }

    private static void saveGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        UUID uuid = GadgetNBT.getUUID(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(UUID_PREFIX + mode.serializedName(), uuid.toString()));
    }

    private static void restoreGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String key = UUID_PREFIX + mode.serializedName();
        UUID uuid = parseUuid(root.getStringOr(key, ""));
        if (uuid == null && (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE)) {
            CompoundTag legacy = root.getCompoundOrEmpty(TEMPLATE_PREFIX + mode.serializedName());
            uuid = parseUuid(legacy.getStringOr("GadgetId", ""));
        }
        if (uuid == null) uuid = UUID.randomUUID();
        stack.set(BG2DataComponents.GADGET_UUID, uuid);
        UUID finalUuid = uuid;
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(key, finalUuid.toString()));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static void saveUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag saved = new CompoundTag();
        LinkedList<UUID> undo = GadgetNBT.getUndoList(stack);
        saved.putInt("Size", undo.size());
        for (int i = 0; i < undo.size(); i++) saved.putString("Entry" + i, undo.get(i).toString());
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(UNDO_PREFIX + mode.serializedName(), saved));
    }

    private static void restoreUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag saved = root.getCompoundOrEmpty(UNDO_PREFIX + mode.serializedName());
        LinkedList<UUID> undo = new LinkedList<>();
        int size = saved.getIntOr("Size", 0);
        for (int i = 0; i < size; i++) {
            UUID uuid = parseUuid(saved.getStringOr("Entry" + i, ""));
            if (uuid != null) undo.add(uuid);
        }
        GadgetNBT.setUndoList(stack, undo);
    }
}
