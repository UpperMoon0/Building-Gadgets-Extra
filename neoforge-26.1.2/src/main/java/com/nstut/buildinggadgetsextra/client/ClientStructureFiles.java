package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.StructureDownloads;
import com.nstut.packetweave.api.ChunkedTransfer;
import java.time.Duration;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.PendingSaveTarget;
import com.nstut.buildinggadgetsextra.network.StructureDownloadPayload;
import com.nstut.buildinggadgetsextra.network.StructureFilePayload;
import com.nstut.buildinggadgetsextra.network.StructureUploadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ClientStructureFiles {
    // Packet receive always runs on the Minecraft client thread. Keep this map
    // strictly client-thread-owned; the native file-dialog thread never touches it.
    private static final StructureDownloads DOWNLOADS = new StructureDownloads();
    private static final Map<UUID, PendingSaveTarget> SAVE_DESTINATIONS = new HashMap<>();

    private ClientStructureFiles() {}
    private static volatile Object connection;
    private static volatile long generation;
    public static void tick() {
        Object current=Minecraft.getInstance().getConnection();
        if(current!=connection) {
            connection=current; generation++;
            DOWNLOADS.clear();
            synchronized(SAVE_DESTINATIONS) { SAVE_DESTINATIONS.clear(); }
        }
        pruneDownloads(); pruneSaveDestinations();
    }


    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("building_gadgets_extra").resolve("structures");
    }

    public static void chooseSave() {
        final long requestGeneration=generation;
        dialogThread(() -> {
            try {
                Files.createDirectories(root());
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    String result = TinyFileDialogs.tinyfd_saveFileDialog(
                            Component.translatable(ExtraConstants.DIALOG_SAVE_STRUCTURE).getString(),
                            root().resolve("structure.nbt").toString(), filters(stack),
                            Component.translatable(ExtraConstants.DIALOG_NBT_FILES).getString());
                    if (result == null) return;
                    Path path = Path.of(result);
                    if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbt")) {
                        path = Path.of(result + ".nbt");
                    }
                    String name = transferName(path);
                    UUID requestId = UUID.randomUUID();
                    pruneSaveDestinations();
                    synchronized (SAVE_DESTINATIONS) {
                        if (SAVE_DESTINATIONS.size() >= ExtraConstants.MAX_STRUCTURE_TRANSFERS_PER_PLAYER) {
                            message(ExtraConstants.BUSY);
                            return;
                        }
                        SAVE_DESTINATIONS.put(requestId, new PendingSaveTarget(path));
                    }
                    Minecraft.getInstance().execute(() -> {
                        if(requestGeneration==generation && connection!=null) ClientPacketDistributor.sendToServer(new StructureFilePayload(false, name, requestId));
                        else removeDestination(requestId);
                    });
                }
            } catch (Exception error) {
                message(ExtraConstants.STRUCTURE_SAVE_FAILED, "structure");
            }
        });
    }

    public static void chooseLoad() {
        final long requestGeneration=generation;
        dialogThread(() -> {
            try {
                Files.createDirectories(root());
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    String result = TinyFileDialogs.tinyfd_openFileDialog(
                            Component.translatable(ExtraConstants.DIALOG_OPEN_STRUCTURE).getString(),
                            root().resolve("structure.nbt").toString(), filters(stack),
                            Component.translatable(ExtraConstants.DIALOG_NBT_FILES).getString(), false);
                    if (result != null && requestGeneration==generation) upload(Path.of(result));
                }
            } catch (Exception error) {
                message(ExtraConstants.STRUCTURE_LOAD_FAILED, "structure");
            }
        });
    }

    public static void receive(StructureDownloadPayload payload) {
        pruneDownloads();
        pruneSaveDestinations();
        if (!hasDestination(payload.transferId())) {
            DOWNLOADS.cancel(payload.transferId());
            return;
        }
        try {
            byte[] bytes = DOWNLOADS.accept(payload.transferId(), payload.name(), payload.index(), payload.total(), payload.data());
            if (bytes == null) {
                if (!DOWNLOADS.active(payload.transferId())) removeDestination(payload.transferId());
                return;
            }

            Path file = removeDestination(payload.transferId());
            if (file == null) return;
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, bytes);
            message(ExtraConstants.STRUCTURE_SAVED, file.getFileName().toString());
        } catch (Exception error) {
            DOWNLOADS.cancel(payload.transferId());
            removeDestination(payload.transferId());
            message(ExtraConstants.STRUCTURE_SAVE_FAILED, payload.name());
        }
    }

    private static void upload(Path file) {
        final Object uploadConnection=Minecraft.getInstance().getConnection();
        String name = transferName(file);
        try {
            UUID id = UUID.randomUUID();
            ChunkedTransfer.streamFileBatched(file, ExtraConstants.STRUCTURE_CHUNK_SIZE,
                    ExtraConstants.MAX_STRUCTURE_FILE_BYTES, 4, Minecraft.getInstance()::execute, Duration.ofSeconds(30),
                    (index, total, bytes) -> {
                        if(uploadConnection==null || Minecraft.getInstance().getConnection()!=uploadConnection) throw new java.io.IOException("Disconnected");
                        ClientPacketDistributor.sendToServer(new StructureUploadPayload(id, name, index, total, bytes));
                    });
        } catch (Exception error) {
            message(ExtraConstants.STRUCTURE_LOAD_FAILED, file.getFileName().toString());
        }
    }

    private static void pruneDownloads() {
        DOWNLOADS.reapExpired();
    }

    private static void pruneSaveDestinations() {
        synchronized (SAVE_DESTINATIONS) {
            SAVE_DESTINATIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    private static boolean hasDestination(UUID requestId) {
        synchronized (SAVE_DESTINATIONS) {
            return SAVE_DESTINATIONS.containsKey(requestId);
        }
    }

    private static Path removeDestination(UUID requestId) {
        synchronized (SAVE_DESTINATIONS) {
            PendingSaveTarget target = SAVE_DESTINATIONS.remove(requestId);
            return target == null ? null : target.path();
        }
    }

    private static PointerBuffer filters(MemoryStack stack) {
        PointerBuffer filters = stack.mallocPointer(1);
        filters.put(stack.UTF8("*.nbt"));
        return filters.flip();
    }

    private static String transferName(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".nbt")) name = name.substring(0, name.length() - 4);
        name = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return name.isEmpty() ? "structure" : name;
    }

    private static void dialogThread(Runnable action) {
        Thread thread = new Thread(action, "Building Gadgets Extra File Dialog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void message(String key, Object... arguments) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendOverlayMessage(Component.translatable(key, arguments));
            }
        });
    }
}
