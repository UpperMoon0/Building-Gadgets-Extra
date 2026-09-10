package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the Forge 1.20.1 integration runs to the finalized parent JVM launcher arguments. */
class Forge1201LauncherContractTest {
    @Test
    void integrationRunsRefreshJvmArgsFromFinalizedParentWithoutMergingDuplicates() throws Exception {
        if (!"1.20.1".equals(System.getProperty("bge.minecraftVersion"))
                || !"forge".equals(System.getProperty("bge.loader"))) return;

        Path module = Paths.get(requiredProperty("bge.moduleDir"));
        String build = new String(Files.readAllBytes(module.resolve("build.gradle")), StandardCharsets.UTF_8);

        contains(build, "childRun.getJvmArgs().clear()", "stale child JVM argument removal");
        contains(build, "childRun.jvmArgs(parentRun.getJvmArgs())", "exact finalized-parent JVM argument refresh");
        contains(build, "childRun.inheritJvmArgs(false)", "no second ForgeGradle JVM inheritance pass");
        assertFalse(build.contains("parentRun.getJvmArgs() + childRun.getJvmArgs()"),
                "Appending parent and child launcher lists duplicates Forge module-path entries");
        assertFalse(build.contains(".unique()"),
                "Raw token de-duplication corrupts repeated option/value pairs such as --add-opens");
        assertFalse(build.contains("mergeJvmArgsByUnit"),
                "Integration children have no custom JVM args, so unit merging is unnecessary and risks path duplication");
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
