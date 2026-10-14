package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public final class ProfileIconButton extends GuiIconActionable {
    private final ResourceLocation icon;
    private final int sourceSize;

    private final boolean selectable;
    private final java.util.function.Predicate<Boolean> action;
    public ProfileIconButton(int x,int y,String iconName,ITextComponent tooltip,boolean selectable,java.util.function.Predicate<Boolean> action) {
        super(x,y,iconName,tooltip,selectable,action);
        this.icon=new ResourceLocation("buildinggadgets", "textures/gui/setting/"+iconName+".png");
        this.sourceSize=RadialIconLayout.SOURCE_TEXTURE_SIZE;
        this.selectable=selectable; this.action=action;
        this.width=24; this.height=24;
    }
    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean hover=mouseX>=x && mouseX<x+width && mouseY>=y && mouseY<y+height;
        int edge=active?(selectable && action.test(false)?ProfileUiStyle.SELECTED:hover?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER):ProfileUiStyle.BORDER;
        net.minecraft.client.gui.AbstractGui.fill(poseStack,x,y,x+width,y+height,ProfileUiStyle.CONTROL);
        net.minecraft.client.gui.AbstractGui.fill(poseStack,x,y,x+width,y+1,edge); net.minecraft.client.gui.AbstractGui.fill(poseStack,x,y+height-1,x+width,y+height,edge);
        net.minecraft.client.gui.AbstractGui.fill(poseStack,x,y,x+1,y+height,edge); net.minecraft.client.gui.AbstractGui.fill(poseStack,x+width-1,y,x+width,y+height,edge);
        RenderSystem.enableBlend();
        RenderSystem.color4f(1, 1, 1, alpha);
        minecraft.getTextureManager().bind(icon);
        blit(poseStack, x+4, y+4, width-8, height-8,
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
