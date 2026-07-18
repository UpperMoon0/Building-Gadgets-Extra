package com.nstut.buildinggadgetsextra.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ChunkAccumulator {
    private final long createdAt = System.currentTimeMillis();
    private final byte[][] chunks;
    private int receivedChunks;
    private int receivedBytes;

    public ChunkAccumulator(int totalChunks) {
        int maximumChunks = (ExtraConstants.MAX_STRUCTURE_FILE_BYTES
                + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1) / ExtraConstants.STRUCTURE_CHUNK_SIZE;
        if (totalChunks < 1 || totalChunks > maximumChunks) {
            throw new IllegalArgumentException("Invalid chunk count");
        }
        this.chunks = new byte[totalChunks][];
    }

    public boolean accept(int index, byte[] data) {
        if (index < 0 || index >= chunks.length || data == null
                || data.length > ExtraConstants.STRUCTURE_CHUNK_SIZE || chunks[index] != null
                || receivedBytes + data.length > ExtraConstants.MAX_STRUCTURE_FILE_BYTES) {
            return false;
        }
        chunks[index] = data;
        receivedChunks++;
        receivedBytes += data.length;
        return true;
    }

    public boolean isComplete() {
        return receivedChunks == chunks.length;
    }

    public byte[] join() {
        if (!isComplete()) throw new IllegalStateException("Transfer is incomplete");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(receivedBytes);
            for (byte[] chunk : chunks) output.write(chunk);
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 30_000L;
    }
}
