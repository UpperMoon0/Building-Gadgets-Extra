package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiTextFieldBase;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Defer multitool copy-coordinate changes until Confirm and synchronize all valid typed coordinates before
 * BG2 reads or converts the incrementer values. Upstream gadgets retain BG2's native behavior.
 */
@Mixin(value = CopyGUI.class, remap = false)
public abstract class CopyGUIMultitoolMixin {
    @Shadow private ItemStack copyPasteTool;
    @Shadow private List<GuiIncrementer> fields;

    @Inject(method = "onChange", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$deferMultitoolCopyChanges(int value, CallbackInfo ci) {
        if (copyPasteTool.getItem() instanceof BuildersMultitool) {
            ci.cancel();
        }
    }

    // Both methods are owned by BG2, so the remap=false Mixin stays valid in production.
    // The mode switch calls updateTextFields immediately afterward and would otherwise
    // replace typed drafts with the incrementers' old cached integers.
    @Inject(method = "coordsModeSwitch", at = @At("HEAD"))
    private void buildingGadgetsExtra$syncBeforeModeSwitch(CallbackInfo ci) {
        buildingGadgetsExtra$syncAllValidCopyValues();
    }

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void buildingGadgetsExtra$syncBeforeConfirm(CallbackInfo ci) {
        buildingGadgetsExtra$syncAllValidCopyValues();
    }

    private void buildingGadgetsExtra$syncAllValidCopyValues() {
        if (!(copyPasteTool.getItem() instanceof BuildersMultitool)) return;
        for (GuiIncrementer incrementer : fields) {
            GuiTextFieldBase text = ((GuiIncrementerAccessor) (Object) incrementer).buildingGadgetsExtra$getField();
            String visible = text.getValue();
            if (visible == null || visible.isEmpty() || "-".equals(visible)) continue;
            // Numeric input is validated by BG2, but parse explicitly so incomplete drafts
            // cannot silently turn into zero. Keep previous values for invalid drafts.
            try {
                int value = Integer.parseInt(visible);
                if (value != incrementer.getValue()) incrementer.setValue(value, false);
            } catch (NumberFormatException ignored) {
                // Preserve the last committed value rather than corrupting the selection.
            }
        }
    }
}
