package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class StructureUploadPacket {
    private static final Map<String, TransferState> TRANSFERS = new HashMap<>();

    final UUID id;
    final String name;
    final int index;
    final int total;
    final byte[] data;

    public StructureUploadPacket(UUID id, String name, int index, int total, byte[] data) {
        this.id = id;
        this.name = name;
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(StructureUploadPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.id);
        buffer.writeUtf(packet.name, 128);
        buffer.writeVarInt(packet.index);
        buffer.writeVarInt(packet.total);
        buffer.writeByteArray(packet.data);
    }

    public static StructureUploadPacket decode(PacketBuffer buffer) {
        return new StructureUploadPacket(buffer.readUUID(), buffer.readUtf(128), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));
    }

    public static void handle(StructureUploadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null || !StructureFileName.isValid(packet.name)) return;

            TRANSFERS.entrySet().removeIf(entry -> entry.getValue().chunks.isExpired());
            String prefix = player.getUUID() + ":";
            String key = prefix + packet.id;
            TransferState transfer = TRANSFERS.get(key);
            if (transfer == null) {
                if (TRANSFERS.keySet().stream().filter(value -> value.startsWith(prefix)).count()
                        >= ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER) return;
                transfer = TransferState.capture(player, packet.total);
                if (transfer == null) return;
                TRANSFERS.put(key, transfer);
            }

            if (!transfer.matches(player)) {
                TRANSFERS.remove(key);
                return;
            }

            try {
                if (!transfer.chunks.accept(packet.index, packet.data)) {
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

    private static final class TransferState {
        private final ChunkAccumulator chunks;
        private final UUID gadgetId;
        private final boolean multitool;

        private TransferState(ChunkAccumulator chunks, UUID gadgetId, boolean multitool) {
            this.chunks = chunks;
            this.gadgetId = gadgetId;
            this.multitool = multitool;
        }

        private static TransferState capture(ServerPlayerEntity player, int total) {
            ItemStack stack = AbstractGadget.getGadget(player);
            if (!(stack.getItem() instanceof GadgetCopyPaste)) return null;
            boolean isMultitool = stack.getItem() instanceof BuildersMultitool;
            if (isMultitool && MultitoolState.getActiveMode(stack) != MultitoolMode.COPY_PASTE) return null;
            if (GadgetCopyPaste.getToolMode(stack) != GadgetCopyPaste.ToolMode.PASTE) return null;
            try {
                return new TransferState(new ChunkAccumulator(total),
                        ((AbstractGadget) stack.getItem()).getUUID(stack), isMultitool);
            } catch (IllegalArgumentException error) {
                return null;
            }
        }

        private boolean matches(ServerPlayerEntity player) {
            ItemStack stack = AbstractGadget.getGadget(player);
            if (!(stack.getItem() instanceof GadgetCopyPaste)) return false;
            if (((AbstractGadget) stack.getItem()).getUUID(stack).equals(gadgetId) == false) return false;
            if (GadgetCopyPaste.getToolMode(stack) != GadgetCopyPaste.ToolMode.PASTE) return false;
            if (!multitool) return !(stack.getItem() instanceof BuildersMultitool);
            return stack.getItem() instanceof BuildersMultitool
                    && MultitoolState.getActiveMode(stack) == MultitoolMode.COPY_PASTE;
        }
    }
}
