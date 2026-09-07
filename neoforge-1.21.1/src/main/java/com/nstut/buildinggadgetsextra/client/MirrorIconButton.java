package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Matches Building Gadgets 2's radial-menu setting buttons with explicit texture ownership. */
public final class MirrorIconButton extends GuiIconActionable {
    private static final String BG2_MOD_ID = "buildinggadgets2";
    private final ResourceLocation icon;
    private final int sourceSize;

    public MirrorIconButton(int x, int y, String iconName, Component tooltip, Runnable action) {
        this(x, y, addonSettingIcon(iconName), RadialIconLayout.SOURCE_TEXTURE_SIZE, tooltip, action);
    }

    /** Explicit-size setting icons are upstream BG2 assets. Modern Cut uses this overload. */
    public MirrorIconButton(int x, int y, String iconName, int sourceSize, Component tooltip, Runnable action) {
        this(x, y, upstreamSettingIcon(iconName), sourceSize, tooltip, action);
    }

    public MirrorIconButton(int x, int y, ResourceLocation icon, int sourceSize,
                            Component tooltip, Runnable action) {
        // GuiIconActionable supplies the same click handling and beep as the upstream radial-menu buttons.
        // Use a valid BG2 fallback, while this class owns the actual texture draw below.
        super(x, y, "cut", tooltip, false, send -> {
            if (send) {
                action.run();
            }
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
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height,
                RadialIconLayout.BACKGROUND_COLOR);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, this.alpha);
        guiGraphics.blit(icon, getX(), getY(), width, height,
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
