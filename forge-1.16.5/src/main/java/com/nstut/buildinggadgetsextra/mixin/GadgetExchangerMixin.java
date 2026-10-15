package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.direwolf20.buildinggadgets.common.items.GadgetExchanger.class, remap = false)
public class GadgetExchangerMixin {
    @Inject(method = "getGadget", at = @At("HEAD"), cancellable = true, remap = false)
    private static void buildinggadgetsextra$allowMultitool(PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = AbstractGadget.getGadget(player);
        if (stack.getItem() instanceof BuildersMultitool) {
            cir.setReturnValue(stack);
        }
    }

    @org.spongepowered.asm.mixin.Shadow private void exchangeBlock(net.minecraft.world.server.ServerWorld world, net.minecraft.entity.player.ServerPlayerEntity player, com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex index, net.minecraft.util.math.BlockPos pos, com.direwolf20.buildinggadgets.common.tainted.building.BlockData data) { throw new AssertionError(); }
    @Inject(method="exchangeBlock",at=@At("HEAD"),cancellable=true)
    private void bge$state(net.minecraft.world.server.ServerWorld world, net.minecraft.entity.player.ServerPlayerEntity player, com.direwolf20.buildinggadgets.common.tainted.inventory.IItemIndex index, net.minecraft.util.math.BlockPos pos, com.direwolf20.buildinggadgets.common.tainted.building.BlockData data, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!(pos instanceof com.nstut.buildinggadgetsextra.transform.LegacyLivePlan.Position)) return;
        exchangeBlock(world, player, index, new net.minecraft.util.math.BlockPos(pos.getX(),pos.getY(),pos.getZ()), com.nstut.buildinggadgetsextra.transform.LegacyLivePlan.data(data,pos));
        ci.cancel();
    }
}
