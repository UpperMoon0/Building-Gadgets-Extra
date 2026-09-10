package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GadgetNBT.class, remap = false)
public abstract class GadgetNBTMultitoolMixin {
    @Inject(method = "getMode", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$freshProfileDefault(ItemStack stack,
                                                                  CallbackInfoReturnable<com.direwolf20.buildinggadgets2.util.modes.BaseMode> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        if (!stack.getOrDefault(BG2DataComponents.GADGET_MODE, "").isEmpty()) return;

        MultitoolMode active = MultitoolState.getActiveMode(stack);
        GadgetTarget target = active == MultitoolMode.DESTRUCTION
                ? GadgetTarget.COPYPASTE : BuildersMultitool.target(active);
        String wanted = defaultMode(active);
        GadgetModes.INSTANCE.getModesForGadget(target).stream()
                .filter(mode -> mode.getId().getPath().equals(wanted))
                .findFirst()
                .ifPresent(cir::setReturnValue);
    }

    @Redirect(method = "getMode", at = @At(value = "INVOKE",
            target = "Lcom/direwolf20/buildinggadgets2/common/items/BaseGadget;gadgetTarget()Lcom/direwolf20/buildinggadgets2/api/gadgets/GadgetTarget;"))
    private static GadgetTarget buildingGadgetsExtra$activeTarget(BaseGadget gadget, ItemStack stack) {
        if (!(gadget instanceof BuildersMultitool multitool)) return gadget.gadgetTarget();
        return MultitoolState.getActiveMode(stack) == MultitoolMode.DESTRUCTION
                ? GadgetTarget.COPYPASTE
                : multitool.target(stack);
    }

    @Inject(method = "getPasteReplace", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$nativeCutReplaceDefault(ItemStack stack,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)
                || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE) return;
        var component = BG2DataComponents.SETTING_TOGGLES.get(GadgetNBT.ToggleableSettings.PASTE_REPLACE);
        if (!stack.has(component)) {
            GadgetNBT.toggleSetting(stack, GadgetNBT.ToggleableSettings.PASTE_REPLACE.getName());
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
