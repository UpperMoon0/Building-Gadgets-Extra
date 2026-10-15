package com.nstut.buildinggadgetsextra.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MultitoolCutSelectionPacket(BlockPos start, BlockPos end) {
    public static void encode(MultitoolCutSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.start());
        buffer.writeBlockPos(packet.end());
    }
    public static MultitoolCutSelectionPacket decode(FriendlyByteBuf buffer) {
        return new MultitoolCutSelectionPacket(buffer.readBlockPos(), buffer.readBlockPos());
    }
    public static void handle(MultitoolCutSelectionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null)
                MultitoolCutSelectionHandler.apply(context.getSender(), packet.start(), packet.end());
        });
        context.setPacketHandled(true);
    }
}
