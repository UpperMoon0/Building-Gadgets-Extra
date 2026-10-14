package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Bounded replacement for BG2's single, size-limited compound-tag packet. */
public record CopyDataPayload(UUID transferId, UUID gadgetId, UUID copyId,
                              int index, int total, byte[] data) implements CustomPacketPayload {
    public static final Type<CopyDataPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "copy_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CopyDataPayload> STREAM_CODEC =
            StreamCodec.of(CopyDataPayload::write, CopyDataPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, CopyDataPayload value) {
        if (value.data.length > ExtraConstants.STRUCTURE_CHUNK_SIZE) {
            throw new IllegalArgumentException("Copy fragment exceeds chunk limit");
        }
        buffer.writeUUID(value.transferId);
        buffer.writeUUID(value.gadgetId);
        buffer.writeUUID(value.copyId);
        buffer.writeVarInt(value.index);
        buffer.writeVarInt(value.total);
        buffer.writeByteArray(value.data);
    }

    private static CopyDataPayload read(RegistryFriendlyByteBuf buffer) {
        return new CopyDataPayload(buffer.readUUID(), buffer.readUUID(), buffer.readUUID(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
