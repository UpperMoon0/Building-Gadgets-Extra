package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetTarget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GadgetNBT.class, remap = false)
public abstract class GadgetNBTMultitoolMixin {
    @Redirect(method = "getMode", at = @At(value = "INVOKE",
            target = "Lcom/direwolf20/buildinggadgets2/common/items/BaseGadget;gadgetTarget()Lcom/direwolf20/buildinggadgets2/api/gadgets/GadgetTarget;"))
    private static GadgetTarget buildingGadgetsExtra$activeTarget(BaseGadget gadget, ItemStack stack) {
        if (!(gadget instanceof BuildersMultitool multitool)) return gadget.gadgetTarget();
        // GadgetModes intentionally has an empty DESTRUCTION set. BG2's generic
        // preview renderer still calls GadgetNBT.getMode for our wrapper, so use
        // its concrete CopyPaste fallback there and keep destruction settings in
        // the dedicated submenu/GUI.
        return MultitoolState.getActiveMode(stack) == MultitoolMode.DESTRUCTION
                ? GadgetTarget.COPYPASTE
                : multitool.target(stack);
    }
}


