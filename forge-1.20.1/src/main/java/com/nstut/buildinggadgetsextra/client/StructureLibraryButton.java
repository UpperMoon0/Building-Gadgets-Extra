package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class StructureLibraryButton extends GuiIconActionable {
    public StructureLibraryButton(int x,int y,Component tooltip,Runnable action){
        super(x,y,"buildinggadgetsextra_placeholder",tooltip,false,send->{if(send)action.run();return false;});setWidth(24);setHeight(24);
    }
    @Override public void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        if(!visible)return; Minecraft minecraft=Minecraft.getInstance();
        graphics.fill(getX(),getY(),getX()+width,getY()+height,0x32FFFFFF);
        graphics.drawCenteredString(minecraft.font,"NBT",getX()+width/2,getY()+8,0xFFFFFFFF);
        if(isHoveredOrFocused()){String tooltip=getMessage().getString();int x=mouseX>minecraft.getWindow().getGuiScaledWidth()/2?mouseX+2:mouseX-minecraft.font.width(tooltip);graphics.drawString(minecraft.font,tooltip,x,mouseY-10,0xFFFFFFFF,true);}
    }
}
