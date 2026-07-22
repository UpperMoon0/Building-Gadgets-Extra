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

    @Test
    void multitoolPrototypeKeepsNavigationSettingsAndPreviewCompatibilityTogether() throws Exception {
        Path radialPath = module.resolve(
                "src/main/java/com/nstut/buildinggadgetsextra/client/MultitoolRadialScreen.java");
        if (!Files.isRegularFile(radialPath)) return; // Port is introduced version-by-version.

        String radial = read(radialPath);
        contains(radial, "MultitoolMenuState", "shared two-level navigation state");
        contains(radial, "rememberedPage", "submenu persistence across reopen");
        contains(radial, "0xFF00E640", "green selected outline");
        contains(radial, "0xFF3598FF", "blue hovered outline");
        String[] settings = "forge".equals(loader)
                ? new String[]{"PacketUndo", "PacketAnchor", "PacketRangeChange", "PacketRotate",
                "PacketToggleSetting", "PacketRenderChange"}
                : new String[]{"UndoPayload", "AnchorPayload", "RangeChangePayload", "RotatePayload",
                "ToggleSettingPayload", "RenderChangePayload"};
        for (String setting : settings) {
            contains(radial, setting, "upstream setting support");
        }
        contains(radial, "DestructionGUI", "destruction configuration support");
        contains(radial, "MaterialListGUI", "copy material-list support");

        String preview = source("client/MultitoolPreviewRenderer.java");
        contains(preview, "Registration.Building_Gadget", "building preview delegate");
        contains(preview, "Registration.CopyPaste_Gadget", "copy/paste preview delegate");
        contains(preview, "Registration.CutPaste_Gadget", "cut/paste preview delegate");
        contains(preview, "Registration.Destruction_Gadget", "destruction preview delegate");
        contains(preview, "forge".equals(loader) ? "setTag" : "applyComponents",
                "data-identical preview stack");

        String mixins = read(module.resolve("src/main/resources/buildinggadgetsextra.mixins.json"));
        contains(mixins, "RenderLevelLastMultitoolMixin", "multitool preview hook registration");
    }

    @Test
    void multitoolIncludesTheCompleteExchangingModeAndVersionCorrectRecipe() throws Exception {
        String item = source("item/BuildersMultitool.java");
        contains(item, "EXCHANGING", "exchanging mode dispatch");
        contains(item, legacyCut ? "OurItems.EXCHANGING_GADGET_ITEM" : "Registration.Exchanging_Gadget",
                "upstream exchanging gadget delegation");
        contains(item, legacyCut ? "GADGET_EXCHANGER" : "EXCHANGINGGADGET_",
                "exchanging energy configuration");
        assertTrue(item.contains("SILK_TOUCH") || item.contains("isPrimaryItemFor"),
                label("exchanging enchantment compatibility"));

        Path modernRadial = module.resolve("src/main/java/com/nstut/buildinggadgetsextra/client/MultitoolRadialScreen.java");
        Path legacyRadial = module.resolve("src/main/java/com/nstut/buildinggadgetsextra/client/LegacyMultitoolScreen.java");
        String radial = read(Files.isRegularFile(modernRadial) ? modernRadial : legacyRadial);
        contains(radial, "EXCHANGING", "exchanging radial-menu entry");
        contains(radial, legacyCut ? "ExchangingModes" : "BuildersMultitool.target(tool)",
                "all upstream exchanging shapes");
        contains(radial, "fuzzy", "exchanging fuzzy option");
        contains(radial, "connected", "exchanging connected-area option");
        contains(radial, legacyCut ? "addRange" : "forge".equals(loader) ? "PacketRangeChange" : "RangeChangePayload",
                "exchanging range option");

        if (legacyCut) {
            String renderer = source("client/LegacyMultitoolRenderer.java");
            contains(renderer, "EXCHANGING_GADGET_ITEM", "legacy exchanging preview delegate");
        } else {
            String preview = source("client/MultitoolPreviewRenderer.java");
            contains(preview, "Registration.Exchanging_Gadget", "exchanging hologram delegate");
        }

        String recipeFolder = "forge".equals(loader) ? "recipes" : "recipe";
        String recipe = read(module.resolve("src/main/resources/data/buildinggadgetsextra/")
                .resolve(recipeFolder).resolve("builders_multitool.json"));
        contains(recipe, "BNE", "recipe top row: build, Netherite, exchange");
        contains(recipe, "NSN", "recipe middle row: Netherite, Nether Star, Netherite");
        contains(recipe, legacyCut ? "PND" : "PCD",
                legacyCut ? "legacy Netherite cut-gadget substitution" : "five-gadget bottom row");
        contains(recipe, "gadget_exchanging", "Exchanging Gadget ingredient");
        contains(recipe, "nether_star", "Nether Star ingredient");
        if (legacyCut) assertFalse(recipe.contains("gadget_cut_paste"), label("1.16.5 has no native Cut Paste ingredient"));
        else contains(recipe, "gadget_cut_paste", "native Cut Paste ingredient");
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
