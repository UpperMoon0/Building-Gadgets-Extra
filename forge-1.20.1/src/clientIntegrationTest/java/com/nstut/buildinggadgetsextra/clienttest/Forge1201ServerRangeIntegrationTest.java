package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID)
public final class Forge1201ServerRangeIntegrationTest {
    private Forge1201ServerRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ModernServerRangeObserver.setupAndWatch((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)
                || event.phase != TickEvent.Phase.END) return;
        ModernServerRangeObserver.tick();
    }
}
