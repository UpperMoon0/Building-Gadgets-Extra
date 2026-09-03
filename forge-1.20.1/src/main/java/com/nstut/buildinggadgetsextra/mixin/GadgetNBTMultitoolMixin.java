package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GadgetNBT.class, remap = false)
public abstract class GadgetNBTMultitoolMixin {
    @Inject(method = "getMode", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$activeProfileMode(ItemStack stack,
                                                                CallbackInfoReturnable<com.direwolf20.buildinggadgets2.util.modes.BaseMode> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;

        MultitoolMode active = MultitoolState.getActiveMode(stack);
        GadgetTarget target = active == MultitoolMode.DESTRUCTION
                ? GadgetTarget.COPYPASTE : BuildersMultitool.target(active);
        String stored = stack.getOrCreateTag().getString("mode");
        String wanted = stored.isEmpty() ? defaultMode(active) : stored;
        GadgetModes.INSTANCE.getModesForGadget(target).stream()
                .filter(mode -> mode.getId().toString().equals(wanted) || mode.getId().getPath().equals(wanted))
                .findFirst()
                .or(() -> GadgetModes.INSTANCE.getModesForGadget(target).stream()
                        .filter(mode -> mode.getId().getPath().equals(defaultMode(active)))
                        .findFirst())
                .or(() -> GadgetModes.INSTANCE.getModesForGadget(target).stream().findFirst())
                .ifPresent(cir::setReturnValue);
    }

    @Inject(method = "getPasteReplace", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$nativeCutReplaceDefault(ItemStack stack,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE) return;
        if (stack.getTag() == null || !stack.getTag().contains("pastereplace")) {
            stack.getOrCreateTag().putBoolean("pastereplace", true);
            cir.setReturnValue(true);
        }
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
