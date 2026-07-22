package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.PasteGUI;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PasteGUI.class, remap = false)
public interface PasteGUIInvoker {
    @Invoker("<init>")
    static PasteGUI buildingGadgetsExtra$create(ItemStack stack) {
        throw new AssertionError();
    }
}
