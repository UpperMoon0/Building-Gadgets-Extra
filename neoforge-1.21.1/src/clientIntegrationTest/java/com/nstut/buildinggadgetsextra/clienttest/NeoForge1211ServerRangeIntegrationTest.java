package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID)
public final class NeoForge1211ServerRangeIntegrationTest {
    private NeoForge1211ServerRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY)) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ModernServerRangeObserver.setupAndWatch((ServerPlayer) event.getEntity());
    }
}
