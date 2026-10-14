package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Inline profile controls; one local snapshot preserves rapid edits until server echo. */
public final class InlineModifiers {
    private final Font font;
    private final int x, width, top, bottom, profile;
    private final Supplier<ItemStack> held;
    private final int[] draft = new int[LiveModifiers.FIELDS];
    private final EditBox[] fields = new EditBox[7];
    private final List<AbstractWidget> moving = new ArrayList<>();
    private final List<Integer> rows = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<Integer> labelRows = new ArrayList<>();
    private final Button[] toggles = new Button[2], expand = new Button[2];
    private final Button mirrorH, mirrorV, reset, order;
    private final MirrorIconButton arrayIcon, radialIcon;
    private int expanded = -1, scroll, contentHeight, lastFocus = -1, waitTicks;
    private boolean pending;
    private String error = "";
    private Runnable navigation;
    private static final String[] NAMES = {"Copies", "Spacing X", "Spacing Y", "Spacing Z", "Copies", "Center X", "Center Z"};

    public InlineModifiers(Font font, int x, int width, int top, int bottom, Supplier<ItemStack> held, Consumer<AbstractWidget> add) {
        this.font=font; this.x=x; this.width=width; this.top=top; this.bottom=bottom; this.held=held;
        ItemStack stack=held.get(); profile=MultitoolState.getActiveMode(stack).ordinal();
        LiveModifiers saved=MultitoolState.getLiveModifiers(stack);
        for(int i=0;i<draft.length;i++) draft[i]=saved.value(i);
        mirrorH=button("",width,()->com.nstut.buildinggadgetsextra.network.ExtraNetwork.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPacket(false))); add.accept(mirrorH);
        mirrorV=button("",width,()->com.nstut.buildinggadgetsextra.network.ExtraNetwork.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPacket(true))); add.accept(mirrorV);
        arrayIcon=icon("array",()->expand(0)); radialIcon=icon("radial",()->expand(1)); add.accept(arrayIcon); add.accept(radialIcon);
        for(int stage=0;stage<2;stage++) {
            final int selected=stage;
            toggles[stage]=button("",width-60,()->toggle(selected)); toggles[stage].setX(x+28); add.accept(toggles[stage]);
            expand[stage]=button("+",24,()->expand(selected)); expand[stage].setX(x+width-24); add.accept(expand[stage]);
        }
        for(int i=0;i<fields.length;i++) {
            fields[i]=new EditBox(font,x+width-66,top,66,18,Component.literal(NAMES[i]));
            fields[i].setMaxLength(4); fields[i].setValue(Integer.toString(fieldValue(i))); add.accept(fields[i]);
        }
        reset=button("Reset",width,()->reset()); add.accept(reset);
        order=button("",width,()->cycleOrder()); add.accept(order);
        Button up=button("^",18,()->scroll(-36)); up.setX(x+width-38); up.setY(top); add.accept(up);
        Button down=button("v",18,()->scroll(36)); down.setX(x+width-18); down.setY(top); add.accept(down);
        layout();
    }
    public void restoreFrom(InlineModifiers previous) {
        if(previous==null || previous.profile!=profile) return;
        System.arraycopy(previous.draft,0,draft,0,draft.length);
        for(int i=0;i<fields.length;i++) fields[i].setValue(previous.fields[i].getValue());
        expanded=previous.expanded; scroll=previous.scroll; pending=previous.pending;
        error=previous.error; waitTicks=previous.waitTicks; layout();
    }
    private Button button(String label,int width,Runnable action) {
        return Button.builder(Component.literal(label), b->action.run()).bounds(x,top,width,20).build();
    }
    private MirrorIconButton icon(String name,Runnable action) {
        return new MirrorIconButton(x,top,MirrorIconButton.addonSettingIcon(name),44,Component.literal(name+" settings"),action);
    }
    private int fieldValue(int i) { return draft[i==0?8:i==4?9:i]; }
    public void expand(int stage) { if(flush()) { expanded=expanded==stage?-1:stage; scroll=0; layout(); } }
    public void revealField(int index) {
        expanded=index>=4?1:0; layout();
        int at=moving.indexOf(fields[index]);
        if(at>=0) scroll=Math.min(rows.get(at),Math.max(0,contentHeight-(bottom-top-40)));
        layout();
    }
    private void toggle(int stage) {
        if(!flush()) return;
        int i=stage==0?0:4; draft[i]=draft[i]>1?1:draft[stage==0?8:9]; send(); layout();
    }
    private void reset() {
        int first=expanded==1?4:0, end=expanded==1?7:4;
        for(int i=first;i<end;i++) draft[i]=LiveModifiers.NONE.value(i);
        draft[expanded==1?9:8]=2;
        for(int i=first;i<end;i++) fields[i].setValue(Integer.toString(fieldValue(i)));
        error=""; send(); layout();
    }
    private boolean commit(int i) {
        int value;
        try { value=Integer.parseInt(fields[i].getValue()); }
        catch(NumberFormatException invalid) { error=NAMES[i]+": enter a number"; return false; }
        boolean copies=i==0 || i==4;
        if(value<(copies?2:-64) || value>(copies?16:64)) { error=NAMES[i]+(copies?": use 2-16":": use -64..64"); return false; }
        int target=i==0?8:i==4?9:i;
        if(draft[target]!=value) {
            draft[target]=value;
            if(copies && draft[i]>1) draft[i]=value;
            send();
        }
        fields[i].setValue(Integer.toString(value));
        error=""; return true;
    }
    private boolean hit(AbstractWidget widget,double px,double py) { return widget.visible && px>=widget.getX() && px<widget.getX()+widget.getWidth() && py>=widget.getY() && py<widget.getY()+widget.getHeight(); }
    public boolean needsCommitBeforeClick(double px,double py) {
        if(hit(reset,px,py)) return false;
        for(int i=0;i<fields.length;i++) {
            if(fields[i].isFocused() && hit(fields[i],px,py)) return false;
        }
        for(int i=0;i<fields.length;i++) if(!fields[i].getValue().equals(Integer.toString(fieldValue(i)))) return true;
        return false;
    }
    public boolean flush() {
        for(int i=0;i<fields.length;i++) if(!fields[i].getValue().equals(Integer.toString(fieldValue(i))) && !commit(i)) {
            expanded=i>=4?1:0; layout(); return false;
        }
        error=""; return true;
    }
    private void send() {
        LiveModifiers settings=LiveModifiers.from(draft);
        com.nstut.buildinggadgetsextra.network.ExtraNetwork.sendToServer(new com.nstut.buildinggadgetsextra.network.ModifierPacket(profile,settings));
        pending=true; waitTicks=0;
        com.nstut.buildinggadgetsextra.common.LivePlanFeedback.clear(profile);
    }
    public void navigate(Runnable action,boolean discardInvalid) {
        if(!flush()) {
            if(!discardInvalid) return;
            for(int i=0;i<fields.length;i++) fields[i].setValue(Integer.toString(fieldValue(i)));
            error="";
        }
        if(pending) navigation=action; else action.run();
    }
    public void tick() {
        int focus=-1; for(int i=0;i<fields.length;i++) if(fields[i].isFocused()) focus=i;
        if(lastFocus>=0 && focus!=lastFocus) commit(lastFocus);
        lastFocus=focus;
        ItemStack stack=held.get();
        if(MultitoolState.getActiveMode(stack).ordinal()!=profile) { if(navigation!=null) { Runnable action=navigation; navigation=null; action.run(); } return; }
        LiveModifiers saved=MultitoolState.getLiveModifiers(stack);
        boolean same=true; for(int i=0;i<draft.length;i++) same &= saved.value(i)==draft[i];
        if(pending && same) { pending=false; if(navigation!=null) { Runnable action=navigation; navigation=null; action.run(); } }
        else if(pending && ++waitTicks>200) { error="Save not confirmed. Retry edit."; navigation=null; }
        layout();
    }
    public boolean enter() { if(lastFocus<0) return false; commit(lastFocus); return true; }
    public void scroll(int amount) { if(flush()) { scroll=Math.max(0,Math.min(Math.max(0,contentHeight-(bottom-top-40)),scroll+amount)); layout(); } }
    public boolean contains(double mouseX,double mouseY) { return mouseX>=x && mouseX<x+width && mouseY>=top && mouseY<bottom; }
    private void addRow(AbstractWidget widget,int row) { moving.add(widget); rows.add(row); }
    private void layout() {
        moving.clear(); rows.clear(); labels.clear(); labelRows.clear();
        ItemStack stack=held.get();
        mirrorH.setMessage(Component.literal("Horizontal: "+(MultitoolState.isLiveMirrorHorizontal(stack)?"On":"Off")));
        mirrorV.setMessage(Component.literal("Vertical: "+(MultitoolState.isLiveMirrorVertical(stack)?"On":"Off")));
        int row=0; addRow(mirrorH,row); row+=24; addRow(mirrorV,row); row+=28;
        for(int stage=0;stage<2;stage++) {
            int first=stage==0?0:4;
            toggles[stage].setMessage(Component.literal((stage==0?"Array: ":"Radial: ")+(draft[first]>1?"On":"Off")));
            expand[stage].setMessage(Component.literal(expanded==stage?"-":"+"));
            addRow(stage==0?arrayIcon:radialIcon,row); addRow(toggles[stage],row); addRow(expand[stage],row); row+=28;
            if(expanded==stage) {
                labels.add(stage==0?draft[8]+" copies; step "+draft[1]+","+draft[2]+","+draft[3]:draft[9]+" copies around Y"); labelRows.add(row); row+=14;
                labels.add("Copies include original"); labelRows.add(row); row+=16;
                for(int i=first;i<(stage==0?4:7);i++) {
                    addRow(fields[i],row); fields[i].active=draft[first]>1;
                    labels.add(NAMES[i]); labelRows.add(row+5); row+=24;
                }
                reset.setMessage(Component.literal(stage==0?"Reset Array":"Reset Radial")); addRow(reset,row); row+=28;
            }
        }
        String active=activeOrder();
        if(active.indexOf(" > ")>=0) { labels.add("Composition order"); labelRows.add(row); row+=14; order.setMessage(Component.literal(active)); addRow(order,row); row+=24; }
        contentHeight=row;
        scroll=Math.min(scroll,Math.max(0,contentHeight-(bottom-top-40)));
        for(EditBox field:fields) field.visible=false;
        reset.visible=false; order.visible=false;
        for(int i=0;i<moving.size();i++) {
            AbstractWidget widget=moving.get(i); int y=top+24+rows.get(i)-scroll;
            widget.setY(y); widget.visible=y>=top+24 && y+widget.getHeight()<=bottom-16;
            if(widget instanceof EditBox && !widget.visible) widget.setFocused(false);
        }
    }
    private String activeOrder() {
        String[] stages=LiveModifiers.ORDERS[draft[7]].split(" \u2192 ");
        String result=""; boolean mirror=MultitoolState.isLiveMirrorHorizontal(held.get()) || MultitoolState.isLiveMirrorVertical(held.get());
        for(String stage:stages) if((stage.equals("Mirror") && mirror) || (stage.equals("Array") && draft[0]>1) || (stage.equals("Radial") && draft[4]>1)) result+=(result.isEmpty()?"":" > ")+stage;
        return result;
    }
    private boolean labelVisible(int index,int y) {
        boolean fieldLabel=false;
        for(String name:NAMES) fieldLabel |= name.equals(labels.get(index));
        return fieldLabel ? y-5>=top+24 && y+13<=bottom-16 : y>=top+24 && y+9<=bottom-16;
    }
    private void cycleOrder() {
        if(!flush()) return;
        String before=activeOrder();
        for(int i=0;i<6;i++) { draft[7]=(draft[7]+1)%6; if(!activeOrder().equals(before)) break; }
        send(); layout();
    }
    public void background(GuiGraphics g) { g.fill(x-4,top-2,x+width+4,bottom,0xCC161616); }
    public void render(GuiGraphics g) {
        g.drawString(font,(expanded==0?"Array settings":expanded==1?"Radial settings":"Modifiers"),x,top+5,0xFFFFFF,false);
        for(int i=0;i<labels.size();i++) { int y=top+24+labelRows.get(i)-scroll; if(labelVisible(i,y)) g.drawString(font,font.plainSubstrByWidth(labels.get(i),width-4),x+2,y,0xBBBBBB,false); }
        String rejected=com.nstut.buildinggadgetsextra.common.LivePlanFeedback.reason(profile);
        boolean bad=!error.isEmpty() || rejected!=null;
        String status=!error.isEmpty()?error:rejected!=null?rejected:pending?"Saving...":"Saved";
        g.drawString(font,font.plainSubstrByWidth(status,width),x,bottom-12,bad?0xFF7777:0xBBBBBB,false);
    }
}
