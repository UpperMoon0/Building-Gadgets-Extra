package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.UUID;

@GameTestHolder(ExtraConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultitoolCreativeGameTests {
    private MultitoolCreativeGameTests() {}

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void creativeZeroEnergyAndNativeModes(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.assertTrue(BuildingUtils.getEnergyStored(stack) == 0,
                "creative regression must start with an empty multitool battery");

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("surface"),
                "fresh Exchange profile must use BG2's native Surface default");
        multitool.selectTool(stack, MultitoolMode.COPY_PASTE);
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("copy"),
                "fresh Copy/Paste profile must use BG2's native Copy default");
        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("cut"),
                "fresh Cut/Paste profile must use BG2's native Cut default");
        multitool.selectTool(stack, MultitoolMode.BUILD);
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("build_to_me"),
                "fresh Build profile must use BG2's native Build To Me default");

        GadgetNBT.setToolRange(stack, ExtraConfig.multitoolMaxRange() + 20);
        helper.assertTrue(GadgetNBT.getToolRange(stack) == ExtraConfig.multitoolMaxRange(),
                "multitool range reads must clamp stale item data to the server config");

        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(relative);
        ArrayList<StatePos> buildList = new ArrayList<>();
        buildList.add(new StatePos(Blocks.STONE.defaultBlockState(), absolute));
        UUID buildId = BuildingUtils.build(helper.getLevel(), player, buildList, BlockPos.ZERO, stack, false);
        helper.assertTrue(ServerTickHandler.buildMap.containsKey(buildId),
                "creative Builder's Multitool with zero FE must queue real build work");
        helper.succeed();
    }
}
