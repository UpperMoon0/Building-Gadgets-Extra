package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ExtraRegistration {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExtraConstants.MOD_ID);
    public static final RegistryObject<Item> BUILDERS_MULTITOOL = ITEMS.register("builders_multitool", BuildersMultitool::new);
    private ExtraRegistration() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); bus.addListener(ExtraRegistration::addCreativeTabContents); }
    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().getNamespace().equals("buildinggadgets2")) event.accept(BUILDERS_MULTITOOL.get());
    }
}
