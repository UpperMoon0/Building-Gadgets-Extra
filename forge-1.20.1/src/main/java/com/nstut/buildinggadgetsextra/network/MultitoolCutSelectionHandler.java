package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Cut coordinates select an area; only the explicit Cut action may create a buffer. */
public final class MultitoolCutSelectionHandler {
    private MultitoolCutSelectionHandler() {}

    public static void apply(Player player, BlockPos start, BlockPos end) {
        ItemStack stack = BaseGadget.getGadget(player);
        // Reject stale edits after a profile switch or after Cut has accepted this selection.
        if (!(stack.getItem() instanceof BuildersMultitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE
                || GadgetNBT.hasCopyUUID(stack)
                || ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(stack))) return;
        GadgetNBT.setCopyStartPos(stack, start);
        GadgetNBT.setCopyEndPos(stack, end);
        MultitoolState.syncCutPasteMode(stack);
        player.containerMenu.broadcastChanges();
    }
}
