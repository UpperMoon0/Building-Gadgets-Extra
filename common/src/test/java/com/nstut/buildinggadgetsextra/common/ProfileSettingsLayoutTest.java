package com.nstut.buildinggadgetsextra.common;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProfileSettingsLayoutTest {
    @Test void wheelAndSettingsFitAtEveryGuiScale() {
        for(int width:new int[]{320,400,480,540,640,960}) for(int height:new int[]{240,270,480}) {
            ProfileSettingsLayout l=new ProfileSettingsLayout(width,height);
            assertTrue(l.centerX-l.radius-14>=0);
            assertTrue(l.centerX+l.radius+14<l.panelX);
            assertTrue(l.panelX+l.panelWidth<=width-8);
            assertTrue(l.centerY-l.radius-14>=0);
            assertTrue(l.centerY+l.radius+14<=height);
        }
    }
}
