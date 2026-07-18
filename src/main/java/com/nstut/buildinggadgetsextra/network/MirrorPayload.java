package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MirrorPayload(boolean vertical) implements CustomPacketPayload {
    public static final Type<MirrorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BuildingGadgetsExtra.MODID, "mirror"));
    public static final StreamCodec<ByteBuf, MirrorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MirrorPayload::vertical,
            MirrorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
