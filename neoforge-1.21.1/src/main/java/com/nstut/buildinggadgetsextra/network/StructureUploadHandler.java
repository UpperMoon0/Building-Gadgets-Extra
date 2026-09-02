package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.Paste;
import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StructureUploadHandler {
    private static final Map<String, TransferState> TRANSFERS = new HashMap<>();

    private StructureUploadHandler() {}

    public static void handle(StructureUploadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !StructureFileName.isValid(payload.name())) return;

            TRANSFERS.entrySet().removeIf(entry -> entry.getValue().chunks.isExpired());
            String prefix = player.getUUID() + ":";
            String key = prefix + payload.transferId();
            TransferState transfer = TRANSFERS.get(key);
            if (transfer == null) {
                if (TRANSFERS.keySet().stream().filter(value -> value.startsWith(prefix)).count()
                        >= ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER) return;
                transfer = TransferState.capture(player, payload.total());
                if (transfer == null) return;
                TRANSFERS.put(key, transfer);
            }

            if (!transfer.matches(player)) {
                TRANSFERS.remove(key);
                return;
            }

            try {
                if (!transfer.chunks.accept(payload.index(), payload.data())) {
                    TRANSFERS.remove(key);
                    return;
                }
                if (transfer.chunks.isComplete()) {
                    byte[] bytes = transfer.chunks.join();
                    TRANSFERS.remove(key);
                    if (transfer.matches(player)) {
                        NativeStructureBridge.importStructure(player,
                                StructureFileName.normalize(payload.name()), bytes);
                    }
                }
            } catch (IllegalArgumentException error) {
                TRANSFERS.remove(key);
            }
        });
    }

    private static final class TransferState {
        private final ChunkAccumulator chunks;
        private final UUID gadgetId;
        private final MultitoolMode profile;

        private TransferState(ChunkAccumulator chunks, UUID gadgetId, MultitoolMode profile) {
            this.chunks = chunks;
            this.gadgetId = gadgetId;
            this.profile = profile;
        }

        private static TransferState capture(ServerPlayer player, int total) {
            ItemStack stack = BaseGadget.getGadget(player);
            if (stack.getItem() instanceof GadgetCutPaste || !(stack.getItem() instanceof GadgetCopyPaste)) return null;
            MultitoolMode profile = null;
            if (stack.getItem() instanceof BuildersMultitool) {
                profile = MultitoolState.getActiveMode(stack);
                if (profile != MultitoolMode.COPY_PASTE) return null;
            }
            if (!(GadgetNBT.getMode(stack) instanceof Paste)) return null;
            try {
                return new TransferState(new ChunkAccumulator(total), GadgetNBT.getUUID(stack), profile);
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
