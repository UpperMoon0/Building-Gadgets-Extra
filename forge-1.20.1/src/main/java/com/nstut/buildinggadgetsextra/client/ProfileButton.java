package com.nstut.buildinggadgetsextra.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import java.util.function.BooleanSupplier;
/** Flat control with the same focus and selected treatment as the operation wheel. */
public final class ProfileButton extends Button {
    private BooleanSupplier selected;
    public void selected(BooleanSupplier state) { selected=state; }
    public ProfileButton(int x,int y,int w,int h,Component label,Runnable action) { this(x,y,w,h,label,action,()->false); }
    public ProfileButton(int x,int y,int w,int h,Component label,Runnable action,BooleanSupplier selected) { super(x,y,w,h,label,b->action.run(),DEFAULT_NARRATION); this.selected=selected; }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt) {
        if(!visible) return;
        boolean hover=mx>=getX() && mx<getX()+width && my>=getY() && my<getY()+height;
        hover |= isFocused();
        int edge=active?(selected.getAsBoolean()?ProfileUiStyle.SELECTED:hover?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER):ProfileUiStyle.BORDER;
        g.fill(getX(),getY(),getX()+width,getY()+height,ProfileUiStyle.CONTROL);
        g.fill(getX(),getY(),getX()+width,getY()+1,edge); g.fill(getX(),getY()+height-1,getX()+width,getY()+height,edge);
        g.fill(getX(),getY(),getX()+1,getY()+height,edge); g.fill(getX()+width-1,getY(),getX()+width,getY()+height,edge);
        g.flush();
        int color=active?ProfileUiStyle.TEXT:ProfileUiStyle.MUTED;
        g.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+(height-8)/2,color);
    }
}
