package com.nstut.buildinggadgetsextra.client;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.text.StringTextComponent;
public final class ModifierScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int profile;
    private final TextFieldWidget[] fields = new TextFieldWidget[7];
    private int order;
    private Button orderButton;
    private String error = "";
    private static final String[] LABELS = {"Array copies (1-16)", "Array step X (-64-64)", "Array step Y (-64-64)", "Array step Z (-64-64)", "Radial copies (1-16)", "Radial pivot X (-64-64)", "Radial pivot Z (-64-64)"};
    public ModifierScreen(Screen parent, ItemStack stack) {
        super(new StringTextComponent("Live build modifiers")); this.parent=parent; this.stack=stack;
        profile=MultitoolState.getActiveMode(stack).ordinal();
    }
    protected void init() {
        LiveModifiers settings=MultitoolState.getLiveModifiers(stack); order=settings.value(7);
        for(int i=0;i<fields.length;i++) {
            int x=width/2-150+(i<4?0:160), y=height/2-70+(i<4?i:i-4)*34;
            fields[i]=new TextFieldWidget(font,x,y,140,18,new StringTextComponent(LABELS[i]));
            fields[i].setMaxLength(4); fields[i].setValue(Integer.toString(settings.value(i)));
            addButton(fields[i]);
        }
        orderButton=addButton(new Button(width/2-150,height/2+76,300,20,new StringTextComponent(LiveModifiers.ORDERS[order]), button -> {
            order=(order+1)%LiveModifiers.ORDERS.length; button.setMessage(new StringTextComponent(LiveModifiers.ORDERS[order]));
        }));
        addButton(new Button(width/2-150,height/2+102,94,20,new StringTextComponent("Apply"), button -> apply()));
        addButton(new Button(width/2-47,height/2+102,94,20,new StringTextComponent("Reset"), button -> {
            for(int i=0;i<fields.length;i++) fields[i].setValue(Integer.toString(LiveModifiers.NONE.value(i)));
            order=0; orderButton.setMessage(new StringTextComponent(LiveModifiers.ORDERS[order])); error="";
        }));
        addButton(new Button(width/2+56,height/2+102,94,20,new StringTextComponent("Cancel"), button -> onClose()));
    }
    private void apply() {
        try {
            int[] values=new int[LiveModifiers.FIELDS]; for(int i=0;i<fields.length;i++) values[i]=Integer.parseInt(fields[i].getValue()); values[7]=order;
            LiveModifiers settings=LiveModifiers.from(values);
            com.nstut.buildinggadgetsextra.network.ExtraNetwork.sendToServer(new com.nstut.buildinggadgetsextra.network.ModifierPacket(profile,settings));
            onClose();
        } catch(IllegalArgumentException invalid) { error="Copies: 1-16; steps/pivots: -64-64."; }
    }
    public void onClose() { minecraft.setScreen(parent); }
    public boolean isPauseScreen() { return false; }
    public void render(MatrixStack graphics,int mouseX,int mouseY,float partialTick) {
        fill(graphics,width/2-164,height/2-114,width/2+164,height/2+134,0xEE181818);
        super.render(graphics,mouseX,mouseY,partialTick);
        drawCenteredString(graphics,font,title,width/2,height/2-107,0xFFFFFF);
        drawCenteredString(graphics,font,"Counts include original. Radial rotates around Y.",width/2,height/2-94,0xBBBBBB);
        for(int i=0;i<fields.length;i++) drawString(graphics,font,LABELS[i],width/2-150+(i<4?0:160),height/2-82+(i<4?i:i-4)*34,0xFFFFFF);
        drawCenteredString(graphics,font,error,width/2,height/2+63,0xFF7777);
    }
}
