package com.nstut.buildinggadgetsextra;

import com.nstut.buildinggadgetsextra.network.ExtraPayloads;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BuildingGadgetsExtra.MODID)
public final class BuildingGadgetsExtra {
    public static final String MODID = ExtraConstants.MOD_ID;

    public BuildingGadgetsExtra(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ExtraConfig.SPEC);
        ExtraRegistration.register(modEventBus);
        modEventBus.addListener(ExtraPayloads::register);
    }
}
