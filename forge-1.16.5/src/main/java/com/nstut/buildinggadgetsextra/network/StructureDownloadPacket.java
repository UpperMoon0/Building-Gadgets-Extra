package com.nstut.buildinggadgetsextra.network;
import com.nstut.buildinggadgetsextra.client.ClientStructureFiles;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.UUID;import java.util.function.Supplier;
public final class StructureDownloadPacket{public final UUID id;public final String name;public final int index,total;public final byte[] data;public StructureDownloadPacket(UUID id,String name,int index,int total,byte[] data){this.id=id;this.name=name;this.index=index;this.total=total;this.data=data;}public static void encode(StructureDownloadPacket p,PacketBuffer b){b.writeUUID(p.id);b.writeUtf(p.name,128);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeByteArray(p.data);}public static StructureDownloadPacket decode(PacketBuffer b){return new StructureDownloadPacket(b.readUUID(),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));}public static void handle(StructureDownloadPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientStructureFiles.receive(p)));c.setPacketHandled(true);}}
