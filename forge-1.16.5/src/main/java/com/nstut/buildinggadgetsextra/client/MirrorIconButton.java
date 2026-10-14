package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public final class MirrorIconButton extends GuiIconActionable {
    private final ResourceLocation icon;
    private final int sourceSize;
    private boolean profile;
    public MirrorIconButton profileStyle() { profile=true; return this; }

    public MirrorIconButton(int x, int y, String iconName, ITextComponent tooltip, Runnable action) {
        this(x, y, iconName, RadialIconLayout.SOURCE_TEXTURE_SIZE, tooltip, action);
    }

    public MirrorIconButton(int x, int y, String iconName, int sourceSize,
                            ITextComponent tooltip, Runnable action) {
        super(x, y, "buildinggadgetsextra_placeholder", tooltip, false, send -> {
            if (send) action.run();
            return false;
        });
        this.icon = new ResourceLocation(ExtraConstants.MOD_ID, "textures/gui/setting/" + iconName + ".png");
        this.sourceSize = sourceSize;
        this.width = RadialIconLayout.BUTTON_SIZE;
        this.height = RadialIconLayout.BUTTON_SIZE;
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.color4f(1, 1, 1, .2f);
        fill(poseStack, x, y, x + width, y + height, profile?com.nstut.buildinggadgetsextra.common.ProfileUiStyle.CONTROL:RadialIconLayout.BACKGROUND_COLOR);
        RenderSystem.enableTexture();
        int pad=profile?4:0;
        if(profile) {
            int edge=(mouseX>=x && mouseX<x+width && mouseY>=y && mouseY<y+height)?0xFF3598FF:0xFF56616B;
            fill(poseStack,x,y,x+width,y+1,edge); fill(poseStack,x,y+height-1,x+width,y+height,edge);
            fill(poseStack,x,y,x+1,y+height,edge); fill(poseStack,x+width-1,y,x+width,y+height,edge);
        }
        RenderSystem.color4f(1, 1, 1, alpha);
        minecraft.getTextureManager().bind(icon);
        blit(poseStack, x+pad, y+pad, width-pad*2, height-pad*2,
                0, 0,
                sourceSize, sourceSize, sourceSize, sourceSize);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height) {
            String tooltip = getMessage().getString();
            int tooltipX = mouseX > minecraft.getWindow().getGuiScaledWidth() / 2
                    ? mouseX + 2 : mouseX - minecraft.font.width(tooltip);
            drawString(poseStack, minecraft.font, tooltip, tooltipX, mouseY - 10, 0xFFFFFFFF);
        }
    }
}
