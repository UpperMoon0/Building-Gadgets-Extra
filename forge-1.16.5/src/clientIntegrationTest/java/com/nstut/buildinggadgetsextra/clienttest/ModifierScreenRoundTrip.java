package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.InlineModifiers;
import com.nstut.buildinggadgetsextra.client.LegacyMultitoolScreen;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import java.lang.reflect.Field;
/** Real inline controls, field commits, stage resets and authoritative inventory echo. */
public final class ModifierScreenRoundTrip {
    private int phase;
    private boolean preparedCapture;
    private LegacyMultitoolScreen screen;
    private InlineModifiers panel;
    public boolean tick(ItemStack stack) {
        try {
            LiveModifiers current=MultitoolState.getLiveModifiers(stack);
            if(phase==0) {
                if(MultitoolState.getActiveMode(stack)!=com.nstut.buildinggadgetsextra.common.MultitoolMode.BUILD) return false;
                screen=new LegacyMultitoolScreen(stack); Minecraft.getInstance().setScreen(screen); screen.openActiveProfile(); panel=screen.inlineModifiers();
                if(panel==null) throw new IllegalStateException("inline Build settings missing");
                press(((Button[])field("toggles"))[0]); panel.expand(0);
                edit(0,"2"); edit(1,"3"); edit(3,"-2"); phase=1; return false;
            }
            if(phase==1) {
                if(current.value(0)!=2 || current.value(1)!=3 || current.value(3)!=-2) return false;
                if(!preparedCapture) {
                    verifyDraftResize(); panel.revealField(0); preparedCapture=true; return false;
                }
                capture("inline-array.png"); preparedCapture=false;
                press(((Button[])field("toggles"))[1]); panel.expand(1);
                edit(4,"4"); edit(5,"1"); edit(6,"-1"); phase=2; return false;
            }
            if(phase==2) {
                if(current.value(4)!=4 || current.value(5)!=1 || current.value(6)!=-1) return false;
                panel.revealField(4); capture("inline-radial.png");
                press(((Button[])field("toggles"))[0]); phase=6; return false;
            }
            if(phase==6) {
                if(current.value(0)!=1) return false;
                if(current.value(8)!=2 || current.value(1)!=3 || current.value(3)!=-2) throw new IllegalStateException("Off lost Array configuration");
                press(((Button[])field("toggles"))[0]); phase=7; return false;
            }
            if(phase==7) {
                if(current.value(0)!=2) return false;
                panel.expand(0); press((Button)field("reset")); phase=3; return false;
            }
            if(phase==3) {
                if(current.value(0)!=1 || current.value(4)!=4) return false;
                if(current.value(5)!=1 || current.value(6)!=-1) throw new IllegalStateException("reset Array changed Radial");
                panel.expand(1); press((Button)field("reset")); phase=4; return false;
            }
            if(phase==4) {
                if(current.enabled() || current.value(8)!=2 || current.value(9)!=2) return false;
                Minecraft.getInstance().setScreen(null); phase=5;
            }
            return true;
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException("inline editor fixture",failure); }
    }
    private void verifyDraftResize() throws ReflectiveOperationException {
        TextFieldWidget input=((TextFieldWidget[])field("fields"))[1]; input.setValue("-");
        if(panel.flush()) throw new IllegalStateException("invalid draft accepted");
        int w=screen.width,h=screen.height;
        screen.resize(Minecraft.getInstance(),320,240); panel=screen.inlineModifiers();
        if(!((TextFieldWidget[])field("fields"))[1].getValue().equals("-")) throw new IllegalStateException("resize lost numeric draft");
        screen.resize(Minecraft.getInstance(),w,h); panel=screen.inlineModifiers();
        ((TextFieldWidget[])field("fields"))[1].setValue("3");
        if(!panel.flush()) throw new IllegalStateException("valid draft rejected after resize");
    }
    private Object field(String name) throws ReflectiveOperationException { Field f=InlineModifiers.class.getDeclaredField(name); f.setAccessible(true); return f.get(panel); }
    private void edit(int index,String value) throws ReflectiveOperationException {
        TextFieldWidget input=((TextFieldWidget[])field("fields"))[index]; panel.revealField(index); input.setFocus(true); panel.tick(); input.setValue(value);
        screen.keyPressed(257,0,0);
        input.setFocus(false);
        if(!panel.flush()) throw new IllegalStateException("valid numeric input rejected");
    }
    private void press(Button button) {
        for(int i=0;i<20 && !button.visible;i++) panel.scroll(36);
        if(!button.visible) { for(int i=0;i<20 && !button.visible;i++) panel.scroll(-36); }
        if(!button.visible) throw new IllegalStateException("inline control unreachable: "+button.getMessage().getString());
        button.onPress();
    }
    private void capture(String name) {  }
}
