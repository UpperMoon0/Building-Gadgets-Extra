package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class NeoForge1211ClientRangeIntegrationTest {
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(
            new ModernClientRangeAdapter((screen, x, y) -> screen.mouseClicked(x, y, 0)));

    private NeoForge1211ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        SCENARIO.tick();
    }
}
