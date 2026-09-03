package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.MultitoolMenuState;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents submenu controls from being built from the previous server-authoritative tool profile. */
@Mixin(value = MultitoolRadialScreen.class, remap = false)
public abstract class MultitoolRadialScreenProfileSyncMixin {
    @Shadow @Final private MultitoolMenuState navigation;

    @Unique private boolean buildingGadgetsExtra$waitingForProfileSync;

    @Inject(method = "rebuildContextButtons", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$waitForSelectedProfile(CallbackInfo ci) {
        if (buildingGadgetsExtra$isProfilePending()) {
            buildingGadgetsExtra$waitingForProfileSync = true;
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$blockStaleProfileClicks(MouseButtonEvent event, boolean doubleClick,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (buildingGadgetsExtra$waitingForProfileSync) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void buildingGadgetsExtra$reopenAfterProfileSync(CallbackInfo ci) {
        if (!buildingGadgetsExtra$waitingForProfileSync) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack held = BaseGadget.getGadget(minecraft.player);
        if (held.isEmpty() || MultitoolState.getActiveMode(held) != navigation.selectedTool()) return;

        buildingGadgetsExtra$waitingForProfileSync = false;
        minecraft.setScreen(new MultitoolRadialScreen(held));
    }

    @Unique
    private boolean buildingGadgetsExtra$isProfilePending() {
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        ItemStack held = BaseGadget.getGadget(minecraft.player);
        return !held.isEmpty() && MultitoolState.getActiveMode(held) != navigation.selectedTool();
    }
}
