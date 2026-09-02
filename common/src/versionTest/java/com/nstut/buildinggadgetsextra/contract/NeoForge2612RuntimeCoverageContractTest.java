package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the highest-risk 26.1.2 port on real GameTest coverage instead of source contracts alone. */
class NeoForge2612RuntimeCoverageContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");

    @Test
    void neoForge2612RunsEnergyProfileAndCutGameTests() throws Exception {
        if (!"26.1.2".equals(minecraftVersion)) return;

        String tests = source("network/BgeGameTests.java");
        contains(tests, "Capabilities.Energy.ITEM", "runtime FE capability lookup");
        contains(tests, "BuildingUtils.useEnergy", "runtime active-profile FE consumption");
        contains(tests, "MultitoolCutHandler.cut", "runtime multitool Cut operation");
        contains(tests, "ServerTickHandler.gadgetWorking", "runtime Cut queue assertion");

        String build = read(module.resolve("build.gradle"));
        contains(build, "gameTestServer", "26.1.2 GameTest run configuration");
        contains(build, "neoforge.enabledGameTestNamespaces", "26.1.2 GameTest namespace");
    }

    private String source(String relative) throws IOException {
        return read(module.resolve("src/main/java/com/nstut/buildinggadgetsextra").resolve(relative));
    }

    private String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "26.1.2: missing file " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void contains(String source, String expected, String feature) {
        assertTrue(source.contains(expected), "26.1.2: " + feature + " must contain " + expected);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
