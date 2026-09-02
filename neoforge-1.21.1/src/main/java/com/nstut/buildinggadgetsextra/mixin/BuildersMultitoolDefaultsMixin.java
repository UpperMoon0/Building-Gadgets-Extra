package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BuildersMultitool.class, remap = false)
public abstract class BuildersMultitoolDefaultsMixin {
    @Inject(method = "selectTool", at = @At("TAIL"))
    private void buildingGadgetsExtra$nativeProfileDefault(ItemStack stack, MultitoolMode selected, CallbackInfo ci) {
        if (selected == MultitoolMode.DESTRUCTION || MultitoolState.getProfileMode(stack, selected) != null) return;
        String wanted = defaultMode(selected);
        GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(selected)).stream()
                .filter(mode -> mode.getId().getPath().equals(wanted))
                .findFirst()
                .ifPresent(mode -> {
                    GadgetNBT.setMode(stack, mode);
                    MultitoolState.setProfileMode(stack, selected, mode.getId());
                });
    }

    private static String defaultMode(MultitoolMode mode) {
        return switch (mode) {
            case BUILD -> "build_to_me";
            case EXCHANGING -> "surface";
            case COPY_PASTE -> "copy";
            case CUT_PASTE -> "cut";
            case DESTRUCTION -> "copy";
        };
    }
}
