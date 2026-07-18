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
        assertIcon("mirror_horizontal");
        assertIcon("mirror_vertical");
        assertIcon("save");
        assertIcon("load");
        assertIcon("cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE);
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
