package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.StructureDownloadPayload;
import com.nstut.buildinggadgetsextra.network.StructureFilePayload;
import com.nstut.buildinggadgetsextra.network.StructureUploadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ClientStructureFiles {
    private static final Map<UUID,ChunkAccumulator> DOWNLOADS=new HashMap<>();
    private static final Map<String,Deque<Path>> SAVE_DESTINATIONS=new HashMap<>();
    private ClientStructureFiles(){}

    public static Path root(){return Minecraft.getInstance().gameDirectory.toPath().resolve("building_gadgets_extra").resolve("structures");}

    public static void chooseSave(){dialogThread(()->{try{Files.createDirectories(root());try(MemoryStack stack=MemoryStack.stackPush()){PointerBuffer filters=filters(stack);String result=TinyFileDialogs.tinyfd_saveFileDialog(Component.translatable(ExtraConstants.DIALOG_SAVE_STRUCTURE).getString(),root().resolve("structure.nbt").toString(),filters,Component.translatable(ExtraConstants.DIALOG_NBT_FILES).getString());if(result==null)return;Path path=Path.of(result);if(!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbt"))path=Path.of(result+".nbt");String name=transferName(path);synchronized(SAVE_DESTINATIONS){SAVE_DESTINATIONS.computeIfAbsent(name,key->new ArrayDeque<>()).add(path);}Minecraft.getInstance().execute(()->PacketDistributor.sendToServer(new StructureFilePayload(false,name)));}}catch(Exception e){message(ExtraConstants.STRUCTURE_SAVE_FAILED,"structure");}});}

    public static void chooseLoad(){dialogThread(()->{try{Files.createDirectories(root());try(MemoryStack stack=MemoryStack.stackPush()){String result=TinyFileDialogs.tinyfd_openFileDialog(Component.translatable(ExtraConstants.DIALOG_OPEN_STRUCTURE).getString(),root().toString(),filters(stack),Component.translatable(ExtraConstants.DIALOG_NBT_FILES).getString(),false);if(result!=null)upload(Path.of(result));}}catch(Exception e){message(ExtraConstants.STRUCTURE_LOAD_FAILED,"structure");}});}

    public static void receive(StructureDownloadPayload payload){try{ChunkAccumulator transfer=DOWNLOADS.computeIfAbsent(payload.transferId(),ignored->new ChunkAccumulator(payload.total()));if(!transfer.accept(payload.index(),payload.data())){DOWNLOADS.remove(payload.transferId());return;}if(transfer.isComplete()){DOWNLOADS.remove(payload.transferId());Path file; synchronized(SAVE_DESTINATIONS){Deque<Path> paths=SAVE_DESTINATIONS.get(payload.name());file=paths==null?null:paths.poll();if(paths!=null&&paths.isEmpty())SAVE_DESTINATIONS.remove(payload.name());}if(file==null)file=root().resolve(payload.name()+".nbt");Files.createDirectories(file.toAbsolutePath().getParent());Files.write(file,transfer.join());message(ExtraConstants.STRUCTURE_SAVED,file.getFileName().toString());}}catch(Exception e){DOWNLOADS.remove(payload.transferId());message(ExtraConstants.STRUCTURE_SAVE_FAILED,payload.name());}}

    private static void upload(Path file){String name=transferName(file);try{long size=Files.size(file);if(size>ExtraConstants.MAX_STRUCTURE_FILE_BYTES)throw new IllegalArgumentException();byte[] bytes=Files.readAllBytes(file);UUID id=UUID.randomUUID();int total=Math.max(1,(bytes.length+ExtraConstants.STRUCTURE_CHUNK_SIZE-1)/ExtraConstants.STRUCTURE_CHUNK_SIZE);Minecraft.getInstance().execute(()->{for(int i=0;i<total;i++){int start=i*ExtraConstants.STRUCTURE_CHUNK_SIZE,end=Math.min(bytes.length,start+ExtraConstants.STRUCTURE_CHUNK_SIZE);PacketDistributor.sendToServer(new StructureUploadPayload(id,name,i,total,Arrays.copyOfRange(bytes,start,end)));}});}catch(Exception e){message(ExtraConstants.STRUCTURE_LOAD_FAILED,file.getFileName().toString());}}

    private static PointerBuffer filters(MemoryStack stack){PointerBuffer filters=stack.mallocPointer(1);filters.put(stack.UTF8("*.nbt"));return filters.flip();}
    private static String transferName(Path path){String name=path.getFileName().toString();if(name.toLowerCase(Locale.ROOT).endsWith(".nbt"))name=name.substring(0,name.length()-4);name=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]","_");return name.isEmpty()?"structure":name;}
    private static void dialogThread(Runnable action){Thread thread=new Thread(action,"Building Gadgets Extra File Dialog");thread.setDaemon(true);thread.start();}
    private static void message(String key,Object...args){Minecraft.getInstance().execute(()->{if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(Component.translatable(key,args),true);});}
}
