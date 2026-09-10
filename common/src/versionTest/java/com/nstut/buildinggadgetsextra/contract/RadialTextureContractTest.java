package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the radial-menu texture ownership and rendering assumptions across supported ports. */
class RadialTextureContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final Path root = module.getParent();
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final boolean legacyCut = Boolean.parseBoolean(requiredProperty("bge.legacyCut"));

    @Test
    void saveAndLoadSpritesRemainPackagedAddonResources() throws Exception {
        Path settingDir = root.resolve("common/src/main/resources/assets/buildinggadgetsextra/textures/gui/setting");
        assertPngDimensions(settingDir.resolve("save.png"), 44, 44);
        assertPngDimensions(settingDir.resolve("load.png"), 44, 44);
    }

    @Test
    void modernExplicitSizeSettingIconsResolveFromBg2AndAcceptNativeResourceIds() throws Exception {
        if (legacyCut) return;

        String button = source("client/MirrorIconButton.java");
        String screen = source("client/MultitoolRadialScreen.java");
        String layout = read(root.resolve("common/src/main/java/com/nstut/buildinggadgetsextra/common/RadialIconLayout.java"));

        if ("1.20.1".equals(minecraftVersion)) {
            contains(screen, "MirrorIconButton.addonSettingIcon(load ? \"load\" : \"save\")",
                    "Save/Load explicit addon resource ownership");
            contains(screen, "MirrorIconButton.upstreamSettingIcon(\"cut\")",
                    "Cut explicit upstream resource ownership");
            assertFalse(button.contains("MirrorIconButton(int x, int y, String iconName, int sourceSize"),
                    label("Forge 1.20.1 icon ownership must not be inferred from source size"));
        } else {
            contains(button, "addonSettingIcon(iconName)", "default addon-owned setting-icon resolution");
            contains(button, "upstreamSettingIcon(iconName)", "explicit-size upstream setting-icon resolution");
            contains(screen, "\"cut\", RadialIconLayout.MODERN_SETTING_ICON_SIZE",
                    "multitool Cut explicit-size upstream icon path");
        }
        contains(button, "\"buildinggadgets2\"", "BG2 texture namespace");
        contains(button, "\"textures/gui/setting/\" + iconName + \".png\"", "setting texture path");
        assertFalse(button.contains("buildinggadgetsextra_placeholder"),
                label("modern icon button must not construct a guaranteed-missing upstream placeholder texture"));
        contains(layout, "MODERN_SETTING_ICON_SIZE = 15", "BG2 setting sprite source dimensions");
        contains(layout, "SOURCE_TEXTURE_SIZE = 44", "addon setting sprite source dimensions");

        if ("26.1.2".equals(minecraftVersion)) {
            contains(button, "MirrorIconButton(int x, int y, Identifier icon, int sourceSize",
                    "26.1.2 arbitrary Identifier icon constructor");
        } else {
            contains(button, "MirrorIconButton(int x, int y, ResourceLocation icon, int sourceSize",
                    "modern arbitrary ResourceLocation icon constructor");
        }
    }

    @Test
    void forge1201OverridesTheRendererBg2ActuallyCalls() throws Exception {
        if (!"1.20.1".equals(minecraftVersion)) return;

        String button = source("client/MirrorIconButton.java");
        contains(button, "public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)",
                "BG2 1.0.8 direct render override");
        assertFalse(button.contains("void renderWidget(GuiGraphics"),
                label("Forge 1.20.1 must not regress to the unused renderWidget hook"));
    }

    private String source(String relative) throws IOException {
        return read(module.resolve("src/main/java/com/nstut/buildinggadgetsextra").resolve(relative));
    }

    private String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), label("missing file " + path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void assertPngDimensions(Path path, int expectedWidth, int expectedHeight) throws IOException {
        assertTrue(Files.isRegularFile(path), label("missing PNG " + path));
        byte[] bytes = Files.readAllBytes(path);
        assertTrue(bytes.length >= 24, label("truncated PNG " + path));
        byte[] signature = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < signature.length; i++) {
            assertEquals(signature[i], bytes[i], label("invalid PNG signature " + path));
        }
        assertEquals(expectedWidth, readBigEndianInt(bytes, 16), label("PNG width " + path));
        assertEquals(expectedHeight, readBigEndianInt(bytes, 20), label("PNG height " + path));
    }

    private int readBigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private void contains(String source, String expected, String feature) {
        assertTrue(source.contains(expected), label(feature + " must contain " + expected));
    }

    private String label(String message) {
        return minecraftVersion + ": " + message;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
