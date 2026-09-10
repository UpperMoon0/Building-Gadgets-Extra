package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.setup.BG2DataComponents;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GadgetNBT.class, remap = false)
public abstract class GadgetNBTRangeMixin {
    @Inject(method = "getToolValue", at = @At("RETURN"), cancellable = true)
    private static void buildingGadgetsExtra$destructionRange(ItemStack stack, String name, CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        if (!name.equals("left") && !name.equals("right") && !name.equals("up")
                && !name.equals("down") && !name.equals("depth")) return;
        int max = ExtraConfig.multitoolDestructionSide();
        // Bound restored profiles too, including a server lowering its multiplier.
        if (name.equals("right")) max = Math.min(max, ExtraConfig.multitoolDestructionSpan() - GadgetNBT.getToolValue(stack, "left"));
        if (name.equals("down")) max = Math.min(max, ExtraConfig.multitoolDestructionSpan() - GadgetNBT.getToolValue(stack, "up"));
        cir.setReturnValue(Math.max(0, Math.min(max, cir.getReturnValue())));
    }

    @Inject(method = "getToolRange", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$multitoolRange(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        int raw = stack.getOrDefault(BG2DataComponents.GADGET_RANGE, 1);
        cir.setReturnValue(MultitoolRangePolicy.clamp(raw, ExtraConfig.multitoolMaxRange()));
    }
}
