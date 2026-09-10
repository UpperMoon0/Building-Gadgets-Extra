package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.VectorHelper;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = VectorHelper.class, remap = false)
public abstract class VectorHelperMultitoolRangeMixin {
    @ModifyVariable(method = "getLookingAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;",
            at = @At("STORE"), ordinal = 0)
    private static double buildingGadgetsExtra$reach(double original, Player player, ClipContext.Fluid fluid) {
        return BaseGadget.getGadget(player).getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolReach(original) : original;
    }
}

