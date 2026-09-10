package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.KeyBindings;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.data.RangeChangePayload;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.BuildingGadgetsExtra;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.OptionalInt;

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

        if (KeyBindings.menuSettings.consumeClick()) {
            minecraft.setScreen(new MultitoolRadialScreen(stack));
            return;
        }
        if (KeyBindings.range.consumeClick()) {
            OptionalInt nextRange = MultitoolRangePolicy.next(
                    MultitoolState.getActiveMode(stack),
                    GadgetNBT.getToolRange(stack),
                    ExtraConfig.multitoolMaxRange());
            if (!nextRange.isPresent()) return;
            ClientPacketDistributor.sendToServer(new RangeChangePayload(nextRange.getAsInt()));
        }
    }
}
