package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the Builder's Multitool item-model resources across Minecraft model-pipeline versions. */
class MultitoolItemModelContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final Path root = module.getParent();
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");

    @Test
    void multitoolTextureHasTheRequiredModelEntryPoints() throws Exception {
        Path assets = root.resolve("common/src/main/resources/assets/buildinggadgetsextra");
        assertTrue(Files.isRegularFile(assets.resolve("models/item/builders_multitool.json")),
                label("missing shared baked item model"));
        assertTrue(Files.isRegularFile(assets.resolve("textures/item/builders_multitool.png")),
                label("missing shared item texture"));

        if (!"26.1.2".equals(minecraftVersion)) return;

        Path definition = module.resolve(
                "src/main/resources/assets/buildinggadgetsextra/items/builders_multitool.json");
        String json = read(definition);
        assertTrue(json.contains("\"type\": \"minecraft:model\""),
                label("26.1.2 item definition must use minecraft:model"));
        assertTrue(json.contains("\"model\": \"buildinggadgetsextra:item/builders_multitool\""),
                label("26.1.2 item definition must point at the shared baked model"));
    }

    private String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), label("missing file " + path));
        return Files.readString(path, StandardCharsets.UTF_8);
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
