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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public record StructureUploadPacket(UUID id, String name, int index, int total, byte[] data) {
    private static final ContextualTransferRegistry<TransferState> TRANSFERS =
            new ContextualTransferRegistry<>(StructureTransfers.limits(), (id, operation) -> true);

    public static void encode(StructureUploadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.id);
        buffer.writeUtf(packet.name, 128);
        buffer.writeVarInt(packet.index);
        buffer.writeVarInt(packet.total);
        buffer.writeByteArray(packet.data);
    }

    public static StructureUploadPacket decode(FriendlyByteBuf buffer) {
        return new StructureUploadPacket(buffer.readUUID(), buffer.readUtf(128), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));
    }

    public static void handle(StructureUploadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !StructureFileName.isValid(packet.name)) return;

            TRANSFERS.reapExpired();
            TransferId key = new TransferId(ExtraConstants.MOD_ID,player.getUUID(),packet.id);
            TransferState transfer = TRANSFERS.context(key);
            if (transfer == null) {
                transfer = TransferState.capture(player, packet.total, packet.name);
                if (transfer == null) return;
                if (!TRANSFERS.beginBounded(key, packet.total, ExtraConstants.MAX_STRUCTURE_FILE_BYTES, transfer)) return;
            }

            if (!transfer.matches(player) || transfer.totalChunks!=packet.total || !transfer.name.equals(packet.name)) {
                TRANSFERS.cancel(key);
                return;
            }

            try {
                if (!TRANSFERS.accept(key, packet.index, packet.data)) {
                    TRANSFERS.cancel(key);
                    return;
                }
                byte[] bytes = TRANSFERS.complete(key);
                if (bytes != null) {
                    if (transfer.matches(player)) {
                        NativeStructureBridge.importStructure(player,
                                StructureFileName.normalize(packet.name), bytes);
                    }
                }
            } catch (IllegalArgumentException error) {
                TRANSFERS.cancel(key);
            }
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
