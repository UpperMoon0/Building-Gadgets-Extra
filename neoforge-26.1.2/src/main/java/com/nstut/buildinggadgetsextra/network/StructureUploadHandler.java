package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public final class StructureUploadHandler {
    private static final Map<String, ChunkAccumulator> TRANSFERS = new HashMap<>();

    private StructureUploadHandler() {}

    public static void handle(StructureUploadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !StructureFileName.isValid(payload.name())) return;
            TRANSFERS.entrySet().removeIf(entry -> entry.getValue().isExpired());
            String prefix = player.getUUID() + ":";
            String key = prefix + payload.transferId();
            if (!TRANSFERS.containsKey(key)
                    && TRANSFERS.keySet().stream().filter(value -> value.startsWith(prefix)).count() >= 4) return;
            try {
                ChunkAccumulator transfer = TRANSFERS.computeIfAbsent(key,
                        ignored -> new ChunkAccumulator(payload.total()));
                if (!transfer.accept(payload.index(), payload.data())) {
                    TRANSFERS.remove(key);
                    return;
                }
                if (transfer.isComplete()) {
                    TRANSFERS.remove(key);
                    NativeStructureBridge.importStructure(player,
                            StructureFileName.normalize(payload.name()), transfer.join());
                }
            } catch (IllegalArgumentException error) {
                TRANSFERS.remove(key);
            }
        });
    }
}
