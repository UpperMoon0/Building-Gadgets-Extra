package com.nstut.buildinggadgetsextra.network;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.CopyDataAssembly;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.CopyDataPayload;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Runtime coverage for the 26.1 data-driven GameTest framework. */
public final class BgeGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ExtraConstants.MOD_ID);
    private static final List<Spec> SPECS = new ArrayList<>();

    private record Spec(ResourceKey<Consumer<GameTestHelper>> function,
                        Identifier structure, Rotation rotation, int maxTicks) {}

    private BgeGameTests() {}

    public static void register(IEventBus modEventBus) {
        test("multitool_energy_and_profile_state", 40, BgeGameTests::energyAndProfileState);
        test("multitool_cut_queues_real_server_work", 40, BgeGameTests::cutQueuesRealServerWork);
        test("multitool_creative_zero_energy", 40, BgeGameTests::creativeZeroEnergy);
        test("configurable_copy_volume", 40, BgeGameTests::configurableCopyVolume);
        test("million_block_copy_network_round_trip", 100, BgeGameTests::millionBlockCopyNetworkRoundTrip);
        test("copy_network_rejects_malformed_data", 40, BgeGameTests::copyNetworkRejectsMalformedData);
        test("live_mirror_collect_preserves_states_and_profiles", 40, BgeGameTests::liveMirrorCollectPreservesStatesAndProfiles, "bge_live_mirror");
        test("array_and_radial_modifiers", 40, BgeGameTests::arrayAndRadial);
        FUNCTIONS.register(modEventBus);
        modEventBus.addListener(BgeGameTests::onRegisterGameTests);
    }

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        test(name, maxTicks, body, "bge_empty");
    }

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> body, String structure) {
        DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> holder =
                FUNCTIONS.register(name, () -> body);
        SPECS.add(new Spec(holder.getKey(),
                Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, structure),
                Rotation.NONE, maxTicks));
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "default"));
        for (Spec spec : SPECS) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    environment, spec.structure(), spec.maxTicks(),
                    0, true, spec.rotation(), false, 1, 1, false, 1);
            event.registerTest(spec.function().identifier(),
                    new FunctionGameTestInstance(spec.function(), data));
        }
    }

    private static void energyAndProfileState(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        EnergyHandler energy = stack.getCapability(Capabilities.Energy.ITEM, null);
        helper.assertTrue(energy != null,
                "Builder's Multitool must expose Capabilities.Energy.ITEM on 26.1.2");

        int inserted;
        try (Transaction transaction = Transaction.openRoot()) {
            inserted = energy.insert(multitool.getEnergyMax(), transaction);
            transaction.commit();
        }
        helper.assertTrue(inserted > 0, "multitool energy capability must accept FE");

        GadgetNBT.setToolRange(stack, 7);
        GadgetNBT.setGadgetBlockState(stack, Blocks.STONE.defaultBlockState());
        int beforeBuild = energy.getAmountAsInt();
        BuildingUtils.useEnergy(stack);
        helper.assertTrue(energy.getAmountAsInt() == beforeBuild - Config.BUILDINGGADGET_COST.get(),
                "Build profile must consume its configured FE cost");

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 1,
                "fresh Exchange profile must not inherit Build range");
        helper.assertTrue(GadgetNBT.getGadgetBlockState(stack).is(Blocks.AIR),
                "fresh Exchange profile must not inherit Build selected block");
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("surface"),
                "fresh Exchange profile must use BG2's native Surface default");
        GadgetNBT.setToolRange(stack, 3);
        GadgetNBT.setGadgetBlockState(stack, Blocks.DIRT.defaultBlockState());
        int beforeExchange = energy.getAmountAsInt();
        BuildingUtils.useEnergy(stack);
        helper.assertTrue(energy.getAmountAsInt() == beforeExchange - Config.EXCHANGINGGADGET_COST.get(),
                "Exchange profile must consume its configured FE cost");

        multitool.selectTool(stack, MultitoolMode.BUILD);
        helper.assertTrue(GadgetNBT.getToolRange(stack) == 7,
                "Build range must survive a real profile switch");
        helper.assertTrue(GadgetNBT.getGadgetBlockState(stack).is(Blocks.STONE),
                "Build selected block must survive a real profile switch");
        helper.succeed();
    }

    private static void cutQueuesRealServerWork(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        EnergyHandler energy = stack.getCapability(Capabilities.Energy.ITEM, null);
        helper.assertTrue(energy != null, "Cut test requires the multitool FE capability");
        try (Transaction transaction = Transaction.openRoot()) {
            energy.insert(multitool.getEnergyMax(), transaction);
            transaction.commit();
        }

        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        BaseMode cutMode = GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(MultitoolMode.CUT_PASTE))
                .stream().filter(mode -> mode.getId().getPath().equals("cut")).findFirst().orElse(null);
        helper.assertTrue(cutMode != null, "BG2 must expose the native Cut mode");
        GadgetNBT.setMode(stack, cutMode);

        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(relative);
        GadgetNBT.setCopyStartPos(stack, absolute);
        GadgetNBT.setCopyEndPos(stack, absolute);

        MultitoolCutHandler.cut(player);

        helper.assertTrue(GadgetNBT.hasCopyUUID(stack), "successful multitool Cut must create a paste operation UUID");
        UUID buildId = GadgetNBT.getCopyUUID(stack);
        helper.assertTrue(ServerTickHandler.buildMap.containsKey(buildId),
                "successful multitool Cut must queue the generated build UUID in ServerTickHandler");
        helper.assertTrue(ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(stack)),
                "queued multitool Cut must make the active virtual gadget busy");
        helper.assertTrue(GadgetNBT.getCopyStartPos(stack).equals(GadgetNBT.nullPos)
                        && GadgetNBT.getCopyEndPos(stack).equals(GadgetNBT.nullPos),
                "successful multitool Cut must clear the selection");
        helper.succeed();
    }

    private static void creativeZeroEnergy(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        EnergyHandler energy = stack.getCapability(Capabilities.Energy.ITEM, null);
        helper.assertTrue(energy != null && energy.getAmountAsInt() == 0,
                "creative regression must start with an empty multitool battery");

        multitool.selectTool(stack, MultitoolMode.BUILD);
        helper.assertTrue(GadgetNBT.getMode(stack).getId().getPath().equals("build_to_me"),
                "fresh Build profile must use BG2's native Build To Me default");
        GadgetNBT.setToolRange(stack, ExtraConfig.multitoolMaxRange() + 20);
        helper.assertTrue(GadgetNBT.getToolRange(stack) == ExtraConfig.multitoolMaxRange(),
                "multitool range reads must clamp stale item data to the server config");

        multitool.selectTool(stack, MultitoolMode.DESTRUCTION);
        BlockPos destroyRelative = new BlockPos(1, 1, 1);
        helper.setBlock(destroyRelative, Blocks.STONE);
        BlockPos destroyAbsolute = helper.absolutePos(destroyRelative);
        UUID destroyId = BuildingUtils.removeTickHandler(helper.getLevel(), player, List.of(destroyAbsolute),
                false, true, stack);
        helper.assertTrue(ServerTickHandler.buildMap.containsKey(destroyId),
                "creative Builder's Multitool with zero FE must queue real destruction work");

        multitool.selectTool(stack, MultitoolMode.CUT_PASTE);
        BaseMode cutMode = GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(MultitoolMode.CUT_PASTE))
                .stream().filter(mode -> mode.getId().getPath().equals("cut")).findFirst().orElse(null);
        helper.assertTrue(cutMode != null, "BG2 must expose the native Cut mode");
        GadgetNBT.setMode(stack, cutMode);
        BlockPos cutRelative = new BlockPos(2, 1, 1);
        helper.setBlock(cutRelative, Blocks.STONE);
        BlockPos cutAbsolute = helper.absolutePos(cutRelative);
        GadgetNBT.setCopyStartPos(stack, cutAbsolute);
        GadgetNBT.setCopyEndPos(stack, cutAbsolute);
        MultitoolCutHandler.cut(player);
        helper.assertTrue(GadgetNBT.hasCopyUUID(stack),
                "creative Builder's Multitool with zero FE must execute Cut");
        helper.succeed();
    }

    private static void configurableCopyVolume(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
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
                var area = com.direwolf20.buildinggadgets2.util.VecHelpers.aabbFromBlockPos(start, GadgetNBT.getCopyEndPos(stack));
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

    private static void millionBlockCopyNetworkRoundTrip(GameTestHelper helper) {
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
                CopyDataPayload sent = new CopyDataPayload(transferId, gadgetId, copyId, index, total, chunk);
                RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
                try {
                    CopyDataPayload.STREAM_CODEC.encode(wire, sent);
                    helper.assertTrue(wire.readableBytes() < 1024 * 1024,
                            "every encoded fragment must fit the vanilla clientbound payload limit");
                    var received = CopyDataPayload.STREAM_CODEC.decode(wire);
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

    private static void copyNetworkRejectsMalformedData(GameTestHelper helper) {
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

        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
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
                CopyDataPayload.STREAM_CODEC.decode(wire);
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
    public static void liveMirrorCollectPreservesStatesAndProfiles(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
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
    private static void arrayAndRadial(GameTestHelper helper) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"bge-modifiers"));
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
            public net.minecraft.resources.Identifier getId() { return net.minecraft.resources.Identifier.fromNamespaceAndPath("buildinggadgetsextra", "modifier_test"); }
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
