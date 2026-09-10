package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class MultitoolCutPacket {
    private static final int MAX_AXIS = 500;
    private static final int MAX_BLOCKS = 100_000;

    public static void encode(MultitoolCutPacket packet, FriendlyByteBuf buffer) {}
    public static MultitoolCutPacket decode(FriendlyByteBuf buffer) { return new MultitoolCutPacket(); }

    public static void handle(MultitoolCutPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) cut(player);
        });
        context.setPacketHandled(true);
    }

    private static void cut(ServerPlayer player) {
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE
                || !GadgetNBT.getMode(stack).getId().getPath().equals("cut")
                || ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(stack))) return;

        BlockPos start = GadgetNBT.getCopyStartPos(stack);
        BlockPos end = GadgetNBT.getCopyEndPos(stack);
        if (start.equals(GadgetNBT.nullPos) || end.equals(GadgetNBT.nullPos)) return;

        AABB area = new AABB(start, end);
        if (!validSize(player, area)) return;
        long selected = BlockPos.betweenClosedStream(area).count();
        if (selected > MAX_BLOCKS) {
            player.displayClientMessage(Component.translatable("buildinggadgets2.messages.areatoolarge", MAX_BLOCKS, selected), false);
            return;
        }

        Level level = player.level();
        List<BlockPos> accepted = BlockPos.betweenClosedStream(area)
                .map(BlockPos::immutable)
                .filter(pos -> GadgetCutPaste.customCutValidation(level.getBlockState(pos), level, player, pos))
                .sorted(Comparator.comparingInt(Vec3i::getY).reversed())
                .toList();
        if (accepted.isEmpty()) {
            player.displayClientMessage(Component.translatable(ExtraConstants.CUT_NO_VALID_BLOCKS), true);
            return;
        }

        int cutCost = ((GadgetCutPaste) com.direwolf20.buildinggadgets2.setup.Registration.CutPaste_Gadget.get()).getEnergyCost();
        long totalCostLong = (long) cutCost * accepted.size();
        if (totalCostLong > Integer.MAX_VALUE) return;
        int totalCost = (int) totalCostLong;
        if (!player.isCreative() && !BuildingUtils.hasEnoughEnergy(stack, totalCost)) {
            player.displayClientMessage(Component.translatable("buildinggadgets2.messages.notenoughenergy",
                    totalCost, BuildingUtils.getEnergyStored(stack)), false);
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
        player.displayClientMessage(Component.translatable("buildinggadgets2.messages.cutblocks", accepted.size()), true);
    }

    private static boolean validSize(ServerPlayer player, AABB area) {
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
