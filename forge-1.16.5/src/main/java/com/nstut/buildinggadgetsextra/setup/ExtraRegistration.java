package com.nstut.buildinggadgetsextra.setup;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ExtraRegistration {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExtraConstants.MOD_ID);
    public static final RegistryObject<Item> BUILDERS_MULTITOOL = ITEMS.register("builders_multitool", BuildersMultitool::new);
    private ExtraRegistration() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
