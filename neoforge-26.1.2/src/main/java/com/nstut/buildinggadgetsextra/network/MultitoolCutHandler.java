package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VecHelpers;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
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
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public final class MultitoolCutHandler {
    private static final int MAX_AXIS = 500;
    private static final int MAX_BLOCKS = 100_000;

    private MultitoolCutHandler() {}

    public static void handle(MultitoolCutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> cut(context.player()));
    }

    private static void cut(Player player) {
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE
                || !GadgetNBT.getMode(stack).getId().getPath().equals("cut")
                || ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(stack))) return;

        BlockPos start = GadgetNBT.getCopyStartPos(stack);
        BlockPos end = GadgetNBT.getCopyEndPos(stack);
        if (start.equals(GadgetNBT.nullPos) || end.equals(GadgetNBT.nullPos)) return;

        AABB area = VecHelpers.aabbFromBlockPos(start, end);
        if (!validSize(player, area)) return;
        long selected = BlockPos.betweenClosedStream(area).count();
        if (selected > MAX_BLOCKS) {
            player.sendSystemMessage(Component.translatable("buildinggadgets2.messages.areatoolarge", MAX_BLOCKS, selected));
            return;
        }

        Level level = player.level();
        ArrayList<BlockPos> accepted = new ArrayList<>();
        BlockPos.betweenClosedStream(area).map(BlockPos::immutable)
                .sorted(Comparator.comparingInt(Vec3i::getY).reversed()).forEach(pos -> {
                    if (!GadgetCutPaste.customCutValidation(level.getBlockState(pos), level, player, pos)) return;
                    BreakBlockEvent event = new BreakBlockEvent(level, pos, level.getBlockState(pos), player);
                    if (NeoForge.EVENT_BUS.post(event).isCanceled()) return;
                    accepted.add(pos);
                });
        if (accepted.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(ExtraConstants.CUT_NO_VALID_BLOCKS));
            return;
        }

        int cutCost = Registration.CutPaste_Gadget.get().getEnergyCost();
        long totalCostLong = (long) cutCost * accepted.size();
        if (totalCostLong > Integer.MAX_VALUE) return;
        int totalCost = (int) totalCostLong;
        if (!player.isCreative() && !BuildingUtils.hasEnoughEnergy(stack, totalCost)) {
            player.sendSystemMessage(Component.translatable("buildinggadgets2.messages.notenoughenergy",
                    totalCost, BuildingUtils.getEnergyStored(stack)));
            return;
        }

        UUID buildUUID = UUID.randomUUID();
        for (BlockPos pos : accepted) {
            ServerTickHandler.addToMap(buildUUID, new StatePos(Blocks.AIR.defaultBlockState(), pos), level,
                    GadgetNBT.getRenderTypeByte(stack), player, false, false, stack,
                    ServerBuildList.BuildType.CUT, false, BlockPos.ZERO);
        }

        ServerTickHandler.setCutStart(buildUUID, start);
        GadgetNBT.setCopyStartPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setCopyEndPos(stack, GadgetNBT.nullPos);
        GadgetNBT.setCopyUUID(stack, buildUUID);
        BG2Data data = BG2Data.get(Objects.requireNonNull(level.getServer()).overworld());
        data.addToCopyPaste(GadgetNBT.getUUID(stack), new ArrayList<>());
        data.addToTEMap(GadgetNBT.getUUID(stack), new ArrayList<>());
        player.sendOverlayMessage(Component.translatable("buildinggadgets2.messages.cutblocks", accepted.size()));
    }

    private static boolean validSize(Player player, AABB area) {
        double[] sizes = {area.getXsize(), area.getYsize(), area.getZsize()};
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] > MAX_AXIS) {
                player.sendSystemMessage(Component.translatable("buildinggadgets2.messages.axistoolarge",
                        axes[i], MAX_AXIS, sizes[i]));
                return false;
            }
        }
        return true;
    }
}
