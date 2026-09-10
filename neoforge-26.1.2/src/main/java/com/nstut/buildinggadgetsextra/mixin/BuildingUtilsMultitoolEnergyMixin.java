package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.setup.Config;
import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BuildingUtils.class, remap = false)
public abstract class BuildingUtilsMultitoolEnergyMixin {
    @Inject(method = "getEnergyCost", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$activeEnergyCost(ItemStack gadget,
                                                               CallbackInfoReturnable<Integer> cir) {
        if (gadget.getItem() instanceof BuildersMultitool) {
            cir.setReturnValue(cost(MultitoolState.getActiveMode(gadget)));
        }
    }

    @Inject(method = "useEnergy", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$useActiveEnergyCost(ItemStack gadget, CallbackInfo ci) {
        if (!(gadget.getItem() instanceof BuildersMultitool)) return;
        EnergyHandler energy = gadget.getCapability(Capabilities.Energy.ITEM, null);
        if (energy != null) {
            try (Transaction tx = Transaction.openRoot()) {
                energy.extract(cost(MultitoolState.getActiveMode(gadget)), tx);
                tx.commit();
            }
        }
        ci.cancel();
    }

    private static int cost(MultitoolMode mode) {
        return switch (mode) {
            case BUILD -> Config.BUILDINGGADGET_COST.get();
            case EXCHANGING -> Config.EXCHANGINGGADGET_COST.get();
            case COPY_PASTE -> Config.COPYPASTEGADGET_COST.get();
            case CUT_PASTE -> Config.CUTPASTEGADGET_COST.get();
            case DESTRUCTION -> Config.DESTRUCTIONGADGET_COST.get();
        };
    }
}
