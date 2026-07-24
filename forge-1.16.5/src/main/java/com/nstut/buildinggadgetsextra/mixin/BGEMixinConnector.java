package com.nstut.buildinggadgetsextra.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public final class BGEMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("buildinggadgetsextra.mixins.json");
    }
}
