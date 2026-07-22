package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class MultitoolState {
    private static final String ACTIVE_MODE = "BGEActiveTool";
    private static final String PROFILE_PREFIX = "BGEProfileMode_";
    private static final String TEMPLATE_PREFIX = "BGETemplateProfile_";

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
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag profile = stack.getOrCreateTag().getCompound(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            stack.getOrCreateTag().putUUID("uuid", UUID.randomUUID());
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.ZERO);
            return;
        }
        stack.getOrCreateTag().putUUID("uuid", profile.getUUID("GadgetId"));
        if (profile.hasUUID("CopyId")) GadgetNBT.setCopyUUID(stack, profile.getUUID("CopyId"));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("Start")));
        GadgetNBT.setCopyEndPos(stack, net.minecraft.core.BlockPos.of(profile.getLong("End")));
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.of(profile.getLong("Relative")));
    }
}
