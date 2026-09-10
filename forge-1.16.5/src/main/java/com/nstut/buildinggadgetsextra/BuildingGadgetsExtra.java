package com.nstut.buildinggadgetsextra;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.setup.ExtraRegistration;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.spongepowered.asm.mixin.Mixins;

@Mod(ExtraConstants.MOD_ID)
public final class BuildingGadgetsExtra {
    public BuildingGadgetsExtra() {
        Mixins.addConfiguration("buildinggadgetsextra.mixins.json");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ExtraConfig.SPEC);
        ExtraRegistration.register(FMLJavaModLoadingContext.get().getModEventBus());
        ExtraNetwork.register();
    }
}
