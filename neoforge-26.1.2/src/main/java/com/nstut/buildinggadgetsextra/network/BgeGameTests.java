package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
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

import java.util.ArrayList;
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
        FUNCTIONS.register(modEventBus);
        modEventBus.addListener(BgeGameTests::onRegisterGameTests);
    }

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> holder =
                FUNCTIONS.register(name, () -> body);
        SPECS.add(new Spec(holder.getKey(),
                Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID, "bge_empty"),
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
        helper.assertTrue(ServerTickHandler.gadgetWorking(buildId),
                "successful multitool Cut must queue real ServerTickHandler work");
        helper.assertTrue(GadgetNBT.getCopyStartPos(stack).equals(GadgetNBT.nullPos)
                        && GadgetNBT.getCopyEndPos(stack).equals(GadgetNBT.nullPos),
                "successful multitool Cut must clear the selection");
        helper.succeed();
    }
}
