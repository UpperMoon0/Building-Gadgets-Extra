package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID)
public final class Forge1165ServerRangeIntegrationTest {
    private static final int PROVISION_DELAY_TICKS = 20;

    private static ServerPlayerEntity watchedPlayer;
    private static int loginTicks;
    private static int observationTicks;
    private static boolean provisioned;
    private static boolean finished;

    private Forge1165ServerRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

        watchedPlayer = (ServerPlayerEntity) event.getPlayer();
        loginTicks = 0;
        observationTicks = 0;
        provisioned = false;
        finished = false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)
                || event.phase != TickEvent.Phase.END || finished || watchedPlayer == null) return;

        if (!provisioned) {
            loginTicks++;
            if (loginTicks < PROVISION_DELAY_TICKS) return;
            try { verifyModifiers(watchedPlayer); }
            catch (RuntimeException failure) { finished = true; write("server-fail.txt", "legacy modifier regression: " + failure); return; }
            provision(watchedPlayer);
            provisioned = true;
            return;
        }

        observationTicks++;
        ItemStack held = watchedPlayer.getItemInHand(Hand.MAIN_HAND);
        if (held.getItem() instanceof BuildersMultitool
                && GadgetUtils.getToolRange(held) == ClientRangeRoundTripScenario.TARGET_RANGE) {
            finished = true;
            write("server-pass.txt", "authoritative server range=" + GadgetUtils.getToolRange(held));
            return;
        }

        if (observationTicks > ClientRangeRoundTripScenario.TIMEOUT_TICKS) {
            finished = true;
            write("server-fail.txt", "server never observed authoritative range="
                    + ClientRangeRoundTripScenario.TARGET_RANGE + "; current=" + GadgetUtils.getToolRange(held));
        }
    }

    private static void verifyModifiers(ServerPlayerEntity player) {
        ItemStack stack=new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        player.setItemInHand(Hand.MAIN_HAND,stack);
        com.nstut.buildinggadgetsextra.item.MultitoolState.selectTool(stack,com.nstut.buildinggadgetsextra.common.MultitoolMode.BUILD);
        int[] values={1,0,0,0,4,0,0,0};
        if(!com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player,0,values)) throw new IllegalStateException("valid settings rejected");
        if(com.nstut.buildinggadgetsextra.network.ModifierHandler.apply(player,0,new int[]{17,0,0,0,1,0,0,0})) throw new IllegalStateException("hostile settings accepted");
        com.direwolf20.buildinggadgets.common.items.modes.AbstractMode mode=com.direwolf20.buildinggadgets.common.items.modes.BuildingModes.VERTICAL_COLUMN.getMode();
        net.minecraft.util.math.BlockPos pivot=new net.minecraft.util.math.BlockPos(player.blockPosition().getX(),240,player.blockPosition().getZ());
        net.minecraft.block.BlockState state=net.minecraft.block.Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.block.StairsBlock.FACING,net.minecraft.util.Direction.EAST);
        com.direwolf20.buildinggadgets.common.items.modes.AbstractMode.UseContext context=new com.direwolf20.buildinggadgets.common.items.modes.AbstractMode.UseContext(player.level,state,pivot,stack,net.minecraft.util.Direction.UP,false,false);
        boolean previous=com.direwolf20.buildinggadgets.common.config.Config.GENERAL.allowOverwriteBlocks.get();
        try {
            com.direwolf20.buildinggadgets.common.config.Config.GENERAL.allowOverwriteBlocks.set(true);
            java.util.List<net.minecraft.util.math.BlockPos> plan=com.nstut.buildinggadgetsextra.transform.LegacyLivePlan.apply(mode,context,player,java.util.Collections.singletonList(pivot.offset(2,0,0)));
            if(plan.size()!=4) throw new IllegalStateException("radial count="+plan.size());
            net.minecraft.util.math.BlockPos rotated=plan.stream().filter(pos->pos.equals(pivot.offset(0,0,2))).findFirst().orElseThrow(()->new IllegalStateException("missing rotated position"));
            if(com.nstut.buildinggadgetsextra.transform.LegacyLivePlan.state(state,rotated).getValue(net.minecraft.block.StairsBlock.FACING)!=net.minecraft.util.Direction.SOUTH) throw new IllegalStateException("rotated state mismatch");
            GadgetUtils.setAnchor(stack,plan);
            java.util.List<net.minecraft.util.math.BlockPos> restored=GadgetUtils.getAnchor(stack).orElseThrow(()->new IllegalStateException("missing anchor"));
            if(restored.size()!=4 || com.nstut.buildinggadgetsextra.transform.LegacyLivePlan.state(state,restored.get(1)).getValue(net.minecraft.block.StairsBlock.FACING)!=net.minecraft.util.Direction.SOUTH) throw new IllegalStateException("anchor lost state provenance");
            com.nstut.buildinggadgetsextra.item.MultitoolState.selectTool(stack,com.nstut.buildinggadgetsextra.common.MultitoolMode.EXCHANGING);
            if(com.nstut.buildinggadgetsextra.item.MultitoolState.getLiveModifiers(stack).enabled()) throw new IllegalStateException("profile state leaked");
            com.nstut.buildinggadgetsextra.item.MultitoolState.selectTool(stack,com.nstut.buildinggadgetsextra.common.MultitoolMode.BUILD);
            if(com.nstut.buildinggadgetsextra.item.MultitoolState.getLiveModifiers(stack).value(4)!=4) throw new IllegalStateException("profile settings lost");
        } finally { com.direwolf20.buildinggadgets.common.config.Config.GENERAL.allowOverwriteBlocks.set(previous); }
    }

    private static void provision(ServerPlayerEntity player) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        GadgetUtils.setToolRange(stack, ClientRangeRoundTripScenario.START_RANGE);
        player.setItemInHand(Hand.MAIN_HAND, stack);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static void write(String file, String detail) {
        try {
            Path dir = Paths.get(System.getProperty("bge.clientIntegrationResultDir", "build/client-integration"));
            Files.createDirectories(dir);
            Files.write(dir.resolve(file), (detail + "\n").getBytes(StandardCharsets.UTF_8));
            System.out.println("[BGE client integration] " + detail);
        } catch (IOException error) {
            throw new IllegalStateException("cannot write client integration marker " + file, error);
        }
    }
}
