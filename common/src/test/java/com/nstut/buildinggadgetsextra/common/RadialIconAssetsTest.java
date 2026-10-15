package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RadialIconAssetsTest {
    @Test
    void everyRadialIconMatchesTheSharedSourceSize() throws Exception {
        assertIcon("array",16);
        assertIcon("radial",16);
        assertIcon("mirror_horizontal");
        assertIcon("mirror_vertical");
        assertIcon("save");
        assertIcon("load");
        assertIcon("build");
        assertIcon("delete");
        assertIcon("exchange");
        assertIcon("cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE);
    }

    @Test
    void compactModifierIconsHaveOnlyOpaqueWhiteAndTransparentPixels() throws Exception {
        for(String name:new String[]{"array","radial"}) {
            try(InputStream input=getClass().getResourceAsStream("/assets/buildinggadgetsextra/textures/gui/setting/"+name+".png")) {
                BufferedImage image=ImageIO.read(input);int opaque=0;
                for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
                    int pixel=image.getRGB(x,y),alpha=pixel>>>24;
                    org.junit.jupiter.api.Assertions.assertTrue(alpha==0 || pixel==0xFFFFFFFF,"antialiased/detail pixel in "+name);
                    if(alpha==255) opaque++;
                }
                org.junit.jupiter.api.Assertions.assertTrue(opaque>=24 && opaque<=128,"icon must remain a readable sparse silhouette");
            }
        }
    }

    private static void assertIcon(String name) throws Exception {
        assertIcon(name, RadialIconLayout.SOURCE_TEXTURE_SIZE);
    }

    private static void assertIcon(String name, int sourceSize) throws Exception {
        String path = "/assets/buildinggadgetsextra/textures/gui/setting/" + name + ".png";
        try (InputStream input = RadialIconAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image, path);
            assertEquals(sourceSize, image.getWidth(), path + " width");
            assertEquals(sourceSize, image.getHeight(), path + " height");
        }
    }
}
