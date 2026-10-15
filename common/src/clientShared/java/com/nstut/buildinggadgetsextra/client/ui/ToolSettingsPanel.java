package com.nstut.buildinggadgetsextra.client.ui;

import com.nstut.openui.api.*;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.state.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Shared inline editors; native adapters supply values, bounds and server packets. */
public final class ToolSettingsPanel {
    private final String kind;
    private final int[] original, values;
    private final Signal<String>[] text;
    private final TextField[] fields;
    private final Consumer<int[]> save;
    private final Predicate<int[]> valid;
    private final int minimum,maximum;
    private boolean relative,relativeOnly;
    private int[] clearValues;
    private final VStack content=new VStack().gap(4);
    private final TextWidget status=new TextWidget("Saved").color(0xFFB8BEC8);
    private final TextWidget summary=new TextWidget("").color(0xFFB8BEC8);
    private final ButtonWidget coordinates=ModifierPanel.button("Coordinates: World",this::switchCoordinates);

    public ToolSettingsPanel(String kind,int[] initial,int minimum,int maximum,Predicate<int[]> valid,Consumer<int[]> save) {
        this.kind=kind;this.values=initial.clone();this.original=initial.clone();this.minimum=minimum;this.maximum=maximum;this.valid=valid;this.save=save;
        text=new Signal[initial.length];fields=new TextField[initial.length];
        for(int i=0;i<initial.length;i++) {
            final int index=i; text[i]=Signals.of(Integer.toString(initial[i]));
            fields[i]=new TextField(text[i]).maxLength(11).onCommit(value->flush());
            fields[i].height(24);fields[i].tooltip(label(index));
        }
        rebuild();
    }
    public ToolSettingsPanel clearTo(int[] values) { clearValues=values.clone();return this; }
    public ToolSettingsPanel relativeOnly() { relativeOnly=true;relative=true;updateText();rebuild();return this; }
    public TextWidget status() { return status; }
    public UIComponent content() { return content; }
    public TextField field(int index) { return fields[index]; }
    public String kind() { return kind; }
    public void restoreFrom(ToolSettingsPanel previous) {
        if(previous==null || !kind.equals(previous.kind)) return;
        System.arraycopy(previous.original,0,original,0,original.length);
        System.arraycopy(previous.values,0,values,0,values.length);
        relative=relativeOnly || previous.relative;
        for(int i=0;i<text.length;i++) text[i].set(previous.text[i].get());
        status.setText(previous.status.getText().getString());rebuild();
    }
    private String label(int index) {
        if(kind.equals("destruction")) return new String[]{"Left","Right","Up","Down","Depth"}[index];
        return (kind.equals("copy")?(index<3?"Start ":"End "):"Offset ")+new String[]{"X","Y","Z"}[index%3];
    }
    private void rebuild() {
        content.clearChildren();
        content.child(new TextWidget(kind.equals("copy")?"Selection":kind.equals("paste")?"Paste offset":"Destruction dimensions"));
        if(kind.equals("copy")) {
            coordinates.setLabel(relative?"Coordinates: Relative":"Coordinates: World");
            if(!relativeOnly) content.child(coordinates);
            for(int group=0;group<2;group++) {
                HStack row=new HStack().gap(4);
                for(int axis=0;axis<3;axis++) row.child(Ui.column(new TextWidget(axis==0?(group==0?"Start X":"End X"):new String[]{"X","Y","Z"}[axis]),fields[group*3+axis]).gap(3).flex());
                content.child(row);
            }
        } else {
            for(int i=0;i<fields.length;i++) content.child(Ui.row(new TextWidget(label(i)).flex(),fields[i].width(56)).gap(6));
        }
        content.child(ModifierPanel.button(kind.equals("destruction")?"Revert dimensions":kind.equals("copy")?"Clear selection":"Reset offset",this::reset));
        if(kind.equals("destruction")) { updateSummary();content.child(summary); }
    }
    public boolean flush() {
        int[] parsed=new int[values.length];
        for(int i=0;i<parsed.length;i++) {
            try { parsed[i]=Integer.parseInt(text[i].get()); }
            catch(NumberFormatException e) { return error(label(i)+": enter a number"); }
            if(parsed[i]<minimum || parsed[i]>maximum) return error(label(i)+": out of range");
        }
        if(kind.equals("copy") && relative) for(int i=0;i<parsed.length;i++) {
            long absolute=(long)parsed[i]+values[i%3];
            if(absolute<minimum || absolute>maximum) return error("Coordinates out of range");
            parsed[i]=(int)absolute;
        }
        if(!valid.test(parsed)) return error("Dimensions exceed the allowed size");
        if(!Arrays.equals(values,parsed)) {
            System.arraycopy(parsed,0,values,0,values.length);save.accept(parsed.clone());
            if(relative) updateText();
        }
        status.setText("Saved");status.color(0xFFB8BEC8);updateSummary();return true;
    }
    private boolean error(String message) { status.setText(message);status.color(0xFFFF8888);return false; }
    private void switchCoordinates() { if(flush()) { relative=!relative;updateText();rebuild(); } }
    private void updateText() {
        for(int i=0;i<text.length;i++) text[i].set(Integer.toString(relative?values[i]-values[i%3]:values[i]));
    }
    private void updateSummary() {
        if(kind.equals("destruction")) summary.setText("Size: "+(1+values[0]+values[1])+" x "+(1+values[2]+values[3])+" x "+values[4]);
    }
    private void reset() {
        int[] reset=kind.equals("destruction")?original.clone():clearValues!=null?clearValues.clone():new int[values.length];
        System.arraycopy(reset,0,values,0,values.length);relative=relativeOnly;updateText();status.setText("Saved");status.color(0xFFB8BEC8);save.accept(reset);rebuild();
    }
}
