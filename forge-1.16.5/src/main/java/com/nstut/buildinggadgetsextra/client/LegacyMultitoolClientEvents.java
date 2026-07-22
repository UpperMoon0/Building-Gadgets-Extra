package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.KeyBindings;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ExtraConstants.MOD_ID,value=Dist.CLIENT)
public final class LegacyMultitoolClientEvents {
    private LegacyMultitoolClientEvents() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null)return;
        ItemStack stack=AbstractGadget.getGadget(mc.player);
        if(stack.getItem() instanceof BuildersMultitool&&KeyBindings.menuSettings.consumeClick())mc.setScreen(new LegacyMultitoolScreen(stack));
    }
}
