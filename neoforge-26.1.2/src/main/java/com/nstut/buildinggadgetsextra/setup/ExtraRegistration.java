package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ExtraRegistration {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildingGadgetsExtra.MODID);
    public static final DeferredItem<BuildersMultitool> BUILDERS_MULTITOOL = ITEMS.registerItem(
            "builders_multitool", BuildersMultitool::new,
            () -> new Item.Properties().stacksTo(1).enchantable(3));

    private ExtraRegistration() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ExtraRegistration::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().identifier().getNamespace().equals("buildinggadgets2")) {
            event.accept(BUILDERS_MULTITOOL.get());
        }
    }
}

