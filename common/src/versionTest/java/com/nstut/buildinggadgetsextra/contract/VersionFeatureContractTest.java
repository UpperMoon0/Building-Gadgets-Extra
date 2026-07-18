package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures every compiled port keeps the adapters and wiring required by shared behavior. */
class VersionFeatureContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final String loader = requiredProperty("bge.loader");
    private final boolean legacyCut = Boolean.parseBoolean(requiredProperty("bge.legacyCut"));

    @Test
    void radialMenuExposesOnlyContextualActions() throws Exception {
        String radial = source("mixin/ModeRadialMenuMixin.java");
        contains(radial, "RadialButtonPolicy.showMirrorButtons", "paste-only mirror policy");
        contains(radial, "RadialButtonPolicy.fileAction", "contextual save/load policy");
        contains(radial, "mirror_vertical", "vertical mirror button");
        contains(radial, "save", "save action");
        contains(radial, "load", "load action");
        assertFalse(radial.contains("StructureLibrary"), label("obsolete structure library button"));

        if (legacyCut) {
            contains(radial, "showLegacyCutAction", "legacy cut visibility policy");
            contains(radial, "CutSelectionPacket", "legacy cut server request");
        } else {
            contains(radial, "GadgetCutPaste", "native Cut Paste integration");
        }
    }

    @Test
    void mirrorAdapterUsesSharedCoordinateAndStateEngines() throws Exception {
        String file = legacyCut ? "transform/TemplateTransforms.java" : "transform/MirrorTransforms.java";
        String transform = source(file);
        contains(transform, "MirrorEngine", "shared coordinate and block-entity transform");
        contains(transform, "VerticalStateMirror", "shared vertical block-state transform");
        contains(transform, "MirrorPlane.Y", "vertical mirror plane");

        String handler = legacyCut ? source("network/MirrorPacket.java")
                : source("network/" + ("forge".equals(loader)
                ? "MirrorPacketHandler.java" : "MirrorPayloadHandler.java"));
        contains(handler, "vertical", "vertical server handler branch");
        contains(handler, legacyCut ? "TemplateTransforms.vertical" : "MirrorTransforms.vertical",
                "version mirror adapter invocation");
        contains(handler, legacyCut ? ".mirror(player.getDirection().getAxis())" : "MirrorTransforms.horizontal",
                "horizontal mirror adapter invocation");
        contains(handler, "ServerPlayer", "server-authoritative player context");
    }

    @Test
    void nativeStructureDialogsAndTransfersRemainClientServerSafe() throws Exception {
        String client = source("client/ClientStructureFiles.java");
        contains(client, "resolve(\"structures\")", "shared default structure directory");
        contains(client, "tinyfd_saveFileDialog", "native save dialog");
        contains(client, "tinyfd_openFileDialog", "native load dialog");
        contains(client, "MAX_STRUCTURE_FILE_BYTES", "client file-size guard");
        contains(client, "STRUCTURE_CHUNK_SIZE", "client chunked transfer");

        String bridge = source("structure/NativeStructureBridge.java");
        contains(bridge, "exportStructure", "server export bridge");
        contains(bridge, "importStructure", "server import bridge");
        contains(bridge, "MAX_STRUCTURE_FILE_BYTES", "server encoded-size guard");
        contains(bridge, "MAX_STRUCTURE_BLOCKS", "server decoded-volume guard");
        contains(bridge, legacyCut ? "Template" : "StructureTemplate", "vanilla structure implementation");
        contains(bridge, "ServerPlayer", "server-owned structure conversion");

        String registration = source("network/" + ("forge".equals(loader)
                ? "ExtraNetwork.java" : "ExtraPayloads.java"));
        contains(registration, "Mirror", "mirror network registration");
        contains(registration, "StructureFile", "save request registration");
        contains(registration, "StructureUpload", "client-to-server upload registration");
        contains(registration, "StructureDownload", "server-to-client download registration");
        contains(registration, "forge".equals(loader) ? "sendToServer" : "playToServer",
                "server-bound channel direction");
        contains(registration, "forge".equals(loader) ? "sendToPlayer" : "playToClient",
                "client-bound channel direction");
    }

    @Test
    void mixinsMetadataAndAssetsArePackagedForThisPort() throws Exception {
        String mixins = read(module.resolve("src/main/resources/buildinggadgetsextra.mixins.json"));
        contains(mixins, "ModeRadialMenuMixin", "client radial mixin registration");
        contains(mixins, legacyCut ? "VanillaTemplateAccessor" : "StructureTemplateAccessor",
                "vanilla structure accessor registration");

        Path commonResources = module.getParent().resolve("common/src/main/resources").normalize();
        assertTrue(Files.isRegularFile(commonResources.resolve("logo.png")), label("shared mod logo"));
        String[] icons = {"mirror_horizontal.png", "mirror_vertical.png", "save.png", "load.png"};
        for (String icon : icons) {
            assertTrue(Files.isRegularFile(commonResources.resolve(
                    "assets/buildinggadgetsextra/textures/gui/setting/" + icon)), label(icon));
        }

        Path metadata = module.resolve("src/main/" + ("forge".equals(loader)
                ? "resources/META-INF/mods.toml" : "templates/META-INF/neoforge.mods.toml"));
        String metadataText = read(metadata);
        contains(metadataText, "logo.png", "metadata logo declaration");
        contains(metadataText, "mod_authors", "metadata author declaration");
        contains(metadataText, "mod_description", "metadata description declaration");
    }

    @Test
    void cutIntegrationMatchesTheVersionAndIsServerSafe() throws Exception {
        if (!legacyCut) {
            String radial = source("mixin/ModeRadialMenuMixin.java");
            String bridge = source("structure/NativeStructureBridge.java");
            contains(radial, "GadgetCutPaste", "native Cut Paste radial integration");
            contains(bridge, "GadgetCutPaste", "native Cut Paste structure integration");
            contains(bridge, "ServerTickHandler.gadgetWorking", "native cut busy-state protection");
            assertFalse(Files.exists(module.resolve(
                    "src/main/java/com/nstut/buildinggadgetsextra/cut/LegacyCutScheduler.java")),
                    label("modern ports must not duplicate the native cut scheduler"));
            return;
        }

        String handler = source("network/CutSelectionPacketHandler.java");
        contains(handler, "LegacyCutScheduler.schedule", "validated server cut scheduling");
        contains(handler, "BlockEvent.BreakEvent", "protection event validation");
        contains(handler, "requiredEnergy", "whole-operation energy validation");

        String scheduler = source("cut/LegacyCutScheduler.java");
        contains(scheduler, "EffectBlock.Mode.REMOVE", "native shrinking removal animation");
        contains(scheduler, "removeBlockEntity", "container drop prevention");
        contains(scheduler, "CutAnimationPolicy.blocksPerTick", "modern queue pacing");
        contains(scheduler, "EntityJoinWorldEvent", "dependency drop suppression");
        contains(scheduler, "getThrower", "player-thrown item preservation");
        contains(scheduler, "getOwner", "player-owned item preservation");

        contains(source("network/ExtraNetwork.java"), "CutSelectionPacket.class",
                "legacy cut packet registration");
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
