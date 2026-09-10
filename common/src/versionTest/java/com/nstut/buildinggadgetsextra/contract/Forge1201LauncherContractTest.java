package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks Forge 1.20.1 integration runs to ForgeGradle's native parent inheritance. */
class Forge1201LauncherContractTest {
    @Test
    void integrationRunsUseNativeParentInheritanceWithoutManualJvmMutation() throws Exception {
        if (!"1.20.1".equals(System.getProperty("bge.minecraftVersion"))
                || !"forge".equals(System.getProperty("bge.loader"))) return;

        Path module = Paths.get(requiredProperty("bge.moduleDir"));
        String build = new String(Files.readAllBytes(module.resolve("build.gradle")), StandardCharsets.UTF_8);

        contains(build, "parent runs.client", "ForgeGradle client parent inheritance");
        contains(build, "parent runs.server", "ForgeGradle server parent inheritance");
        assertFalse(build.contains("gradle.projectsEvaluated"),
                "ForgeGradle already merges run parents before creating run tasks; manual late launcher mutation duplicates inherited JVM state");
        assertFalse(build.contains("childRun.getJvmArgs()"),
                "Integration runs must not manually copy already inherited Forge JVM arguments");
        assertFalse(build.contains("childRun.jvmArgs("),
                "Integration runs must not append or replace ForgeGradle's inherited JVM argument list");
        assertFalse(build.contains("inheritJvmArgs(false)"),
                "Native ForgeGradle parent inheritance must remain enabled");
        assertFalse(build.contains(".unique()"),
                "Raw token de-duplication corrupts repeated option/value pairs such as --add-opens");
        assertFalse(build.contains("mergeJvmArgsByUnit"),
                "Custom JVM argument merging is unnecessary when ForgeGradle owns parent inheritance");
    }

    private static void contains(String source, String expected, String feature) {
        assertTrue(source.contains(expected), feature + " must contain " + expected);
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
