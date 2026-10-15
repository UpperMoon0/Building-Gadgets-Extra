package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.Paste;
import com.nstut.packetweave.api.ContextualTransferRegistry;
import com.nstut.packetweave.api.TransferId;
import com.nstut.buildinggadgetsextra.common.StructureTransfers;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.UUID;

@EventBusSubscriber(modid = ExtraConstants.MOD_ID)
public final class StructureUploadHandler {
    private static final ContextualTransferRegistry<TransferState> TRANSFERS =
            new ContextualTransferRegistry<>(StructureTransfers.limits(), (id, operation) -> true);

    private StructureUploadHandler() {}

    public static void handle(StructureUploadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !StructureFileName.isValid(payload.name())) return;

            TRANSFERS.reapExpired();
            TransferId key = new TransferId(ExtraConstants.MOD_ID,player.getUUID(),payload.transferId());
            TransferState transfer = TRANSFERS.context(key);
            if (transfer == null) {
                transfer = TransferState.capture(player, payload.total(), payload.name());
                if (transfer == null) return;
                if (!TRANSFERS.beginBounded(key, payload.total(), ExtraConstants.MAX_STRUCTURE_FILE_BYTES, transfer)) return;
            }

            if (!transfer.matches(player) || transfer.totalChunks!=payload.total() || !transfer.name.equals(payload.name())) {
                TRANSFERS.cancel(key);
                return;
            }

            try {
                if (!TRANSFERS.accept(key, payload.index(), payload.data())) {
                    TRANSFERS.cancel(key);
                    return;
                }
                byte[] bytes = TRANSFERS.complete(key);
                if (bytes != null) {
                    if (transfer.matches(player)) {
                        NativeStructureBridge.importStructure(player,
                                StructureFileName.normalize(payload.name()), bytes);
                    }
                }
            } catch (IllegalArgumentException error) {
                TRANSFERS.cancel(key);
            }
        });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TRANSFERS.reapExpired();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRANSFERS.cancelOwner(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRANSFERS.clear();
    }

    private static final class TransferState {
        private final int totalChunks;
        private final String name;
        private final UUID gadgetId;
        private final MultitoolMode profile;

        private TransferState(int totalChunks, String name, UUID gadgetId, MultitoolMode profile) {
            this.totalChunks = totalChunks; this.name = name;
            this.gadgetId = gadgetId;
            this.profile = profile;
        }

        private static TransferState capture(ServerPlayer player, int total, String name) {
            ItemStack stack = BaseGadget.getGadget(player);
            if (stack.getItem() instanceof GadgetCutPaste || !(stack.getItem() instanceof GadgetCopyPaste)) return null;
            MultitoolMode profile = null;
            if (stack.getItem() instanceof BuildersMultitool) {
                profile = MultitoolState.getActiveMode(stack);
                if (profile != MultitoolMode.COPY_PASTE) return null;
            }
            if (!(GadgetNBT.getMode(stack) instanceof Paste)) return null;
            try {
                return new TransferState(total, name, GadgetNBT.getUUID(stack), profile);
            } catch (IllegalArgumentException error) {
                return null;
            }
        }

        private boolean matches(ServerPlayer player) {
            ItemStack stack = BaseGadget.getGadget(player);
            if (stack.getItem() instanceof GadgetCutPaste || !(stack.getItem() instanceof GadgetCopyPaste)) return false;
            if (!GadgetNBT.getUUID(stack).equals(gadgetId)) return false;
            if (!(GadgetNBT.getMode(stack) instanceof Paste)) return false;
            if (profile == null) return !(stack.getItem() instanceof BuildersMultitool);
            return stack.getItem() instanceof BuildersMultitool
                    && MultitoolState.getActiveMode(stack) == profile;
        }
    }
}
