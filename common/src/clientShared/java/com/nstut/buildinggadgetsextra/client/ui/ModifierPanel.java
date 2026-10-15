package com.nstut.buildinggadgetsextra.client.ui;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.LivePlanFeedback;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.openui.api.*;
import com.nstut.openui.controls.*;
import com.nstut.openui.state.*;
import java.util.function.*;

/** One control tree and edit lifecycle shared by every Minecraft adapter. */
public final class ModifierPanel {
    private final int profile;
    private final IntSupplier activeProfile;
    private final Supplier<LiveModifiers> saved;
    private final BooleanSupplier horizontal, vertical;
    private final Consumer<LiveModifiers> save;
    private final Consumer<Boolean> mirror;
    private final int[] draft = new int[LiveModifiers.FIELDS];
    private final Signal<String>[] text = new Signal[7];
    private final TextField[] fields = new TextField[7];
    public final ButtonWidget[] toggles = new ButtonWidget[2], expanders = new ButtonWidget[2];
    private final ButtonWidget mirrorH, mirrorV, order;
    private final TextWidget status = new TextWidget("Saved");
    private final VStack content = new VStack().gap(4);
    private int expanded = -1, waitTicks;
    private Consumer<UIComponent> reveal=target->{};
    private UIComponent expandedEnd;
    private boolean orderVisible;
    private boolean pending;
    private String error = "";
    private Runnable navigation;
    private static final String[] NAMES = {"Copies", "Spacing X", "Spacing Y", "Spacing Z", "Copies", "Pivot X", "Pivot Z"};

    public ModifierPanel(IntSupplier activeProfile, Supplier<LiveModifiers> saved,
                         BooleanSupplier horizontal, BooleanSupplier vertical,
                         Consumer<LiveModifiers> save, Consumer<Boolean> mirror) {
        this.activeProfile=activeProfile; this.profile=activeProfile.getAsInt(); this.saved=saved;
        this.horizontal=horizontal; this.vertical=vertical; this.save=save; this.mirror=mirror;
        LiveModifiers initial=saved.get();
        for(int i=0;i<draft.length;i++) draft[i]=initial.value(i);
        for(int i=0;i<fields.length;i++) {
            final int index=i;
            text[i]=Signals.of(Integer.toString(fieldValue(i)));
            fields[i]=new TextField(text[i]).numeric().maxLength(4).onCommit(value->commit(index));
            fields[i].radius(2); fields[i].height(24); fields[i].tooltip(NAMES[i]);
        }
        mirrorH=button("Horizontal",()->{if(flush()) mirror.accept(false);});
        mirrorV=button("Vertical",()->{if(flush()) mirror.accept(true);});
        for(int stage=0;stage<2;stage++) {
            final int selected=stage;
            toggles[stage]=button("",()->toggle(selected));
            expanders[stage]=button("+",()->expand(selected));
        }
        order=button("",this::cycleOrder);
        rebuild(); tickLabels();
    }
    public static ButtonWidget button(String label,Runnable action) { return new ButtonWidget(label).radius(2).height(24).onPress(action); }
    public static ButtonWidget icon(String name,String label,Runnable action) {
        ButtonWidget button=button(label,action).content(IconWidget.texture(ExtraConstants.MOD_ID,"textures/gui/setting/"+name+".png",44,44).size(16));
        button.width(24); button.tooltip(label); return button;
    }
    public void onReveal(Consumer<UIComponent> reveal) { this.reveal=reveal; }
    public UIComponent content() { return content; }
    public TextWidget status() { return status; }
    public TextField field(int index) { return fields[index]; }
    public int profile() { return profile; }
    public int expanded() { return expanded; }
    private int fieldValue(int i) { return draft[i==0?8:i==4?9:i]; }
    public void restoreFrom(ModifierPanel previous) {
        if(previous==null || previous.profile!=profile) return;
        System.arraycopy(previous.draft,0,draft,0,draft.length);
        for(int i=0;i<fields.length;i++) text[i].set(previous.text[i].get());
        expanded=previous.expanded; pending=previous.pending; waitTicks=previous.waitTicks;
        error=previous.error; navigation=previous.navigation; rebuild(); tickLabels();
    }
    public void expand(int stage) { if(flush()) { expanded=expanded==stage?-1:stage; rebuild(); if(expandedEnd!=null) reveal.accept(expandedEnd); } }
    public void revealField(int i) { expanded=i>=4?1:0; rebuild(); fields[i].requestFocus(); }
    private HStack row(UIComponent... children) { return Ui.row(children).gap(6); }
    private void rebuild() {
        content.clearChildren(); expandedEnd=null;
        content.child(new TextWidget("Modifiers"));
        content.child(row(icon("mirror_horizontal","Horizontal mirror",()->{if(flush()) mirror.accept(false);}), mirrorH.flex()));
        content.child(row(icon("mirror_vertical","Vertical mirror",()->{if(flush()) mirror.accept(true);}), mirrorV.flex()));
        for(int stage=0;stage<2;stage++) {
            final int selected=stage;
            content.child(row(icon(stage==0?"array":"radial",stage==0?"Array settings":"Radial settings",()->expand(selected)), toggles[stage].flex(), expanders[stage].width(24)));
            if(expanded==stage) {
                int first=stage==0?0:4;
                content.child(row(new TextWidget("Total copies").flex(), fields[first].width(50)));
                content.child(new TextWidget(stage==0?"Spacing in blocks":"Pivot from target (blocks)").color(0xFFB8BEC8));
                HStack axes=new HStack().gap(6);
                int count=stage==0?3:2;
                for(int axis=0;axis<count;axis++) axes.child(Ui.column(new TextWidget(stage==0?new String[]{"X","Y","Z"}[axis]:axis==0?"X":"Z"), fields[first+axis+1]).gap(3).flex());
                content.child(axes);
                expandedEnd=button(stage==0?"Reset Array":"Reset Radial",()->reset(selected));
                content.child(expandedEnd);
            }
        }
        orderVisible=activeOrder().contains(" > ");
        if(orderVisible) { content.child(new TextWidget("Composition order").color(0xFFB8BEC8)); content.child(order); }
        tickLabels();
    }
    private void toggle(int stage) { if(flush()) { int i=stage==0?0:4; draft[i]=draft[i]>1?1:draft[stage==0?8:9]; send(); rebuild(); } }
    private void reset(int stage) {
        int first=stage==0?0:4,end=stage==0?4:7;
        for(int i=first;i<end;i++) draft[i]=LiveModifiers.NONE.value(i);
        draft[stage==0?8:9]=2;
        for(int i=first;i<end;i++) text[i].set(Integer.toString(fieldValue(i)));
        error=""; send(); rebuild();
    }
    private boolean commit(int i) {
        final int value;
        try { value=Integer.parseInt(text[i].get()); }
        catch(NumberFormatException invalid) { error=NAMES[i]+": enter a number"; tickLabels(); return false; }
        boolean copies=i==0 || i==4;
        if(value<(copies?2:-64) || value>(copies?16:64)) { error=NAMES[i]+(copies?": use 2-16":": use -64..64"); tickLabels(); return false; }
        int target=i==0?8:i==4?9:i;
        if(draft[target]!=value) { draft[target]=value; if(copies && draft[i]>1) draft[i]=value; send(); }
        text[i].set(Integer.toString(value)); error=""; tickLabels(); return true;
    }
    public boolean flush() {
        for(int i=0;i<fields.length;i++) if(!text[i].get().equals(Integer.toString(fieldValue(i))) && !commit(i)) {
            expanded=i>=4?1:0; rebuild(); return false;
        }
        error=""; return true;
    }
    private void send() { save.accept(LiveModifiers.from(draft)); pending=true; waitTicks=0; LivePlanFeedback.clear(profile); }
    public void navigate(Runnable action,boolean discardInvalid) {
        if(!flush()) {
            if(!discardInvalid) return;
            for(int i=0;i<fields.length;i++) text[i].set(Integer.toString(fieldValue(i)));
            error="";
        }
        if(pending) navigation=action; else action.run();
    }
    public void tick() {
        LiveModifiers current=saved.get();
        boolean same=true; for(int i=0;i<draft.length;i++) same &= current.value(i)==draft[i];
        if(pending && (same || activeProfile.getAsInt()!=profile)) {
            pending=false; if(navigation!=null) { Runnable action=navigation; navigation=null; action.run(); return; }
        } else if(pending && ++waitTicks>200) { error="Save not confirmed. Retry edit."; navigation=null; }
        if(orderVisible!=activeOrder().contains(" > ")) rebuild();
        tickLabels();
    }
    private void tickLabels() {
        mirrorH.setLabel("Horizontal: "+(horizontal.getAsBoolean()?"On":"Off")); mirrorH.setActive(horizontal.getAsBoolean());
        mirrorV.setLabel("Vertical: "+(vertical.getAsBoolean()?"On":"Off")); mirrorV.setActive(vertical.getAsBoolean());
        for(int i=0;i<2;i++) { toggles[i].setLabel((i==0?"Array: ":"Radial: ")+(draft[i==0?0:4]>1?"On":"Off")); toggles[i].setActive(draft[i==0?0:4]>1); expanders[i].setLabel(expanded==i?"-":"+"); }
        order.setLabel(activeOrder());
        String rejected=LivePlanFeedback.reason(profile);
        status.setText(!error.isEmpty()?error:rejected!=null?rejected:pending?"Saving...":"Saved");
        status.color(!error.isEmpty() || rejected!=null?0xFFFF8888:0xFFB8BEC8);
    }
    private String activeOrder() {
        String result=""; boolean mirror=horizontal.getAsBoolean() || vertical.getAsBoolean();
        for(String stage:LiveModifiers.ORDERS[draft[7]].split(" → "))
            if((stage.equals("Mirror") && mirror) || (stage.equals("Array") && draft[0]>1) || (stage.equals("Radial") && draft[4]>1)) result+=(result.isEmpty()?"":" > ")+stage;
        return result;
    }
    private void cycleOrder() {
        if(!flush()) return; String before=activeOrder();
        for(int i=0;i<6;i++) { draft[7]=(draft[7]+1)%6; if(!activeOrder().equals(before)) break; }
        send(); rebuild();
    }
}
