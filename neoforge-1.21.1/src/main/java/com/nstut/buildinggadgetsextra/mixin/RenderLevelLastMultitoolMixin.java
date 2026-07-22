package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.events.RenderLevelLast;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.client.MultitoolPreviewRenderer;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderLevelLast.class, remap = false)
public abstract class RenderLevelLastMultitoolMixin {
    @Inject(method = "renderWorldLastEvent", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$renderActiveTool(RenderLevelStageEvent event, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack held = BaseGadget.getGadget(player);
        if (!(held.getItem() instanceof BuildersMultitool)) return;
        MultitoolPreviewRenderer.render(event, player, held);
        ci.cancel();
    }
}
