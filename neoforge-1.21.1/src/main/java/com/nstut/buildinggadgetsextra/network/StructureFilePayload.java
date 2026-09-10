package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record StructureFilePayload(boolean load, String name, UUID requestId) implements CustomPacketPayload {
    public static final Type<StructureFilePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BuildingGadgetsExtra.MODID, "structure_file"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StructureFilePayload> STREAM_CODEC =
            StreamCodec.of(StructureFilePayload::write, StructureFilePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, StructureFilePayload value) {
        buffer.writeBoolean(value.load);
        buffer.writeUtf(value.name, 128);
        buffer.writeUUID(value.requestId);
    }

    private static StructureFilePayload read(RegistryFriendlyByteBuf buffer) {
        return new StructureFilePayload(buffer.readBoolean(), buffer.readUtf(128), buffer.readUUID());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
