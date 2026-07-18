package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.structure.NativeStructureBridge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public final class StructureFilePacket {
    private final boolean load;private final String name;
    public StructureFilePacket(boolean load,String name){this.load=load;this.name=name;}
    public static void encode(StructureFilePacket packet,PacketBuffer buffer){buffer.writeBoolean(packet.load);buffer.writeUtf(packet.name,StructureFileName.MAX_LENGTH);}
    public static StructureFilePacket decode(PacketBuffer buffer){return new StructureFilePacket(buffer.readBoolean(),buffer.readUtf(StructureFileName.MAX_LENGTH));}
    public static void handle(StructureFilePacket packet,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context context=supplier.get();context.enqueueWork(()->{ServerPlayerEntity player=context.getSender();if(player==null)return;String name=StructureFileName.normalize(packet.name);if(!StructureFileName.isValid(name)){player.displayClientMessage(new TranslationTextComponent(ExtraConstants.INVALID_STRUCTURE_NAME),true);return;}if(packet.load)return;byte[] bytes=NativeStructureBridge.exportStructure(player,name);if(bytes==null)return;java.util.UUID id=java.util.UUID.randomUUID();int total=Math.max(1,(bytes.length+ExtraConstants.STRUCTURE_CHUNK_SIZE-1)/ExtraConstants.STRUCTURE_CHUNK_SIZE);for(int i=0;i<total;i++){int start=i*ExtraConstants.STRUCTURE_CHUNK_SIZE,end=Math.min(bytes.length,start+ExtraConstants.STRUCTURE_CHUNK_SIZE);ExtraNetwork.sendToPlayer(player,new StructureDownloadPacket(id,name,i,total,java.util.Arrays.copyOfRange(bytes,start,end)));}});context.setPacketHandled(true);}
}
