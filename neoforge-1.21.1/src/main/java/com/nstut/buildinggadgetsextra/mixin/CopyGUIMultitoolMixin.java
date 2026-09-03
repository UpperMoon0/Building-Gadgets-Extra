package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BG2 sends copy coordinates on every incrementer change. In relative mode that immediately folds
 * Start X/Y/Z into the stored origin and resets the visible Start fields to zero, which makes the
 * controls look non-editable. Building Gadgets 1 deferred those edits until Confirm; preserve that
 * interaction for the multitool without changing upstream gadgets.
 */
@Mixin(value = CopyGUI.class, remap = false)
public abstract class CopyGUIMultitoolMixin {
    @Shadow private ItemStack copyPasteTool;

    @Inject(method = "onChange", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$deferMultitoolCopyChanges(int value, CallbackInfo ci) {
        if (copyPasteTool.getItem() instanceof BuildersMultitool) {
            ci.cancel();
        }
    }
}
