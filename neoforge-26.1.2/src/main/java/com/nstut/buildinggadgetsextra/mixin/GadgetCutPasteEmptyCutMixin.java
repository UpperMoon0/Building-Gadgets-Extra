package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GadgetCutPaste.class)
public abstract class GadgetCutPasteEmptyCutMixin {
    @Inject(
            method = "cutAndStore",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/direwolf20/buildinggadgets2/common/events/ServerTickHandler;setCutStart(Ljava/util/UUID;Lnet/minecraft/core/BlockPos;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void buildingGadgetsExtra$abortEmptyCut(Player player, ItemStack gadget, CallbackInfo ci) {
        if (!ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(gadget))) {
            player.sendOverlayMessage(Component.translatable(ExtraConstants.CUT_NO_VALID_BLOCKS));
            ci.cancel();
        }
    }
}
