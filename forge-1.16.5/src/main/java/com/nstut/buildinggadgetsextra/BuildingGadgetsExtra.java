package com.nstut.buildinggadgetsextra;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.Mixins;

@Mod(ExtraConstants.MOD_ID)
public final class BuildingGadgetsExtra {
    public BuildingGadgetsExtra() {
        // ForgeGradle 1.16 development launches do not read MixinConfigs from the
        // output JAR manifest. Registering here keeps runClient and packaged JARs
        // on the same path; Mixin ignores a configuration already discovered from
        // the manifest.
        Mixins.addConfiguration("buildinggadgetsextra.mixins.json");
        ExtraNetwork.register();
    }
}
