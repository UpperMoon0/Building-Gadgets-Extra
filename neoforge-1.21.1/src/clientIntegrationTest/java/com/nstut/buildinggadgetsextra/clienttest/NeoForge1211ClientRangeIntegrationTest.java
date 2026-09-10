package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class NeoForge1211ClientRangeIntegrationTest {
    private static final int BOOT_TIMEOUT_TICKS = 600;
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final boolean DEDICATED = Boolean.getBoolean("bge.clientIntegrationDedicated");
    private static final ModernClientRangeAdapter ADAPTER =
            new ModernClientRangeAdapter((screen, x, y) -> screen.mouseClicked(x, y, 0));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static int bootTicks;

    private NeoForge1211ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (++bootTicks > BOOT_TIMEOUT_TICKS) {
                ADAPTER.fail("timeout waiting for " + (DEDICATED ? "dedicated server connection" : "quick-play integration world"), null);
            }
            return;
        }
        SCENARIO.tick();
    }
}
