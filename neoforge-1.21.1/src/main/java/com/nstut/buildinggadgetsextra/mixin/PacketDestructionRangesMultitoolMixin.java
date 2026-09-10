package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.data.DestructionRangesPayload;
import com.direwolf20.buildinggadgets2.common.network.handler.PacketDestructionRanges;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketDestructionRanges.class, remap = false)
public abstract class PacketDestructionRangesMultitoolMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$destruction(DestructionRangesPayload payload, IPayloadContext context, CallbackInfo ci) {
        if (!(BaseGadget.getGadget(context.player()).getItem() instanceof BuildersMultitool)) return;
        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof BuildersMultitool)
                    || MultitoolState.getActiveMode(stack) != MultitoolMode.DESTRUCTION) return;
            if (!MultitoolRangePolicy.validDestruction(payload.left(), payload.right(), payload.up(), payload.down(), payload.depth(),
                    ExtraConfig.multitoolDestructionSide(), ExtraConfig.multitoolDestructionSpan())) return;
            GadgetNBT.setToolValue(stack, payload.left(), "left");
            GadgetNBT.setToolValue(stack, payload.right(), "right");
            GadgetNBT.setToolValue(stack, payload.up(), "up");
            GadgetNBT.setToolValue(stack, payload.down(), "down");
            GadgetNBT.setToolValue(stack, payload.depth(), "depth");
            if (context.player() instanceof ServerPlayer player) player.containerMenu.broadcastChanges();
        });
        ci.cancel();
    }
}

