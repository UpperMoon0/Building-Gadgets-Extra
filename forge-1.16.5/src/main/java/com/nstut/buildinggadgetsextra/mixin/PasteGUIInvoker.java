package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.client.screen.PasteGUI;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.direwolf20.buildinggadgets.client.screen.PasteGUI", remap = false)
public interface PasteGUIInvoker {
    @Invoker("<init>")
    static PasteGUI buildingGadgetsExtra$create(ItemStack stack) {
        throw new AssertionError();
    }
}
