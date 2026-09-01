package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
                .copyTag().getString(ACTIVE_MODE));
    }

    public static void setActiveMode(ItemStack stack, MultitoolMode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(ACTIVE_MODE, mode.serializedName()));
    }

    public static ResourceLocation getProfileMode(ItemStack stack, MultitoolMode mode) {
        String value = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(PROFILE_PREFIX + mode.serializedName());
        return ResourceLocation.tryParse(value);
    }

    public static void setProfileMode(ItemStack stack, MultitoolMode mode, ResourceLocation value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(PROFILE_PREFIX + mode.serializedName(), value.toString()));
    }

    public static void saveTemplateProfile(ItemStack stack, MultitoolMode mode) {
        saveGadgetUuidProfile(stack, mode);
        saveUndoProfile(stack, mode);
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag profile = new CompoundTag();
        profile.putUUID("GadgetId", GadgetNBT.getUUID(stack));
        if (GadgetNBT.hasCopyUUID(stack)) profile.putUUID("CopyId", GadgetNBT.getCopyUUID(stack));
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
        CompoundTag profile = root.getCompound(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.ZERO);
            return;
        }
        if (profile.hasUUID("CopyId")) stack.set(BG2DataComponents.COPY_UUID, profile.getUUID("CopyId"));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("Start")));
        GadgetNBT.setCopyEndPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("End")));
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.of(profile.getLong("Relative")));
    }

    private static void saveGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        UUID uuid = GadgetNBT.getUUID(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putUUID(UUID_PREFIX + mode.serializedName(), uuid));
    }

    private static void restoreGadgetUuidProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String key = UUID_PREFIX + mode.serializedName();
        UUID uuid = root.hasUUID(key) ? root.getUUID(key) : null;
        if (uuid == null && (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE)) {
            CompoundTag legacy = root.getCompound(TEMPLATE_PREFIX + mode.serializedName());
            if (legacy.hasUUID("GadgetId")) uuid = legacy.getUUID("GadgetId");
        }
        if (uuid == null) uuid = UUID.randomUUID();
        stack.set(BG2DataComponents.GADGET_UUID, uuid);
        UUID finalUuid = uuid;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(key, finalUuid));
    }

    private static void saveUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag saved = new CompoundTag();
        LinkedList<UUID> undo = GadgetNBT.getUndoList(stack);
        saved.putInt("Size", undo.size());
        for (int i = 0; i < undo.size(); i++) saved.putUUID("Entry" + i, undo.get(i));
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(UNDO_PREFIX + mode.serializedName(), saved));
    }

    private static void restoreUndoProfile(ItemStack stack, MultitoolMode mode) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag saved = root.getCompound(UNDO_PREFIX + mode.serializedName());
        LinkedList<UUID> undo = new LinkedList<>();
        int size = saved.getInt("Size");
        for (int i = 0; i < size; i++) {
            String key = "Entry" + i;
            if (saved.hasUUID(key)) undo.add(saved.getUUID(key));
        }
        GadgetNBT.setUndoList(stack, undo);
    }
}
