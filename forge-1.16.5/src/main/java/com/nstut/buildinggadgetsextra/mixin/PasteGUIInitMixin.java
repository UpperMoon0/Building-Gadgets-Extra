package com.nstut.buildinggadgetsextra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(value = com.direwolf20.buildinggadgets.client.screen.PasteGUI.class, remap = false)
public class PasteGUIInitMixin {
    @Inject(method = "init", at = @At("HEAD"), remap = false)
    private void clearFieldsBeforeInit(CallbackInfo ci) {
        try {
            Field fieldsField = this.getClass().getDeclaredField("fields");
            fieldsField.setAccessible(true);
            List<?> fields = (List<?>) fieldsField.get(this);
            if (fields != null) {
                fields.clear();
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
