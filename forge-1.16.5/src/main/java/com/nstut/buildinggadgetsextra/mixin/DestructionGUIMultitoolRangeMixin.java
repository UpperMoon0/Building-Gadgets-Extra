package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.client.screen.DestructionGUI;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = DestructionGUI.class, remap = false)
public abstract class DestructionGUIMultitoolRangeMixin {
    @Shadow @Final private ItemStack destructionTool;
    @ModifyVariable(method = "isWithinBounds", at = @At("STORE"), ordinal = 3)
    private int buildingGadgetsExtra$dimension(int original) {
        return destructionTool.getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolDestructionSpan() : original;
    }
}

