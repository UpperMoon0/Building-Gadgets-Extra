package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Matches Building Gadgets 2's radial-menu setting buttons with explicit texture ownership. */
public final class ProfileIconButton extends GuiIconActionable {
    private static final String BG2_MOD_ID = "buildinggadgets2";
    private final ResourceLocation icon;
    private final int sourceSize;

    private final boolean selectable;
    private final java.util.function.Predicate<Boolean> action;
    public ProfileIconButton(int x,int y,String iconName,Component tooltip,boolean selectable,java.util.function.Predicate<Boolean> action) {
        super(x,y,iconName,tooltip,selectable,action);
        this.icon=settingIcon("buildinggadgets2",iconName);
        this.sourceSize=RadialIconLayout.MODERN_SETTING_ICON_SIZE;
        this.selectable=selectable; this.action=action;
        setWidth(24); setHeight(24);
    }
    public static ResourceLocation settingIcon(String namespace, String iconName) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/setting/" + iconName + ".png");
    }

    public static ResourceLocation addonSettingIcon(String iconName) {
        return settingIcon(BuildingGadgetsExtra.MODID, iconName);
    }

    public static ResourceLocation upstreamSettingIcon(String iconName) {
        return settingIcon(BG2_MOD_ID, iconName);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean hover=mouseX>=getX() && mouseX<getX()+width && mouseY>=getY() && mouseY<getY()+height;
        int edge=active?(selectable && action.test(false)?ProfileUiStyle.SELECTED:hover?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER):ProfileUiStyle.BORDER;
        guiGraphics.fill(getX(),getY(),getX()+width,getY()+height,ProfileUiStyle.CONTROL);
        guiGraphics.fill(getX(),getY(),getX()+width,getY()+1,edge); guiGraphics.fill(getX(),getY()+height-1,getX()+width,getY()+height,edge);
        guiGraphics.fill(getX(),getY(),getX()+1,getY()+height,edge); guiGraphics.fill(getX()+width-1,getY(),getX()+width,getY()+height,edge);
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, this.alpha);
        guiGraphics.blit(icon, getX()+4, getY()+4, width-8, height-8,
                0, 0,
                sourceSize, sourceSize,
                sourceSize, sourceSize);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (isHoveredOrFocused()) {
            String tooltip = getMessage().getString();
            int tooltipX = mouseX > minecraft.getWindow().getGuiScaledWidth() / 2
                    ? mouseX + 2
                    : mouseX - minecraft.font.width(tooltip);
            guiGraphics.drawString(minecraft.font, tooltip, tooltipX, mouseY - 10, 0xFFFFFFFF, true);
        }
    }
}
