package com.nstut.buildinggadgetsextra.gametest;

import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.modes.BuildToMe;
import com.mojang.authlib.GameProfile;
import com.nstut.buildinggadgetsextra.common.CopyDataAssembly;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.CopyDataPacket;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
                GadgetNBT.setCopyEndPos(stack, start.offset(120, 120, 10));
                // Use this version's native box semantics to measure the regression fixture.
                var area = new net.minecraft.world.phys.AABB(start, GadgetNBT.getCopyEndPos(stack));
                int expected = (int) BlockPos.betweenClosedStream(area).count();
                BlockPos last = BlockPos.betweenClosedStream(area).reduce((a, b) -> b.immutable()).orElseThrow().subtract(start);
                helper.assertTrue(expected > 100_000 && expected < 1_000_000, "fixture must cross the old cap");
                com.direwolf20.buildinggadgets2.util.modes.Copy copy = new com.direwolf20.buildinggadgets2.util.modes.Copy();
                ExtraConfig.MAX_COPY_VOLUME.set(100_000);
                helper.assertTrue(copy.collectWorld(net.minecraft.core.Direction.UP, player, start,
                        Blocks.AIR.defaultBlockState()).isEmpty(), "default cap must reject this large air selection");
                ExtraConfig.MAX_COPY_VOLUME.set(expected);
                var copied = copy.collectWorld(net.minecraft.core.Direction.UP, player, start, Blocks.AIR.defaultBlockState());
                helper.assertTrue(copied.size() == expected, "configured cap must accept every position including air, got " + copied.size());
                helper.assertTrue(copied.stream().allMatch(entry -> entry.state.isAir()), "air entries must be preserved");
                helper.assertTrue(copied.stream().anyMatch(entry -> entry.pos.equals(BlockPos.ZERO))
                        && copied.stream().anyMatch(entry -> entry.pos.equals(last)),
                        "relative selection bounds must be preserved");
                ExtraConfig.MAX_COPY_VOLUME.set(expected - 1);
                helper.assertTrue(copy.collectWorld(net.minecraft.core.Direction.UP, player, start,
                        Blocks.AIR.defaultBlockState()).isEmpty(), "lowering the server cap must reject existing selections");
            }
        } finally {
            ExtraConfig.MAX_COPY_VOLUME.set(previous);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "bge_empty", timeoutTicks = 100)
    public static void millionBlockCopyNetworkRoundTrip(GameTestHelper helper) {
        int previous = ExtraConfig.MAX_COPY_VOLUME.get();
        try {
            ExtraConfig.MAX_COPY_VOLUME.set(1_000_000);
            CompoundTag original = millionBlockTemplate();
            byte[] bytes = CopyDataTransfer.encode(original);
            helper.assertTrue(bytes.length > 2 * 1024 * 1024,
                    "fixture must exceed the old NBT quota, regardless of compression");
            int total = (bytes.length + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1)
                    / ExtraConstants.STRUCTURE_CHUNK_SIZE;
            UUID transferId = UUID.randomUUID();
            UUID gadgetId = UUID.randomUUID();
            UUID copyId = UUID.randomUUID();
            CopyDataAssembly assembly = new CopyDataAssembly();
            byte[] complete = null;
            for (int index = 0; index < total; index++) {
                int offset = index * ExtraConstants.STRUCTURE_CHUNK_SIZE;
                byte[] chunk = Arrays.copyOfRange(bytes, offset,
                        Math.min(bytes.length, offset + ExtraConstants.STRUCTURE_CHUNK_SIZE));
                CopyDataPacket sent = new CopyDataPacket(transferId, gadgetId, copyId, index, total, chunk);
                FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    CopyDataPacket.encode(sent, wire);
                    helper.assertTrue(wire.readableBytes() < 1024 * 1024,
                            "every encoded fragment must fit the vanilla clientbound payload limit");
                    var received = CopyDataPacket.decode(wire);
                    helper.assertTrue(!wire.isReadable(), "fragment decoder must consume the entire packet");
                    helper.assertTrue(received.transferId().equals(transferId)
                                    && received.gadgetId().equals(gadgetId) && received.copyId().equals(copyId),
                            "fragment wire codec must preserve transfer, gadget and copy identities");
                    helper.assertTrue(received.index() == index && received.total() == total
                                    && Arrays.equals(received.data(), chunk),
                            "fragment wire codec must preserve chunk ordering and bytes");
                    complete = assembly.accept(received.transferId(), received.gadgetId(), received.copyId(),
                            received.index(), received.total(), received.data());
                    helper.assertTrue((complete != null) == (index == total - 1),
                            "a template must only become available after every fragment arrives");
                } finally {
                    wire.release();
                }
            }
            helper.assertTrue(Arrays.equals(bytes, complete), "reassembly must preserve all serialized template bytes");
            CompoundTag restored = CopyDataTransfer.decode(complete);
            helper.assertTrue(original.equals(restored), "network round trip must preserve the complete BG2 template NBT");
            var positions = BG2Data.statePosListFromNBTMapArray(restored);
            helper.assertTrue(positions.size() == 1_000_000, "BG2 must reconstruct every selected position, including air");
            for (int index = 0; index < positions.size(); index++) {
                StatePos entry = positions.get(index);
                if (entry.pos.getX() != index % 100 || entry.pos.getY() != (index / 100) % 100
                        || entry.pos.getZ() != index / 10_000
                        || !entry.state.is((index & 1) == 0 ? Blocks.AIR : Blocks.STONE)) {
                    throw new IllegalStateException("BG2 template changed relative position or palette state at " + index);
                }
            }
            helper.assertTrue(CopyDataTransfer.decode(CopyDataTransfer.encode(new CompoundTag())).isEmpty(),
                    "empty upstream copy templates must still round trip");
        } catch (IOException exception) {
            throw new IllegalStateException("million-block template must survive its real network codecs", exception);
        } finally {
            ExtraConfig.MAX_COPY_VOLUME.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "bge_empty", timeoutTicks = 100)
    public static void copyNetworkRejectsMalformedData(GameTestHelper helper) {
        // Exercise allocation and input bounds without visiting blocks or allocating a million StatePos objects.
        boolean rejected = false;
        try {
            CopyDataTransfer.decode(new byte[ExtraConstants.MAX_STRUCTURE_FILE_BYTES + 1]);
        } catch (IOException exception) {
            rejected = true;
        }
        helper.assertTrue(rejected, "serialized templates above the byte budget must be rejected");
        CompoundTag invalid = twoBlockTemplate();
        invalid.putIntArray("statelist", new int[]{0});
        assertTemplateRejected(helper, invalid, "palette index count must match the selected volume");
        invalid = twoBlockTemplate();
        invalid.putIntArray("statelist", new int[]{0, 2});
        assertTemplateRejected(helper, invalid, "out-of-range palette indices must be rejected");
        invalid = twoBlockTemplate();
        CompoundTag justOverCap = new CompoundTag();
        justOverCap.putInt("X", 1_000_000);
        justOverCap.putInt("Y", 0);
        justOverCap.putInt("Z", 0);
        invalid.put("endpos", justOverCap);
        invalid.putIntArray("statelist", new int[1_000_001]);
        assertTemplateRejected(helper, invalid, "a well-sized template of 1,000,001 positions must be rejected");
        invalid = twoBlockTemplate();
        CompoundTag farCorner = new CompoundTag();
        farCorner.putInt("X", Integer.MAX_VALUE);
        farCorner.putInt("Y", Integer.MAX_VALUE);
        farCorner.putInt("Z", Integer.MAX_VALUE);
        invalid.put("endpos", farCorner);
        assertTemplateRejected(helper, invalid, "oversized geometry must be rejected before BG2 expands it");

        FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
        try {
            // Deliberately bypass the sender to test the untrusted wire decoder's own chunk-size limit.
            wire.writeUUID(UUID.randomUUID());
            wire.writeUUID(UUID.randomUUID());
            wire.writeUUID(UUID.randomUUID());
            wire.writeVarInt(0);
            wire.writeVarInt(1);
            wire.writeByteArray(new byte[ExtraConstants.STRUCTURE_CHUNK_SIZE + 1]);
            rejected = false;
            try {
                CopyDataPacket.decode(wire);
            } catch (RuntimeException exception) {
                rejected = true;
            }
            helper.assertTrue(rejected, "wire decoder must reject oversized fragments before assembly");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    private static void assertTemplateRejected(GameTestHelper helper, CompoundTag tag, String message) {
        boolean rejected = false;
        try {
            // Bypass sender validation: malformed peers must be rejected by the receiving decoder itself.
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                NbtIo.write(tag, output);
            }
            CopyDataTransfer.decode(bytes.toByteArray());
        } catch (IOException exception) {
            rejected = true;
        }
        helper.assertTrue(rejected, message);
    }

    private static CompoundTag twoBlockTemplate() {
        ArrayList<StatePos> seed = new ArrayList<>();
        seed.add(new StatePos(Blocks.AIR.defaultBlockState(), BlockPos.ZERO));
        seed.add(new StatePos(Blocks.STONE.defaultBlockState(), new BlockPos(1, 0, 0)));
        return BG2Data.statePosListToNBTMapArray(seed);
    }

    private static CompoundTag millionBlockTemplate() {
        // Get the palette and version-specific position NBT from BG2 itself. Expanding only its index array
        // avoids both a huge world scan and the serializer's million-entry temporary position map.
        CompoundTag tag = twoBlockTemplate();
        ArrayList<StatePos> corner = new ArrayList<>();
        corner.add(new StatePos(Blocks.STONE.defaultBlockState(), new BlockPos(99, 99, 99)));
        tag.put("endpos", BG2Data.statePosListToNBTMapArray(corner).get("endpos"));
        int[] indices = new int[1_000_000];
        for (int index = 0; index < indices.length; index++) {
            indices[index] = index & 1;
        }
        tag.putIntArray("statelist", indices);
        return tag;
    }
    @GameTest(template = "bge_empty", timeoutTicks = 40)
    public static void arrayAndRadialUseServerSettingsAndRotateStates(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bge-modifiers"));
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool tool = (BuildersMultitool) stack.getItem();
        tool.selectTool(stack, MultitoolMode.BUILD);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        int[] settings = {1,0,0,0,4,0,0,0};
        helper.assertTrue(com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player, MultitoolMode.BUILD.ordinal(), settings), "server must accept valid modifier parameters");
        helper.assertTrue(!com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player, MultitoolMode.EXCHANGING.ordinal(), settings), "stale profile request must be rejected");
        helper.assertTrue(!com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player, MultitoolMode.BUILD.ordinal(), new int[]{17,0,0,0,1,0,0,0}), "hostile copy count must be rejected");
        var sourceState = Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST);
        var mode = new com.direwolf20.buildinggadgets2.util.modes.BaseMode(false) {
            public net.minecraft.resources.ResourceLocation getId() { return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("buildinggadgetsextra", "modifier_test"); }
            public java.util.ArrayList<com.direwolf20.buildinggadgets2.util.datatypes.StatePos> collectWorld(Direction side, net.minecraft.world.entity.player.Player actor, BlockPos start, net.minecraft.world.level.block.state.BlockState state) {
                return new java.util.ArrayList<>(java.util.List.of(new com.direwolf20.buildinggadgets2.util.datatypes.StatePos(state,new BlockPos(2,1,0))));
            }
            public boolean isPosValid(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player actor, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { return true; }
        };
        var start = helper.absolutePos(new BlockPos(3,1,3));
        var plan = mode.collect(Direction.UP,player,start,sourceState);
        helper.assertTrue(plan.size()==4,"radial must produce four candidates");
        helper.assertTrue(plan.stream().anyMatch(e->e.pos.equals(new BlockPos(0,1,2)) && e.state.getValue(net.minecraft.world.level.block.StairBlock.FACING)==Direction.SOUTH),"radial state and position must rotate together");
        helper.assertTrue(plan.stream().anyMatch(e->e.pos.equals(new BlockPos(-2,1,0)) && e.state.getValue(net.minecraft.world.level.block.StairBlock.FACING)==Direction.WEST),"half-turn must preserve stair orientation");
        tool.selectTool(stack,MultitoolMode.EXCHANGING);
        helper.assertTrue(!MultitoolState.getLiveModifiers(stack).enabled(),"modifier settings must not leak between profiles");
        tool.selectTool(stack,MultitoolMode.BUILD);
        helper.assertTrue(MultitoolState.getLiveModifiers(stack).value(4)==4,"profile switching must restore modifiers");
        helper.assertTrue(com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player,MultitoolMode.BUILD.ordinal(),new int[]{3,-2,1,0,1,0,0,0}),"array settings must apply");
        plan=mode.collect(Direction.UP,player,start,sourceState);
        helper.assertTrue(plan.size()==3 && plan.stream().anyMatch(e->e.pos.equals(new BlockPos(-2,3,0))),"array must translate on all selected axes");
        ItemStack nativeTool = new ItemStack(com.direwolf20.buildinggadgets2.setup.Registration.Building_Gadget.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,nativeTool);
        helper.assertTrue(!com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player,MultitoolMode.BUILD.ordinal(),settings),"native gadgets must reject addon modifier settings");
        helper.assertTrue(mode.collect(Direction.UP,player,start,sourceState).size()==1,"native gadget plan must remain unchanged");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        helper.succeed();
    }
}
