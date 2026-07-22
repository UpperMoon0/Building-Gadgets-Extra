package com.nstut.buildinggadgetsextra.setup;

import com.direwolf20.buildinggadgets2.common.capabilities.EnergyStorageItemstack;
import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExtraRegistration {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BuildingGadgetsExtra.MODID);
    public static final DeferredHolder<Item, BuildersMultitool> BUILDERS_MULTITOOL =
            ITEMS.register("builders_multitool", BuildersMultitool::new);

    private ExtraRegistration() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ExtraRegistration::registerCapabilities);
        bus.addListener(ExtraRegistration::addCreativeTabContents);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new EnergyStorageItemstack(((BuildersMultitool) stack.getItem()).getEnergyMax(), stack),
                BUILDERS_MULTITOOL.get());
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().getNamespace().equals("buildinggadgets2")) {
            event.accept(BUILDERS_MULTITOOL.get());
        }
    }
}
