package com.nstut.buildinggadgetsextra.common;
public final class ProfileSettingsLayout {
    public final int panelX, panelWidth, centerX, centerY, radius;
    public ProfileSettingsLayout(int width, int height) {
        panelWidth = width >= 540 ? 200 : 154;
        panelX = width - panelWidth - 8;
        centerX = panelX / 2; centerY = height / 2;
        radius = width >= 540 ? 82 : 52;
    }
}
