package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1201ClientRangeIntegrationTest {
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ClientRangeRoundTripScenario SCENARIO =
            new ClientRangeRoundTripScenario(new ModernClientRangeAdapter());

    private Forge1201ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        SCENARIO.tick();
    }
}
