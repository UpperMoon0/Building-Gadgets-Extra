package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class StructureFilePayloadHandler {
    private StructureFilePayloadHandler() {}

    public static void handle(StructureFilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            String name = StructureFileName.normalize(payload.name());
            if (!StructureFileName.isValid(name)) {
                player.displayClientMessage(Component.translatable(ExtraConstants.INVALID_STRUCTURE_NAME), true);
                return;
            }
            if (payload.load()) return;
            byte[] bytes = NativeStructureBridge.exportStructure(player, name);
            if (bytes == null) return;
            try {
                com.nstut.packetweave.api.ChunkedTransfer.send(bytes, ExtraConstants.STRUCTURE_CHUNK_SIZE,
                        (index, total, chunk) -> PacketDistributor.sendToPlayer(player,
                                new StructureDownloadPayload(payload.requestId(), name, index, total, chunk)));
            } catch (java.io.IOException | IllegalArgumentException error) {
                player.displayClientMessage(Component.translatable(ExtraConstants.STRUCTURE_SAVE_FAILED, name), true);
            }
        });
    }
}
