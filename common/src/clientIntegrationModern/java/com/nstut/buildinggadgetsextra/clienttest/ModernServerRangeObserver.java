package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Shared authoritative-server observer for BG2-based ports (1.20.1+). */
public final class ModernServerRangeObserver {
    private static ServerPlayer watchedPlayer;
    private static int ticks;
    private static boolean finished;

    private ModernServerRangeObserver() {}

    public static void setupAndWatch(ServerPlayer player) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        GadgetNBT.setToolRange(stack, ClientRangeRoundTripScenario.START_RANGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.containerMenu.broadcastChanges();
        watchedPlayer = player;
        ticks = 0;
        finished = false;
    }

    /** Must be called from the loader's normal post-server-tick event. */
    public static void tick() {
        if (finished || watchedPlayer == null) return;
        ticks++;

        ItemStack held = watchedPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof BuildersMultitool
                && GadgetNBT.getToolRange(held) == ClientRangeRoundTripScenario.TARGET_RANGE) {
            finished = true;
            write("server-pass.txt", "authoritative server range=" + GadgetNBT.getToolRange(held));
            return;
        }

        if (ticks > ClientRangeRoundTripScenario.TIMEOUT_TICKS) {
            finished = true;
            write("server-fail.txt", "server never observed authoritative range="
                    + ClientRangeRoundTripScenario.TARGET_RANGE + "; current=" + GadgetNBT.getToolRange(held));
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
