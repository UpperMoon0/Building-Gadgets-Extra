package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.Paste;
import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.UploadTransferRegistry;
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
    private static final UploadTransferRegistry<TransferState> TRANSFERS =
            new UploadTransferRegistry<>(transfer -> transfer.chunks);

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

            TRANSFERS.pruneExpired();
            String prefix = player.getUUID() + ":";
            String key = prefix + packet.id;
            TransferState transfer = TRANSFERS.get(key);
            if (transfer == null) {
                transfer = TransferState.capture(player, packet.total);
                if (transfer == null) return;
                if (!TRANSFERS.put(key, transfer)) return;
            }

            if (!transfer.matches(player)) {
                TRANSFERS.remove(key);
                return;
            }

            try {
                if (!TRANSFERS.accept(key, packet.index, packet.data)) {
                    TRANSFERS.remove(key);
                    return;
                }
                if (transfer.chunks.isComplete()) {
                    byte[] bytes = transfer.chunks.join();
                    TRANSFERS.remove(key);
                    if (transfer.matches(player)) {
                        NativeStructureBridge.importStructure(player,
                                StructureFileName.normalize(packet.name), bytes);
                    }
                }
            } catch (IllegalArgumentException error) {
                TRANSFERS.remove(key);
            }
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TRANSFERS.pruneExpired();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRANSFERS.removePlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRANSFERS.clear();
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
