package com.nstut.buildinggadgetsextra.common;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProfileSettingsLayoutTest {
    @Test void wheelAndSettingsFitAtEveryGuiScale() {
        for(int width:new int[]{320,400,480,540,640,960}) for(int height:new int[]{240,270,480}) {
            ProfileSettingsLayout l=new ProfileSettingsLayout(width,height);
            assertTrue(l.centerX-l.radius-18>=0);
            assertTrue(l.centerX+l.radius+18<l.panelX);
            assertTrue(l.panelX+l.panelWidth<=width-8);
            assertTrue((int)(l.radius/Math.sqrt(2))-18>=l.backRadius+2, "diagonal action overlaps Back");
            assertTrue(l.centerY-l.radius-18>=0);
            assertTrue(l.centerY+l.radius+18<=height);
        }
    }
    @Test void squareActionCornersAndGapsUseTheirVisibleBounds() {
        ProfileSettingsLayout l=new ProfileSettingsLayout(480,270);
        assertEquals(1,l.actionAt(8,22,-22));
        assertEquals(0,l.actionAt(8,0,-54));
        assertEquals(-1,l.actionAt(8,0,0));
        assertEquals(-1,l.actionAt(8,0,-30));
        assertEquals(-1,l.actionAt(8,0,-80));
    }
}
