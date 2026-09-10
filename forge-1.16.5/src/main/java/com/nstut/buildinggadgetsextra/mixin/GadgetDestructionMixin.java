package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.direwolf20.buildinggadgets.common.items.GadgetDestruction.class, remap = false)
public class GadgetDestructionMixin {
    @Inject(method = "getToolValue", at = @At("RETURN"), cancellable = true)
    private static void buildingGadgetsExtra$destructionRange(ItemStack stack, String name, CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        if (!name.equals("left") && !name.equals("right") && !name.equals("up")
                && !name.equals("down") && !name.equals("depth")) return;
        int max = ExtraConfig.multitoolDestructionSide();
        if (name.equals("right")) max = Math.min(max, ExtraConfig.multitoolDestructionSpan() - GadgetDestruction.getToolValue(stack, "left"));
        if (name.equals("down")) max = Math.min(max, ExtraConfig.multitoolDestructionSpan() - GadgetDestruction.getToolValue(stack, "up"));
        cir.setReturnValue(Math.max(0, Math.min(max, cir.getReturnValue())));
    }

    @Inject(method = "getGadget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void buildinggadgetsextra$allowMultitool(PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (stack.getItem() instanceof BuildersMultitool) {
            cir.setReturnValue(stack);
        }
    }
}
