package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.direwolf20.buildinggadgets.common.util.helpers.VectorHelper;
import net.minecraft.util.math.RayTraceContext;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = VectorHelper.class, remap = false)
public abstract class VectorHelperMultitoolRangeMixin {
    @ModifyVariable(method = "getLookingAt(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/RayTraceContext$FluidMode;)Lnet/minecraft/util/math/BlockRayTraceResult;",
            at = @At("STORE"), ordinal = 0)
    private static double buildingGadgetsExtra$reach(double original, PlayerEntity player, RayTraceContext.FluidMode fluid) {
        return AbstractGadget.getGadget(player).getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolReach(original) : original;
    }
}

