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
    private static ServerPlayerEntity watchedPlayer;
    private static int ticks;
    private static boolean finished;

    private Forge1165ServerRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        GadgetUtils.setToolRange(stack, ClientRangeRoundTripScenario.START_RANGE);
        player.setItemInHand(Hand.MAIN_HAND, stack);
        player.containerMenu.broadcastChanges();
        watchedPlayer = player;
        ticks = 0;
        finished = false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)
                || event.phase != TickEvent.Phase.END || finished || watchedPlayer == null) return;
        ticks++;

        ItemStack held = watchedPlayer.getItemInHand(Hand.MAIN_HAND);
        if (held.getItem() instanceof BuildersMultitool
                && GadgetUtils.getToolRange(held) == ClientRangeRoundTripScenario.TARGET_RANGE) {
            finished = true;
            write("server-pass.txt", "authoritative server range=" + GadgetUtils.getToolRange(held));
            return;
        }

        if (ticks > ClientRangeRoundTripScenario.TIMEOUT_TICKS) {
            finished = true;
            write("server-fail.txt", "server never observed authoritative range="
                    + ClientRangeRoundTripScenario.TARGET_RANGE + "; current=" + GadgetUtils.getToolRange(held));
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
