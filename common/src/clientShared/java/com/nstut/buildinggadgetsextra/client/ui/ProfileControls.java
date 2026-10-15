package com.nstut.buildinggadgetsextra.client.ui;
import com.nstut.openui.api.*;
import com.nstut.openui.controls.*;
import com.nstut.openui.layout.*;
import com.nstut.openui.state.*;
import java.util.*;
import java.util.function.*;

/** Framework composition shared by all native screens; no Minecraft drawing or hit logic. */
public final class ProfileControls {
    private final List<UIComponent> context=new ArrayList<>();
    private final List<Runnable> refresh=new ArrayList<>();
    private ScrollView scroll;
    private UIComponent undo;
    private double savedScroll;
    private int lastProfile=-1;
    public void clear() { if(scroll!=null) savedScroll=scroll.scrollOffset(); context.clear(); refresh.clear(); undo=null; }
    public void icon(String namespace,String path,int source,String label,BooleanSupplier selected,Runnable action) {
        ButtonWidget button=ModifierPanel.button(label,action).content(IconWidget.texture(namespace,path,source,source).size(16));
        button.width(24); button.tooltip(label);
        if(path.endsWith("/undo.png")) undo=button; else context.add(button); refresh.add(()->button.setActive(selected.getAsBoolean()));
    }
    public void tick() { for(Runnable update:refresh) update.run(); }
    public UIComponent root(String title,Radial wheel,String modeLabel,int panelWidth,UIComponent range,ModifierPanel modifiers) { return root(title,wheel,modeLabel,panelWidth,range,modifiers,null); }
    public UIComponent root(String title,Radial wheel,String modeLabel,int panelWidth,UIComponent range,ModifierPanel modifiers,ToolSettingsPanel extra) {
        int profile=modifiers==null?title.hashCode():modifiers.profile();
        if(lastProfile!=profile) savedScroll=0;
        lastProfile=profile;
        VStack settings=new VStack().gap(6);
        int columns=Math.max(1,(panelWidth-16+2)/26);
        int index=0;
        // Undo stays docked to the right of the first row, including wrapped toolbars.
        HStack first=new HStack().gap(2);
        int firstCount=Math.min(context.size(),Math.max(0,columns-(undo!=null?1:0)));
        while(index<firstCount) first.child(context.get(index++));
        if(undo!=null) first.child(new Spacer().flex()).child(undo);
        if(first.childCount()>0) settings.child(first);
        while(index<context.size()) {
            HStack row=new HStack().gap(2);
            for(int count=0;count<columns && index<context.size();count++) row.child(context.get(index++));
            settings.child(row);
        }
        if(range!=null) settings.child(range);
        if(extra!=null) settings.child(extra.content());
        if(modifiers!=null) settings.child(modifiers.content());
        scroll=new ScrollView(settings).scrollTo(savedScroll);
        if(modifiers!=null) modifiers.onReveal(target->scroll.reveal(target));
        VStack right=Ui.column(new TextWidget("Settings"),scroll.flex(),modifiers!=null?modifiers.status():extra!=null?extra.status():new TextWidget("")).gap(6);
        Panel panel=new Panel().radius(4).padding(8).child(right); panel.width(panelWidth);
        VStack left=Ui.column(new TextWidget(title).centered(),wheel,new TextWidget(modeLabel).centered()).gap(12).align(Alignment.CENTER).justify(Justification.CENTER);
        left.flex();
        return new Panel(0).radius(0).padding(8).child(Ui.row(left,panel).gap(12));
    }
    public static UIComponent range(int initial,int maximum,IntConsumer save) {
        Signal<Double> value=Signals.of((double)initial);
        TextWidget label=new TextWidget("Range: "+initial);
        final int[] last={initial};
        value.subscribe(next->{ int rounded=Math.max(1,Math.min(maximum,(int)Math.round(next))); label.setText("Range: "+rounded); if(rounded!=last[0]) { last[0]=rounded; save.accept(rounded); } });
        ButtonWidget minus=ModifierPanel.button("-",()->value.set((double)Math.max(1,last[0]-1)));
        ButtonWidget plus=ModifierPanel.button("+",()->value.set((double)Math.min(maximum,last[0]+1)));
        return Ui.column(label, Ui.row(minus.width(24),new Slider(value,1,maximum).flex(),plus.width(24)).gap(6)).gap(4);
    }
}
