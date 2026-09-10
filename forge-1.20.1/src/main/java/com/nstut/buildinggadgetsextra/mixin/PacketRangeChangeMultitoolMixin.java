package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.packets.PacketRangeChange;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.network.MultitoolRangePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * BG2 1.0.8 rejects the Builder's Multitool because PacketRangeChange only accepts its
 * concrete Building/Exchanging gadget classes. Redirect multitool packets into our
 * server-authoritative range implementation instead.
 */
@Mixin(value = PacketRangeChange.class, remap = false)
public abstract class PacketRangeChangeMultitoolMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$handleMultitool(PacketRangeChange message,
                                                              Supplier<NetworkEvent.Context> supplier,
                                                              CallbackInfo ci) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) return;
        ItemStack held = BaseGadget.getGadget(sender);
        if (!(held.getItem() instanceof BuildersMultitool)) return;

        int requested = ((PacketRangeChangeAccessor) (Object) message).buildingGadgetsExtra$getRange();
        context.enqueueWork(() -> MultitoolRangePacket.apply(sender, requested, "bg2-range-packet"));
        context.setPacketHandled(true);
        ci.cancel();
    }
}
