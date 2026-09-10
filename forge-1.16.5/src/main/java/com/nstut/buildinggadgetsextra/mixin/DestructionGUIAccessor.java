package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.client.screen.DestructionGUI;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DestructionGUI.class, remap = false)
public interface DestructionGUIAccessor {
    @Accessor("destructionTool") ItemStack buildingGadgetsExtra$getTool();
}

