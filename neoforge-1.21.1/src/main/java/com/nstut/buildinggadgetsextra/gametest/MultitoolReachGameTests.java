package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.direwolf20.buildinggadgets2.util.modes.BuildToMe;
import com.mojang.authlib.GameProfile;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(ExtraConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultitoolReachGameTests {
    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void allProfilesReachFurtherAndBuildToMeUsesTheExtendedReach(GameTestHelper helper) {
        Player player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bge-reach"));
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool tool = (BuildersMultitool) stack.getItem();
        int oldNative = Config.RAYTRACE_RANGE.get();
        double oldMultiplier = ExtraConfig.MULTITOOL_RANGE_MULTIPLIER.get();
        int oldOverride = ExtraConfig.MULTITOOL_MAX_RANGE.get();
        try {
            Config.RAYTRACE_RANGE.set(32);
            ExtraConfig.MULTITOOL_RANGE_MULTIPLIER.set(2.0);
            ExtraConfig.MULTITOOL_MAX_RANGE.set(0);
            player.setPos(0.5, 300, 0.5);
            player.setYRot(0);
            player.setXRot(0);
            player.setOldPosAndRot();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            for (MultitoolMode mode : MultitoolMode.values()) {
                tool.selectTool(stack, mode);
                double distance = VectorHelper.getLookingAt(player, stack).getLocation().distanceTo(player.getEyePosition(0));
                helper.assertTrue(Math.abs(distance - 64) < 0.01, mode + " must target at 64 blocks, got " + distance);
            }
            tool.selectTool(stack, MultitoolMode.BUILD);
            GadgetNBT.setToolRange(stack, 999);
            helper.assertTrue(GadgetNBT.getToolRange(stack) == 30, "automatic Build/Exchange cap must be twice 15");
            BuildToMe mode = new BuildToMe() {
                @Override public boolean isPosValid(Level level, Player actor, BlockPos pos, BlockState state) { return true; }
            };
            helper.assertTrue(mode.collectWorld(Direction.NORTH, player, new BlockPos(0, 300, 48),
                    Blocks.STONE.defaultBlockState()).size() == 47, "Build To Me must cross its old 32-block cap");
            tool.selectTool(stack, MultitoolMode.DESTRUCTION);
            GadgetNBT.setToolValue(stack, 20, "left");
            GadgetNBT.setToolValue(stack, 32, "depth");
            helper.assertTrue(GadgetNBT.getToolValue(stack, "left") == 20, "destruction must exceed 16");
            ExtraConfig.MULTITOOL_RANGE_MULTIPLIER.set(1.0);
            helper.assertTrue(GadgetNBT.getToolValue(stack, "left") == 16
                    && GadgetNBT.getToolValue(stack, "depth") == 16, "lower config must bound existing destruction values");
            ItemStack nativeTool = new ItemStack(Registration.Building_Gadget.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, nativeTool);
            ExtraConfig.MULTITOOL_RANGE_MULTIPLIER.set(3.0);
            double nativeDistance = VectorHelper.getLookingAt(player, nativeTool).getLocation().distanceTo(player.getEyePosition(0));
            helper.assertTrue(Math.abs(nativeDistance - 32) < 0.01, "native gadget reach must be unchanged");
            helper.assertTrue(mode.collectWorld(Direction.NORTH, player, new BlockPos(0, 300, 48),
                    Blocks.STONE.defaultBlockState()).size() == 31, "native Build To Me must be unchanged");
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Config.RAYTRACE_RANGE.set(oldNative);
            ExtraConfig.MULTITOOL_RANGE_MULTIPLIER.set(oldMultiplier);
            ExtraConfig.MULTITOOL_MAX_RANGE.set(oldOverride);
        }
        helper.succeed();
    }

    @GameTest(template = "bge_empty", timeoutTicks = 40)
    public static void configurableCopyVolumePreservesAirAndServerBounds(GameTestHelper helper) {
        Player player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bge-copy-volume"));
        int previous = ExtraConfig.MAX_COPY_VOLUME.get();
        try {
            ItemStack nativeCopy = new ItemStack(com.direwolf20.buildinggadgets2.setup.Registration.CopyPaste_Gadget.get());
            ItemStack multitool = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
            ((BuildersMultitool) multitool.getItem()).selectTool(multitool, MultitoolMode.COPY_PASTE);
            for (ItemStack stack : new ItemStack[]{nativeCopy, multitool}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                // Above world height: a large all-air selection without loading extra chunks.
                BlockPos start = new BlockPos(0, 1000, 0);
                GadgetNBT.setCopyStartPos(stack, start);
                GadgetNBT.setCopyEndPos(stack, start.offset(109, 99, 9));
                com.direwolf20.buildinggadgets2.util.modes.Copy copy = new com.direwolf20.buildinggadgets2.util.modes.Copy();
                ExtraConfig.MAX_COPY_VOLUME.set(100_000);
                helper.assertTrue(copy.collectWorld(net.minecraft.core.Direction.UP, player, start,
                        Blocks.AIR.defaultBlockState()).isEmpty(), "default cap must reject 110,000 air positions");
                ExtraConfig.MAX_COPY_VOLUME.set(110_000);
                var copied = copy.collectWorld(net.minecraft.core.Direction.UP, player, start, Blocks.AIR.defaultBlockState());
                helper.assertTrue(copied.size() == 110_000, "configured cap must accept every position including air, got " + copied.size());
                helper.assertTrue(copied.stream().allMatch(entry -> entry.state.isAir()), "air entries must be preserved");
                helper.assertTrue(copied.stream().anyMatch(entry -> entry.pos.equals(BlockPos.ZERO))
                        && copied.stream().anyMatch(entry -> entry.pos.equals(new BlockPos(109, 99, 9))),
                        "relative selection bounds must be preserved");
                ExtraConfig.MAX_COPY_VOLUME.set(109_999);
                helper.assertTrue(copy.collectWorld(net.minecraft.core.Direction.UP, player, start,
                        Blocks.AIR.defaultBlockState()).isEmpty(), "lowering the server cap must reject existing selections");
            }
        } finally {
            ExtraConfig.MAX_COPY_VOLUME.set(previous);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }
}
