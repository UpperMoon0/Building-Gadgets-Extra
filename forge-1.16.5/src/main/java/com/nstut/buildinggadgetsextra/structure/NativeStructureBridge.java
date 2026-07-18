package com.nstut.buildinggadgetsextra.structure;
import com.direwolf20.buildinggadgets.common.capability.CapabilityTemplate;
import com.direwolf20.buildinggadgets.common.items.*;
import com.direwolf20.buildinggadgets.common.tainted.building.*;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.*;
import com.direwolf20.buildinggadgets.common.tainted.template.*;
import com.google.common.collect.ImmutableMap;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.mixin.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.fml.network.PacketDistributor;
import java.io.*;
import java.util.*;

public final class NativeStructureBridge{
 private NativeStructureBridge(){}
 public static byte[] exportStructure(ServerPlayerEntity player,String name){Context c=context(player);if(c==null)return null;com.direwolf20.buildinggadgets.common.tainted.template.Template source=c.provider.getTemplateForKey(c.key);Map<BlockPos,BlockData> map=((TemplateAccessor)(Object)source).buildingGadgetsExtra$getMap();if(map.isEmpty()){message(player,ExtraConstants.NO_TEMPLATE);return null;}int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;for(BlockPos p:map.keySet()){minX=Math.min(minX,p.getX());minY=Math.min(minY,p.getY());minZ=Math.min(minZ,p.getZ());maxX=Math.max(maxX,p.getX());maxY=Math.max(maxY,p.getY());maxZ=Math.max(maxZ,p.getZ());}List<Template.BlockInfo> blocks=new ArrayList<>(map.size());for(Map.Entry<BlockPos,BlockData> e:map.entrySet()){CompoundNBT nbt=null;if(e.getValue().getTileData() instanceof NBTTileEntityData){nbt=((NBTTileEntityData)e.getValue().getTileData()).getNBT().copy();nbt.remove("x");nbt.remove("y");nbt.remove("z");}blocks.add(new Template.BlockInfo(e.getKey().offset(-minX,-minY,-minZ),e.getValue().getState(),nbt));}Template template=new Template();VanillaTemplateAccessor a=(VanillaTemplateAccessor)(Object)template;a.buildingGadgetsExtra$getPalettes().add(VanillaPaletteAccessor.buildingGadgetsExtra$create(blocks));a.buildingGadgetsExtra$setSize(new BlockPos(maxX-minX+1,maxY-minY+1,maxZ-minZ+1));template.setAuthor(player.getGameProfile().getName());try{ByteArrayOutputStream out=new ByteArrayOutputStream();CompressedStreamTools.writeCompressed(template.save(new CompoundNBT()),out);byte[] bytes=out.toByteArray();if(bytes.length>ExtraConstants.MAX_STRUCTURE_FILE_BYTES){message(player,ExtraConstants.STRUCTURE_TOO_LARGE,name);return null;}return bytes;}catch(Exception e){message(player,ExtraConstants.STRUCTURE_SAVE_FAILED,name);return null;}}
 public static void importStructure(ServerPlayerEntity player,String name,byte[] bytes){Context c=context(player);if(c==null)return;final Template template;try{template=player.getServer().getStructureManager().readStructure(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes)));}catch(Exception e){message(player,ExtraConstants.STRUCTURE_LOAD_FAILED,name);return;}VanillaTemplateAccessor a=(VanillaTemplateAccessor)(Object)template;if(a.buildingGadgetsExtra$getPalettes().isEmpty()||template.getSize().getX()<=0||template.getSize().getY()<=0||template.getSize().getZ()<=0){message(player,ExtraConstants.STRUCTURE_LOAD_FAILED,name);return;}long volume=(long)template.getSize().getX()*template.getSize().getY()*template.getSize().getZ();if(volume>ExtraConstants.MAX_STRUCTURE_BLOCKS){message(player,ExtraConstants.STRUCTURE_TOO_LARGE,name);return;}Map<BlockPos,Template.BlockInfo> nativeBlocks=new HashMap<>();for(Template.BlockInfo info:a.buildingGadgetsExtra$getPalettes().get(0).blocks())nativeBlocks.put(info.pos,info);ImmutableMap.Builder<BlockPos,BlockData> builder=ImmutableMap.builder();BlockPos max=new BlockPos(template.getSize().getX()-1,template.getSize().getY()-1,template.getSize().getZ()-1);for(BlockPos p:BlockPos.betweenClosed(BlockPos.ZERO,max)){Template.BlockInfo info=nativeBlocks.get(p);builder.put(p.immutable(),info==null||info.state.is(Blocks.STRUCTURE_VOID)?BlockData.AIR:new BlockData(info.state,info.nbt==null?TileSupport.dummyTileEntityData():new NBTTileEntityData(info.nbt.copy())));}Region bounds=new Region(BlockPos.ZERO,max);TemplateHeader header=TemplateHeader.builder(bounds).name(name).author(player.getGameProfile().getName()).build();c.provider.setTemplate(c.key,new com.direwolf20.buildinggadgets.common.tainted.template.Template(builder.build(),header).normalize());c.provider.requestRemoteUpdate(c.key,PacketDistributor.PLAYER.with(()->player));message(player,ExtraConstants.STRUCTURE_LOADED,name);}
 private static Context context(ServerPlayerEntity player){ItemStack gadget=AbstractGadget.getGadget(player);if(!(gadget.getItem() instanceof GadgetCopyPaste))return null;ITemplateProvider p=player.level.getCapability(CapabilityTemplate.TEMPLATE_PROVIDER_CAPABILITY).orElse(null);ITemplateKey k=gadget.getCapability(CapabilityTemplate.TEMPLATE_KEY_CAPABILITY).orElse(null);return p==null||k==null?null:new Context(p,k);}
 private static void message(ServerPlayerEntity p,String key,Object...args){p.displayClientMessage(new TranslationTextComponent(key,args),true);}
 private static final class Context{final ITemplateProvider provider;final ITemplateKey key;Context(ITemplateProvider p,ITemplateKey k){provider=p;key=k;}}
}
