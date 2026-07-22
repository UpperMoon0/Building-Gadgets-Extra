package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.renders.CopyPasteRender;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public final class LegacyMultitoolRenderer extends CopyPasteRender {
    public static final LegacyMultitoolRenderer INSTANCE = new LegacyMultitoolRenderer();
    private LegacyMultitoolRenderer() {}
    @Override public void render(RenderWorldLastEvent event, PlayerEntity player, ItemStack stack) {
        AbstractGadget delegate;
        MultitoolMode mode = MultitoolState.getActiveMode(stack);
        if (mode == MultitoolMode.BUILD) delegate = (AbstractGadget) OurItems.BUILDING_GADGET_ITEM.get();
        else if (mode == MultitoolMode.EXCHANGING) delegate = (AbstractGadget) OurItems.EXCHANGING_GADGET_ITEM.get();
        else if (mode == MultitoolMode.DESTRUCTION) delegate = (AbstractGadget) OurItems.DESTRUCTION_GADGET_ITEM.get();
        else delegate = (AbstractGadget) OurItems.COPY_PASTE_GADGET_ITEM.get();
        delegate.getRender().render(event, player, stack);
    }
    @Override public boolean isLinkable() { return true; }
}
