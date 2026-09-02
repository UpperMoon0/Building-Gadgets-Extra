package com.nstut.buildinggadgetsextra.mixin;

import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = MultitoolRadialScreen.class, remap = false)
public abstract class MultitoolRadialScreenRangeMixin {
    @ModifyConstant(method = "rebuildContextButtons", constant = @Constant(intValue = 15), require = 1)
    private int buildingGadgetsExtra$multitoolMaxRange(int original) {
        return ExtraConfig.multitoolMaxRange();
    }
}
