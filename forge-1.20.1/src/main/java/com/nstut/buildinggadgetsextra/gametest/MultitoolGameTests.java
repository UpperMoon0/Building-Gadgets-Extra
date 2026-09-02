package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedList;
import java.util.UUID;

@GameTestHolder(ExtraConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultitoolGameTests {
    private MultitoolGameTests() {}

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void profileIdentityAndUndoStayIsolated(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();

        helper.assertTrue(MultitoolState.getActiveMode(stack) == MultitoolMode.BUILD,
                "fresh multitool must start in the build profile");

        UUID buildId = GadgetNBT.getUUID(stack);
        UUID buildUndo = UUID.randomUUID();
        LinkedList<UUID> buildUndoList = new LinkedList<>();
        buildUndoList.add(buildUndo);
        GadgetNBT.setUndoList(stack, buildUndoList);

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        UUID exchangeId = GadgetNBT.getUUID(stack);
        helper.assertTrue(!buildId.equals(exchangeId), "profiles must not share gadget UUIDs");
        helper.assertTrue(GadgetNBT.getUndoList(stack).isEmpty(),
                "new profile must not inherit another profile's undo history");

        UUID exchangeUndo = UUID.randomUUID();
        LinkedList<UUID> exchangeUndoList = new LinkedList<>();
        exchangeUndoList.add(exchangeUndo);
        GadgetNBT.setUndoList(stack, exchangeUndoList);

        multitool.selectTool(stack, MultitoolMode.BUILD);
        helper.assertTrue(GadgetNBT.getUUID(stack).equals(buildId), "build UUID must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getUndoList(stack).size() == 1 && GadgetNBT.getUndoList(stack).getLast().equals(buildUndo),
                "build undo history must restore after a live mode switch");

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        helper.assertTrue(GadgetNBT.getUUID(stack).equals(exchangeId), "exchange UUID must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getUndoList(stack).size() == 1 && GadgetNBT.getUndoList(stack).getLast().equals(exchangeUndo),
                "exchange undo history must restore after a live mode switch");
        helper.succeed();
    }
}
