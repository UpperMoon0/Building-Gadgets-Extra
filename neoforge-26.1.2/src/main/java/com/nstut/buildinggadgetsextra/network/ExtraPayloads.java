package com.nstut.buildinggadgetsextra.network;

import com.nstut.buildinggadgetsextra.client.ClientStructureFiles;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ExtraPayloads {
    private ExtraPayloads() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("8");
        registrar.playToClient(CopyDataPayload.TYPE, CopyDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.nstut.buildinggadgetsextra.client.ClientCopyData.receive(payload)));
        registrar.playToServer(ModifierPayload.TYPE, ModifierPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
                        ModifierHandler.apply(player, payload.profile(), payload.values());
                }));
        registrar.playToServer(MirrorPayload.TYPE, MirrorPayload.STREAM_CODEC, MirrorPayloadHandler::handle);
        registrar.playToServer(MultitoolSelectionPayload.TYPE, MultitoolSelectionPayload.STREAM_CODEC,
                MultitoolSelectionHandler::handle);
        registrar.playToServer(MultitoolCutSelectionPayload.TYPE, MultitoolCutSelectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player)
                        MultitoolCutSelectionHandler.apply(player, payload.start(), payload.end());
                }));
        registrar.playToServer(MultitoolCutPayload.TYPE, MultitoolCutPayload.STREAM_CODEC,
                MultitoolCutHandler::handle);
        registrar.playToServer(StructureFilePayload.TYPE, StructureFilePayload.STREAM_CODEC,
                StructureFilePayloadHandler::handle);
        registrar.playToServer(StructureUploadPayload.TYPE, StructureUploadPayload.STREAM_CODEC,
                StructureUploadHandler::handle);
        registrar.playToClient(StructureDownloadPayload.TYPE, StructureDownloadPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientStructureFiles.receive(payload)));
    }
}
