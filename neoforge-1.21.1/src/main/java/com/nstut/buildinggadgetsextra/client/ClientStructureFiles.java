package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.network.StructureDownloadPayload;
import com.nstut.buildinggadgetsextra.network.StructureUploadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientStructureFiles {
    private static final Map<UUID,ChunkAccumulator> DOWNLOADS=new HashMap<>();
    private ClientStructureFiles(){}
    public static Path root(){return Minecraft.getInstance().gameDirectory.toPath().resolve("building_gadgets_extra").resolve("structures");}
    public static void receive(StructureDownloadPayload payload){try{ChunkAccumulator transfer=DOWNLOADS.computeIfAbsent(payload.transferId(),ignored->new ChunkAccumulator(payload.total()));if(!transfer.accept(payload.index(),payload.data())){DOWNLOADS.remove(payload.transferId());return;}if(transfer.isComplete()){DOWNLOADS.remove(payload.transferId());Path file=root().resolve(payload.name()+".nbt").normalize();if(!file.startsWith(root()))return;Files.createDirectories(file.getParent());Files.write(file,transfer.join());message(ExtraConstants.STRUCTURE_SAVED,payload.name());}}catch(Exception error){DOWNLOADS.remove(payload.transferId());message(ExtraConstants.STRUCTURE_SAVE_FAILED,payload.name());}}
    public static void upload(String rawName){String name=StructureFileName.normalize(rawName);try{Path file=root().resolve(name+".nbt").normalize();if(!file.startsWith(root())||!Files.exists(file)){message(ExtraConstants.STRUCTURE_NOT_FOUND,name);return;}byte[] bytes=Files.readAllBytes(file);if(bytes.length>ExtraConstants.MAX_STRUCTURE_FILE_BYTES)throw new IllegalArgumentException();UUID id=UUID.randomUUID();int total=Math.max(1,(bytes.length+ExtraConstants.STRUCTURE_CHUNK_SIZE-1)/ExtraConstants.STRUCTURE_CHUNK_SIZE);for(int i=0;i<total;i++){int start=i*ExtraConstants.STRUCTURE_CHUNK_SIZE,end=Math.min(bytes.length,start+ExtraConstants.STRUCTURE_CHUNK_SIZE);byte[] chunk=new byte[end-start];System.arraycopy(bytes,start,chunk,0,chunk.length);PacketDistributor.sendToServer(new StructureUploadPayload(id,name,i,total,chunk));}}catch(Exception error){message(ExtraConstants.STRUCTURE_LOAD_FAILED,name);}}
    private static void message(String key,Object...args){if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(Component.translatable(key,args),true);}
}
