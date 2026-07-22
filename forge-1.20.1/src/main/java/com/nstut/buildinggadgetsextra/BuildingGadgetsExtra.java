package com.nstut.buildinggadgetsextra;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExtraConstants.MOD_ID)
public final class BuildingGadgetsExtra {
    public BuildingGadgetsExtra() {
        ExtraRegistration.register(FMLJavaModLoadingContext.get().getModEventBus());
        ExtraNetwork.register();
    }
}
