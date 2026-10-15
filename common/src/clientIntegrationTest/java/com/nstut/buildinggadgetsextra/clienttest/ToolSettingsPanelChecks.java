package com.nstut.buildinggadgetsextra.clienttest;

import com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel;
import com.nstut.openui.api.*;
import java.util.Arrays;

/** Runs against the real OpenUI fields in each native client. */
public final class ToolSettingsPanelChecks {
    public static void run(com.nstut.openui.runtime.UiRuntime runtime) {
        final int[][] sent={null};final int[] sends={0};
        ToolSettingsPanel copy=new ToolSettingsPanel("copy",new int[]{10,64,20,12,66,22},-30000000,30000000,v->true,v->{sent[0]=v;sends[0]++;});
        runtime.setRoot(copy.content());
        press(copy.content(),"Coordinates: World");
        require(copy.field(0).getValue().equals("0") && copy.field(3).getValue().equals("2"),"relative coordinate conversion");
        copy.field(0).setValue("1");require(copy.flush(),"relative coordinate commit");
        require(Arrays.equals(sent[0],new int[]{11,64,20,12,66,22}),"relative edit changed selection extent or wrong origin");
        copy.field(1).setValue("-");require(!copy.flush(),"invalid copy draft accepted");int before=sends[0];
        ToolSettingsPanel restored=new ToolSettingsPanel("copy",new int[6],-30000000,30000000,v->true,v->{sent[0]=v;sends[0]++;});
        restored.restoreFrom(copy);runtime.setRoot(restored.content());require(restored.field(1).getValue().equals("-"),"resize lost invalid coordinate draft");
        require(sends[0]==before,"restoring draft sent a packet");
        ToolSettingsPanel paste=new ToolSettingsPanel("paste",new int[3],-16,16,v->true,v->{sent[0]=v;sends[0]++;});
        runtime.setRoot(paste.content());
        paste.field(0).setValue("17");require(!paste.flush(),"paste bounds bypassed");
        paste.field(0).setValue("-16");require(paste.flush(),"valid paste offset rejected");press(paste.content(),"Reset offset");
        require(Arrays.equals(sent[0],new int[3]),"paste reset");
        ToolSettingsPanel destruction=new ToolSettingsPanel("destruction",new int[]{1,2,3,4,5},0,32,v->v[0]+v[1]<=32&&v[2]+v[3]<=32,v->{sent[0]=v;sends[0]++;});
        runtime.setRoot(destruction.content());
        destruction.field(0).setValue("32");require(!destruction.flush(),"destruction span validation bypassed");
        press(destruction.content(),"Revert dimensions");require(Arrays.equals(sent[0],new int[]{1,2,3,4,5}),"destruction revert");
    }
    private static void require(boolean value,String detail) { if(!value) throw new IllegalStateException(detail); }
    private static void press(UIComponent root,String label) {
        if(root instanceof ButtonWidget && ((ButtonWidget)root).getLabel().getString().equals(label)) { root.keyPressed(257,0,0);return; }
        for(UIComponent child:root.children()) if(find(child,label)) { press(child,label);return; }
        throw new IllegalStateException("missing control "+label);
    }
    private static boolean find(UIComponent root,String label) {
        if(root instanceof ButtonWidget && ((ButtonWidget)root).getLabel().getString().equals(label)) return true;
        for(UIComponent child:root.children()) if(find(child,label)) return true;
        return false;
    }
}
