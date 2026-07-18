package com.nstut.buildinggadgetsextra.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MirrorPacket {
    private final boolean vertical;

    public MirrorPacket(boolean vertical) {
        this.vertical = vertical;
    }

    public static void encode(MirrorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.vertical);
    }

    public static MirrorPacket decode(FriendlyByteBuf buffer) {
        return new MirrorPacket(buffer.readBoolean());
    }

    public static void handle(MirrorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> MirrorPacketHandler.handle(packet.vertical, context.getSender()));
        context.setPacketHandled(true);
    }
}
