package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.ModeRadialMenu;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.nstut.buildinggadgetsextra.client.MirrorIconButton;
import com.nstut.buildinggadgetsextra.client.StructureLibraryButton;
import com.nstut.buildinggadgetsextra.client.StructureLibraryScreen;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.MirrorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModeRadialMenu.class)
public abstract class ModeRadialMenuMixin extends Screen {
    protected ModeRadialMenuMixin(Component title) {
        super(title);
    }

    // BG2 overrides Screen#init. Forge's production jar uses the SRG name while dev uses Mojmap.
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"), remap = false)
    private void buildingGadgetsExtra$addMirrorButtons(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        ItemStack gadget = BaseGadget.getGadget(minecraft.player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)
                && !(gadget.getItem() instanceof GadgetCutPaste)) return;

        AbstractWidget rotateButton = this.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget.getMessage().equals(
                        Component.translatable("buildinggadgets2.radialmenu.rotate")))
                .findFirst().orElse(null);
        if (rotateButton == null) return;

        int x = rotateButton.getX() - 34;
        int y = rotateButton.getY();
        this.addRenderableWidget(new MirrorIconButton(x, y, "mirror_horizontal",
                Component.translatable(ExtraConstants.MIRROR_HORIZONTAL),
                () -> ExtraNetwork.sendToServer(new MirrorPacket(false))));
        this.addRenderableWidget(new MirrorIconButton(x, y + 34, "mirror_vertical",
                Component.translatable(ExtraConstants.MIRROR_VERTICAL),
                () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
        this.addRenderableWidget(new StructureLibraryButton(x, y + 68,
                Component.translatable(ExtraConstants.STRUCTURE_LIBRARY),
                () -> minecraft.setScreen(new StructureLibraryScreen(this))));
    }
}
