package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.items.modes.BuildToMeMode;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BuildToMeMode.class, remap = false)
public abstract class BuildToMeMultitoolRangeMixin {
    @Redirect(method = "collect", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
    private double buildingGadgetsExtra$reach(double original, double distance,
            AbstractMode.UseContext context, PlayerEntity player, BlockPos start) {
        return Math.min(AbstractGadget.getGadget(player).getItem() instanceof BuildersMultitool
                ? ExtraConfig.multitoolReach(original) : original, distance);
    }
}

