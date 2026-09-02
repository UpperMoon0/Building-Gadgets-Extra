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
        assertFalse(client.contains("Deque<PendingSaveTarget>"), label("save targets must not be filename/FIFO correlated"));
        assertFalse(client.contains("private static void pruneTransfers"),
                label("dialog thread must not share a cleanup method that mutates DOWNLOADS"));

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
    void uploadsRequirePasteModeAtStartAndRevalidation() throws Exception {
        String upload = source("network/" + ("forge".equals(loader)
                ? "StructureUploadPacket.java" : "StructureUploadHandler.java"));
        String pasteGuard = "1.16.5".equals(minecraftVersion)
                ? "GadgetCopyPaste.ToolMode.PASTE"
                : "instanceof Paste";
        assertTrue(occurrences(upload, pasteGuard) >= 2,
                label("upload authorization must require Paste mode both at transfer capture and revalidation"));
        contains(upload, "if (!transfer.matches(player))", "per-chunk upload revalidation");
        contains(upload, "if (transfer.matches(player))", "final pre-commit upload revalidation");
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

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
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
