package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MirrorPayload(boolean vertical) implements CustomPacketPayload {
    public static final Type<MirrorPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "mirror"));
    public static final StreamCodec<ByteBuf, MirrorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MirrorPayload::vertical,
            MirrorPayload::new);

    @Override
    public Type<MirrorPayload> type() {
        return TYPE;
    }
}
