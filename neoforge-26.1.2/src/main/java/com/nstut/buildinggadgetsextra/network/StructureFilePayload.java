package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StructureFilePayload(boolean load, String name) implements CustomPacketPayload {
    public static final Type<StructureFilePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "structure_file"));
    public static final StreamCodec<ByteBuf, StructureFilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StructureFilePayload::load,
            ByteBufCodecs.STRING_UTF8, StructureFilePayload::name,
            StructureFilePayload::new);

    @Override
    public Type<StructureFilePayload> type() {
        return TYPE;
    }
}
