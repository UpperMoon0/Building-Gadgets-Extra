package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MultitoolCutSelectionPayload(BlockPos start, BlockPos end) implements CustomPacketPayload {
    public static final Type<MultitoolCutSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BuildingGadgetsExtra.MODID, "multitool_cut_selection"));
    public static final StreamCodec<ByteBuf, MultitoolCutSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MultitoolCutSelectionPayload::start,
            BlockPos.STREAM_CODEC, MultitoolCutSelectionPayload::end,
            MultitoolCutSelectionPayload::new);

    @Override public Type<MultitoolCutSelectionPayload> type() { return TYPE; }
}
