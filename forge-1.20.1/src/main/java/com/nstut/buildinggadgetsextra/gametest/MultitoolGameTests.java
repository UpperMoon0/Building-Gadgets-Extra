package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.util.DimBlockPos;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.MultitoolRangePacket;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(ExtraConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultitoolGameTests {
    private MultitoolGameTests() {}

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void profileStateStaysIsolated(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();

        helper.assertTrue(MultitoolState.getActiveMode(stack) == MultitoolMode.BUILD,
                "fresh multitool must start in the build profile");

        UUID buildId = GadgetNBT.getUUID(stack);
        UUID buildUndo = UUID.randomUUID();
        LinkedList<UUID> buildUndoList = new LinkedList<>();
        buildUndoList.add(buildUndo);
        GadgetNBT.setUndoList(stack, buildUndoList);
        GadgetNBT.setToolRange(stack, 7);
        GadgetNBT.setGadgetBlockState(stack, Blocks.STONE.defaultBlockState());
        GadgetNBT.toggleSetting(stack, "placeontop");
        DimBlockPos buildBoundPos = new DimBlockPos(helper.getLevel(), new BlockPos(1, 2, 3));
        GadgetNBT.setBoundPos(stack, buildBoundPos);
        GadgetNBT.setToolValue(stack, Direction.NORTH.ordinal(), "binddirection");

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        UUID exchangeId = GadgetNBT.getUUID(stack);
        helper.assertTrue(!buildId.equals(exchangeId), "profiles must not share gadget UUIDs");
        helper.assertTrue(GadgetNBT.getUndoList(stack).isEmpty(),
                "new profile must not inherit another profile's undo history");
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 1,
                "new exchange profile must start at the native range default");
        helper.assertTrue(!GadgetNBT.getSetting(stack, "placeontop"),
                "build-only settings must not leak into exchange");
        helper.assertTrue(GadgetNBT.getBoundPos(stack) == null,
                "new exchange profile must not inherit the build bound inventory");
        helper.assertTrue(!stack.getOrCreateTag().contains("binddirection"),
                "new exchange profile must not inherit the build bound face");

        UUID exchangeUndo = UUID.randomUUID();
        LinkedList<UUID> exchangeUndoList = new LinkedList<>();
        exchangeUndoList.add(exchangeUndo);
        GadgetNBT.setUndoList(stack, exchangeUndoList);
        GadgetNBT.setToolRange(stack, 3);
        GadgetNBT.setGadgetBlockState(stack, Blocks.DIRT.defaultBlockState());
        GadgetNBT.toggleSetting(stack, "affecttiles");
        DimBlockPos exchangeBoundPos = new DimBlockPos(helper.getLevel(), new BlockPos(4, 5, 6));
        GadgetNBT.setBoundPos(stack, exchangeBoundPos);
        GadgetNBT.setToolValue(stack, Direction.UP.ordinal(), "binddirection");

        multitool.selectTool(stack, MultitoolMode.BUILD);
        helper.assertTrue(GadgetNBT.getUUID(stack).equals(buildId), "build UUID must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getUndoList(stack).size() == 1 && GadgetNBT.getUndoList(stack).getLast().equals(buildUndo),
                "build undo history must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 7, "build range must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getGadgetBlockState(stack).is(Blocks.STONE),
                "build selected block must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getSetting(stack, "placeontop"), "build setting must restore");
        helper.assertTrue(!GadgetNBT.getSetting(stack, "affecttiles"), "exchange setting must not leak into build");
        helper.assertTrue(buildBoundPos.equals(GadgetNBT.getBoundPos(stack)),
                "build bound inventory must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getToolValue(stack, "binddirection") == Direction.NORTH.ordinal(),
                "build bound face must restore with its bound inventory");

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        helper.assertTrue(GadgetNBT.getUUID(stack).equals(exchangeId), "exchange UUID must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getUndoList(stack).size() == 1 && GadgetNBT.getUndoList(stack).getLast().equals(exchangeUndo),
                "exchange undo history must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 3, "exchange range must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getGadgetBlockState(stack).is(Blocks.DIRT),
                "exchange selected block must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getSetting(stack, "affecttiles"), "exchange setting must restore");
        helper.assertTrue(!GadgetNBT.getSetting(stack, "placeontop"), "build setting must not leak into exchange");
        helper.assertTrue(exchangeBoundPos.equals(GadgetNBT.getBoundPos(stack)),
                "exchange bound inventory must restore after a live mode switch");
        helper.assertTrue(GadgetNBT.getToolValue(stack, "binddirection") == Direction.UP.ordinal(),
                "exchange bound face must restore with its bound inventory");

        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        helper.assertTrue(GadgetNBT.getPasteReplace(stack),
                "fresh cut profile must inherit native Paste Replace = true");
        helper.succeed();
    }

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void rangePacketPersistsAndSynchronizesHeldStack(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        multitool.selectTool(stack, MultitoolMode.BUILD);

        AtomicReference<ItemStack> clientSlotUpdate = new AtomicReference<>(ItemStack.EMPTY);
        player.containerMenu.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items,
                                        ItemStack carried, int[] data) {
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack itemStack) {
                if (itemStack.getItem() instanceof BuildersMultitool) {
                    clientSlotUpdate.set(itemStack.copy());
                }
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu menu, ItemStack itemStack) {
            }

            @Override
            public void sendDataChange(AbstractContainerMenu menu, int id, int value) {
            }
        });

        helper.assertTrue(GadgetNBT.getToolRange(stack) == 1,
                "range sync regression must start at the native range default");
        helper.assertTrue(MultitoolRangePacket.apply(player, 7),
                "server must accept a Build-profile multitool range update");
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 7,
                "server-authoritative held stack must persist the requested range");
        helper.assertTrue(!clientSlotUpdate.get().isEmpty(),
                "range mutation must broadcast a changed inventory slot back toward the client");
        helper.assertTrue(GadgetNBT.getToolRange(clientSlotUpdate.get()) == 7,
                "client-bound inventory stack must contain the persisted range used when the UI reopens");
        helper.succeed();
    }
}
