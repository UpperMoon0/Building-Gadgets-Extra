package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record StructureUploadPayload(UUID transferId, String name, int index, int total, byte[] data)
        implements CustomPacketPayload {
    public static final Type<StructureUploadPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "structure_upload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureUploadPayload> STREAM_CODEC =
            StreamCodec.of(StructureUploadPayload::write, StructureUploadPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, StructureUploadPayload value) {
        buffer.writeUUID(value.transferId);
        buffer.writeUtf(value.name, 128);
        buffer.writeVarInt(value.index);
        buffer.writeVarInt(value.total);
        buffer.writeByteArray(value.data);
    }

    private static StructureUploadPayload read(RegistryFriendlyByteBuf buffer) {
        return new StructureUploadPayload(buffer.readUUID(), buffer.readUtf(128), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));
    }

    @Override
    public Type<StructureUploadPayload> type() {
        return TYPE;
    }
}
