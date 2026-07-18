package com.nstut.buildinggadgetsextra.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class CutSelectionPacket {
    public static void encode(CutSelectionPacket packet, PacketBuffer buffer) {
    }

    public static CutSelectionPacket decode(PacketBuffer buffer) {
        return new CutSelectionPacket();
    }

    public static void handle(CutSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CutSelectionPacketHandler.handle(context.getSender()));
        context.setPacketHandled(true);
    }
}
