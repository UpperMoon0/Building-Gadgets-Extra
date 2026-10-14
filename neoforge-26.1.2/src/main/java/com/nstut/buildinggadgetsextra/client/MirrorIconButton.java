package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MirrorIconButton extends GuiIconActionable {
    private static final String BG2_MOD_ID = "buildinggadgets2";
    private final Identifier icon;
    private final int sourceSize;
    private boolean profile;
    public MirrorIconButton profileStyle() { profile=true; return this; }

    public MirrorIconButton(int x, int y, String iconName, Component tooltip, Runnable action) {
        this(x, y, addonSettingIcon(iconName), RadialIconLayout.SOURCE_TEXTURE_SIZE, tooltip, action);
    }

    /** Explicit-size setting icons are upstream BG2 assets. Modern Cut uses this overload. */
    public MirrorIconButton(int x, int y, String iconName, int sourceSize, Component tooltip, Runnable action) {
        this(x, y, upstreamSettingIcon(iconName), sourceSize, tooltip, action);
    }

    public MirrorIconButton(int x, int y, Identifier icon, int sourceSize,
                            Component tooltip, Runnable action) {
        // Preserve BG2 click/beep behavior while this class owns texture resolution and extraction.
        super(x, y, "cut", tooltip, false, send -> {
            if (send) action.run();
            return false;
        });
        this.icon = icon;
        this.sourceSize = sourceSize;
        setWidth(RadialIconLayout.BUTTON_SIZE);
        setHeight(RadialIconLayout.BUTTON_SIZE);
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
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                profile?com.nstut.buildinggadgetsextra.common.ProfileUiStyle.CONTROL:RadialIconLayout.BACKGROUND_COLOR);
        int pad=profile?4:0;
        if(profile) {
            int edge=(mouseX>=getX() && mouseX<getX()+width && mouseY>=getY() && mouseY<getY()+height)?0xFF3598FF:0xFF56616B;
            graphics.fill(getX(),getY(),getX()+width,getY()+1,edge); graphics.fill(getX(),getY()+height-1,getX()+width,getY()+height,edge);
            graphics.fill(getX(),getY(),getX()+1,getY()+height,edge); graphics.fill(getX()+width-1,getY(),getX()+width,getY()+height,edge);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX()+pad, getY()+pad, 0, 0,
                width-pad*2, height-pad*2,
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
