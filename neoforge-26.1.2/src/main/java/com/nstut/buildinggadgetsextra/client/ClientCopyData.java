package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient;
import com.mojang.logging.LogUtils;
import com.nstut.buildinggadgetsextra.common.CopyDataAssembly;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.CopyDataPayload;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;

/** Only accessed on the client thread, including payload handlers. */
@EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class ClientCopyData {
    private static final CopyDataAssembly ASSEMBLY = new CopyDataAssembly();
    private static ClientLevel level;

    private ClientCopyData() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        updateWorld();
        ASSEMBLY.pruneExpired();
    }

    private static void updateWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            ASSEMBLY.clear();
            level = current;
        }
    }

    public static void receive(CopyDataPayload payload) {
        updateWorld();
        if (level == null) return;
        try {
            byte[] bytes = ASSEMBLY.accept(payload.transferId(), payload.gadgetId(), payload.copyId(),
                    payload.index(), payload.total(), payload.data());
            if (bytes == null) return;
            BG2DataClient.updateLookupFromNBT(payload.gadgetId(), payload.copyId(),
                    BG2Data.statePosListFromNBTMapArray(CopyDataTransfer.decode(bytes)));
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("Rejected invalid Building Gadgets copy transfer", error);
        }
    }
}
