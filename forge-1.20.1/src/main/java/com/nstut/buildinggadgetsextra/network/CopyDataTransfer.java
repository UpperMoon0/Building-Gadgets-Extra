package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.BoundedByteArrayOutputStream;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.util.Arrays;
import java.util.UUID;

/** Bounded, template-specific transport; vanilla and unrelated NBT quotas stay unchanged. */
public final class CopyDataTransfer {
    private CopyDataTransfer() {}

    public static byte[] encode(CompoundTag tag) throws IOException {
        validate(tag);
        BoundedByteArrayOutputStream output = new BoundedByteArrayOutputStream(ExtraConstants.MAX_STRUCTURE_FILE_BYTES);
        NbtIo.write(tag, new DataOutputStream(output));
        return output.toByteArray();
    }

    public static CompoundTag decode(byte[] bytes) throws IOException {
        if (bytes.length > ExtraConstants.MAX_STRUCTURE_FILE_BYTES) throw new IOException("Copy data exceeds byte budget");
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        CompoundTag tag = NbtIo.read(input, new NbtAccounter(ExtraConstants.MAX_STRUCTURE_NBT_BYTES));
        if (input.available() != 0) throw new IOException("Trailing copy data");
        validate(tag);
        return tag;
    }

    private static void validate(CompoundTag tag) throws IOException {
        if (tag.isEmpty()) return; // BG2 represents an empty copy with an empty compound.
        if (!(tag.get("startpos") instanceof CompoundTag start)
                || !(tag.get("endpos") instanceof CompoundTag end)
                || !(tag.get("statelist") instanceof IntArrayTag states)
                || !(tag.get("blockstatemap") instanceof ListTag palette)
                || palette.isEmpty() || palette.size() > ExtraConstants.HARD_MAX_COPY_VOLUME) {
            throw new IOException("Invalid copy template fields");
        }
        long volume = 1L;
        for (String axis : new String[]{"X", "Y", "Z"}) {
            if (!(start.get(axis) instanceof IntTag) || !(end.get(axis) instanceof IntTag)) {
                throw new IOException("Invalid copy template coordinates");
            }
            long length = Math.abs((long) start.getInt(axis) - end.getInt(axis)) + 1L;
            if (length > ExtraConstants.HARD_MAX_COPY_VOLUME / volume) {
                throw new IOException("Copy template exceeds maximum volume");
            }
            volume *= length;
        }
        int[] indices = states.getAsIntArray();
        if (indices.length != volume) throw new IOException("Copy template volume mismatch");
        for (int index : indices) {
            if (index < 0 || index >= palette.size()) throw new IOException("Invalid copy palette index");
        }
        for (int i = 0; i < palette.size(); i++) {
            if (!(palette.get(i) instanceof CompoundTag)) throw new IOException("Invalid copy palette entry");
        }
    }

    public static void send(ServerPlayer player, UUID gadgetId, UUID copyId, CompoundTag tag) {
        final byte[] bytes;
        try {
            bytes = encode(tag);
        } catch (IOException invalid) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Copy preview exceeds the safe transfer limits."), false);
            return;
        }
        UUID transferId = UUID.randomUUID();
        int total = (bytes.length + ExtraConstants.STRUCTURE_CHUNK_SIZE - 1) / ExtraConstants.STRUCTURE_CHUNK_SIZE;
        for (int index = 0; index < total; index++) {
            int offset = index * ExtraConstants.STRUCTURE_CHUNK_SIZE;
            ExtraNetwork.sendToPlayer(player, new CopyDataPacket(transferId, gadgetId, copyId, index, total,
                    Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + ExtraConstants.STRUCTURE_CHUNK_SIZE))));
        }
    }
}
