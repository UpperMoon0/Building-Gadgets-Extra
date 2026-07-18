package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public final class StructureFilePayloadHandler {
    private StructureFilePayloadHandler() {}

    public static void handle(StructureFilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String name = StructureFileName.normalize(payload.name());
            if (!StructureFileName.isValid(name)) {
                player.sendOverlayMessage(Component.translatable(ExtraConstants.INVALID_STRUCTURE_NAME));
                return;
            }
            if (payload.load()) return;
            byte[] bytes = NativeStructureBridge.exportStructure(player, name);
            if (bytes == null) return;

            UUID transferId = UUID.randomUUID();
            int total = Math.max(1, (bytes.length + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1)
                    / ExtraConstants.STRUCTURE_CHUNK_SIZE);
            for (int index = 0; index < total; index++) {
                int start = index * ExtraConstants.STRUCTURE_CHUNK_SIZE;
                int end = Math.min(bytes.length, start + ExtraConstants.STRUCTURE_CHUNK_SIZE);
                byte[] chunk = new byte[end - start];
                System.arraycopy(bytes, start, chunk, 0, chunk.length);
                PacketDistributor.sendToPlayer(player,
                        new StructureDownloadPayload(transferId, name, index, total, chunk));
            }
        });
    }
}
