package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraft.core.BlockPos;
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
    private static boolean buildRangeObserved;
    private static boolean exchangeRangeObserved;
    private static boolean copySelectionObserved;
    private static boolean destructionRangesObserved;

    private ModernServerRangeObserver() {}

    public static void setupAndWatch(ServerPlayer player) {
        ItemStack stack = new ItemStack(ExtraRegistration.BUILDERS_MULTITOOL.get());
        BuildersMultitool multitool = (BuildersMultitool) stack.getItem();

        // Seed distinct profiles so the client test can detect a submenu built from stale profile data.
        GadgetNBT.setToolRange(stack, ClientRangeRoundTripScenario.START_RANGE);

        multitool.selectTool(stack, MultitoolMode.EXCHANGING);
        GadgetNBT.setToolRange(stack, ClientRangeRoundTripScenario.EXCHANGE_INITIAL_RANGE);

        multitool.selectTool(stack, MultitoolMode.COPY_PASTE);
        GadgetNBT.setCopyStartPos(stack, BlockPos.ZERO);
        GadgetNBT.setCopyEndPos(stack, new BlockPos(2, 2, 2));

        multitool.selectTool(stack, MultitoolMode.DESTRUCTION);
        GadgetNBT.setToolValue(stack, 0, "left");
        GadgetNBT.setToolValue(stack, 0, "right");
        GadgetNBT.setToolValue(stack, 0, "up");
        GadgetNBT.setToolValue(stack, 0, "down");
        GadgetNBT.setToolValue(stack, 0, "depth");

        multitool.selectTool(stack, MultitoolMode.BUILD);

        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.containerMenu.broadcastChanges();
        watchedPlayer = player;
        ticks = 0;
        finished = false;
        buildRangeObserved = false;
        exchangeRangeObserved = false;
        copySelectionObserved = false;
        destructionRangesObserved = false;
    }

    /** Must be called from the loader's normal post-server-tick event. */
    public static void tick() {
        if (finished || watchedPlayer == null) return;
        ticks++;

        ItemStack held = watchedPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof BuildersMultitool) {
            MultitoolMode mode = MultitoolState.getActiveMode(held);
            if (mode == MultitoolMode.BUILD
                    && GadgetNBT.getToolRange(held) == ClientRangeRoundTripScenario.TARGET_RANGE) {
                buildRangeObserved = true;
            }
            if (mode == MultitoolMode.EXCHANGING
                    && GadgetNBT.getToolRange(held) == ClientRangeRoundTripScenario.EXCHANGE_TARGET_RANGE) {
                exchangeRangeObserved = true;
            }
            if (mode == MultitoolMode.COPY_PASTE
                    && GadgetNBT.getCopyStartPos(held).getX() == ClientRangeRoundTripScenario.COPY_START_X_TARGET) {
                copySelectionObserved = true;
            }
            if (mode == MultitoolMode.DESTRUCTION
                    && GadgetNBT.getToolValue(held, "left") == ClientRangeRoundTripScenario.DESTRUCTION_LEFT_TARGET
                    && GadgetNBT.getToolValue(held, "depth") == ClientRangeRoundTripScenario.DESTRUCTION_DEPTH_TARGET) {
                destructionRangesObserved = true;
            }
        }

        if (buildRangeObserved && exchangeRangeObserved && copySelectionObserved && destructionRangesObserved) {
            finished = true;
            write("server-pass.txt", "authoritative server observed build/exchange profiles, copy selection, and destruction ranges");
            return;
        }

        if (ticks > ClientRangeRoundTripScenario.TIMEOUT_TICKS) {
            finished = true;
            write("server-fail.txt", "server did not observe all UI mutations; build=" + buildRangeObserved
                    + " exchange=" + exchangeRangeObserved
                    + " copy=" + copySelectionObserved
                    + " destruction=" + destructionRangesObserved
                    + " mode=" + MultitoolState.getActiveMode(held)
                    + " range=" + GadgetNBT.getToolRange(held)
                    + " copyStart=" + GadgetNBT.getCopyStartPos(held)
                    + " left=" + GadgetNBT.getToolValue(held, "left")
                    + " depth=" + GadgetNBT.getToolValue(held, "depth"));
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
