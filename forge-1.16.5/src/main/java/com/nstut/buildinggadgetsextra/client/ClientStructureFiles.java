package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.StructureDownloads;
import com.nstut.packetweave.api.ChunkedTransfer;
import java.time.Duration;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.PendingSaveTarget;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.StructureDownloadPacket;
import com.nstut.buildinggadgetsextra.network.StructureFilePacket;
import com.nstut.buildinggadgetsextra.network.StructureUploadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        dialog(() -> {
            try {
                Files.createDirectories(root());
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    String result = TinyFileDialogs.tinyfd_saveFileDialog(
                            new TranslationTextComponent(ExtraConstants.DIALOG_SAVE_STRUCTURE).getString(),
                            root().resolve("structure.nbt").toString(), filters(stack),
                            new TranslationTextComponent(ExtraConstants.DIALOG_NBT_FILES).getString());
                    if (result == null) return;
                    Path path = Paths.get(result);
                    if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbt")) {
                        path = Paths.get(result + ".nbt");
                    }
                    String name = name(path);
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
                        if(requestGeneration==generation && connection!=null) ExtraNetwork.sendToServer(new StructureFilePacket(false, name, requestId));
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
        dialog(() -> {
            try {
                Files.createDirectories(root());
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    String result = TinyFileDialogs.tinyfd_openFileDialog(
                            new TranslationTextComponent(ExtraConstants.DIALOG_OPEN_STRUCTURE).getString(),
                            root().resolve("structure.nbt").toString(), filters(stack),
                            new TranslationTextComponent(ExtraConstants.DIALOG_NBT_FILES).getString(), false);
                    if (result != null && requestGeneration==generation) upload(Paths.get(result));
                }
            } catch (Exception error) {
                message(ExtraConstants.STRUCTURE_LOAD_FAILED, "structure");
            }
        });
    }

    public static void receive(StructureDownloadPacket packet) {
        pruneDownloads();
        pruneSaveDestinations();
        if (!hasDestination(packet.id)) {
            DOWNLOADS.cancel(packet.id);
            return;
        }
        try {
            byte[] bytes = DOWNLOADS.accept(packet.id, packet.name, packet.index, packet.total, packet.data);
            if (bytes == null) {
                if (!DOWNLOADS.active(packet.id)) removeDestination(packet.id);
                return;
            }

            Path file = removeDestination(packet.id);
            if (file == null) return;
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, bytes);
            message(ExtraConstants.STRUCTURE_SAVED, file.getFileName().toString());
        } catch (Exception error) {
            DOWNLOADS.cancel(packet.id);
            removeDestination(packet.id);
            message(ExtraConstants.STRUCTURE_SAVE_FAILED, packet.name);
        }
    }

    private static void upload(Path file) {
        final Object uploadConnection=Minecraft.getInstance().getConnection();
        String name = name(file);
        try {
            UUID id = UUID.randomUUID();
            ChunkedTransfer.streamFileBatched(file, ExtraConstants.STRUCTURE_CHUNK_SIZE,
                    ExtraConstants.MAX_STRUCTURE_FILE_BYTES, 4, Minecraft.getInstance()::execute, Duration.ofSeconds(30),
                    (index, total, bytes) -> {
                        if(uploadConnection==null || Minecraft.getInstance().getConnection()!=uploadConnection) throw new java.io.IOException("Disconnected");
                        ExtraNetwork.sendToServer(new StructureUploadPacket(id, name, index, total, bytes));
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

    private static String name(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".nbt")) name = name.substring(0, name.length() - 4);
        name = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return name.isEmpty() ? "structure" : name;
    }

    private static void dialog(Runnable action) {
        Thread thread = new Thread(action, "Building Gadgets Extra File Dialog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void message(String key, Object... args) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(new TranslationTextComponent(key, args), true);
            }
        });
    }
}
