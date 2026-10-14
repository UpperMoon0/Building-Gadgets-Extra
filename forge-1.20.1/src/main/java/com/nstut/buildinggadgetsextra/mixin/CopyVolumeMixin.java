package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.util.modes.Copy;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keep upstream dense copy semantics and validation, replacing only its fixed volume cap. */
@Mixin(value = Copy.class, remap = false)
public abstract class CopyVolumeMixin {
    @ModifyConstant(method = "collectWorld", constant = @Constant(intValue = 100000), require = 1)
    private int buildingGadgetsExtra$configuredCopyVolume(int original) {
        return ExtraConfig.maxCopyVolume();
    }
}
