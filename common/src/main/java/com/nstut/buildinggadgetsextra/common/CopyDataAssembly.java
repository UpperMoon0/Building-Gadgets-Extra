package com.nstut.buildinggadgetsextra.common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Clientbound template reassembly. Minecraft's main client thread owns each instance. */
public final class CopyDataAssembly {
    private static final int MAX_PENDING = 4;
    private final Map<UUID, Pending> pending = new LinkedHashMap<>();

    public byte[] accept(UUID transferId, UUID gadgetId, UUID copyId, int index, int total, byte[] data) {
        pruneExpired();
        if (transferId == null || gadgetId == null || copyId == null) return null;
        Pending transfer = pending.get(transferId);
        if (index == 0 && transfer == null) {
            // A newer snapshot supersedes an incomplete old snapshot for the same gadget.
            pending.values().removeIf(value -> value.gadgetId.equals(gadgetId));
            if (pending.size() >= MAX_PENDING) {
                Iterator<UUID> oldest = pending.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            try {
                transfer = new Pending(gadgetId, copyId, total);
            } catch (IllegalArgumentException invalid) {
                return null;
            }
            pending.put(transferId, transfer);
        }
        if (transfer == null) return null; // Never resurrect an abandoned transfer from a tail chunk.
        if (!transfer.gadgetId.equals(gadgetId) || !transfer.copyId.equals(copyId) || transfer.total != total
                || !transfer.chunks.accept(index, data)) {
            pending.remove(transferId);
            return null;
        }
        if (!transfer.chunks.isComplete()) return null;
        pending.remove(transferId);
        return transfer.chunks.join();
    }

    public void pruneExpired() { pending.values().removeIf(value -> value.chunks.isExpired()); }

    public void clear() { pending.clear(); }

    private static final class Pending {
        final UUID gadgetId;
        final UUID copyId;
        final int total;
        final ChunkAccumulator chunks;
        Pending(UUID gadgetId, UUID copyId, int total) {
            this.gadgetId = gadgetId;
            this.copyId = copyId;
            this.total = total;
            this.chunks = new ChunkAccumulator(total);
        }
    }
}
