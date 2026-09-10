package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the Forge 1.20.1 child-run JVM merge to option/value units instead of raw tokens. */
class Forge1201LauncherContractTest {
    @Test
    void repeatedJvmOptionsKeepTheirValuesWithoutDuplicatingInheritedModulePaths() throws Exception {
        if (!"1.20.1".equals(System.getProperty("bge.minecraftVersion"))
                || !"forge".equals(System.getProperty("bge.loader"))) return;

        Path module = Paths.get(requiredProperty("bge.moduleDir"));
        String build = new String(Files.readAllBytes(module.resolve("build.gradle")), StandardCharsets.UTF_8);

        contains(build, "mergeJvmArgsByUnit", "unit-aware JVM argument merge");
        contains(build, "'--add-opens'", "paired --add-opens handling");
        contains(build, "'-p'", "paired module-path handling");
        contains(build, "def unit = [arg, value]", "option/value pair identity");
        contains(build, "seenUnits.add(unit)", "duplicate unit suppression");
        contains(build, "mergeJvmArgsByUnit(parentRun.getJvmArgs() + childRun.getJvmArgs())",
                "late parent/child JVM argument merge");
        assertFalse(build.contains(".unique()"),
                "Raw token de-duplication corrupts repeated option/value pairs such as --add-opens");
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
