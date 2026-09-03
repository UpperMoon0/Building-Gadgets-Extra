package com.nstut.buildinggadgetsextra.structure;

import com.direwolf20.buildinggadgets.common.capability.CapabilityTemplate;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.NBTTileEntityData;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.TileSupport;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.google.common.collect.ImmutableMap;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.StructureLimits;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.mixin.TemplateAccessor;
import com.nstut.buildinggadgetsextra.mixin.VanillaPaletteAccessor;
import com.nstut.buildinggadgetsextra.mixin.VanillaTemplateAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.fml.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class NativeStructureBridge {
    private NativeStructureBridge() {}

    public static byte[] exportStructure(ServerPlayerEntity player, String name) {
        Context c = context(player, false);
        if (c == null) return null;
        com.direwolf20.buildinggadgets.common.tainted.template.Template source = c.provider.getTemplateForKey(c.key);
        Map<BlockPos, BlockData> map = ((TemplateAccessor) (Object) source).buildingGadgetsExtra$getMap();
        if (map.isEmpty()) {
            message(player, ExtraConstants.NO_TEMPLATE);
            return null;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : map.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }

        List<Template.BlockInfo> blocks = new ArrayList<>(map.size());
        for (Map.Entry<BlockPos, BlockData> e : map.entrySet()) {
            CompoundNBT nbt = null;
            if (e.getValue().getTileData() instanceof NBTTileEntityData) {
                nbt = ((NBTTileEntityData) e.getValue().getTileData()).getNBT().copy();
                nbt.remove("x"); nbt.remove("y"); nbt.remove("z");
            }
            blocks.add(new Template.BlockInfo(e.getKey().offset(-minX, -minY, -minZ), e.getValue().getState(), nbt));
        }

        Template template = new Template();
        VanillaTemplateAccessor a = (VanillaTemplateAccessor) (Object) template;
        a.buildingGadgetsExtra$getPalettes().add(VanillaPaletteAccessor.buildingGadgetsExtra$create(blocks));
        a.buildingGadgetsExtra$setSize(new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1));
        template.setAuthor(player.getGameProfile().getName());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(template.save(new CompoundNBT()), out);
            byte[] bytes = out.toByteArray();
            if (bytes.length > ExtraConstants.MAX_STRUCTURE_FILE_BYTES) {
                message(player, ExtraConstants.STRUCTURE_TOO_LARGE, name);
                return null;
            }
            return bytes;
        } catch (Exception e) {
            message(player, ExtraConstants.STRUCTURE_SAVE_FAILED, name);
            return null;
        }
    }

    public static void importStructure(ServerPlayerEntity player, String name, byte[] bytes) {
        Context c = context(player, true);
        if (c == null) return;
        final Template template;
        try (DataInputStream input = new DataInputStream(
                new GZIPInputStream(new ByteArrayInputStream(bytes)))) {
            CompoundNBT root = CompressedStreamTools.read(
                    input, new NBTSizeTracker(ExtraConstants.MAX_STRUCTURE_NBT_BYTES));
            template = player.getServer().getStructureManager().readStructure(root);
        } catch (Exception e) {
            message(player, ExtraConstants.STRUCTURE_LOAD_FAILED, name);
            return;
        }

        VanillaTemplateAccessor a = (VanillaTemplateAccessor) (Object) template;
        if (a.buildingGadgetsExtra$getPalettes().isEmpty()
                || template.getSize().getX() <= 0 || template.getSize().getY() <= 0 || template.getSize().getZ() <= 0) {
            message(player, ExtraConstants.STRUCTURE_LOAD_FAILED, name);
            return;
        }
        long volume = StructureLimits.checkedVolume(
                template.getSize().getX(), template.getSize().getY(), template.getSize().getZ());
        if (volume < 0) {
            message(player, ExtraConstants.STRUCTURE_TOO_LARGE, name);
            return;
        }

        Map<BlockPos, Template.BlockInfo> nativeBlocks = new HashMap<>();
        for (Template.BlockInfo info : a.buildingGadgetsExtra$getPalettes().get(0).blocks()) {
            nativeBlocks.put(info.pos, info);
        }

        boolean strippedBlockEntityData = false;
        ImmutableMap.Builder<BlockPos, BlockData> builder = ImmutableMap.builder();
        BlockPos max = new BlockPos(template.getSize().getX() - 1,
                template.getSize().getY() - 1, template.getSize().getZ() - 1);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, max)) {
            Template.BlockInfo info = nativeBlocks.get(p);
            if (info == null || info.state.is(Blocks.STRUCTURE_VOID)) {
                builder.put(p.immutable(), BlockData.AIR);
            } else {
                strippedBlockEntityData |= info.nbt != null;
                builder.put(p.immutable(), new BlockData(info.state, TileSupport.dummyTileEntityData()));
            }
        }

        Region bounds = new Region(BlockPos.ZERO, max);
        TemplateHeader header = TemplateHeader.builder(bounds)
                .name(name).author(player.getGameProfile().getName()).build();
        c.provider.setTemplate(c.key,
                new com.direwolf20.buildinggadgets.common.tainted.template.Template(builder.build(), header).normalize());
        c.provider.requestRemoteUpdate(c.key, PacketDistributor.PLAYER.with(() -> player));
        message(player, strippedBlockEntityData
                ? ExtraConstants.STRUCTURE_BLOCK_ENTITY_STRIPPED
                : ExtraConstants.STRUCTURE_LOADED, name);
    }

    private static Context context(ServerPlayerEntity player, boolean importing) {
        ItemStack gadget = AbstractGadget.getGadget(player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)) return null;
        if (importing && gadget.getItem() instanceof BuildersMultitool
                && MultitoolState.getActiveMode(gadget) != MultitoolMode.COPY_PASTE) {
            message(player, ExtraConstants.STRUCTURE_IMPORT_REQUIRES_COPY);
            return null;
        }
        ITemplateProvider provider = player.level.getCapability(CapabilityTemplate.TEMPLATE_PROVIDER_CAPABILITY).orElse(null);
        ITemplateKey key = gadget.getCapability(CapabilityTemplate.TEMPLATE_KEY_CAPABILITY).orElse(null);
        return provider == null || key == null ? null : new Context(provider, key);
    }

    private static void message(ServerPlayerEntity player, String key, Object... args) {
        player.displayClientMessage(new TranslationTextComponent(key, args), true);
    }

    private static final class Context {
        final ITemplateProvider provider;
        final ITemplateKey key;
        Context(ITemplateProvider provider, ITemplateKey key) {
            this.provider = provider;
            this.key = key;
        }
    }
}
