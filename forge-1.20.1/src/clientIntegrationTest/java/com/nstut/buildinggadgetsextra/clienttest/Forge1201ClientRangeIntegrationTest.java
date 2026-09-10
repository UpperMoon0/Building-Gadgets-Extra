package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1201ClientRangeIntegrationTest {
    private static final long BOOT_TIMEOUT_NANOS = 120_000_000_000L;
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final boolean DEDICATED = Boolean.getBoolean("bge.clientIntegrationDedicated");
    private static final ModernClientRangeAdapter ADAPTER = new ModernClientRangeAdapter(
            (screen, x, y) -> screen.mouseClicked(x, y, 0));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static final long BOOT_STARTED_NANOS = System.nanoTime();

    private Forge1201ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (System.nanoTime() - BOOT_STARTED_NANOS > BOOT_TIMEOUT_NANOS) {
                ADAPTER.fail("timeout waiting for " + (DEDICATED ? "dedicated server connection" : "quick-play integration world"), null);
            }
            return;
        }

        SCENARIO.tick();
    }
}
