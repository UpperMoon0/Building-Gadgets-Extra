package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.ChunkAccumulator;
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ClientStructureFiles {
    private static final Map<UUID, ChunkAccumulator> DOWNLOADS = new HashMap<>();
    private static final Map<String, Deque<PendingSaveTarget>> DESTINATIONS = new HashMap<>();

    private ClientStructureFiles() {}

    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("building_gadgets_extra").resolve("structures");
    }

    public static void chooseSave() {
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
                    pruneTransfers();
                    synchronized (DESTINATIONS) {
                        DESTINATIONS.computeIfAbsent(name, key -> new ArrayDeque<>())
                                .add(new PendingSaveTarget(path));
                    }
                    Minecraft.getInstance().execute(
                            () -> ExtraNetwork.sendToServer(new StructureFilePacket(false, name)));
                }
            } catch (Exception error) {
                message(ExtraConstants.STRUCTURE_SAVE_FAILED, "structure");
            }
        });
    }

    public static void chooseLoad() {
        dialog(() -> {
            try {
                Files.createDirectories(root());
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    String result = TinyFileDialogs.tinyfd_openFileDialog(
                            new TranslationTextComponent(ExtraConstants.DIALOG_OPEN_STRUCTURE).getString(),
                            root().resolve("structure.nbt").toString(), filters(stack),
                            new TranslationTextComponent(ExtraConstants.DIALOG_NBT_FILES).getString(), false);
                    if (result != null) upload(Paths.get(result));
                }
            } catch (Exception error) {
                message(ExtraConstants.STRUCTURE_LOAD_FAILED, "structure");
            }
        });
    }

    public static void receive(StructureDownloadPacket packet) {
        pruneTransfers();
        try {
            ChunkAccumulator transfer = DOWNLOADS.computeIfAbsent(packet.id,
                    key -> new ChunkAccumulator(packet.total));
            if (!transfer.accept(packet.index, packet.data)) {
                DOWNLOADS.remove(packet.id);
                return;
            }
            if (!transfer.isComplete()) return;

            DOWNLOADS.remove(packet.id);
            Path file = pollDestination(packet.name);
            if (file == null) file = root().resolve(packet.name + ".nbt");
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.write(file, transfer.join());
            message(ExtraConstants.STRUCTURE_SAVED, file.getFileName().toString());
        } catch (Exception error) {
            DOWNLOADS.remove(packet.id);
            message(ExtraConstants.STRUCTURE_SAVE_FAILED, packet.name);
        }
    }

    private static void upload(Path file) {
        String name = name(file);
        try {
            if (Files.size(file) > ExtraConstants.MAX_STRUCTURE_FILE_BYTES) throw new IllegalArgumentException();
            byte[] bytes = Files.readAllBytes(file);
            UUID id = UUID.randomUUID();
            int total = Math.max(1, (bytes.length + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1)
                    / ExtraConstants.STRUCTURE_CHUNK_SIZE);
            Minecraft.getInstance().execute(() -> {
                for (int i = 0; i < total; i++) {
                    int start = i * ExtraConstants.STRUCTURE_CHUNK_SIZE;
                    int end = Math.min(bytes.length, start + ExtraConstants.STRUCTURE_CHUNK_SIZE);
                    ExtraNetwork.sendToServer(new StructureUploadPacket(
                            id, name, i, total, Arrays.copyOfRange(bytes, start, end)));
                }
            });
        } catch (Exception error) {
            message(ExtraConstants.STRUCTURE_LOAD_FAILED, file.getFileName().toString());
        }
    }

    private static void pruneTransfers() {
        DOWNLOADS.entrySet().removeIf(entry -> entry.getValue().isExpired());
        synchronized (DESTINATIONS) {
            DESTINATIONS.values().forEach(queue -> queue.removeIf(PendingSaveTarget::isExpired));
            DESTINATIONS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
    }

    private static Path pollDestination(String name) {
        synchronized (DESTINATIONS) {
            Deque<PendingSaveTarget> queue = DESTINATIONS.get(name);
            PendingSaveTarget target = queue == null ? null : queue.poll();
            if (queue != null && queue.isEmpty()) DESTINATIONS.remove(name);
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
