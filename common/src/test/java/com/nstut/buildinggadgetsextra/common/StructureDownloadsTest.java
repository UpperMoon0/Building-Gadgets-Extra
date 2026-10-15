package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StructureDownloadsTest {
    private byte[] head() { return new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE]; }

    @Test void responsePinsNameAndChunkCountUntilCompletion() {
        StructureDownloads downloads=new StructureDownloads();
        UUID id=UUID.randomUUID();
        assertNull(downloads.accept(id,"chosen",0,2,head()));
        assertTrue(downloads.active(id));
        assertNull(downloads.accept(id,"different",1,2,new byte[]{1}));
        assertFalse(downloads.active(id));
        assertNull(downloads.accept(id,"chosen",0,2,head()));
        assertNull(downloads.accept(id,"chosen",1,3,head()));
        assertFalse(downloads.active(id));
    }

    @Test void completeResponseReleasesItsHeaderAndConnectionResetDropsPendingBytes() {
        StructureDownloads downloads=new StructureDownloads();
        UUID id=UUID.randomUUID();
        assertNull(downloads.accept(id,"structure",0,2,head()));
        byte[] bytes=downloads.accept(id,"structure",1,2,new byte[]{42});
        assertNotNull(bytes);
        assertEquals(ExtraConstants.STRUCTURE_CHUNK_SIZE+1,bytes.length);
        assertEquals(42,bytes[bytes.length-1]);
        assertFalse(downloads.active(id));
        assertNull(downloads.accept(id,"structure",0,2,head()));
        downloads.clear();
        assertFalse(downloads.active(id));
    }

    @Test void separateResponsesNeverShareBytesAndCancellationAllowsAnotherSave() {
        StructureDownloads downloads=new StructureDownloads();
        UUID first=UUID.randomUUID(), second=UUID.randomUUID(), third=UUID.randomUUID();
        assertNull(downloads.accept(first,"same-name",0,2,head()));
        assertNull(downloads.accept(second,"same-name",0,2,head()));
        assertNull(downloads.accept(third,"same-name",0,2,head()));
        assertFalse(downloads.active(third));
        downloads.cancel(first);
        assertNull(downloads.accept(third,"same-name",0,2,head()));
        byte[] secondBytes=downloads.accept(second,"same-name",1,2,new byte[]{2});
        byte[] thirdBytes=downloads.accept(third,"same-name",1,2,new byte[]{3});
        assertEquals(2,secondBytes[secondBytes.length-1]);
        assertEquals(3,thirdBytes[thirdBytes.length-1]);
    }
}
