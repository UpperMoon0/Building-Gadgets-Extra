package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.network.data.SendCopyDataPayload;
import com.mojang.logging.LogUtils;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.CopyDataPayload;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.UUID;

/** Covers copy, login/resync, template-manager and imported-template sends alike. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class CopyDataPacketMixin {
    @Shadow public abstract void send(Packet<?> packet);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void buildinggadgetsextra$sendCopyChunks(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ClientboundCustomPayloadPacket custom)
                || !(custom.payload() instanceof SendCopyDataPayload copy)) return;
        // Cancel even rejected templates rather than falling back to the unsafe upstream codec.
        ci.cancel();
        try {
            byte[] bytes = CopyDataTransfer.encode(copy.tag());
            UUID transferId = UUID.randomUUID();
            com.nstut.packetweave.api.ChunkedTransfer.send(bytes, ExtraConstants.STRUCTURE_CHUNK_SIZE,
                    (index, total, chunk) -> send(new ClientboundCustomPayloadPacket(new CopyDataPayload(
                            transferId, copy.gadgetUUID(), copy.copyUUID(), index, total, chunk))));
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("Rejected invalid or oversized Building Gadgets copy template", error);
        }
    }
}
