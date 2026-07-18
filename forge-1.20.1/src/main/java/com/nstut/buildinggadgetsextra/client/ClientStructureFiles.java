package com.nstut.buildinggadgetsextra.client;
import com.nstut.buildinggadgetsextra.common.*;
import com.nstut.buildinggadgetsextra.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
public final class ClientStructureFiles{
 private static final Map<UUID,ChunkAccumulator> DOWNLOADS=new HashMap<>();private ClientStructureFiles(){}
 public static Path root(){return Minecraft.getInstance().gameDirectory.toPath().resolve("building_gadgets_extra").resolve("structures");}
 public static void receive(StructureDownloadPacket p){try{ChunkAccumulator t=DOWNLOADS.computeIfAbsent(p.id(),k->new ChunkAccumulator(p.total()));if(!t.accept(p.index(),p.data())){DOWNLOADS.remove(p.id());return;}if(t.isComplete()){DOWNLOADS.remove(p.id());Path file=root().resolve(p.name()+".nbt").normalize();if(!file.startsWith(root()))return;Files.createDirectories(file.getParent());Files.write(file,t.join());message(ExtraConstants.STRUCTURE_SAVED,p.name());}}catch(Exception e){DOWNLOADS.remove(p.id());message(ExtraConstants.STRUCTURE_SAVE_FAILED,p.name());}}
 public static void upload(String raw){String name=StructureFileName.normalize(raw);try{Path file=root().resolve(name+".nbt").normalize();if(!file.startsWith(root())||!Files.exists(file)){message(ExtraConstants.STRUCTURE_NOT_FOUND,name);return;}byte[] bytes=Files.readAllBytes(file);if(bytes.length>ExtraConstants.MAX_STRUCTURE_FILE_BYTES)throw new IllegalArgumentException();UUID id=UUID.randomUUID();int total=Math.max(1,(bytes.length+ExtraConstants.STRUCTURE_CHUNK_SIZE-1)/ExtraConstants.STRUCTURE_CHUNK_SIZE);for(int i=0;i<total;i++){int start=i*ExtraConstants.STRUCTURE_CHUNK_SIZE,end=Math.min(bytes.length,start+ExtraConstants.STRUCTURE_CHUNK_SIZE);byte[] chunk=Arrays.copyOfRange(bytes,start,end);ExtraNetwork.sendToServer(new StructureUploadPacket(id,name,i,total,chunk));}}catch(Exception e){message(ExtraConstants.STRUCTURE_LOAD_FAILED,name);}}
 private static void message(String key,Object...args){if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(Component.translatable(key,args),true);}
}
