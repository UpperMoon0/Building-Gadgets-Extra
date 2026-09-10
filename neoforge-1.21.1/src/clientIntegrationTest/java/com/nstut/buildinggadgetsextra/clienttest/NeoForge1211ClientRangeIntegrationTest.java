package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class NeoForge1211ClientRangeIntegrationTest {
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ModernClientRangeAdapter ADAPTER =
            new ModernClientRangeAdapter((screen, x, y) -> screen.mouseClicked(x, y, 0));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static boolean worldOpenRequested;
    private static int bootTicks;

    private NeoForge1211ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (++bootTicks > 600) {
                ADAPTER.fail("timeout waiting for integration world/server", null);
                return;
            }
            if (!Boolean.getBoolean("bge.clientIntegrationDedicated") && !worldOpenRequested && minecraft.screen != null) {
                worldOpenRequested = true;
                minecraft.createWorldOpenFlows().openWorld("bge-client-integration",
                        () -> ADAPTER.fail("opening integration world was cancelled", null));
            }
            return;
        }
        SCENARIO.tick();
    }
}
