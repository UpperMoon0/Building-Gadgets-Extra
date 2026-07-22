package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MultitoolSelectionPayload(int toolOrdinal, String gadgetMode) implements CustomPacketPayload {
    public static final Type<MultitoolSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BuildingGadgetsExtra.MODID, "multitool_selection"));
    public static final StreamCodec<ByteBuf, MultitoolSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MultitoolSelectionPayload::toolOrdinal,
            ByteBufCodecs.STRING_UTF8, MultitoolSelectionPayload::gadgetMode,
            MultitoolSelectionPayload::new);

    @Override
    public Type<MultitoolSelectionPayload> type() {
        return TYPE;
    }
}
