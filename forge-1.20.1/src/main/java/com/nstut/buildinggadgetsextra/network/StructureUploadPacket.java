package com.nstut.buildinggadgetsextra.network;
import com.nstut.buildinggadgetsextra.common.*;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;
public record StructureUploadPacket(UUID id,String name,int index,int total,byte[] data){
 private static final Map<String,ChunkAccumulator> TRANSFERS=new HashMap<>();
 public static void encode(StructureUploadPacket p,FriendlyByteBuf b){b.writeUUID(p.id);b.writeUtf(p.name,128);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeByteArray(p.data);}
 public static StructureUploadPacket decode(FriendlyByteBuf b){return new StructureUploadPacket(b.readUUID(),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readByteArray(ExtraConstants.STRUCTURE_CHUNK_SIZE));}
 public static void handle(StructureUploadPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{ServerPlayer player=c.getSender();if(player==null||!StructureFileName.isValid(p.name))return;TRANSFERS.entrySet().removeIf(e->e.getValue().isExpired());String prefix=player.getUUID()+":";String key=prefix+p.id;if(!TRANSFERS.containsKey(key)&&TRANSFERS.keySet().stream().filter(v->v.startsWith(prefix)).count()>=4)return;try{ChunkAccumulator transfer=TRANSFERS.computeIfAbsent(key,k->new ChunkAccumulator(p.total));if(!transfer.accept(p.index,p.data)){TRANSFERS.remove(key);return;}if(transfer.isComplete()){TRANSFERS.remove(key);NativeStructureBridge.importStructure(player,StructureFileName.normalize(p.name),transfer.join());}}catch(IllegalArgumentException e){TRANSFERS.remove(key);}});c.setPacketHandled(true);}
}
