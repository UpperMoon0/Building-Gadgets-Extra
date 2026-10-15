package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CopyDataAssemblyTest {
    private static byte[] chunk(byte value) {
        byte[] data = new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE];
        java.util.Arrays.fill(data, value); return data;
    }
    private static byte[] joined(byte value, byte tail) {
        byte[] data = java.util.Arrays.copyOf(chunk(value), ExtraConstants.STRUCTURE_CHUNK_SIZE + 1);
        data[data.length-1] = tail; return data;
    }
    @Test void preservesIdentityAndOnlyCommitsCompleteData() {
        CopyDataAssembly assembly = new CopyDataAssembly();
        UUID transfer = UUID.randomUUID(), gadget = UUID.randomUUID(), copy = UUID.randomUUID();
        assertNull(assembly.accept(transfer, gadget, copy, 0, 2, chunk((byte)1)));
        assertArrayEquals(joined((byte)1, (byte)3), assembly.accept(transfer, gadget, copy, 1, 2, new byte[]{3}));
        assertNull(assembly.accept(transfer, gadget, copy, 1, 2, new byte[]{3}));
    }
    @Test void rejectsInconsistentIdentityAndDuplicateChunks() {
        CopyDataAssembly assembly = new CopyDataAssembly();
        UUID transfer = UUID.randomUUID(), gadget = UUID.randomUUID(), copy = UUID.randomUUID();
        assertNull(assembly.accept(transfer, gadget, copy, 0, 2, chunk((byte)1)));
        assertNull(assembly.accept(transfer, gadget, UUID.randomUUID(), 1, 2, new byte[]{2}));
        assertNull(assembly.accept(transfer, gadget, copy, 1, 2, new byte[]{2}));
        assertNull(assembly.accept(transfer, gadget, copy, 0, 2, chunk((byte)1)));
        assertNull(assembly.accept(transfer, gadget, copy, 0, 2, chunk((byte)1)));
        assertNull(assembly.accept(transfer, gadget, copy, 1, 2, new byte[]{2}));
    }
    @Test void supersedesOldSnapshotAndClearsOnDisconnect() {
        CopyDataAssembly assembly = new CopyDataAssembly();
        UUID old = UUID.randomUUID(), next = UUID.randomUUID(), gadget = UUID.randomUUID(), copy = UUID.randomUUID();
        assertNull(assembly.accept(old, gadget, copy, 0, 2, chunk((byte)1)));
        assertNull(assembly.accept(next, gadget, copy, 0, 2, chunk((byte)2)));
        assertNull(assembly.accept(old, gadget, copy, 1, 2, new byte[]{1}));
        assembly.clear();
        assertNull(assembly.accept(next, gadget, copy, 1, 2, new byte[]{2}));
    }
    @Test void boundsPendingTransferCountAndIndividualChunks() {
        CopyDataAssembly assembly = new CopyDataAssembly();
        UUID first = UUID.randomUUID(), gadget = UUID.randomUUID(), copy = UUID.randomUUID();
        assembly.accept(first, gadget, copy, 0, 2, chunk((byte)1));
        for (int i = 0; i < 4; i++) assembly.accept(UUID.randomUUID(), UUID.randomUUID(), copy, 0, 2, chunk((byte)1));
        assertNull(assembly.accept(first, gadget, copy, 1, 2, new byte[]{1}));
        assertNull(assembly.accept(first, gadget, copy, 0, Integer.MAX_VALUE, new byte[]{1}));
        assertNull(assembly.accept(first, gadget, copy, 0, 1, new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE + 1]));
    }
    @Test void boundedEncodingFailsBeforeGrowingPastLimit() throws IOException {
        BoundedByteArrayOutputStream output = new BoundedByteArrayOutputStream(3);
        output.write(new byte[]{1, 2});
        output.write(3);
        assertThrows(IOException.class, () -> output.write(4));
        assertThrows(IOException.class, () -> output.write(new byte[]{5, 6}, 0, 2));
        assertArrayEquals(new byte[]{1, 2, 3}, output.toByteArray());
    }
}
