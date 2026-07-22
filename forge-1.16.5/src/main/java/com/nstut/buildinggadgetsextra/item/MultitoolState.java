package com.nstut.buildinggadgetsextra.item;

import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.items.modes.BuildingModes;
import com.direwolf20.buildinggadgets.common.items.modes.ExchangingModes;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class MultitoolState {
    private static final String ACTIVE = "BGEActiveTool";
    private static final String PROFILE = "BGEProfile_";
    private MultitoolState() {}

    public static MultitoolMode getActiveMode(ItemStack stack) {
        return MultitoolMode.parse(stack.getOrCreateTag().getString(ACTIVE));
    }

    public static void setActiveMode(ItemStack stack, MultitoolMode mode) {
        stack.getOrCreateTag().putString(ACTIVE, mode.serializedName());
    }

    public static int getProfile(ItemStack stack, MultitoolMode mode) {
        return stack.getOrCreateTag().getInt(PROFILE + mode.serializedName());
    }

    public static void saveCurrentProfile(ItemStack stack, MultitoolMode mode) {
        int index = 0;
        if (mode == MultitoolMode.BUILD) index = GadgetBuilding.getToolMode(stack).ordinal();
        else if (mode == MultitoolMode.EXCHANGING) index = GadgetExchanger.getToolMode(stack).ordinal();
        else if (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE) index = GadgetCopyPaste.getToolMode(stack).ordinal();
        stack.getOrCreateTag().putInt(PROFILE + mode.serializedName(), index);
    }

    public static void applyProfile(ItemStack stack, MultitoolMode mode, int requested) {
        int index = Math.max(0, requested);
        if (mode == MultitoolMode.BUILD) {
            index %= BuildingModes.values().length;
            ((GadgetBuilding) OurItems.BUILDING_GADGET_ITEM.get()).setMode(stack, index);
        } else if (mode == MultitoolMode.EXCHANGING) {
            index %= ExchangingModes.values().length;
            ((GadgetExchanger) OurItems.EXCHANGING_GADGET_ITEM.get()).setMode(stack, index);
        } else if (mode == MultitoolMode.COPY_PASTE || mode == MultitoolMode.CUT_PASTE) {
            index %= GadgetCopyPaste.ToolMode.values().length;
            ((GadgetCopyPaste) OurItems.COPY_PASTE_GADGET_ITEM.get()).setMode(stack, index);
        }
        stack.getOrCreateTag().putInt(PROFILE + mode.serializedName(), index);
    }

    public static void selectTool(ItemStack stack, MultitoolMode selected) {
        MultitoolMode previous = getActiveMode(stack);
        saveCurrentProfile(stack, previous);
        setActiveMode(stack, selected);
        if (selected != MultitoolMode.DESTRUCTION) applyProfile(stack, selected, getProfile(stack, selected));
    }
}
