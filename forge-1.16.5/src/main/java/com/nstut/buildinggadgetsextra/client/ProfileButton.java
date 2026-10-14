package com.nstut.buildinggadgetsextra.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import java.util.function.BooleanSupplier;
/** Flat control with the same focus and selected treatment as the operation wheel. */
public final class ProfileButton extends Button {
    private BooleanSupplier selected;
    public void selected(BooleanSupplier state) { selected=state; }
    public ProfileButton(int x,int y,int w,int h,ITextComponent label,Runnable action) { this(x,y,w,h,label,action,()->false); }
    public ProfileButton(int x,int y,int w,int h,ITextComponent label,Runnable action,BooleanSupplier selected) { super(x,y,w,h,label,b->action.run()); this.selected=selected; }
    @Override public void render(MatrixStack g,int mx,int my,float dt) {
        if(!visible) return;
        boolean hover=mx>=x && mx<x+width && my>=y && my<y+height;
        hover |= isFocused();
        int edge=active?(selected.getAsBoolean()?ProfileUiStyle.SELECTED:hover?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER):ProfileUiStyle.BORDER;
        net.minecraft.client.gui.AbstractGui.fill(g,x,y,x+width,y+height,ProfileUiStyle.CONTROL);
        net.minecraft.client.gui.AbstractGui.fill(g,x,y,x+width,y+1,edge); net.minecraft.client.gui.AbstractGui.fill(g,x,y+height-1,x+width,y+height,edge);
        net.minecraft.client.gui.AbstractGui.fill(g,x,y,x+1,y+height,edge); net.minecraft.client.gui.AbstractGui.fill(g,x+width-1,y,x+width,y+height,edge);
        int color=active?ProfileUiStyle.TEXT:ProfileUiStyle.MUTED;
        net.minecraft.client.gui.AbstractGui.drawCenteredString(g,Minecraft.getInstance().font,getMessage(),x+width/2,y+(height-8)/2,color);
    }
}
