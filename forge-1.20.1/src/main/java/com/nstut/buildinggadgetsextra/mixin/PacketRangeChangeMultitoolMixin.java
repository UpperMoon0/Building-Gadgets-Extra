package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.packets.PacketRangeChange;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

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

        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(sender);
            if (!(stack.getItem() instanceof BuildersMultitool)) return;
            MultitoolMode mode = MultitoolState.getActiveMode(stack);
            if (mode != MultitoolMode.BUILD && mode != MultitoolMode.EXCHANGING) return;
            int requested = ((PacketRangeChangeAccessor) (Object) message).buildingGadgetsExtra$getRange();
            int range = MultitoolRangePolicy.clamp(requested, ExtraConfig.multitoolMaxRange());
            GadgetNBT.setToolRange(stack, range);
            sender.displayClientMessage(Component.translatable("buildinggadgets2.messages.range_set", range), true);
        });
        context.setPacketHandled(true);
        ci.cancel();
    }
}
