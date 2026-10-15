package com.nstut.buildinggadgetsextra.client;
import com.nstut.buildinggadgetsextra.client.ui.ModifierPanel;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.openui.api.UIComponent;
import net.minecraft.world.item.ItemStack;
import java.util.function.*;
/** Native packet/item adapter. OpenUI owns all modifier controls and interaction. */
public final class InlineModifiers {
    public final ModifierPanel panel;
    public InlineModifiers(Supplier<ItemStack> held) {
        final int profile=MultitoolState.getActiveMode(held.get()).ordinal();
        panel=new ModifierPanel(()->MultitoolState.getActiveMode(held.get()).ordinal(),
                ()->MultitoolState.getLiveModifiers(held.get()),
                ()->MultitoolState.isLiveMirrorHorizontal(held.get()), ()->MultitoolState.isLiveMirrorVertical(held.get()),
                settings->net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.ModifierPayload(profile,settings)),
                vertical->net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPayload(vertical)));
    }
    public void restoreFrom(InlineModifiers previous) { if(previous!=null) panel.restoreFrom(previous.panel); }
    public void expand(int stage) { panel.expand(stage); }
    public void revealField(int index) { panel.revealField(index); }
    public boolean flush() { return panel.flush(); }
    public void navigate(Runnable action,boolean discard) { panel.navigate(action,discard); }
    public void tick() { panel.tick(); }
    public boolean enter() { for(int i=0;i<7;i++) if(panel.field(i).isFocused()) return panel.field(i).keyPressed(257,0,0); return false; }
    public boolean needsCommitBeforeClick(double x,double y) { return false; }
}
