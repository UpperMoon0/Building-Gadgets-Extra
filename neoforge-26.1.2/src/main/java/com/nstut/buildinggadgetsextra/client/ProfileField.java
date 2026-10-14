package com.nstut.buildinggadgetsextra.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
public final class ProfileField extends EditBox {
    public ProfileField(Font font,int x,int y,int width,int height,Component label) { super(font,x,y,width,height,label); setBordered(false); }
    @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        if(!visible) return;
        int oldY=getY(),left=getX();
        int edge=isFocused()?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER;
        g.fill(left,oldY,left+getWidth(),oldY+getHeight(),ProfileUiStyle.CONTROL);
        g.fill(left,oldY+getHeight()-1,left+getWidth(),oldY+getHeight(),edge);
        setX(left+4); setY(oldY+7);
        try { super.extractWidgetRenderState(g,mx,my,dt); } finally { setX(left); setY(oldY); }
    }
}
