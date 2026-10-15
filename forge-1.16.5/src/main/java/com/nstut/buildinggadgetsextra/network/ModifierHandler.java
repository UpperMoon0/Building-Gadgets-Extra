package com.nstut.buildinggadgetsextra.network;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;

public final class ModifierHandler {
    private ModifierHandler() {}
    public static boolean apply(ServerPlayerEntity player, int profile, int[] values) {
        if (player == null) return false;
        ItemStack stack = AbstractGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return false;
        MultitoolMode mode = MultitoolState.getActiveMode(stack);
        if (mode.ordinal() != profile || (mode != MultitoolMode.BUILD && mode != MultitoolMode.EXCHANGING)) return false;
        final LiveModifiers settings;
        try { settings = LiveModifiers.from(values); } catch (IllegalArgumentException invalid) { return false; }
        MultitoolState.setLiveModifiers(stack, settings);
        com.direwolf20.buildinggadgets.common.util.GadgetUtils.setAnchor(stack);
        player.containerMenu.broadcastChanges();
        return true;
    }
}
