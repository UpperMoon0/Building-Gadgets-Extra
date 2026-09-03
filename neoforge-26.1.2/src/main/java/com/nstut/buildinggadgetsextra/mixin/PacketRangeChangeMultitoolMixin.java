package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.data.RangeChangePayload;
import com.direwolf20.buildinggadgets2.common.network.handler.PacketRangeChange;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(value = PacketRangeChange.class, remap = false)
public abstract class PacketRangeChangeMultitoolMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$handleMultitool(RangeChangePayload payload, IPayloadContext context,
                                                       CallbackInfo ci) {
        ItemStack held = BaseGadget.getGadget(context.player());
        if (!(held.getItem() instanceof BuildersMultitool)) return;
        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof BuildersMultitool)) return;
            OptionalInt range = MultitoolRangePolicy.resolve(
                    MultitoolState.getActiveMode(stack), payload.range(), ExtraConfig.multitoolMaxRange());
            if (!range.isPresent()) return;
            GadgetNBT.setToolRange(stack, range.getAsInt());
            context.player().sendOverlayMessage(Component.translatable(
                    "buildinggadgets2.messages.range_set", range.getAsInt()));
        });
        ci.cancel();
    }
}
