package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private final List<Integer> labelRows = new ArrayList<>(), labelXs = new ArrayList<>();
    private final List<Boolean> labelFields = new ArrayList<>();
    private final Button[] toggles = new Button[2], expand = new Button[2];
    private final Button mirrorH, mirrorV, reset, order;
    private final MirrorIconButton arrayIcon, radialIcon, mirrorHIcon, mirrorVIcon;
    private final Button up, down;
    private int expanded = -1, scroll, contentHeight, lastFocus = -1, waitTicks;
    private boolean pending;
    private String error = "";
    private Runnable navigation;
    private static final String[] NAMES = {"Copies", "Spacing X", "Spacing Y", "Spacing Z", "Copies", "Pivot X", "Pivot Z"};

    public InlineModifiers(Font font, int x, int width, int top, int bottom, Supplier<ItemStack> held, Consumer<AbstractWidget> add) {
        this.font=font; this.x=x; this.width=width; this.top=top; this.bottom=bottom; this.held=held;
        ItemStack stack=held.get(); profile=MultitoolState.getActiveMode(stack).ordinal();
        LiveModifiers saved=MultitoolState.getLiveModifiers(stack);
        for(int i=0;i<draft.length;i++) draft[i]=saved.value(i);
        mirrorH=button("",width-28,()->net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPayload(false))); add.accept(mirrorH);
        mirrorV=button("",width-28,()->net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPayload(true))); add.accept(mirrorV);
        mirrorH.setX(x+28); mirrorV.setX(x+28);
        mirrorHIcon=icon("mirror_horizontal",()->net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPayload(false))); mirrorVIcon=icon("mirror_vertical",()->net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.MirrorPayload(true))); add.accept(mirrorHIcon); add.accept(mirrorVIcon);
        arrayIcon=icon("array",()->expand(0)); radialIcon=icon("radial",()->expand(1)); add.accept(arrayIcon); add.accept(radialIcon);
        for(int stage=0;stage<2;stage++) {
            final int selected=stage;
            toggles[stage]=button("",width-60,()->toggle(selected)); toggles[stage].setX(x+28); add.accept(toggles[stage]);
            expand[stage]=button("+",24,()->expand(selected)); expand[stage].setX(x+width-24); add.accept(expand[stage]);
        }
        for(int i=0;i<fields.length;i++) {
            fields[i]=new ProfileField(font,x+width-66,top,66,24,Component.literal(NAMES[i]));
            fields[i].setMaxLength(4); fields[i].setValue(Integer.toString(fieldValue(i))); add.accept(fields[i]);
        }
        reset=button("Reset",width,()->reset()); add.accept(reset);
        order=button("",width,()->cycleOrder()); add.accept(order);
        up=button("^",18,()->scroll(-36)); up.setX(x+width-38); up.setY(top); add.accept(up);
        down=button("v",18,()->scroll(36)); down.setX(x+width-18); down.setY(top); add.accept(down);
        ((ProfileButton)mirrorH).selected(()->MultitoolState.isLiveMirrorHorizontal(held.get()));
        ((ProfileButton)mirrorV).selected(()->MultitoolState.isLiveMirrorVertical(held.get()));
        ((ProfileButton)toggles[0]).selected(()->draft[0]>1); ((ProfileButton)toggles[1]).selected(()->draft[4]>1);
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
        return new ProfileButton(x,top,width,24,Component.literal(label),action);
    }
    private MirrorIconButton icon(String name,Runnable action) {
        return new MirrorIconButton(x,top,MirrorIconButton.addonSettingIcon(name),44,Component.literal(name.equals("mirror_horizontal")?"Horizontal mirror":name.equals("mirror_vertical")?"Vertical mirror":name.equals("array")?"Array settings":"Radial settings"),action).profileStyle();
    }
    private int fieldValue(int i) { return draft[i==0?8:i==4?9:i]; }
    public void expand(int stage) { if(flush()) { expanded=expanded==stage?-1:stage; scroll=0; layout(); if(expanded==stage) { scroll=stage==0?56:84; layout(); } } }
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
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new com.nstut.buildinggadgetsextra.network.ModifierPayload(profile,settings));
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
        moving.clear(); rows.clear(); labels.clear(); labelRows.clear(); labelXs.clear(); labelFields.clear();
        ItemStack stack=held.get();
        mirrorH.setMessage(Component.literal("Horizontal: "+(MultitoolState.isLiveMirrorHorizontal(stack)?"On":"Off")));
        mirrorV.setMessage(Component.literal("Vertical: "+(MultitoolState.isLiveMirrorVertical(stack)?"On":"Off")));
        int row=0; addRow(mirrorHIcon,row); addRow(mirrorH,row); row+=28; addRow(mirrorVIcon,row); addRow(mirrorV,row); row+=28;
        for(int stage=0;stage<2;stage++) {
            int first=stage==0?0:4;
            toggles[stage].setMessage(Component.literal((stage==0?"Array: ":"Radial: ")+(draft[first]>1?"On":"Off")));
            expand[stage].setMessage(Component.literal(expanded==stage?"-":"+"));
            addRow(stage==0?arrayIcon:radialIcon,row); addRow(toggles[stage],row); addRow(expand[stage],row); row+=28;
            if(expanded==stage) {
                fields[first].setWidth(66); fields[first].setX(x+width-66);
                addRow(fields[first],row); fields[first].active=draft[first]>1;
                label("Total copies",row+8,2,true); row+=28;
                label(stage==0?"Spacing in blocks":"Pivot from target (blocks)",row,2,false); row+=16;
                int axes=stage==0?3:2, cell=width/axes;
                for(int axis=0;axis<axes;axis++) {
                    int i=first+1+axis; fields[i].setWidth(cell-20); fields[i].setX(x+axis*cell+18);
                    addRow(fields[i],row); fields[i].active=draft[first]>1;
                    label(stage==0?new String[]{"X","Y","Z"}[axis]:axis==0?"X":"Z",row+8,axis*cell+2,true);
                }
                row+=28;
                reset.setMessage(Component.literal(stage==0?"Reset Array":"Reset Radial")); addRow(reset,row); row+=28;
            }
        }
        String active=activeOrder();
        if(active.indexOf(" > ")>=0) { label("Composition order",row,2,false); row+=14; order.setMessage(Component.literal(active)); addRow(order,row); row+=24; }
        contentHeight=row;
        up.visible=down.visible=contentHeight>bottom-top-40;
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
    private void label(String text,int row,int offset,boolean field) {
        labels.add(text); labelRows.add(row); labelXs.add(offset); labelFields.add(field);
    }
    private boolean labelVisible(int index,int y) {
        return labelFields.get(index) ? y-8>=top+24 && y+16<=bottom-16 : y>=top+24 && y+9<=bottom-16;
    }
    private void cycleOrder() {
        if(!flush()) return;
        String before=activeOrder();
        for(int i=0;i<6;i++) { draft[7]=(draft[7]+1)%6; if(!activeOrder().equals(before)) break; }
        send(); layout();
    }
    public void background(GuiGraphicsExtractor g) { g.fill(x-6,8,x+width+6,bottom,com.nstut.buildinggadgetsextra.common.ProfileUiStyle.PANEL); }
    public void render(GuiGraphicsExtractor g) {
        g.text(font,"Settings",x,14,0xFFFFFF);
        g.text(font,"Modifiers",x,top+5,0xFFFFFF,false);
        for(int i=0;i<labels.size();i++) { int y=top+24+labelRows.get(i)-scroll; if(labelVisible(i,y)) g.text(font,font.plainSubstrByWidth(labels.get(i),width-labelXs.get(i)-2),x+labelXs.get(i),y,0xBBBBBB,false); }
        String rejected=com.nstut.buildinggadgetsextra.common.LivePlanFeedback.reason(profile);
        boolean bad=!error.isEmpty() || rejected!=null;
        String status=!error.isEmpty()?error:rejected!=null?rejected:pending?"Saving...":"Saved";
        g.text(font,font.plainSubstrByWidth(status,width),x,bottom-12,bad?0xFF7777:0xBBBBBB,false);
    }
}
