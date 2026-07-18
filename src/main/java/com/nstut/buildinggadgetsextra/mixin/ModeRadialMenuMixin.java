package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.ModeRadialMenu;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.nstut.buildinggadgetsextra.client.MirrorIconButton;
import com.nstut.buildinggadgetsextra.network.MirrorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModeRadialMenu.class)
public abstract class ModeRadialMenuMixin extends Screen {
    protected ModeRadialMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void buildingGadgetsExtra$addMirrorButtons(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack gadget = BaseGadget.getGadget(minecraft.player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)
                && !(gadget.getItem() instanceof GadgetCutPaste)) {
            return;
        }

        AbstractWidget rotateButton = this.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().equals(
                        Component.translatable("buildinggadgets2.radialmenu.rotate")))
                .findFirst()
                .orElse(null);
        if (rotateButton == null) {
            return;
        }

        int x = rotateButton.getX() - 34;
        int y = rotateButton.getY();
        this.addRenderableWidget(new MirrorIconButton(
                x, y, "mirror_horizontal",
                Component.translatable("buildinggadgetsextra.radialmenu.mirror_horizontal"),
                () -> PacketDistributor.sendToServer(new MirrorPayload(false))));
        this.addRenderableWidget(new MirrorIconButton(
                x, y + 34, "mirror_vertical",
                Component.translatable("buildinggadgetsextra.radialmenu.mirror_vertical"),
                () -> PacketDistributor.sendToServer(new MirrorPayload(true))));
    }
}
