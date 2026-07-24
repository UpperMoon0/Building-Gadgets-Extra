package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.direwolf20.buildinggadgets.common.items.GadgetExchanger", remap = false)
public class GadgetExchangerMixin {
    @Inject(method = "getGadget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void buildinggadgetsextra$allowMultitool(PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (stack.getItem() instanceof BuildersMultitool) {
            cir.setReturnValue(stack);
        }
    }
}
