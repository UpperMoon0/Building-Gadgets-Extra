package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

public final class ExtraNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ExtraConstants.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ExtraNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, MirrorPacket.class,
                MirrorPacket::encode, MirrorPacket::decode, MirrorPacket::handle);
        CHANNEL.registerMessage(1, StructureFilePacket.class,
                StructureFilePacket::encode, StructureFilePacket::decode, StructureFilePacket::handle);
        CHANNEL.registerMessage(2,StructureUploadPacket.class,StructureUploadPacket::encode,StructureUploadPacket::decode,StructureUploadPacket::handle);
        CHANNEL.registerMessage(3,StructureDownloadPacket.class,StructureDownloadPacket::encode,StructureDownloadPacket::decode,StructureDownloadPacket::handle);
    }

    public static void sendToServer(MirrorPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(StructureFilePacket packet) { CHANNEL.sendToServer(packet); }
    public static void sendToServer(StructureUploadPacket packet){CHANNEL.sendToServer(packet);}
    public static void sendToPlayer(ServerPlayer player,StructureDownloadPacket packet){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);}
}
