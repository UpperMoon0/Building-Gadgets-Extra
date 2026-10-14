package com.nstut.buildinggadgetsextra.network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;

public final class ModifierHandler {
    private ModifierHandler() {}
    public static boolean apply(ServerPlayer player, int profile, int[] values) {
        if (player == null) return false;
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return false;
        MultitoolMode mode = MultitoolState.getActiveMode(stack);
        if (mode.ordinal() != profile || (mode != MultitoolMode.BUILD && mode != MultitoolMode.EXCHANGING)) return false;
        final LiveModifiers settings;
        try { settings = LiveModifiers.from(values); } catch (IllegalArgumentException invalid) { return false; }
        MultitoolState.setLiveModifiers(stack, settings);
        player.containerMenu.broadcastChanges();
        return true;
    }
}
