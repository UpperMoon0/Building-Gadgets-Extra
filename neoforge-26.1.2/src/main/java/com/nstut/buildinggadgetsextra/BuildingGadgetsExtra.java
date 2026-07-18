package com.nstut.buildinggadgetsextra;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BuildingGadgetsExtra.MODID)
public final class BuildingGadgetsExtra {
    public static final String MODID = ExtraConstants.MOD_ID;

    public BuildingGadgetsExtra(IEventBus modEventBus) {
        modEventBus.addListener(ExtraPayloads::register);
    }
}
