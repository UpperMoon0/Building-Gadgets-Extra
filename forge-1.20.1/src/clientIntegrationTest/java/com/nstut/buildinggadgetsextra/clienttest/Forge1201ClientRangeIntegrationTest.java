package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1201ClientRangeIntegrationTest {
    private static final String WORLD_NAME = "bge-client-integration";
    private static final int BOOT_TIMEOUT_TICKS = 600;
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ModernClientRangeAdapter ADAPTER = new ModernClientRangeAdapter(
            (screen, x, y) -> screen.mouseClicked(x, y, 0));
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(ADAPTER);
    private static boolean worldOpenRequested;
    private static int bootTicks;

    private Forge1201ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            if (++bootTicks > BOOT_TIMEOUT_TICKS) {
                ADAPTER.fail("timeout opening integrated test world " + WORLD_NAME, null);
                return;
            }
            if (!worldOpenRequested && minecraft.screen != null) {
                worldOpenRequested = true;
                System.out.println("[BGE client integration] opening integrated test world " + WORLD_NAME);
                minecraft.createWorldOpenFlows().loadLevel(minecraft.screen, WORLD_NAME);
            }
            return;
        }

        SCENARIO.tick();
    }
}
