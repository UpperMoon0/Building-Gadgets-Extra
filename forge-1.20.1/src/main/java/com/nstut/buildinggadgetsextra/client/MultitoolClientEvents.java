package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.KeyBindings;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.MultitoolRangePacket;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class MultitoolClientEvents {
    private MultitoolClientEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack stack = BaseGadget.getGadget(minecraft.player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return;

        if (KeyBindings.menuSettings.consumeClick()) {
            minecraft.setScreen(new MultitoolRadialScreen(stack));
            return;
        }
        if (KeyBindings.range.consumeClick()) {
            MultitoolMode mode = MultitoolState.getActiveMode(stack);
            if (mode != MultitoolMode.BUILD && mode != MultitoolMode.EXCHANGING) return;
            int max = ExtraConfig.multitoolMaxRange();
            int oldRange = GadgetNBT.getToolRange(stack);
            int newRange = oldRange >= max ? 1 : oldRange + 1;
            ExtraNetwork.sendToServer(new MultitoolRangePacket(newRange));
        }
    }
}
