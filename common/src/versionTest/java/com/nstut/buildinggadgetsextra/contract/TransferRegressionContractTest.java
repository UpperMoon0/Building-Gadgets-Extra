package com.nstut.buildinggadgetsextra.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents structure transfer correlation, authorization, and client-thread ownership regressions. */
class TransferRegressionContractTest {
    private final Path module = Paths.get(requiredProperty("bge.moduleDir"));
    private final String minecraftVersion = requiredProperty("bge.minecraftVersion");
    private final String loader = requiredProperty("bge.loader");

    @Test
    void saveResponsesAreRequestBoundAndDownloadStateStaysClientThreadOwned() throws Exception {
        String client = source("client/ClientStructureFiles.java");
        contains(client, "Map<UUID, PendingSaveTarget>", "request-id keyed pending save targets");
        contains(client, "UUID requestId = UUID.randomUUID()", "fresh save request id");
        contains(client, "removeDestination", "response lookup by request id");
        contains(client, "pruneDownloads", "client-thread download cleanup");
        contains(client, "pruneSaveDestinations", "separately synchronized save-target cleanup");
        contains(client, "MAX_STRUCTURE_TRANSFERS_PER_PLAYER", "bounded outstanding client save responses");
        contains(client, "if (!hasDestination(", "unsolicited download rejection");
        assertFalse(client.contains("Deque<PendingSaveTarget>"), label("save targets must not be filename/FIFO correlated"));
        assertFalse(client.contains("private static void pruneTransfers"),
                label("dialog thread must not share a cleanup method that mutates DOWNLOADS"));
        assertFalse(client.contains("file = root().resolve("),
                label("server-provided download names must never select a client filesystem path"));

        int requestGuard = client.indexOf("if (!hasDestination(");
        int accumulator = client.indexOf("DOWNLOADS.computeIfAbsent");
        assertTrue(requestGuard >= 0 && accumulator > requestGuard,
                label("request-id authorization must happen before allocating download state"));

        int pendingCap = client.indexOf("SAVE_DESTINATIONS.size() >= ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER");
        int pendingPut = client.indexOf("SAVE_DESTINATIONS.put");
        assertTrue(pendingCap >= 0 && pendingPut > pendingCap,
                label("client pending-save cap must be enforced before retaining a destination"));

        String request = source("network/" + ("forge".equals(loader)
                ? "StructureFilePacket.java" : "StructureFilePayload.java"));
        contains(request, "requestId", "save request id on the wire");
        contains(request, "writeUUID", "save request id serialization");
        contains(request, "readUUID", "save request id deserialization");

        String handler = source("network/" + ("forge".equals(loader)
                ? "StructureFilePacket.java" : "StructureFilePayloadHandler.java"));
        contains(handler, "requestId", "server echoes the request id as the download transfer id");
    }

    @Test
    void uploadsRequirePasteModeAtStartAndThroughFinalCommit() throws Exception {
        String upload = source("network/" + ("forge".equals(loader)
                ? "StructureUploadPacket.java" : "StructureUploadHandler.java"));
        String pasteGuard = "1.16.5".equals(minecraftVersion)
                ? "GadgetCopyPaste.ToolMode.PASTE"
                : "instanceof Paste";

        String capture = section(upload, "private static TransferState capture", "private boolean matches");
        String matches = upload.substring(upload.indexOf("private boolean matches"));
        contains(capture, pasteGuard, "Paste-mode authorization at transfer capture");
        contains(matches, pasteGuard, "Paste-mode authorization during transfer revalidation");

        int perChunkRevalidation = upload.indexOf("if (!transfer.matches(player))");
        int acceptChunk = upload.indexOf("TRANSFERS.accept");
        assertTrue(perChunkRevalidation >= 0 && acceptChunk > perChunkRevalidation,
                label("authorization must be revalidated before accepting each upload chunk"));

        int complete = upload.indexOf("if (transfer.chunks.isComplete())");
        int finalRevalidation = upload.indexOf("if (transfer.matches(player))", complete);
        int commit = upload.indexOf("NativeStructureBridge.importStructure", finalRevalidation);
        assertTrue(complete >= 0 && finalRevalidation > complete && commit > finalRevalidation,
                label("Paste mode and gadget/profile identity must be revalidated immediately before import commit"));
    }

    @Test
    void uploadsHaveServerLifecycleCleanupAndGlobalBudgets() throws Exception {
        String upload = source("network/" + ("forge".equals(loader)
                ? "StructureUploadPacket.java" : "StructureUploadHandler.java"));
        contains(upload, "@", "event subscriber registration");
        contains(upload, "EventBusSubscriber(modid = ExtraConstants.MOD_ID", "server event subscriber");
        contains(upload, "UploadTransferRegistry<TransferState>", "shared bounded upload storage");
        contains(upload, "if (!TRANSFERS.put(key, transfer)) return;", "upload admission limit");
        contains(upload, "TRANSFERS.accept(key,", "global byte ceiling before chunk retention");
        contains(upload, "@SubscribeEvent\n    public static void onServerTick", "independent expiry event");
        contains(upload, "PlayerEvent.PlayerLoggedOutEvent", "logout event");
        contains(upload, "TRANSFERS.removePlayer(", "per-player logout cleanup");
        contains(upload, "ServerStoppedEvent event", "shutdown event");
        contains(upload, "TRANSFERS.clear();", "shutdown cleanup");
    }

    @Test
    void modernModeIdentifiersUseSmallWireBounds() throws Exception {
        if ("1.16.5".equals(minecraftVersion)) return;
        String selection = source("network/" + ("forge".equals(loader)
                ? "MultitoolSelectionPacket.java" : "MultitoolSelectionPayload.java"));
        if ("forge".equals(loader)) {
            contains(selection, "readUtf(128)", "bounded mode decoding");
            contains(selection, "writeUtf(packet.gadgetMode, 128)", "bounded mode encoding");
        } else {
            contains(selection, "ByteBufCodecs.stringUtf8(128)", "bounded mode codec");
        }
    }

    private String source(String relative) throws IOException {
        return read(module.resolve("src/main/java/com/nstut/buildinggadgetsextra").resolve(relative));
    }

    private String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), label("missing file " + path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private void contains(String source, String expected, String feature) {
        assertTrue(source.contains(expected), label(feature + " must contain " + expected));
    }

    private String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = start < 0 ? -1 : source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0 && end > start,
                label("unable to locate source section between " + startMarker + " and " + endMarker));
        return source.substring(start, end);
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
