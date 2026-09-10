package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the malformed-profile and real-client startup regressions fixed during PR review. */
class ProfileHardeningContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final String loader = requiredProperty("bge.loader");
    private final boolean legacyCut = Boolean.parseBoolean(requiredProperty("bge.legacyCut"));

    @Test
    void bg2ProfileRestoreBoundsUndoHistory() throws Exception {
        if (legacyCut) return;

        String state = source("item/MultitoolState.java");
        contains(state, "MAX_UNDO_ENTRIES = 10", "upstream-compatible undo history bound");
        contains(state, "boundedUndoSize(undo.size())", "bounded undo serialization");
        if ("26.1.2".equals(minecraftVersion)) {
            contains(state, "boundedUndoSize(saved.getIntOr(\"Size\", 0))", "bounded undo restoration");
        } else {
            contains(state, "boundedUndoSize(saved.getInt(\"Size\"))", "bounded undo restoration");
        }
        contains(state, "Math.max(0, Math.min(size, MAX_UNDO_ENTRIES))", "negative and oversized undo count clamp");
    }

    @Test
    void neoForge2612UsesCollectionSizedAnchorsAndSafeTemplateUuidParsing() throws Exception {
        if (!"26.1.2".equals(minecraftVersion)) return;

        String state = source("item/MultitoolState.java");
        contains(state, "profile.putLongArray(\"AnchorList\", anchorValues)", "collection-sized anchor serialization");
        contains(state, "profile.getLongArray(\"AnchorList\").orElseGet(() -> new long[0])", "collection-sized anchor restoration");
        assertFalse(state.contains("AnchorListSize"), label("anchor restoration must not trust an independent count field"));
        contains(state, "UUID copyId = parseUuid(profile.getStringOr(\"CopyId\", \"\"))", "defensive CopyId parsing");
        assertFalse(state.contains("UUID.fromString(copyId)"), label("CopyId must not be parsed without validation"));
    }

    @Test
    void realClientRangeScenarioWaitsForRoundTripBeforeReadingTheWidget() throws Exception {
        Path root = module.getParent();
        String scenario = read(root.resolve("common/src/clientIntegrationTest/java/com/nstut/buildinggadgetsextra/clienttest/ClientRangeRoundTripScenario.java"));
        contains(scenario,
                "case WAIT_FOR_SYNC:\n                    if (adapter.clientRange() != TARGET_RANGE) return;\n                    if (adapter.visibleScreenRange() != TARGET_RANGE) return;",
                "initial range authoritative sync wait");
        contains(scenario,
                "case WAIT_EXCHANGE_SYNC:\n                    if (adapter.clientRange() != EXCHANGE_TARGET_RANGE) return;\n                    if (adapter.visibleScreenRange() != EXCHANGE_TARGET_RANGE) return;",
                "exchange range authoritative sync wait");
    }

    @Test
    void modernDedicatedClientsUseDeterministicConnectionStartup() throws Exception {
        if (legacyCut) return;

        String adapterName;
        if ("1.20.1".equals(minecraftVersion)) {
            adapterName = "Forge1201ClientRangeIntegrationTest.java";
        } else if ("1.21.1".equals(minecraftVersion)) {
            adapterName = "NeoForge1211ClientRangeIntegrationTest.java";
        } else if ("26.1.2".equals(minecraftVersion)) {
            adapterName = "NeoForge2612ClientRangeIntegrationTest.java";
        } else {
            return;
        }

        String adapter = read(module.resolve("src/clientIntegrationTest/java/com/nstut/buildinggadgetsextra/clienttest").resolve(adapterName));
        contains(adapter, "ConnectScreen.startConnecting", "explicit dedicated client connection");
        contains(adapter, "DEDICATED_ADDRESS", "fixed loopback integration endpoint");

        String build = read(module.resolve("build.gradle"));
        assertFalse(dedicatedRunBlock(build).contains("quickPlayMultiplayer"),
                label("dedicated connection must have one owner instead of racing Quick Play"));

        if ("26.1.2".equals(minecraftVersion)) {
            contains(adapter, "createWorldOpenFlows().openWorld", "deterministic prepared-world startup");
            assertFalse(build.contains("quickPlaySingleplayer"),
                    label("26.1.2 prepared-world startup must have one owner instead of racing Quick Play"));
        }
    }

    @Test
    void forge1201IntegrationServerInheritsResolvedServerLauncher() throws Exception {
        if (!"1.20.1".equals(minecraftVersion) || !"forge".equals(loader)) return;

        String build = read(module.resolve("build.gradle"));
        String serverRun = runBlock(build, "clientIntegrationTestServer");
        contains(serverRun, "parent runs.server", "ForgeGradle server parent relation");
        assertFalse(serverRun.contains("merge runs.server"), label("integration server must not eagerly merge unresolved run metadata"));
        contains(build, "clientIntegrationTestServer: 'server'", "late resolved launcher fallback");
        contains(build, "childRun.main(parentRun.main)", "late resolved ForgeGradle main-class copy");
    }

    private String source(String relative) throws IOException {
        return read(module.resolve("src/main/java/com/nstut/buildinggadgetsextra").resolve(relative));
    }

    private String dedicatedRunBlock(String build) {
        return runBlock(build, "dedicatedClientIntegrationTest");
    }

    private String runBlock(String build, String runName) {
        int start = build.indexOf(runName + " {");
        if (start < 0) return "";
        int depth = 0;
        for (int i = build.indexOf('{', start); i < build.length(); i++) {
            char c = build.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return build.substring(start, i + 1);
        }
        return build.substring(start);
    }

    private String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), label("missing file " + path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void contains(String source, String expected, String feature) {
        assertTrue(source.contains(expected), label(feature + " must contain " + expected));
    }

    private String label(String message) {
        return minecraftVersion + " " + loader + ": " + message;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
