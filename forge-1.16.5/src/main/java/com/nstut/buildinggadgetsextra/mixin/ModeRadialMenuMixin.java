package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.client.screen.ModeRadialMenu;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.nstut.buildinggadgetsextra.client.MirrorIconButton;
import com.nstut.buildinggadgetsextra.client.StructureLibraryButton;
import com.nstut.buildinggadgetsextra.client.StructureLibraryScreen;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.MirrorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModeRadialMenu.class, remap = false)
public abstract class ModeRadialMenuMixin extends Screen {
    protected ModeRadialMenuMixin(ITextComponent title) {
        super(title);
    }

    @Inject(method = {"init", "func_231160_c_"}, at = @At("TAIL"))
    private void buildingGadgetsExtra$addVerticalMirror(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        ItemStack gadget = AbstractGadget.getGadget(minecraft.player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)) return;

        Widget mirrorButton = this.buttons.stream()
                .filter(widget -> widget.getMessage().getString().equals(
                        new TranslationTextComponent("buildinggadgets.radialmenu.mirror").getString()))
                .findFirst().orElse(null);
        if (mirrorButton == null) return;

        this.addButton(new MirrorIconButton(mirrorButton.x - 34, mirrorButton.y,
                "mirror_vertical", new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
        this.addButton(new StructureLibraryButton(mirrorButton.x - 34, mirrorButton.y + 34,
                new TranslationTextComponent(ExtraConstants.STRUCTURE_LIBRARY),
                () -> minecraft.setScreen(new StructureLibraryScreen(this))));
    }
}
