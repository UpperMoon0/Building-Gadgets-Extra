package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.KeyBindings;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BuildingGadgetsExtra.MODID, value = Dist.CLIENT)
public final class MultitoolClientEvents {
    private MultitoolClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack stack = BaseGadget.getGadget(minecraft.player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return;
        if (KeyBindings.menuSettings.consumeClick()) minecraft.setScreen(new MultitoolRadialScreen(stack));
    }
}


