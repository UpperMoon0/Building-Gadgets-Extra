package com.nstut.buildinggadgetsextra.client;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class ModifierScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int profile;
    private final EditBox[] fields = new EditBox[7];
    private int order;
    private Button orderButton;
    private String error = "";
    private static final String[] LABELS = {"Array copies (1-16)", "Array step X (-64-64)", "Array step Y (-64-64)", "Array step Z (-64-64)", "Radial copies (1-16)", "Radial pivot X (-64-64)", "Radial pivot Z (-64-64)"};
    public ModifierScreen(Screen parent, ItemStack stack) {
        super(Component.literal("Live build modifiers")); this.parent=parent; this.stack=stack;
        profile=MultitoolState.getActiveMode(stack).ordinal();
    }
    protected void init() {
        LiveModifiers settings=MultitoolState.getLiveModifiers(stack); order=settings.value(7);
        for(int i=0;i<fields.length;i++) {
            int x=width/2-150+(i<4?0:160), y=height/2-70+(i<4?i:i-4)*34;
            fields[i]=new EditBox(font,x,y,140,18,Component.literal(LABELS[i]));
            fields[i].setMaxLength(4); fields[i].setValue(Integer.toString(settings.value(i)));
            addRenderableWidget(fields[i]);
        }
        orderButton=addRenderableWidget(Button.builder(Component.literal(LiveModifiers.ORDERS[order]), button -> {
            order=(order+1)%LiveModifiers.ORDERS.length; button.setMessage(Component.literal(LiveModifiers.ORDERS[order]));
        }).bounds(width/2-150,height/2+76,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> apply()).bounds(width/2-150,height/2+102,94,20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            for(int i=0;i<fields.length;i++) fields[i].setValue(Integer.toString(LiveModifiers.NONE.value(i)));
            order=0; orderButton.setMessage(Component.literal(LiveModifiers.ORDERS[order])); error="";
        }).bounds(width/2-47,height/2+102,94,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(width/2+56,height/2+102,94,20).build());
    }
    private void apply() {
        try {
            int[] values=new int[LiveModifiers.FIELDS]; for(int i=0;i<fields.length;i++) values[i]=Integer.parseInt(fields[i].getValue()); values[7]=order;
            LiveModifiers settings=LiveModifiers.from(values);
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.ModifierPayload(profile,settings));
            onClose();
        } catch(IllegalArgumentException invalid) { error="Copies: 1-16; steps/pivots: -64-64."; }
    }
    public void onClose() { minecraft.setScreen(parent); }
    public boolean isPauseScreen() { return false; }
    public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        graphics.fill(width/2-164,height/2-114,width/2+164,height/2+134,0xEE181818);
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
        graphics.centeredText(font,title,width/2,height/2-107,0xFFFFFF);
        graphics.centeredText(font,"Counts include original. Radial rotates around Y.",width/2,height/2-94,0xBBBBBB);
        for(int i=0;i<fields.length;i++) graphics.text(font,LABELS[i],width/2-150+(i<4?0:160),height/2-82+(i<4?i:i-4)*34,0xFFFFFF,false);
        graphics.centeredText(font,error,width/2,height/2+63,0xFF7777);
    }
}
