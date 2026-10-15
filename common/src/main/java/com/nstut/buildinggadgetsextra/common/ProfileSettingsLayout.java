package com.nstut.buildinggadgetsextra.common;
public final class ProfileSettingsLayout {
    public final int panelX, panelWidth, centerX, centerY, radius, backRadius;
    public ProfileSettingsLayout(int width, int height) {
        panelWidth = width >= 440 ? 200 : 154;
        panelX = width - panelWidth - 8;
        centerX = panelX / 2; centerY = height / 2;
        radius = width >= 600 && height >= 320 ? 82 : 54;
        backRadius = radius == 54 ? 18 : 28;
    }
}
