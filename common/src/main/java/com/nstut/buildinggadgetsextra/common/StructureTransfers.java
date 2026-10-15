package com.nstut.buildinggadgetsextra.common;
import com.nstut.packetweave.api.TransferLimits;
import java.time.Duration;
/** Structure-specific policy; PacketWeave owns transport and resource accounting. */
public final class StructureTransfers {
    private StructureTransfers() {}
    public static TransferLimits limits() {
        return new TransferLimits(ExtraConstants.STRUCTURE_CHUNK_SIZE,ExtraConstants.MAX_STRUCTURE_FILE_BYTES,
                ExtraConstants.MAX_STRUCTURE_UPLOADS,ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER,
                ExtraConstants.MAX_STRUCTURE_UPLOAD_BYTES,Duration.ofSeconds(30));
    }
}
