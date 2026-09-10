package com.nstut.buildinggadgetsextra.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Server-thread-only upload storage. Limits cover all players, including idle uploads. */
public final class UploadTransferRegistry<T> {
    private final Map<String, T> transfers = new HashMap<>();
    private final Function<T, ChunkAccumulator> chunks;

    public UploadTransferRegistry(Function<T, ChunkAccumulator> chunks) {
        this.chunks = chunks;
    }

    public T get(String key) { return transfers.get(key); }

    public boolean put(String key, T transfer) {
        pruneExpired();
        String prefix = key.substring(0, key.indexOf(':') + 1);
        if (transfers.containsKey(key)
                || transfers.size() >= ExtraConstants.MAX_STRUCTURE_UPLOADS
                || transfers.keySet().stream().filter(value -> value.startsWith(prefix)).count()
                    >= ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER
                || retainedBytes() + chunks.apply(transfer).receivedBytes()
                    > ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES) return false;
        transfers.put(key, transfer);
        return true;
    }

    public boolean accept(String key, int index, byte[] data) {
        T transfer = transfers.get(key);
        if (transfer == null) return false;
        ChunkAccumulator accumulator = chunks.apply(transfer);
        if (data == null || accumulator.isExpired()
                || retainedBytes() + data.length > ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES
                || !accumulator.accept(index, data)) {
            remove(key);
            return false;
        }
        return true;
    }

    public void remove(String key) { transfers.remove(key); }

    public void removePlayer(UUID playerId) {
        String prefix = playerId + ":";
        transfers.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void pruneExpired() {
        transfers.values().removeIf(transfer -> chunks.apply(transfer).isExpired());
    }

    public void clear() { transfers.clear(); }

    public int size() { return transfers.size(); }

    public long retainedBytes() {
        // Derive the total from retained buffers so every removal path releases its budget.
        long bytes = 0;
        for (T transfer : transfers.values()) bytes += chunks.apply(transfer).receivedBytes();
        return bytes;
    }
}
