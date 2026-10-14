package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.nstut.buildinggadgetsextra.transform.LegacyLivePlan;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractMode.class,remap=false)
public abstract class LegacyModeModifiersMixin {
    @Inject(method="getCollection",at=@At("RETURN"),cancellable=true)
    private void bge$plan(AbstractMode.UseContext context, PlayerEntity player, CallbackInfoReturnable<List<BlockPos>> cir) {
        cir.setReturnValue(LegacyLivePlan.apply((AbstractMode)(Object)this,context,player,cir.getReturnValue()));
    }
}
