package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkAccumulatorTest {
    @Test
    void assemblesOutOfOrderChunksInIndexOrder() {
        ChunkAccumulator transfer = new ChunkAccumulator(3);
        assertTrue(transfer.accept(2, new byte[]{5}));
        assertTrue(transfer.accept(0, new byte[]{1, 2}));
        assertTrue(transfer.accept(1, new byte[]{3, 4}));
        assertTrue(transfer.isComplete());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, transfer.join());
    }

    @Test
    void rejectsDuplicateInvalidAndOversizedChunks() {
        ChunkAccumulator transfer = new ChunkAccumulator(2);
        assertFalse(transfer.accept(-1, new byte[0]));
        assertFalse(transfer.accept(2, new byte[0]));
        assertFalse(transfer.accept(0, null));
        assertFalse(transfer.accept(0, new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE + 1]));
        assertTrue(transfer.accept(0, new byte[]{1}));
        assertFalse(transfer.accept(0, new byte[]{2}));
        assertFalse(transfer.isComplete());
    }

    @Test
    void rejectsInvalidTransferSizesAndIncompleteJoin() {
        int maximumChunks = ExtraConstants.MAX_STRUCTURE_FILE_BYTES / ExtraConstants.STRUCTURE_CHUNK_SIZE;
        assertThrows(IllegalArgumentException.class, () -> new ChunkAccumulator(0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkAccumulator(maximumChunks + 1));
        assertThrows(IllegalStateException.class, () -> new ChunkAccumulator(2).join());
    }
}
