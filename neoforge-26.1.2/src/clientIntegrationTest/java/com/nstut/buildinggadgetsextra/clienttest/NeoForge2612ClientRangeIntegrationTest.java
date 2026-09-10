package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class NeoForge2612ClientRangeIntegrationTest {
    private static final long BOOT_TIMEOUT_NANOS = 120_000_000_000L;
    private static final String DEDICATED_ADDRESS = "127.0.0.1:25565";
    private static final String INTEGRATION_WORLD = "bge-client-integration";
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final boolean DEDICATED = Boolean.getBoolean("bge.clientIntegrationDedicated");
    private static final ModernClientRangeAdapter ADAPTER = new ModernClientRangeAdapter((screen, x, y) ->
            screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static final long BOOT_STARTED_NANOS = System.nanoTime();
    private static boolean startupRequested;

    private NeoForge2612ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (!startupRequested && minecraft.screen != null) {
                startupRequested = true;
                if (DEDICATED) {
                    ServerAddress address = ServerAddress.parseString(DEDICATED_ADDRESS);
                    ServerData server = new ServerData("BGE integration", DEDICATED_ADDRESS, ServerData.Type.OTHER);
                    ConnectScreen.startConnecting(minecraft.screen, minecraft, address, server, true, null);
                } else if (minecraft.getLevelSource().levelExists(INTEGRATION_WORLD)) {
                    minecraft.createWorldOpenFlows().openWorld(INTEGRATION_WORLD,
                            () -> ADAPTER.fail("prepared integration world open was cancelled", null));
                } else {
                    ADAPTER.fail("prepared integration world is missing: " + INTEGRATION_WORLD, null);
                }
            }
            if (System.nanoTime() - BOOT_STARTED_NANOS > BOOT_TIMEOUT_NANOS) {
                ADAPTER.fail("timeout waiting for " + (DEDICATED ? "dedicated server connection" : "prepared integration world"), null);
            }
            return;
        }

        SCENARIO.tick();
    }
}
