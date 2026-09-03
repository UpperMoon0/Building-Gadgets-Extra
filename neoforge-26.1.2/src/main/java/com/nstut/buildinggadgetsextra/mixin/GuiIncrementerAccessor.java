package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiTextFieldBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GuiIncrementer.class, remap = false)
public interface GuiIncrementerAccessor {
    @Accessor("field") GuiTextFieldBase buildingGadgetsExtra$getField();
}
