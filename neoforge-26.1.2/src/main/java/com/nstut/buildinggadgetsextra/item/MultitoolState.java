package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class MultitoolState {
    private static final String ACTIVE_MODE = "BGEActiveTool";
    private static final String PROFILE_PREFIX = "BGEProfileMode_";
    private static final String TEMPLATE_PREFIX = "BGETemplateProfile_";

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
        if (mode != MultitoolMode.COPY_PASTE && mode != MultitoolMode.CUT_PASTE) return;
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag profile = root.getCompoundOrEmpty(TEMPLATE_PREFIX + mode.serializedName());
        if (profile.isEmpty()) {
            stack.set(BG2DataComponents.GADGET_UUID, UUID.randomUUID());
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
            GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.ZERO);
            return;
        }
        stack.set(BG2DataComponents.GADGET_UUID, UUID.fromString(profile.getStringOr("GadgetId", UUID.randomUUID().toString())));
        String copyId = profile.getStringOr("CopyId", "");
        if (!copyId.isEmpty()) stack.set(BG2DataComponents.COPY_UUID, UUID.fromString(copyId));
        else GadgetNBT.clearCopyUUID(stack);
        GadgetNBT.setCopyStartPos(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("Start", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setCopyEndPos(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("End", GadgetNBT.nullPos.asLong())));
        GadgetNBT.setRelativePaste(stack, net.minecraft.core.BlockPos.of(profile.getLongOr("Relative", 0L)));
    }
}

