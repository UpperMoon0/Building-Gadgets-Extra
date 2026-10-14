package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.network.PacketHandler;
import com.direwolf20.buildinggadgets2.common.network.packets.PacketSendCopyData;
import com.nstut.buildinggadgetsextra.network.CopyDataTransfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercept before Forge constructs its size-limited custom payload packet. */
@Mixin(value = PacketHandler.class, remap = false)
public abstract class CopyDataSendMixin {
    @Inject(method = "sendTo", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$chunkCopyData(Object message, ServerPlayer player, CallbackInfo callback) {
        if (!(message instanceof PacketSendCopyData)) return;
        if (!(player instanceof FakePlayer)) {
            PacketSendCopyDataAccessor packet = (PacketSendCopyDataAccessor) message;
            CopyDataTransfer.send(player, packet.buildingGadgetsExtra$getGadgetId(),
                    packet.buildingGadgetsExtra$getCopyId(), packet.buildingGadgetsExtra$getTag());
        }
        callback.cancel();
    }
}
