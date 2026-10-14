package com.nstut.buildinggadgetsextra.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
public final class ProfileField extends TextFieldWidget {
    public ProfileField(FontRenderer font,int x,int y,int width,int height,ITextComponent label) { super(font,x,y,width,height,label); setBordered(false); }
    @Override public void render(MatrixStack g,int mx,int my,float dt) {
        if(!visible) return;
        int oldY=y,left=x;
        int edge=isFocused()?ProfileUiStyle.HOVER:ProfileUiStyle.BORDER;
        net.minecraft.client.gui.AbstractGui.fill(g,left,oldY,left+getWidth(),oldY+getHeight(),ProfileUiStyle.CONTROL);
        net.minecraft.client.gui.AbstractGui.fill(g,left,oldY+getHeight()-1,left+getWidth(),oldY+getHeight(),edge);
        x=left+4; y=oldY+7;
        try { super.render(g,mx,my,dt); } finally { x=left; y=oldY; }
    }
}
