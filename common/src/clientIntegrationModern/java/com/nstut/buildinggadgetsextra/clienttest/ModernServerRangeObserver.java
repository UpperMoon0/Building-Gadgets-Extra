package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared authoritative-server observer for BG2-based ports (1.20.1+). */
public final class ModernServerRangeObserver {
    private ModernServerRangeObserver() {}

    public static void setupAndWatch(ServerPlayer player) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        GadgetNBT.setToolRange(stack, ClientRangeRoundTripScenario.START_RANGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.containerMenu.broadcastChanges();

        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("test player has no server");
        Thread watcher = new Thread(() -> watch(server, player), "BGE client integration server observer");
        watcher.setDaemon(true);
        watcher.start();
    }

    private static void watch(MinecraftServer server, ServerPlayer player) {
        try {
            for (int i = 0; i < ClientRangeRoundTripScenario.TIMEOUT_TICKS; i++) {
                AtomicBoolean matched = new AtomicBoolean(false);
                CountDownLatch latch = new CountDownLatch(1);
                server.execute(() -> {
                    try {
                        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                        matched.set(held.getItem() instanceof BuildersMultitool
                                && GadgetNBT.getToolRange(held) == ClientRangeRoundTripScenario.TARGET_RANGE);
                        if (matched.get()) write("server-pass.txt", "authoritative server range=" + GadgetNBT.getToolRange(held));
                    } finally {
                        latch.countDown();
                    }
                });
                latch.await(2, TimeUnit.SECONDS);
                if (matched.get()) return;
                Thread.sleep(50L);
            }
            write("server-fail.txt", "server never observed authoritative range=" + ClientRangeRoundTripScenario.TARGET_RANGE);
        } catch (Throwable error) {
            error.printStackTrace();
            try {
                write("server-fail.txt", error.toString());
            } catch (IOException ignored) {
            }
        }
    }

    private static void write(String file, String detail) throws IOException {
        Path dir = Paths.get(System.getProperty("bge.clientIntegrationResultDir", "build/client-integration"));
        Files.createDirectories(dir);
        Files.write(dir.resolve(file), (detail + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("[BGE client integration] " + detail);
    }
}
