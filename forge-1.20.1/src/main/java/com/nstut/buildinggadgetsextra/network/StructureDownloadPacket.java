package com.nstut.buildinggadgetsextra.network;
import com.nstut.buildinggadgetsextra.client.ClientStructureFiles;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public record StructureDownloadPacket(UUID id,String name,int index,int total,byte[] data){
 public static void encode(StructureDownloadPacket p,FriendlyByteBuf b){b.writeUUID(p.id);b.writeUtf(p.name,128);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeByteArray(p.data);}
 public static StructureDownloadPacket decode(FriendlyByteBuf b){return new StructureDownloadPacket(b.readUUID(),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));}
 public static void handle(StructureDownloadPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientStructureFiles.receive(p)));c.setPacketHandled(true);}
}
