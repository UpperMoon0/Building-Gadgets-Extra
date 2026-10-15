package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=AbstractMode.class,remap=false)
public interface LegacyModeAccessor {
    @Invoker("exchangingValidator") boolean bge$exchangingValidator(BlockPos pos, BlockState lookingAt, AbstractMode.UseContext context);
}
