package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import java.util.ArrayList;
import java.util.List;

import com.direwolf20.buildinggadgets2.util.DimBlockPos;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.mojang.authlib.GameProfile;
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
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(ExtraConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultitoolGameTests {
    private MultitoolGameTests() {}

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void cutCoordinatesOnlySelectAndClear(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "bge-cut-coords"));
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(source, Blocks.STONE.defaultBlockState(), 3);
        var data = com.direwolf20.buildinggadgets2.common.worlddata.BG2Data.get(helper.getLevel().getServer().overworld());
        UUID gadget = GadgetNBT.getUUID(stack);
        var before = data.getCopyPasteList(gadget, false);
        for (int attempt=0; attempt<2; attempt++) {
            com.nstut.buildinggadgetsextra.network.MultitoolCutSelectionHandler.apply(player, GadgetNBT.nullPos, GadgetNBT.nullPos);
            helper.assertTrue(!GadgetNBT.hasCopyUUID(stack), "clearing an empty Cut selection must not create a buffer");
            com.nstut.buildinggadgetsextra.network.MultitoolCutSelectionHandler.apply(player, source, source);
            helper.assertTrue(source.equals(GadgetNBT.getCopyStartPos(stack)) && source.equals(GadgetNBT.getCopyEndPos(stack)), "typed Cut coordinates must update the selection");
            helper.assertTrue(!GadgetNBT.hasCopyUUID(stack), "typed Cut coordinates must not copy the selected blocks");
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("cut"), "coordinate edits must retain Cut mode");
            helper.assertTrue(helper.getLevel().getBlockState(source).is(Blocks.STONE), "coordinate edits must not cut the world");
            helper.assertTrue(data.getCopyPasteList(gadget, false) == before, "coordinate edits must not replace the copy snapshot");
            com.nstut.buildinggadgetsextra.network.MultitoolCutSelectionHandler.apply(player, GadgetNBT.nullPos, GadgetNBT.nullPos);
            helper.assertTrue(GadgetNBT.getCopyStartPos(stack).equals(GadgetNBT.nullPos) && GadgetNBT.getCopyEndPos(stack).equals(GadgetNBT.nullPos), "Clear must remove both selection corners");
            helper.assertTrue(!GadgetNBT.hasCopyUUID(stack), "clearing a populated selection must not create a buffer");
        }
        UUID pending = UUID.randomUUID();
        GadgetNBT.setCopyUUID(stack, pending);
        com.nstut.buildinggadgetsextra.network.MultitoolCutSelectionHandler.apply(player, source, source);
        helper.assertTrue(pending.equals(GadgetNBT.getCopyUUID(stack)) && GadgetNBT.getCopyStartPos(stack).equals(GadgetNBT.nullPos), "late coordinates must not change pending Cut contents");
        GadgetNBT.clearCopyUUID(stack);
        multitool.selectTool(stack, MultitoolMode.COPY_PASTE);
        GadgetNBT.setCopyStartPos(stack, source);GadgetNBT.setCopyEndPos(stack, source);GadgetNBT.setCopyUUID(stack, pending);
        com.nstut.buildinggadgetsextra.network.MultitoolCutSelectionHandler.apply(player, GadgetNBT.nullPos, GadgetNBT.nullPos);
        helper.assertTrue(source.equals(GadgetNBT.getCopyStartPos(stack)) && source.equals(GadgetNBT.getCopyEndPos(stack))
                && pending.equals(GadgetNBT.getCopyUUID(stack)), "stale Cut edits must leave the Copy profile unchanged");
        helper.succeed();
    }

    @GameTest(template = "bge_empty", timeoutTicks = 20)
    public static void cutAutomaticallyPreparesPasteAndRejectsSecondCut(GameTestHelper helper) throws Exception {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "bge-cut-flow"));
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(source, Blocks.STONE.defaultBlockState(), 3);
        GadgetNBT.setCopyStartPos(stack, source);
        GadgetNBT.setCopyEndPos(stack, source);
        stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).ifPresent(e -> e.receiveEnergy(Integer.MAX_VALUE, false));
        java.lang.reflect.Method cut = com.nstut.buildinggadgetsextra.network.MultitoolCutPacket.class.getDeclaredMethod("cut", ServerPlayer.class);
        cut.setAccessible(true);
        UUID buildId = null;
        try {
            cut.invoke(null, player);
            helper.assertTrue(GadgetNBT.hasCopyUUID(stack), "accepted cut must store its contents");
            buildId = GadgetNBT.getCopyUUID(stack);
            GadgetNBT.setMode(stack, new com.direwolf20.buildinggadgets2.util.modes.Cut());
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("paste"), "older cut buffers must preview Paste before the first click");
            MultitoolState.syncCutPasteMode(stack);
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("paste"), "cut must activate paste without reopening the menu");
            helper.assertTrue(MultitoolState.getProfileMode(stack, MultitoolMode.CUT_PASTE).getPath().equals("paste"), "saved action must follow paste");
            multitool.selectTool(stack, MultitoolMode.BUILD);
            multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
            helper.assertTrue(buildId.equals(GadgetNBT.getCopyUUID(stack)), "profile navigation must retain pending cut contents");
            multitool.selectGadgetMode(stack, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("buildinggadgets2", "cut"));
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("paste"), "stale selection packet must not revert to cutting");
            cut.invoke(null, player);
            helper.assertTrue(buildId.equals(GadgetNBT.getCopyUUID(stack)), "second cut must not replace pending contents");
            GadgetNBT.clearCopyUUID(stack);
            GadgetNBT.setMode(stack, new com.direwolf20.buildinggadgets2.util.modes.Paste());
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("cut"), "empty buffer must never preview a stale Paste action");
            MultitoolState.syncCutPasteMode(stack);
            helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("cut"), "consumed buffer returns to selection");
            helper.succeed();
        } finally {
            if (buildId != null) com.direwolf20.buildinggadgets2.common.events.ServerTickHandler.buildMap.remove(buildId);
        }
    }


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
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "bge-range-test"));
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
    @GameTest(template = "bge_live_mirror", timeoutTicks = 20)
    public static void liveMirrorCollectPreservesStatesAndProfiles(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        helper.assertTrue(player.isCreative() && player.mayBuild(),
                "live mirror collection requires a creative player allowed to build");
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // Facing north selects the X mirror plane. Use a nonzero world origin to catch
        // accidentally reflecting absolute positions or validating local offsets as world positions.
        player.setYRot(180.0F);
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.BOTTOM);
        try {
            for (MultitoolMode profile : new MultitoolMode[]{MultitoolMode.BUILD, MultitoolMode.EXCHANGING}) {
                multitool.selectTool(stack, profile);
                helper.assertTrue(!MultitoolState.isLiveMirrorHorizontal(stack)
                                && !MultitoolState.isLiveMirrorVertical(stack),
                        "fresh profile must not inherit another profile's live mirror toggles");
                GadgetNBT.setGadgetBlockState(stack, stairs);
                if (profile == MultitoolMode.EXCHANGING && !GadgetNBT.getSetting(stack, "fuzzy")) {
                    GadgetNBT.toggleSetting(stack, "fuzzy");
                }
                for (int x : new int[]{-1, 1}) {
                    for (int y : new int[]{-1, 1}) {
                        helper.getLevel().setBlockAndUpdate(start.offset(x, y, 0),
                                (profile == MultitoolMode.EXCHANGING ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                    }
                }
                GadgetNBT.setAnchorPos(stack, start);
                GadgetNBT.setAnchorList(stack, new ArrayList<>(List.of(new BlockPos(1, 1, 0))));
                var mode = GadgetNBT.getMode(stack);
                var plain = mode.collect(Direction.UP, player, start, stairs);
                helper.assertTrue(plain.size() == 1, "disabled mirrors must retain native anchor collection");
                MultitoolState.toggleLiveMirror(stack, false);
                MultitoolState.toggleLiveMirror(stack, true);
                var mirrored = mode.collect(Direction.UP, player, start, stairs);
                helper.assertTrue(mirrored.size() == 4, profile + " must generate four local copies through BaseMode.collect");
                assertMirrorEntry(helper, mirrored, new BlockPos(1, 1, 0), Direction.EAST, Half.BOTTOM);
                assertMirrorEntry(helper, mirrored, new BlockPos(-1, 1, 0), Direction.WEST, Half.BOTTOM);
                assertMirrorEntry(helper, mirrored, new BlockPos(1, -1, 0), Direction.EAST, Half.TOP);
                assertMirrorEntry(helper, mirrored, new BlockPos(-1, -1, 0), Direction.WEST, Half.TOP);

                BlockPos rejectedCopy = new BlockPos(-1, 1, 0);
                helper.getLevel().setBlockAndUpdate(start.offset(rejectedCopy),
                        (profile == MultitoolMode.EXCHANGING ? Blocks.AIR : Blocks.STONE).defaultBlockState());
                var filtered = mode.collect(Direction.UP, player, start, stairs);
                helper.assertTrue(filtered.size() == 3
                                && filtered.stream().noneMatch(entry -> entry.pos.equals(rejectedCopy)),
                        "generated copies must honor native " + profile + " validity at start + local offset");
                helper.getLevel().setBlockAndUpdate(start.offset(rejectedCopy),
                        (profile == MultitoolMode.EXCHANGING ? Blocks.STONE : Blocks.AIR).defaultBlockState());

                // Both original positions are already selected. Neither may be replaced by
                // a differently oriented reflected copy from the other source position.
                GadgetNBT.setAnchorList(stack, new ArrayList<>(List.of(
                        new BlockPos(1, 1, 0), new BlockPos(-1, 1, 0))));
                var overlaps = mode.collect(Direction.UP, player, start, stairs);
                helper.assertTrue(overlaps.size() == 4, "overlapping mirror copies must be deduplicated");
                assertMirrorEntry(helper, overlaps, new BlockPos(1, 1, 0), Direction.EAST, Half.BOTTOM);
                assertMirrorEntry(helper, overlaps, new BlockPos(-1, 1, 0), Direction.EAST, Half.BOTTOM);

                MultitoolState.toggleLiveMirror(stack, true);
                MultitoolState.toggleLiveMirror(stack, false);
                helper.assertTrue(mode.collect(Direction.UP, player, start, stairs).size() == 2,
                        "disabling both mirrors must immediately restore the original selection");
                // Leave different flags in the two profiles and verify their snapshots below.
                MultitoolState.toggleLiveMirror(stack, profile == MultitoolMode.EXCHANGING);
            }
            multitool.selectTool(stack, MultitoolMode.BUILD);
            helper.assertTrue(MultitoolState.isLiveMirrorHorizontal(stack) && !MultitoolState.isLiveMirrorVertical(stack),
                    "Build must restore its own horizontal-only mirror flags");
            multitool.selectTool(stack, MultitoolMode.EXCHANGING);
            helper.assertTrue(!MultitoolState.isLiveMirrorHorizontal(stack) && MultitoolState.isLiveMirrorVertical(stack),
                    "Exchange must restore its own vertical-only mirror flags");
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    private static void assertMirrorEntry(GameTestHelper helper, List<StatePos> entries,
                                          BlockPos position, Direction facing, Half half) {
        var matches = entries.stream().filter(entry -> entry.pos.equals(position)).toList();
        helper.assertTrue(matches.size() == 1, "expected exactly one local copy at " + position);
        BlockState state = matches.get(0).state;
        helper.assertTrue(state.is(Blocks.OAK_STAIRS) && state.getValue(StairBlock.FACING) == facing
                        && state.getValue(StairBlock.HALF) == half,
                "copy at " + position + " must preserve its own transformed facing/half, got " + state);
    }
}
