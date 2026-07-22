package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VecHelpers;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public final class MultitoolCutHandler {
    private static final int MAX_AXIS = 500;
    private static final int MAX_BLOCKS = 100_000;

    private MultitoolCutHandler() {
    }

    public static void handle(MultitoolCutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> cut(context.player()));
    }

    private static void cut(Player player) {
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool multitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE
                || !GadgetNBT.getMode(stack).getId().getPath().equals("cut")
                || ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(stack))) return;

        BlockPos start = GadgetNBT.getCopyStartPos(stack);
        BlockPos end = GadgetNBT.getCopyEndPos(stack);
        if (start.equals(GadgetNBT.nullPos) || end.equals(GadgetNBT.nullPos)) return;

        AABB area = VecHelpers.aabbFromBlockPos(start, end);
        if (!validSize(player, area)) return;
        long size = BlockPos.betweenClosedStream(area).count();
        if (size > MAX_BLOCKS) {
            player.displayClientMessage(Component.translatable("buildinggadgets2.messages.areatoolarge", MAX_BLOCKS, size), false);
            return;
        }

        int totalCost = multitool.getEnergyCost() * (int) size;
        if (!player.isCreative() && !BuildingUtils.hasEnoughEnergy(stack, totalCost)) {
            player.displayClientMessage(Component.translatable("buildinggadgets2.messages.notenoughenergy",
                    totalCost, BuildingUtils.getEnergyStored(stack)), false);
            return;
        }

        Level level = player.level();
        UUID buildUUID = UUID.randomUUID();
        BlockPos.betweenClosedStream(area).map(BlockPos::immutable)
                .sorted(Comparator.comparingInt(Vec3i::getY).reversed()).forEach(pos -> {
                    if (!GadgetCutPaste.customCutValidation(level.getBlockState(pos), level, player, pos)) return;
                    BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
                    if (NeoForge.EVENT_BUS.post(event).isCanceled()) return;
                    ServerTickHandler.addToMap(buildUUID, new StatePos(Blocks.AIR.defaultBlockState(), pos), level,
                            GadgetNBT.getRenderTypeByte(stack), player, false, false, stack,
                            ServerBuildList.BuildType.CUT, false, BlockPos.ZERO);
                });

        ServerTickHandler.setCutStart(buildUUID, start);
        GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setCopyUUID(stack, buildUUID);
        BG2Data data = BG2Data.get(Objects.requireNonNull(level.getServer()).overworld());
        data.addToCopyPaste(GadgetNBT.getUUID(stack), new ArrayList<>());
        data.addToTEMap(GadgetNBT.getUUID(stack), new ArrayList<>());
        player.displayClientMessage(Component.translatable("buildinggadgets2.messages.cutblocks", size), true);
    }

    private static boolean validSize(Player player, AABB area) {
        double[] sizes = {area.getXsize(), area.getYsize(), area.getZsize()};
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] > MAX_AXIS) {
                player.displayClientMessage(Component.translatable("buildinggadgets2.messages.axistoolarge",
                        axes[i], MAX_AXIS, sizes[i]), false);
                return false;
            }
        }
        return true;
    }
}
