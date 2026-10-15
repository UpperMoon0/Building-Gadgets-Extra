package com.nstut.buildinggadgetsextra.common;

import com.nstut.packetweave.api.ContextualTransferRegistry;
import com.nstut.packetweave.api.TransferId;
import com.nstut.packetweave.api.TransferLimits;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Client snapshot policy. PacketWeave owns assembly, expiry and byte budgets. */
public final class CopyDataAssembly {
    private static final UUID CONNECTION = new UUID(0, 0);
    private static final int MAX_PENDING = 4;
    private final ContextualTransferRegistry<Pending> pending = new ContextualTransferRegistry<>(
            new TransferLimits(ExtraConstants.STRUCTURE_CHUNK_SIZE, ExtraConstants.MAX_STRUCTURE_FILE_BYTES,
                    MAX_PENDING, MAX_PENDING, 4L * ExtraConstants.MAX_STRUCTURE_FILE_BYTES, Duration.ofSeconds(30)),
            (id, operation) -> true);

    public byte[] accept(UUID transferId, UUID gadgetId, UUID copyId, int index, int total, byte[] data) {
        if (transferId == null || gadgetId == null || copyId == null) return null;
        TransferId key = new TransferId(ExtraConstants.MOD_ID, CONNECTION, transferId);
        Pending transfer = pending.context(key);
        if (index == 0 && transfer == null) {
            // Only a first chunk can supersede a snapshot. Late tails cannot resurrect it.
            for (Map.Entry<TransferId, Pending> entry : pending.contexts().entrySet()) {
                if (entry.getValue().gadgetId.equals(gadgetId)) pending.cancel(entry.getKey());
            }
            if (pending.activeSessions() >= MAX_PENDING) pending.cancel(pending.contexts().keySet().iterator().next());
            transfer = new Pending(gadgetId, copyId, total);
            if (!pending.beginBounded(key, total, ExtraConstants.MAX_STRUCTURE_FILE_BYTES, transfer)) return null;
        }
        if (transfer == null) return null;
        if (!transfer.gadgetId.equals(gadgetId) || !transfer.copyId.equals(copyId) || transfer.total != total) {
            pending.cancel(key);
            return null;
        }
        if (!pending.accept(key, index, data)) return null;
        return pending.complete(key);
    }
    public void pruneExpired() { pending.reapExpired(); }
    public void clear() { pending.clear(); }
    private static final class Pending {
        final UUID gadgetId, copyId;
        final int total;
        Pending(UUID gadgetId, UUID copyId, int total) {
            this.gadgetId = gadgetId; this.copyId = copyId; this.total = total;
        }
    }
}
