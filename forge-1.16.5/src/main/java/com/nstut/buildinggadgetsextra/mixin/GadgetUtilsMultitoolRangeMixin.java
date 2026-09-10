package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GadgetUtils.class, remap = false)
public abstract class GadgetUtilsMultitoolRangeMixin {
    @Inject(method = "getToolRange", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$multitoolRange(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        int raw = stack.getOrCreateTag().getInt("range");
        cir.setReturnValue(MultitoolRangePolicy.clamp(raw, ExtraConfig.multitoolMaxRange()));
    }
}
