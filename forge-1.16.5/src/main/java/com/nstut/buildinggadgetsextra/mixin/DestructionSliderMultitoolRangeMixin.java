package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets.client.screen.components.GuiSliderInt;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "com.direwolf20.buildinggadgets.client.screen.DestructionGUI$GuiDestructionSlider", remap = false)
public abstract class DestructionSliderMultitoolRangeMixin {
    @Shadow @Final private DestructionGUI this$0;
    @ModifyConstant(method = "<init>", constant = @Constant(doubleValue = 16))
    private static double buildingGadgetsExtra$slider(double original, DestructionGUI screen, int x, int y, String prefix, int current) {
        return ((DestructionGUIAccessor) screen).buildingGadgetsExtra$getTool().getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolDestructionSide() : original;
    }
    @ModifyConstant(method = "lambda$new$0", constant = @Constant(intValue = 16))
    private static int buildingGadgetsExtra$increment(int original, GuiSliderInt slider, Integer amount) {
        DestructionGUI screen = ((DestructionSliderMultitoolRangeMixin) (Object) slider).this$0;
        return ((DestructionGUIAccessor) screen).buildingGadgetsExtra$getTool().getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolDestructionSide() : original;
    }
}

