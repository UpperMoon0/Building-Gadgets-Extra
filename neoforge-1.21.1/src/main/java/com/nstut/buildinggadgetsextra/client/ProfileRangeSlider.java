package com.nstut.buildinggadgetsextra.client;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.nstut.buildinggadgetsextra.common.ProfileUiStyle;
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Consumer;
/** Native range drag/step behavior, styled consistently with profile controls. */
public final class ProfileRangeSlider extends IncrementalSliderWidget {
    private final int maximum;
    public ProfileRangeSlider(int x,int y,int width,int max,int initial,Consumer<IncrementalSliderWidget> update) {
        super(x,y,width,24,1,max,Component.literal("Range: "),initial,update); maximum=max;
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt) {
        if(!visible) return;
        g.fill(getX(),getY(),getX()+width,getY()+height,ProfileUiStyle.CONTROL);
        g.fill(getX(),getY()+height-2,getX()+width,getY()+height,ProfileUiStyle.BORDER);
        int marker=(int)((width-4)*(getValueInt()-1.0)/Math.max(1,maximum-1));
        g.fill(getX()+marker,getY()+height-3,getX()+marker+4,getY()+height,ProfileUiStyle.SELECTED);
        g.flush();
        g.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+8,ProfileUiStyle.TEXT);
    }
    @Override public Collection<AbstractWidget> getComponents() {
        Collection<AbstractWidget> result=new ArrayList<>();
        for(AbstractWidget original:super.getComponents()) {
            if(original==this) result.add(this);
            else result.add(new ProfileButton(original.getX(),original.getY(),original.getWidth(),original.getHeight(),original.getMessage(),()->{ ((Button)original).onPress(); }));
        }
        return result;
    }
}
