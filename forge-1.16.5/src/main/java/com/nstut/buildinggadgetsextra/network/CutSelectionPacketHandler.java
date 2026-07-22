package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets.common.capability.CapabilityTemplate;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.cut.LegacyCutScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.energy.CapabilityEnergy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CutSelectionPacketHandler {
    private CutSelectionPacketHandler() {
    }

    public static void handle(ServerPlayerEntity player) {
        if (player == null) return;

        ItemStack gadget = AbstractGadget.getGadget(player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)
                || GadgetCopyPaste.getToolMode(gadget) != GadgetCopyPaste.ToolMode.COPY) return;

        Optional<Region> selected = GadgetCopyPaste.getSelectedRegion(gadget);
        if (!selected.isPresent()) {
            message(player, ExtraConstants.CUT_NO_SELECTION);
            return;
        }

        Region region = selected.get();
        Template template = player.level.getCapability(CapabilityTemplate.TEMPLATE_PROVIDER_CAPABILITY)
                .resolve().flatMap(provider -> gadget.getCapability(CapabilityTemplate.TEMPLATE_KEY_CAPABILITY)
                        .resolve().map(provider::getTemplateForKey)).orElse(null);
        if (template == null || !sameSize(region, template.getHeader().getBoundingBox())) {
            message(player, ExtraConstants.CUT_TEMPLATE_MISMATCH);
            return;
        }

        ServerWorld world = player.getLevel();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos pos : (Iterable<BlockPos>) region.stream()::iterator) {
            if (!world.isLoaded(pos)) {
                message(player, ExtraConstants.CUT_NOT_ALLOWED);
                return;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isAir(world, pos)) continue;
            if (state.getDestroySpeed(world, pos) < 0
                    || !world.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, gadget)
                    || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, pos, state, player))) {
                message(player, ExtraConstants.CUT_NOT_ALLOWED);
                return;
            }
            blocks.add(pos.immutable());
        }

        GadgetCopyPaste item = (GadgetCopyPaste) gadget.getItem();
        long requiredEnergy = player.isCreative() || item.getEnergyMax() == 0
                ? 0L : (long) item.getEnergyCost(gadget) * blocks.size();
        int storedEnergy = gadget.getCapability(CapabilityEnergy.ENERGY)
                .map(storage -> storage.getEnergyStored()).orElse(0);
        if (requiredEnergy > storedEnergy) {
            message(player, ExtraConstants.CUT_NOT_ENOUGH_ENERGY);
            return;
        }

        for (int i = 0; i < blocks.size(); i++) {
            item.applyDamage(gadget, player);
        }
        LegacyCutScheduler.schedule(world, region, blocks);
        item.setMode(gadget, GadgetCopyPaste.ToolMode.PASTE.ordinal());
        gadget.getOrCreateTag().putBoolean("cutBufferActive", true);
        player.inventory.setChanged();
        player.displayClientMessage(new TranslationTextComponent(ExtraConstants.CUT_COMPLETE, blocks.size()), true);
    }

    private static boolean sameSize(Region first, Region second) {
        return first.getXSize() == second.getXSize()
                && first.getYSize() == second.getYSize()
                && first.getZSize() == second.getZSize();
    }

    private static void message(ServerPlayerEntity player, String key) {
        player.displayClientMessage(new TranslationTextComponent(key), true);
    }
}
