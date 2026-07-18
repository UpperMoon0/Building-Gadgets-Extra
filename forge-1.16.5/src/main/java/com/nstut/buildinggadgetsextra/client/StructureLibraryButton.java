package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public final class StructureLibraryButton extends GuiIconActionable {
    public StructureLibraryButton(int x,int y,ITextComponent tooltip,Runnable action){super(x,y,"buildinggadgetsextra_placeholder",tooltip,false,send->{if(send)action.run();return false;});width=24;height=24;}
    @Override public void render(MatrixStack stack,int mouseX,int mouseY,float partialTick){if(!visible)return;Minecraft minecraft=Minecraft.getInstance();fill(stack,x,y,x+width,y+height,0x32FFFFFF);drawCenteredString(stack,minecraft.font,"NBT",x+width/2,y+8,0xFFFFFFFF);if(mouseX>=x&&mouseY>=y&&mouseX<x+width&&mouseY<y+height){String tooltip=getMessage().getString();int tx=mouseX>minecraft.getWindow().getGuiScaledWidth()/2?mouseX+2:mouseX-minecraft.font.width(tooltip);drawString(stack,minecraft.font,tooltip,tx,mouseY-10,0xFFFFFFFF);}}
}
