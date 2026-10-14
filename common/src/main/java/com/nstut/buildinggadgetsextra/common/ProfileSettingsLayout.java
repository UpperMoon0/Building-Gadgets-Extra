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
    /** Use the drawn square bounds, so inner corners are clickable and gaps stay inactive. */
    public int actionAt(int count,double dx,double dy) {
        for(int i=0;i<count;i++) {
            double angle=-Math.PI/2+i*(Math.PI*2/count);
            int x=(int)(Math.cos(angle)*radius), y=(int)(Math.sin(angle)*radius);
            if(dx>=x-18 && dx<x+18 && dy>=y-18 && dy<y+18) return i;
        }
        return -1;
    }
}
