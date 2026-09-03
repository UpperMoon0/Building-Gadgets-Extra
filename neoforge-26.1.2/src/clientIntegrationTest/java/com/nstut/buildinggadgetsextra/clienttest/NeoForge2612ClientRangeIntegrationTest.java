package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class NeoForge2612ClientRangeIntegrationTest {
    private static final String WORLD_NAME = "bge-client-integration";
    private static final int BOOT_TIMEOUT_TICKS = 600;
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final boolean DEDICATED = Boolean.getBoolean("bge.clientIntegrationDedicated");
    private static final ModernClientRangeAdapter ADAPTER = new ModernClientRangeAdapter((screen, x, y) ->
            screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static boolean worldOpenRequested;
    private static int bootTicks;

    private NeoForge2612ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (++bootTicks > BOOT_TIMEOUT_TICKS) {
                ADAPTER.fail("timeout waiting for " + (DEDICATED ? "dedicated server connection" : "integrated test world " + WORLD_NAME), null);
                return;
            }
            if (!DEDICATED && !worldOpenRequested && minecraft.screen != null) {
                worldOpenRequested = true;
                System.out.println("[BGE client integration] opening integrated test world " + WORLD_NAME);
                minecraft.createWorldOpenFlows().openWorld(WORLD_NAME,
                        () -> ADAPTER.fail("opening integrated test world was cancelled: " + WORLD_NAME, null));
            }
            return;
        }

        SCENARIO.tick();
    }
}
