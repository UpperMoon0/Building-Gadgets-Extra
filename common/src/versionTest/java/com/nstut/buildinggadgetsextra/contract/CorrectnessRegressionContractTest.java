package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks in the upstream BG/BG2 assumptions that previously caused runtime regressions. */
class CorrectnessRegressionContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final String loader = requiredProperty("bge.loader");
    private final boolean legacyCut = Boolean.parseBoolean(requiredProperty("bge.legacyCut"));

    @Test
    void externalStructureImportsStayOnTheUntrustedCopyPastePath() throws Exception {
        String bridge = source("structure/NativeStructureBridge.java");
        contains(bridge, "StructureLimits.checkedVolume", "overflow-safe decoded structure volume validation");
        contains(bridge, "STRUCTURE_BLOCK_ENTITY_STRIPPED", "untrusted block-entity stripping feedback");

        if (legacyCut) {
            contains(bridge, "TileSupport.dummyTileEntityData()", "BG1 safe imported tile-data path");
            contains(bridge, "NBTSizeTracker", "BG1 bounded decompressed NBT budget");
            contains(bridge, "MAX_STRUCTURE_NBT_BYTES", "BG1 configured decompressed NBT limit");
            assertFalse(bridge.contains("new NBTTileEntityData(info.nbt.copy())"),
                    label("BG1 must not replay arbitrary imported tile NBT"));
        } else {
            contains(bridge, "STRUCTURE_IMPORT_REQUIRES_COPY", "server-side Copy/Paste-only import gate");
            contains(bridge, "GadgetCutPaste", "explicit native Cut/Paste rejection");
            contains(bridge, "MultitoolMode.COPY_PASTE", "multitool Copy/Paste-only import gate");
            contains(bridge, "GadgetUtils.cleanBlockState", "upstream state sanitization");
            contains(bridge, "MAX_STRUCTURE_NBT_BYTES", "bounded decompressed NBT budget");
        }

        String upload = source("network/" + ("forge".equals(loader)
                ? "StructureUploadPacket.java" : "StructureUploadHandler.java"));
        contains(upload, "TransferState", "server-side upload session identity");
        contains(upload, "gadgetId", "initiating gadget UUID binding");
        contains(upload, "MAX_STRUCTURE_TRANSFERS_PER_PLAYER", "per-player transfer cap");
        contains(upload, "matches(player)", "target revalidation before commit");
    }

    @Test
    void allPortsShareMultitoolRangeDecisions() throws Exception {
        if (legacyCut) {
            String packet = source("network/LegacyMultitoolPacket.java");
            contains(packet, "MultitoolRangePolicy.resolve", "shared legacy range validation and clamping");
            return;
        }

        String client = source("client/MultitoolClientEvents.java");
        contains(client, "MultitoolRangePolicy.next", "shared range-hotkey mode and wraparound policy");

        String server = "forge".equals(loader)
                ? source("network/MultitoolRangePacket.java")
                : source("mixin/PacketRangeChangeMultitoolMixin.java");
        contains(server, "MultitoolRangePolicy.resolve", "shared server range validation and clamping");
    }

    @Test
    void bg2MultitoolKeepsIdentityUndoGeneralStateAndEnergyProfileLocal() throws Exception {
        if (legacyCut) return;

        String state = source("item/MultitoolState.java");
        contains(state, "BGEUndoProfile_", "per-profile undo history");
        contains(state, "BGEGadgetProfile_", "per-profile gadget UUID");
        contains(state, "restoreGadgetUuidProfile", "profile UUID restoration");
        contains(state, "restoreUndoProfile", "profile undo restoration");
        contains(state, "BGEStateProfile_", "per-profile general gadget state");
        contains(state, "saveGeneralProfile", "general gadget state snapshot");
        contains(state, "restoreGeneralProfile", "general gadget state restoration");

        String energy = source("mixin/BuildingUtilsMultitoolEnergyMixin.java");
        contains(energy, "BUILDINGGADGET_COST", "building energy parity");
        contains(energy, "EXCHANGINGGADGET_COST", "exchange energy parity");
        contains(energy, "COPYPASTEGADGET_COST", "copy/paste energy parity");
        contains(energy, "DESTRUCTIONGADGET_COST", "destruction energy parity");
        contains(energy, "MultitoolState.getActiveMode", "active-profile energy dispatch");
        if ("1.20.1".equals(minecraftVersion)) {
            contains(energy, "CUTPASTEGADGET_NEWCOST", "BG2 1.0.8 non-legacy Cut cost");
        } else {
            contains(energy, "CUTPASTEGADGET_COST", "native Cut/Paste energy parity");
        }

        String mixins = read(module.resolve("src/main/resources/buildinggadgetsextra.mixins.json"));
        contains(mixins, "BuildingUtilsMultitoolEnergyMixin", "active-profile energy mixin registration");

        String cut = source("network/" + ("forge".equals(loader)
                ? "MultitoolCutPacket.java" : "MultitoolCutHandler.java"));
        assertFalse(cut.contains(".cutAndStore("), label("multitool Cut must not call the native class-guarded cutAndStore"));
        contains(cut, "accepted", "accepted-block Cut accounting");
        contains(cut, "CUT_NO_VALID_BLOCKS", "zero-valid-block Cut guard");
    }

    @Test
    void newerBg2PortsPatchTheNativeEmptyCutCrash() throws Exception {
        if (legacyCut || "forge".equals(loader)) return;

        String mixins = read(module.resolve("src/main/resources/buildinggadgetsextra.mixins.json"));
        contains(mixins, "GadgetCutPasteEmptyCutMixin", "native BG2 empty-Cut guard registration");

        String patch = source("mixin/GadgetCutPasteEmptyCutMixin.java");
        contains(patch, "remap = false", "BG2-owned target must not be remapped as Minecraft code");
        contains(patch, "setCutStart", "guard immediately before the upstream null dereference");
        contains(patch, "ServerTickHandler.gadgetWorking", "detect whether native Cut queued any work");
        contains(patch, "ci.cancel()", "abort empty native Cut before upstream state mutation");
    }

    @Test
    void forge1201RangePacketPersistsAndPublishesTheAuthoritativeStack() throws Exception {
        if (!"1.20.1".equals(minecraftVersion) || !"forge".equals(loader)) return;

        String packet = source("network/MultitoolRangePacket.java");
        contains(packet, "BuildersMultitool", "multitool-specific range acceptance");
        contains(packet, "MultitoolRangePolicy.resolve", "shared server-authoritative range validation");
        contains(packet, "GadgetNBT.setToolRange", "server-authoritative range persistence");
        contains(packet, "containerMenu.broadcastChanges", "server-to-client held-stack publication");

        String upstreamBridge = source("mixin/PacketRangeChangeMultitoolMixin.java");
        contains(upstreamBridge, "MultitoolRangePacket.apply", "BG2 radial range packet bridge");

        String network = source("network/ExtraNetwork.java");
        contains(network, "MultitoolRangePacket.class", "range hotkey packet registration");

        String gameTest = source("gametest/MultitoolGameTests.java");
        contains(gameTest, "rangePacketPersistsAndSynchronizesHeldStack", "runtime range sync regression");
        contains(gameTest, "ContainerSynchronizer", "runtime client-bound inventory sync observation");
        contains(gameTest, "clientSlotUpdate", "runtime synchronized stack assertion");
    }

    @Test
    void neoForge2612RegistersTheMultitoolEnergyCapability() throws Exception {
        if (!"26.1.2".equals(minecraftVersion)) return;

        String registration = source("setup/ExtraRegistration.java");
        contains(registration, "RegisterCapabilitiesEvent", "26.1.2 explicit capability registration");
        contains(registration, "Capabilities.Energy.ITEM", "26.1.2 Transfer API energy capability");
        contains(registration, "ItemAccessEnergyHandler", "BG2 1.4.6-compatible energy handler");
        contains(registration, "BG2DataComponents.FORGE_ENERGY", "BG2-owned energy data component");
        contains(registration, "BUILDERS_MULTITOOL.get()", "multitool capability target");
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
