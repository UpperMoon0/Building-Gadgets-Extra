package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient;
import com.nstut.buildinggadgetsextra.common.CopyDataAssembly;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.CopyDataPacket;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class ClientCopyData {
    private static final CopyDataAssembly ASSEMBLY = new CopyDataAssembly();
    private static Object level;
    private ClientCopyData() {}
    private static void refresh() {
        Object current = Minecraft.getInstance().level;
        if (current != level) { ASSEMBLY.clear(); level = current; }
        ASSEMBLY.pruneExpired();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) refresh();
    }
    public static void receive(CopyDataPacket packet) {
        refresh();
        if (level == null) return;
        byte[] bytes = ASSEMBLY.accept(packet.transferId(), packet.gadgetId(), packet.copyId(), packet.index(), packet.total(), packet.data());
        if (bytes == null) return;
        try {
            var states = BG2Data.statePosListFromNBTMapArray(CopyDataTransfer.decode(bytes));
            BG2DataClient.updateLookupFromNBT(packet.gadgetId(), packet.copyId(), states);
        } catch (java.io.IOException | RuntimeException invalid) {
            // Never replace an existing preview with incomplete or invalid data.
            com.mojang.logging.LogUtils.getLogger().warn("Rejected invalid copy preview transfer", invalid);
        }
    }
}
