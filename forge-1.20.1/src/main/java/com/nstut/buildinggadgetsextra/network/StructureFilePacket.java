package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record StructureFilePacket(boolean load, String name, UUID requestId) {
    public static void encode(StructureFilePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.load);
        buffer.writeUtf(packet.name, StructureFileName.MAX_LENGTH);
        buffer.writeUUID(packet.requestId);
    }

    public static StructureFilePacket decode(FriendlyByteBuf buffer) {
        return new StructureFilePacket(buffer.readBoolean(),
                buffer.readUtf(StructureFileName.MAX_LENGTH), buffer.readUUID());
    }

    public static void handle(StructureFilePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            String name = StructureFileName.normalize(packet.name);
            if (!StructureFileName.isValid(name)) {
                player.displayClientMessage(Component.translatable(ExtraConstants.INVALID_STRUCTURE_NAME), true);
                return;
            }
            if (packet.load) return;
            byte[] bytes = NativeStructureBridge.exportStructure(player, name);
            if (bytes == null) return;
            int total = Math.max(1, (bytes.length + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1)
                    / ExtraConstants.STRUCTURE_CHUNK_SIZE);
            for (int i = 0; i < total; i++) {
                int start = i * ExtraConstants.STRUCTURE_CHUNK_SIZE;
                int end = Math.min(bytes.length, start + ExtraConstants.STRUCTURE_CHUNK_SIZE);
                byte[] chunk = java.util.Arrays.copyOfRange(bytes, start, end);
                ExtraNetwork.sendToPlayer(player,
                        new StructureDownloadPacket(packet.requestId, name, i, total, chunk));
            }
        });
        context.setPacketHandled(true);
    }
}
