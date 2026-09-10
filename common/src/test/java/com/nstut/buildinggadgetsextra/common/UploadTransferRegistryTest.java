package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class UploadTransferRegistryTest {
    private UploadTransferRegistry<ChunkAccumulator> registry() {
        return new UploadTransferRegistry<>(chunks -> chunks);
    }

    private String key(UUID player, int transfer) { return player + ":" + transfer; }

    @Test
    void globalAndPerPlayerCountsAreBounded() {
        UploadTransferRegistry<ChunkAccumulator> uploads = registry();
        UUID player = UUID.randomUUID();
        assertTrue(uploads.put(key(player, 0), new ChunkAccumulator(2)));
        assertTrue(uploads.put(key(player, 1), new ChunkAccumulator(2)));
        assertFalse(uploads.put(key(player, 2), new ChunkAccumulator(2)));
        for (int i = 2; i < ExtraConstants.MAX_STRUCTURE_UPLOADS; i++) {
            assertTrue(uploads.put(key(UUID.randomUUID(), 0), new ChunkAccumulator(2)));
        }
        assertFalse(uploads.put(key(UUID.randomUUID(), 0), new ChunkAccumulator(2)));
        uploads.removePlayer(player);
        assertEquals(ExtraConstants.MAX_STRUCTURE_UPLOADS - 2, uploads.size());
        assertTrue(uploads.put(key(player, 3), new ChunkAccumulator(2)));
    }

    @Test
    void byteCeilingRejectsBeforeRetainingAndCleanupReleasesBudget() {
        UploadTransferRegistry<ChunkAccumulator> uploads = registry();
        int chunksPerFile = ExtraConstants.MAX_STRUCTURE_FILE_BYTES / ExtraConstants.STRUCTURE_CHUNK_SIZE;
        for (int i = 0; i < ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES / ExtraConstants.MAX_STRUCTURE_FILE_BYTES; i++) {
            String key = key(UUID.randomUUID(), 0);
            assertTrue(uploads.put(key, new ChunkAccumulator(chunksPerFile)));
            for (int chunk = 0; chunk < chunksPerFile; chunk++) {
                assertTrue(uploads.accept(key, chunk, new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE]));
            }
        }
        assertEquals(ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES, uploads.retainedBytes());
        String overflow = key(UUID.randomUUID(), 0);
        assertTrue(uploads.put(overflow, new ChunkAccumulator(2)));
        assertFalse(uploads.accept(overflow, 0, new byte[1]));
        assertNull(uploads.get(overflow));
        assertEquals(ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES, uploads.retainedBytes());
        uploads.clear();
        assertEquals(0, uploads.size());
        assertEquals(0, uploads.retainedBytes());
        assertTrue(uploads.put(overflow, new ChunkAccumulator(2)));
        assertTrue(uploads.accept(overflow, 0, new byte[1]));
    }

    @Test
    void expiryWithoutNewPacketsAndLogoutReleaseOnlyAffectedUploads() {
        UploadTransferRegistry<ChunkAccumulator> uploads = registry();
        AtomicLong clock = new AtomicLong();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String old = key(first, 0);
        assertTrue(uploads.put(old, new ChunkAccumulator(2, clock::get)));
        assertTrue(uploads.accept(old, 0, new byte[10]));
        clock.set(20_000);
        String fresh = key(second, 0);
        assertTrue(uploads.put(fresh, new ChunkAccumulator(2, clock::get)));
        assertTrue(uploads.accept(fresh, 0, new byte[20]));
        clock.set(30_001);
        uploads.pruneExpired(); // Simulates a server tick with no further upload traffic.
        assertNull(uploads.get(old));
        assertNotNull(uploads.get(fresh));
        assertEquals(20, uploads.retainedBytes());
        uploads.removePlayer(first);
        assertNotNull(uploads.get(fresh));
        uploads.removePlayer(second);
        assertEquals(0, uploads.retainedBytes());
        assertEquals(0, uploads.size());
    }

    @Test
    void invalidChunksAndCompletedTransfersReleaseTheirBuffers() {
        UploadTransferRegistry<ChunkAccumulator> uploads = registry();
        String key = key(UUID.randomUUID(), 0);
        assertTrue(uploads.put(key, new ChunkAccumulator(2)));
        assertTrue(uploads.accept(key, 0, new byte[10]));
        assertFalse(uploads.accept(key, 0, new byte[10]));
        assertNull(uploads.get(key));
        assertEquals(0, uploads.retainedBytes());
        assertTrue(uploads.put(key, new ChunkAccumulator(1)));
        assertTrue(uploads.accept(key, 0, new byte[]{1, 2}));
        assertArrayEquals(new byte[]{1, 2}, uploads.get(key).join());
        uploads.remove(key);
        assertEquals(0, uploads.retainedBytes());
    }
}
