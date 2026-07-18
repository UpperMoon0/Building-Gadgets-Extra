package com.nstut.buildinggadgetsextra.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ExtraPayloads {
    private ExtraPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MirrorPayload.TYPE, MirrorPayload.STREAM_CODEC, MirrorPayloadHandler::handle);
    }
}
