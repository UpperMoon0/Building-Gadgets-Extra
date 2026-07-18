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
    private final Identifier icon;

    public MirrorIconButton(int x, int y, String iconName, Component tooltip, Runnable action) {
        super(x, y, "buildinggadgetsextra_placeholder", tooltip, false, send -> {
            if (send) action.run();
            return false;
        });
        icon = Identifier.fromNamespaceAndPath(ExtraConstants.MOD_ID,
                "textures/gui/setting/" + iconName + ".png");
        setWidth(RadialIconLayout.BUTTON_SIZE);
        setHeight(RadialIconLayout.BUTTON_SIZE);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                RadialIconLayout.BACKGROUND_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX(), getY(), 0, 0,
                width, height,
                RadialIconLayout.SOURCE_TEXTURE_SIZE, RadialIconLayout.SOURCE_TEXTURE_SIZE,
                RadialIconLayout.SOURCE_TEXTURE_SIZE, RadialIconLayout.SOURCE_TEXTURE_SIZE,
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
