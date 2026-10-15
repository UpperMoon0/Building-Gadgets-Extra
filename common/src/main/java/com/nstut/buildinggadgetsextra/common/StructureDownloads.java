package com.nstut.buildinggadgetsextra.common;

import com.nstut.packetweave.api.ContextualTransferRegistry;
import com.nstut.packetweave.api.TransferId;
import java.util.UUID;

/** Pins response metadata; callers authorize the request ID before admitting any bytes. */
public final class StructureDownloads {
    private static final UUID CONNECTION = new UUID(0, 0);
    private final ContextualTransferRegistry<Header> downloads = new ContextualTransferRegistry<>(
            StructureTransfers.limits(), (id, operation) -> true);
    private TransferId key(UUID id) { return new TransferId(ExtraConstants.MOD_ID, CONNECTION, id); }
    public byte[] accept(UUID id, String name, int index, int total, byte[] data) {
        TransferId key = key(id);
        Header header = downloads.context(key);
        if (header == null) {
            header = new Header(name, total);
            if (!downloads.beginBounded(key, total, ExtraConstants.MAX_STRUCTURE_FILE_BYTES, header)) return null;
        }
        if (!header.name.equals(name) || header.total != total) { downloads.cancel(key); return null; }
        if (!downloads.accept(key, index, data)) return null;
        return downloads.complete(key);
    }
    public boolean active(UUID id) { return downloads.context(key(id)) != null; }
    public void cancel(UUID id) { downloads.cancel(key(id)); }
    public void reapExpired() { downloads.reapExpired(); }
    public void clear() { downloads.clear(); }
    private static final class Header {
        final String name; final int total;
        Header(String name, int total) { this.name = name; this.total = total; }
    }
}
