package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the first composable-planner release slice across version ports. */
class LiveMirrorContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final String loader = requiredProperty("bge.loader");
    private final boolean legacyCut = Boolean.parseBoolean(requiredProperty("bge.legacyCut"));

    @Test
    void liveMirrorIsOnlyExposedWherePerPositionStateTransformsAreSafe() throws Exception {
        Path mixin = module.resolve("src/main/java/com/nstut/buildinggadgetsextra/mixin/BaseModeLiveMirrorMixin.java");
        Path modernRadial = module.resolve("src/main/java/com/nstut/buildinggadgetsextra/client/MultitoolRadialScreen.java");
        Path legacyRadial = module.resolve("src/main/java/com/nstut/buildinggadgetsextra/client/LegacyMultitoolScreen.java");

        if (legacyCut) {
            assertFalse(Files.exists(mixin), label("legacy BG1 must not claim live mirror without per-position block states"));
            String radial = read(legacyRadial);
            assertFalse(radial.contains("LIVE_MIRROR_HORIZONTAL"), label("legacy UI must keep unsupported live mirror hidden"));
            return;
        }

        assertTrue(Files.isRegularFile(mixin), label("live mirror planner mixin"));
        String mixinSource = read(mixin);
        contains(mixinSource, "OperationPlanner.mirrorCopies", "shared planner usage");
        contains(mixinSource, "MAX_LIVE_PLAN_POSITIONS", "bounded interactive plan");
        contains(mixinSource, "PlanPos.ZERO", "BG2 local-coordinate mirror pivot");
        contains(mixinSource, "start.offset(localPos)", "local-to-world validation conversion");
        contains(mixinSource, "isPosValid", "generated-copy upstream validation");
        contains(mixinSource, "MirrorTransforms.mirrorState", "per-copy block-state transform");
        contains(mixinSource, "MultitoolMode.BUILD", "building-mode gate");
        contains(mixinSource, "MultitoolMode.EXCHANGING", "exchanging-mode gate");

        String mixins = read(module.resolve("src/main/resources/buildinggadgetsextra.mixins.json"));
        contains(mixins, "BaseModeLiveMirrorMixin", "mixin registration");

        String radial = read(modernRadial);
        contains(radial, "LIVE_MIRROR_HORIZONTAL", "horizontal live-mirror control");
        contains(radial, "LIVE_MIRROR_VERTICAL", "vertical live-mirror control");
        contains(radial, "Mirror", "existing mirror protocol reuse");

        String handler = source("network/" + ("forge".equals(loader)
                ? "MirrorPacketHandler.java" : "MirrorPayloadHandler.java"));
        contains(handler, "toggleLiveMirror", "server-authoritative live toggle");
        contains(handler, "containerMenu.broadcastChanges", "stack-state synchronization");
        contains(handler, "MultitoolMode.BUILD", "contextual mirror semantics");
        contains(handler, "MultitoolMode.EXCHANGING", "contextual mirror semantics");

        String state = source("item/MultitoolState.java");
        contains(state, "LIVE_MIRROR_HORIZONTAL", "persisted horizontal modifier state");
        contains(state, "LIVE_MIRROR_VERTICAL", "persisted vertical modifier state");
        if ("forge".equals(loader)) {
            contains(state, "LIVE_MIRROR_HORIZONTAL, LIVE_MIRROR_VERTICAL", "Forge profile snapshot isolation");
        } else {
            contains(state, "profile.putBoolean(LIVE_MIRROR_HORIZONTAL", "NeoForge profile snapshot isolation");
            contains(state, "setLiveMirror(stack, false", "NeoForge profile restore isolation");
        }
    }

    @Test
    void sharedPlannerAndTranslationsArePresent() throws Exception {
        Path common = module.getParent().resolve("common").normalize();
        String planner = read(common.resolve("src/main/java/com/nstut/buildinggadgetsextra/common/planner/OperationPlanner.java"));
        contains(planner, "mirrorCopies", "shared live-mirror composition");
        contains(planner, "LinkedHashMap", "deterministic dedupe order");
        String position = read(common.resolve("src/main/java/com/nstut/buildinggadgetsextra/common/planner/PlanPos.java"));
        contains(position, "PlanPos ZERO", "shared local operation origin");

        String lang = read(common.resolve("src/main/resources/assets/buildinggadgetsextra/lang/en_us.json"));
        contains(lang, "buildinggadgetsextra.radialmenu.live_mirror_horizontal", "horizontal live-mirror label");
        contains(lang, "buildinggadgetsextra.radialmenu.live_mirror_vertical", "vertical live-mirror label");
        contains(lang, "buildinggadgetsextra.message.live_mirror_horizontal_enabled", "toggle feedback");
        contains(lang, "buildinggadgetsextra.message.live_mirror_vertical_disabled", "toggle feedback");
    }

    private String source(String relative) throws IOException {
        return read(module.resolve("src/main/java/com/nstut/buildinggadgetsextra").resolve(relative));
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
