package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiTextFieldBase;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Defer multitool copy-coordinate changes until Confirm and keep typed text synchronized with the
 * incrementer's integer value. Upstream gadgets retain BG2's native behavior.
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

    @Inject(method = "charTyped", at = @At("RETURN"))
    private void buildingGadgetsExtra$syncTypedCopyValue(CharacterEvent event,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(copyPasteTool.getItem() instanceof BuildersMultitool)) return;
        for (GuiIncrementer incrementer : fields) {
            GuiTextFieldBase text = ((GuiIncrementerAccessor) (Object) incrementer).buildingGadgetsExtra$getField();
            if (!text.isFocused()) continue;
            String visible = text.getValue();
            if (visible == null || visible.isEmpty() || "-".equals(visible)) continue;
            incrementer.setValue(text.getInt(), false);
        }
    }
}
