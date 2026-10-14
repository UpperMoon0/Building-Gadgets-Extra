package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ProfileIconButton extends GuiIconActionable {
    private static final String BG2_MOD_ID = "buildinggadgets2";
    private final Identifier icon;
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
    public static Identifier settingIcon(String namespace, String iconName) {
        return Identifier.fromNamespaceAndPath(namespace, "textures/gui/setting/" + iconName + ".png");
    }

    public static Identifier addonSettingIcon(String iconName) {
        return settingIcon(ExtraConstants.MOD_ID, iconName);
    }

    public static Identifier upstreamSettingIcon(String iconName) {
        return settingIcon(BG2_MOD_ID, iconName);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover=mouseX>=getX() && mouseX<getX()+width && mouseY>=getY() && mouseY<getY()+height;
        int edge=active?(selectable && action.test(false)?ProfileUiStyle.SELECTED:hover?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER):ProfileUiStyle.BORDER;
        graphics.fill(getX(),getY(),getX()+width,getY()+height,ProfileUiStyle.CONTROL);
        graphics.fill(getX(),getY(),getX()+width,getY()+1,edge); graphics.fill(getX(),getY()+height-1,getX()+width,getY()+height,edge);
        graphics.fill(getX(),getY(),getX()+1,getY()+height,edge); graphics.fill(getX()+width-1,getY(),getX()+width,getY()+height,edge);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX()+4, getY()+4, 0, 0,
                width-8, height-8,
                sourceSize, sourceSize, sourceSize, sourceSize,
                0xFFFFFFFF);
        if (isHoveredOrFocused()) {
            Minecraft minecraft = Minecraft.getInstance();
            String tooltip = getMessage().getString();
            int x = mouseX > minecraft.getWindow().getGuiScaledWidth() / 2
                    ? mouseX + 2 : mouseX - minecraft.font.width(tooltip);
            graphics.text(minecraft.font, tooltip, x, mouseY - 10, 0xFFFFFFFF, true);
        }
    }
}
