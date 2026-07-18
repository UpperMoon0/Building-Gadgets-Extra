package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.client.screen.ModeRadialMenu;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.nstut.buildinggadgetsextra.client.ClientStructureFiles;
import com.nstut.buildinggadgetsextra.client.MirrorIconButton;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialButtonPolicy;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.MirrorPacket;
import com.nstut.buildinggadgetsextra.network.CutSelectionPacket;
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
                .filter(widget -> widget.getMessage() instanceof TranslationTextComponent)
                .filter(widget -> "buildinggadgets.radialmenu.mirror".equals(
                        ((TranslationTextComponent) widget.getMessage()).getKey()))
                .findFirst().orElse(null);

        boolean pasteMode = GadgetCopyPaste.getToolMode(gadget) == GadgetCopyPaste.ToolMode.PASTE;
        String mode = pasteMode ? "paste" : "copy";
        int mirrorX = mirrorButton == null ? this.width / 2 - 94 : mirrorButton.x;
        int mirrorY = mirrorButton == null ? this.height / 2 - 29 : mirrorButton.y;
        boolean showMirrors = RadialButtonPolicy.showMirrorButtons(mode);
        if (mirrorButton != null) mirrorButton.visible = showMirrors;
        int addonX = mirrorX - RadialIconLayout.BUTTON_SPACING;
        int fileY = mirrorY;
        if (showMirrors) {
            this.addButton(new MirrorIconButton(
                    addonX, mirrorY,
                    "mirror_vertical", new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                    () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
            fileY += RadialIconLayout.BUTTON_SPACING;
        }

        RadialButtonPolicy.FileAction fileAction = RadialButtonPolicy.fileAction(false, mode);
        if (fileAction != RadialButtonPolicy.FileAction.NONE) {
            boolean load = fileAction == RadialButtonPolicy.FileAction.LOAD;
            this.addButton(new MirrorIconButton(
                    addonX, fileY,
                    load ? "load" : "save",
                    new TranslationTextComponent(load
                            ? ExtraConstants.LOAD_STRUCTURE : ExtraConstants.SAVE_STRUCTURE),
                    load ? ClientStructureFiles::chooseLoad : ClientStructureFiles::chooseSave));
            fileY += RadialIconLayout.BUTTON_SPACING;
        }

        if (RadialButtonPolicy.showLegacyCutAction(false, mode)) {
            this.addButton(new MirrorIconButton(
                    addonX, fileY, "cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                    new TranslationTextComponent(ExtraConstants.CUT_SELECTION),
                    () -> ExtraNetwork.sendToServer(new CutSelectionPacket())));
        }
    }
}
