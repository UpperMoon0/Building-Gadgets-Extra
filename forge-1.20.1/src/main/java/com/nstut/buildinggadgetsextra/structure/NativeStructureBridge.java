package com.nstut.buildinggadgetsextra.structure;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.mixin.StructurePaletteInvoker;
import com.nstut.buildinggadgetsextra.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class NativeStructureBridge {
    private NativeStructureBridge(){}
    public static byte[] exportStructure(ServerPlayer player,String name){
        ItemStack gadget=gadget(player);if(gadget==null||unavailable(player,gadget,true))return null;UUID id=GadgetNBT.getUUID(gadget);BG2Data data=BG2Data.get(player.server.overworld());ArrayList<StatePos> source=data.getCopyPasteList(id,false);if(source==null||source.isEmpty()){message(player,ExtraConstants.NO_TEMPLATE);return null;}
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;for(StatePos entry:source){minX=Math.min(minX,entry.pos.getX());minY=Math.min(minY,entry.pos.getY());minZ=Math.min(minZ,entry.pos.getZ());maxX=Math.max(maxX,entry.pos.getX());maxY=Math.max(maxY,entry.pos.getY());maxZ=Math.max(maxZ,entry.pos.getZ());}
        Map<BlockPos,CompoundTag> tags=new HashMap<>();ArrayList<TagPos> sourceTags=data.peekTEMap(id);if(sourceTags!=null)for(TagPos tag:sourceTags)tags.put(tag.pos,tag.tag);List<StructureTemplate.StructureBlockInfo> blocks=new ArrayList<>(source.size());for(StatePos entry:source){CompoundTag tag=tags.get(entry.pos);if(tag!=null){tag=tag.copy();tag.remove("x");tag.remove("y");tag.remove("z");}blocks.add(new StructureTemplate.StructureBlockInfo(entry.pos.offset(-minX,-minY,-minZ),entry.state,tag));}
        StructureTemplate template=new StructureTemplate();StructureTemplateAccessor accessor=(StructureTemplateAccessor)(Object)template;accessor.buildingGadgetsExtra$getPalettes().add(StructurePaletteInvoker.buildingGadgetsExtra$create(blocks));accessor.buildingGadgetsExtra$setSize(new BlockPos(maxX-minX+1,maxY-minY+1,maxZ-minZ+1));template.setAuthor(player.getGameProfile().getName());try{ByteArrayOutputStream output=new ByteArrayOutputStream();NbtIo.writeCompressed(template.save(new CompoundTag()),output);byte[] bytes=output.toByteArray();if(bytes.length>ExtraConstants.MAX_STRUCTURE_FILE_BYTES){message(player,ExtraConstants.STRUCTURE_TOO_LARGE,name);return null;}return bytes;}catch(Exception error){message(player,ExtraConstants.STRUCTURE_SAVE_FAILED,name);return null;}
    }
    public static void importStructure(ServerPlayer player,String name,byte[] bytes){
        ItemStack gadget=gadget(player);if(gadget==null||unavailable(player,gadget,false))return;final StructureTemplate template;try{DataInputStream input=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)));template=player.server.getStructureManager().readStructure(NbtIo.read(input,new NbtAccounter(512L*1024L*1024L)));}catch(Exception error){message(player,ExtraConstants.STRUCTURE_LOAD_FAILED,name);return;}StructureTemplateAccessor accessor=(StructureTemplateAccessor)(Object)template;if(accessor.buildingGadgetsExtra$getPalettes().isEmpty()||template.getSize().getX()<=0||template.getSize().getY()<=0||template.getSize().getZ()<=0){message(player,ExtraConstants.STRUCTURE_LOAD_FAILED,name);return;}long volume=(long)template.getSize().getX()*template.getSize().getY()*template.getSize().getZ();if(volume>ExtraConstants.MAX_STRUCTURE_BLOCKS){message(player,ExtraConstants.STRUCTURE_TOO_LARGE,name);return;}
        Map<BlockPos,StructureTemplate.StructureBlockInfo> nativeBlocks=new HashMap<>();for(StructureTemplate.StructureBlockInfo info:accessor.buildingGadgetsExtra$getPalettes().get(0).blocks())nativeBlocks.put(info.pos(),info);ArrayList<StatePos> blocks=new ArrayList<>();ArrayList<TagPos> tags=new ArrayList<>();BlockPos max=new BlockPos(template.getSize().getX()-1,template.getSize().getY()-1,template.getSize().getZ()-1);for(BlockPos mutable:BlockPos.betweenClosed(BlockPos.ZERO,max)){BlockPos pos=mutable.immutable();StructureTemplate.StructureBlockInfo info=nativeBlocks.get(pos);blocks.add(new StatePos(info==null||info.state().is(Blocks.STRUCTURE_VOID)?Blocks.AIR.defaultBlockState():info.state(),pos));if(info!=null&&info.nbt()!=null)tags.add(new TagPos(info.nbt().copy(),pos));}UUID id=GadgetNBT.getUUID(gadget);BG2Data data=BG2Data.get(player.server.overworld());data.addToCopyPaste(id,blocks);data.addToTEMap(id,tags);GadgetNBT.setCopyUUID(gadget);message(player,ExtraConstants.STRUCTURE_LOADED,name);
    }
    private static ItemStack gadget(ServerPlayer player){ItemStack stack=BaseGadget.getGadget(player);return stack.getItem() instanceof GadgetCopyPaste||stack.getItem() instanceof GadgetCutPaste?stack:null;}
    private static boolean unavailable(ServerPlayer player,ItemStack gadget,boolean required){if(required&&!GadgetNBT.hasCopyUUID(gadget)){message(player,ExtraConstants.NO_TEMPLATE);return true;}if(gadget.getItem() instanceof GadgetCutPaste&&ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(gadget))){message(player,ExtraConstants.BUSY);return true;}return false;}
    private static void message(ServerPlayer player,String key,Object...args){player.displayClientMessage(Component.translatable(key,args),true);}
}
