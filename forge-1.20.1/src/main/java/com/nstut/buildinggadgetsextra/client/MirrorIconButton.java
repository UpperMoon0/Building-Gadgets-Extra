package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MirrorIconButton extends GuiIconActionable {
    private static final String BG2_MOD_ID = "buildinggadgets2";
    private final ResourceLocation icon;
    private final int sourceSize;

    public MirrorIconButton(int x, int y, ResourceLocation icon, int sourceSize,
                            Component tooltip, Runnable action) {
        // Keep upstream click/beep behavior without relying on its hard-coded texture namespace.
        // "cut" is real in BG2, so an accidental upstream fallback cannot create a missing texture.
        super(x, y, "cut", tooltip, false, send -> {
            if (send) action.run();
            return false;
        });
        this.icon = icon;
        this.sourceSize = sourceSize;
        this.setWidth(RadialIconLayout.BUTTON_SIZE);
        this.setHeight(RadialIconLayout.BUTTON_SIZE);
    }

    public static ResourceLocation settingIcon(String namespace, String iconName) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/setting/" + iconName + ".png");
    }

    public static ResourceLocation addonSettingIcon(String iconName) {
        return settingIcon(ExtraConstants.MOD_ID, iconName);
    }

    public static ResourceLocation upstreamSettingIcon(String iconName) {
        return settingIcon(BG2_MOD_ID, iconName);
    }

    /**
     * BG2 1.0.8 overrides GuiIconActionable#render directly, so renderWidget is never called here.
     * Override the actual entry point or the parent paints its own hard-coded BG2 texture instead.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(getX(), getY(), getX() + width, getY() + height,
                RadialIconLayout.BACKGROUND_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, this.alpha);
        graphics.blit(icon, getX(), getY(), width, height,
                0, 0,
                sourceSize, sourceSize, sourceSize, sourceSize);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height) {
            String tooltip = getMessage().getString();
            int tooltipX = mouseX > minecraft.getWindow().getGuiScaledWidth() / 2
                    ? mouseX + 2 : mouseX - minecraft.font.width(tooltip);
            graphics.drawString(minecraft.font, tooltip, tooltipX, mouseY - 10, 0xFFFFFFFF, true);
        }
    }
}
