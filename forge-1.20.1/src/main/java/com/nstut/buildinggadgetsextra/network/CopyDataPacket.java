package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.client.ClientCopyData;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record CopyDataPacket(UUID transferId, UUID gadgetId, UUID copyId, int index, int total, byte[] data) {
    public static void encode(CopyDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.transferId); buffer.writeUUID(packet.gadgetId); buffer.writeUUID(packet.copyId);
        buffer.writeVarInt(packet.index); buffer.writeVarInt(packet.total); buffer.writeByteArray(packet.data);
    }
    public static CopyDataPacket decode(FriendlyByteBuf buffer) {
        return new CopyDataPacket(buffer.readUUID(), buffer.readUUID(), buffer.readUUID(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));
    }
    public static void handle(CopyDataPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT)
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCopyData.receive(packet)));
        context.setPacketHandled(true);
    }
}
