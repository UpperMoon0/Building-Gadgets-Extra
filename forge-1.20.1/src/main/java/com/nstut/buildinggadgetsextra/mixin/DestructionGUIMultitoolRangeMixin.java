package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.DestructionGUI;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = DestructionGUI.class, remap = false)
public abstract class DestructionGUIMultitoolRangeMixin {
    @Shadow @Final private ItemStack destructionGadget;

    @ModifyConstant(method = "createSlider", constant = @Constant(doubleValue = 16))
    private double buildingGadgetsExtra$slider(double original) {
        return destructionGadget.getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolDestructionSide() : original;
    }

    @ModifyConstant(method = "isWithinBounds", constant = @Constant(intValue = 16))
    private int buildingGadgetsExtra$depth(int original) {
        return destructionGadget.getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolDestructionSide() : original;
    }

}

