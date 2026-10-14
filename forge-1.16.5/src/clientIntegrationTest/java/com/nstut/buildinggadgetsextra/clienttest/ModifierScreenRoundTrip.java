package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.ModifierScreen;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import java.lang.reflect.Field;
/** Drives the real editor, waits for server inventory echo, then resets through the editor. */
public final class ModifierScreenRoundTrip {
    private int phase;
    public boolean tick(ItemStack stack) {
        LiveModifiers current=MultitoolState.getLiveModifiers(stack);
        if(phase==0) { submit(stack,new int[]{2,3,0,-2,4,1,-1}); phase=1; return false; }
        if(phase==1) {
            if(current.value(0)!=2 || current.value(4)!=4 || current.value(1)!=3 || current.value(3)!=-2 || current.value(5)!=1 || current.value(6)!=-1) return false;
            submit(stack,new int[]{1,0,0,0,1,0,0}); phase=2; return false;
        }
        if(phase==2 && current.enabled()) return false;
        phase=3; return true;
    }
    private void submit(ItemStack stack,int[] values) {
        try {
            ModifierScreen screen=new ModifierScreen(null,stack);
            Minecraft.getInstance().setScreen(screen);
            Field field=ModifierScreen.class.getDeclaredField("fields"); field.setAccessible(true);
            TextFieldWidget[] inputs=(TextFieldWidget[])field.get(screen);
            for(int i=0;i<inputs.length;i++) inputs[i].setValue(Integer.toString(values[i]));
            Button apply=screen.children().stream().filter(child->child instanceof Button).map(child->(Button)child)
                    .filter(button->button.getMessage().getString().equals("Apply")).findFirst().orElseThrow(() -> new IllegalStateException("missing Apply button"));
            apply.onPress();
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException("modifier editor fixture",failure); }
    }
}
