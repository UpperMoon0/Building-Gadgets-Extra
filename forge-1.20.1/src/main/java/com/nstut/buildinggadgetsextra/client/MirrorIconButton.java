package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MirrorIconButton extends GuiIconActionable {
    private static final int SIZE = 24;
    private static final int TEXTURE_SIZE = 44;
    private static final int BACKGROUND_COLOR = 0x32FFFFFF;
    private final ResourceLocation icon;

    public MirrorIconButton(int x, int y, String iconName, Component tooltip, Runnable action) {
        super(x, y, "buildinggadgetsextra_placeholder", tooltip, false, send -> {
            if (send) action.run();
            return false;
        });
        this.icon = ResourceLocation.fromNamespaceAndPath(
                ExtraConstants.MOD_ID, "textures/gui/setting/" + iconName + ".png");
        this.setWidth(SIZE);
        this.setHeight(SIZE);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(getX(), getY(), getX() + width, getY() + height, BACKGROUND_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, this.alpha);
        graphics.blit(icon, getX(), getY(), width, height,
                0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (isHoveredOrFocused()) {
            String tooltip = getMessage().getString();
            int tooltipX = mouseX > minecraft.getWindow().getGuiScaledWidth() / 2
                    ? mouseX + 2 : mouseX - minecraft.font.width(tooltip);
            graphics.drawString(minecraft.font, tooltip, tooltipX, mouseY - 10, 0xFFFFFFFF, true);
        }
    }
}
