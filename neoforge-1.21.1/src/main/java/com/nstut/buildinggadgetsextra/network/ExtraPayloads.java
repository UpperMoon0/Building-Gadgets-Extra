package com.nstut.buildinggadgetsextra.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ExtraPayloads {
    private ExtraPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(MultitoolSelectionPayload.TYPE, MultitoolSelectionPayload.STREAM_CODEC,
                MultitoolSelectionHandler::handle);
        registrar.playToServer(MultitoolCutPayload.TYPE, MultitoolCutPayload.STREAM_CODEC,
                MultitoolCutHandler::handle);
        registrar.playToServer(MirrorPayload.TYPE, MirrorPayload.STREAM_CODEC, MirrorPayloadHandler::handle);
        registrar.playToServer(StructureFilePayload.TYPE, StructureFilePayload.STREAM_CODEC,
                StructureFilePayloadHandler::handle);
        registrar.playToServer(StructureUploadPayload.TYPE,StructureUploadPayload.STREAM_CODEC,StructureUploadHandler::handle);
        registrar.playToClient(StructureDownloadPayload.TYPE,StructureDownloadPayload.STREAM_CODEC,
                (payload,context)->context.enqueueWork(()->com.nstut.buildinggadgetsextra.client.ClientStructureFiles.receive(payload)));
    }
}
