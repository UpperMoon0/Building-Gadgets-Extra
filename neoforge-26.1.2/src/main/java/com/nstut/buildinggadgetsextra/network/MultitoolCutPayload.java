package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MultitoolCutPayload() implements CustomPacketPayload {
    public static final MultitoolCutPayload INSTANCE = new MultitoolCutPayload();
    public static final Type<MultitoolCutPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BuildingGadgetsExtra.MODID, "multitool_cut"));
    public static final StreamCodec<ByteBuf, MultitoolCutPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<MultitoolCutPayload> type() {
        return TYPE;
    }
}

