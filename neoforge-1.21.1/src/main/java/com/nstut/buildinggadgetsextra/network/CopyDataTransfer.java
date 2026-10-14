package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.BoundedByteArrayOutputStream;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Copy-specific bounds; never increases vanilla's global NBT decoder allowance. */
public final class CopyDataTransfer {
    private CopyDataTransfer() {}

    public static byte[] encode(CompoundTag tag) throws IOException {
        validate(tag);
        BoundedByteArrayOutputStream bytes =
                new BoundedByteArrayOutputStream(ExtraConstants.MAX_STRUCTURE_FILE_BYTES);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
        }
        return bytes.toByteArray();
    }

    public static CompoundTag decode(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > ExtraConstants.MAX_STRUCTURE_FILE_BYTES) {
            throw new IOException("Invalid copy transfer size");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            CompoundTag tag = NbtIo.read(input, NbtAccounter.create(ExtraConstants.MAX_STRUCTURE_NBT_BYTES));
            if (input.available() != 0) throw new IOException("Trailing copy transfer data");
            validate(tag);
            return tag;
        }
    }

    /** Validate geometry and palette lookups before BG2 allocates its StatePos list. */
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
}
