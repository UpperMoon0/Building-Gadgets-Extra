package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import com.nstut.buildinggadgetsextra.transform.MirrorTransforms;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.UUID;

public final class MirrorPayloadHandler {
    private MirrorPayloadHandler() {
    }

    public static void handle(MirrorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack gadget = BaseGadget.getGadget(player);
            if (!(gadget.getItem() instanceof GadgetCopyPaste)
                    && !(gadget.getItem() instanceof GadgetCutPaste)) {
                return;
            }

            if (!GadgetNBT.hasCopyUUID(gadget)) {
                player.displayClientMessage(
                        Component.translatable(ExtraConstants.NO_TEMPLATE), true);
                return;
            }

            UUID gadgetId = GadgetNBT.getUUID(gadget);
            boolean cut = gadget.getItem() instanceof GadgetCutPaste
                    || gadget.getItem() instanceof BuildersMultitool
                    && MultitoolState.getActiveMode(gadget) == MultitoolMode.CUT_PASTE;
            if (cut && ServerTickHandler.gadgetWorking(gadgetId)) {
                player.displayClientMessage(
                        Component.translatable(ExtraConstants.BUSY), true);
                return;
            }

            BG2Data data = BG2Data.get(player.server.overworld());
            ArrayList<StatePos> blocks = data.getCopyPasteList(gadgetId, false);
            if (blocks == null || blocks.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(ExtraConstants.NO_TEMPLATE), true);
                return;
            }

            ArrayList<TagPos> blockEntities = data.peekTEMap(gadgetId);
            ArrayList<StatePos> mirrored = payload.vertical()
                    ? MirrorTransforms.vertical(blocks, blockEntities)
                    : MirrorTransforms.horizontal(blocks, blockEntities, player.getDirection());

            data.addToCopyPaste(gadgetId, mirrored);
            if (blockEntities != null && !blockEntities.isEmpty()) {
                data.addToTEMap(gadgetId, blockEntities);
            }
            GadgetNBT.setCopyUUID(gadget);
            player.displayClientMessage(Component.translatable(payload.vertical()
                    ? ExtraConstants.MIRRORED_VERTICAL
                    : ExtraConstants.MIRRORED_HORIZONTAL), true);
        });
    }
}
