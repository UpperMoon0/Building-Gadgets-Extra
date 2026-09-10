package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.util.modes.BuildToMe;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = BuildToMe.class, remap = false)
public abstract class BuildToMeMultitoolRangeMixin {
    @ModifyConstant(method = "collectWorld", constant = @Constant(intValue = 32))
    private int buildingGadgetsExtra$reach(int original, Direction side, Player player, BlockPos start, BlockState state) {
        return BaseGadget.getGadget(player).getItem() instanceof BuildersMultitool
                ? (int) ExtraConfig.multitoolReach(Config.RAYTRACE_RANGE.get()) : original;
    }
}

