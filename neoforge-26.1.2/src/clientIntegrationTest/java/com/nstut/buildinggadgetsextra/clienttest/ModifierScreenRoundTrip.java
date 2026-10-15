package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.InlineModifiers;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import net.minecraft.world.item.ItemStack;

/** Real inline controls, field commits, stage resets and authoritative inventory echo. */
public final class ModifierScreenRoundTrip {
    private int phase;
    private int reportedPhase=-1, diagnosticTicks;
    private boolean preparedCapture;
    private int captureTicks;
    private MultitoolRadialScreen screen;
    private InlineModifiers panel;
    public boolean tick(ItemStack stack) {
        try {
            LiveModifiers current=MultitoolState.getLiveModifiers(stack);
            if(reportedPhase!=phase || ++diagnosticTicks>=100) {
                reportedPhase=phase; diagnosticTicks=0;
                InlineModifiers visible=screen==null?null:screen.inlineModifiers();
                System.out.println("[BGE modifier fixture] phase="+phase+" mode="+MultitoolState.getActiveMode(stack)
                        +" spacingX="+current.value(1)+" visibleProfile="+(visible==null?-1:visible.panel.profile())
                        +" screenCurrent="+(screen!=null && Minecraft.getInstance().screen==screen));
            }
            if(phase==0) {
                if(MultitoolState.getActiveMode(stack)!=com.nstut.buildinggadgetsextra.common.MultitoolMode.BUILD) return false;

                screen=new MultitoolRadialScreen(stack); Minecraft.getInstance().setScreen(screen); screen.openActiveProfile(); ToolSettingsPanelChecks.run(screen.uiRuntime()); screen.openActiveProfile(); panel=screen.inlineModifiers();
                if(screen.uiRuntime()==null) throw new IllegalStateException("OpenUI screen runtime was not initialized");
                if(panel==null) throw new IllegalStateException("inline Build settings missing");
                captureTicks=0; phase=8; return false;
            }
            if(phase==8) {
                if(++captureTicks<20) return false;
                capture("inline-collapsed-scale4.png");
                press(panel.panel.toggles[0]); panel.expand(0);
                edit(0,"2"); edit(1,"3"); edit(3,"-2"); phase=1; return false;
            }
            if(phase==1) {
                if(current.value(0)!=2 || current.value(1)!=3 || current.value(3)!=-2) return false;
                if(!preparedCapture) {
                    verifyDraftResize(); panel.expand(0); panel.expand(0); preparedCapture=true; captureTicks=0; return false;
                }
                if(++captureTicks<20) return false;
                capture("inline-array.png"); preparedCapture=false;
                press(panel.panel.toggles[1]); panel.expand(1);
                edit(4,"4"); edit(5,"1"); edit(6,"-1"); phase=2; return false;
            }
            if(phase==2) {
                if(current.value(4)!=4 || current.value(5)!=1 || current.value(6)!=-1) return false;
                if(!preparedCapture) { panel.expand(1); panel.expand(1); preparedCapture=true; captureTicks=0; return false; }
                if(++captureTicks<20) return false;
                capture("inline-radial.png"); preparedCapture=false;
                press(panel.panel.toggles[0]); phase=6; return false;
            }
            if(phase==6) {
                if(current.value(0)!=1) return false;
                if(current.value(8)!=2 || current.value(1)!=3 || current.value(3)!=-2) throw new IllegalStateException("Off lost Array configuration");
                press(panel.panel.toggles[0]); phase=7; return false;
            }
            if(phase==7) {
                if(current.value(0)!=2) return false;
                panel.expand(0); press(findButton(panel.panel.content(),panel.panel.expanded()==0?"Reset Array":"Reset Radial")); phase=3; return false;
            }
            if(phase==3) {
                if(current.value(0)!=1 || current.value(4)!=4) return false;
                if(current.value(5)!=1 || current.value(6)!=-1) throw new IllegalStateException("reset Array changed Radial");
                panel.expand(1); press(findButton(panel.panel.content(),panel.panel.expanded()==0?"Reset Array":"Reset Radial")); phase=4; return false;
            }
            if(phase==4) {
                if(current.enabled() || current.value(8)!=2 || current.value(9)!=2) return false;
                selectProfile(MultitoolMode.EXCHANGING); phase=20; return false;
            }
            if(phase==20) {
                if(!profileReady(stack,MultitoolMode.EXCHANGING)) return false;
                if(!panel.panel.field(1).getValue().equals(Integer.toString(current.value(1))))
                    throw new IllegalStateException("profile navigation retained the previous profile draft");
                edit(1,"7"); phase=21; return false;
            }
            if(phase==21) {
                if(current.value(1)!=7) return false;
                selectProfile(MultitoolMode.BUILD); phase=22; return false;
            }
            if(phase==22) {
                if(!profileReady(stack,MultitoolMode.BUILD)) return false;
                if(current.value(1)!=LiveModifiers.NONE.value(1) || !panel.panel.field(1).getValue().equals(Integer.toString(current.value(1))))
                    throw new IllegalStateException("exchange edit contaminated the Build profile");
                selectProfile(MultitoolMode.EXCHANGING); phase=23; return false;
            }
            if(phase==23) {
                if(!profileReady(stack,MultitoolMode.EXCHANGING)) return false;
                if(current.value(1)!=7 || !panel.panel.field(1).getValue().equals("7"))
                    throw new IllegalStateException("return navigation lost the Exchange draft");
                panel.expand(0); press(findButton(panel.panel.content(),"Reset Array")); phase=24; return false;
            }
            if(phase==24) {
                if(current.value(1)!=LiveModifiers.NONE.value(1)) return false;
                selectProfile(MultitoolMode.BUILD); phase=25; return false;
            }
            if(phase==25) {
                if(!profileReady(stack,MultitoolMode.BUILD)) return false;
                Minecraft.getInstance().setScreen(null); phase=5;
            }
            return true;
        } catch(RuntimeException failure) { throw new IllegalStateException("inline editor fixture phase="+phase,failure); }
    }
    private boolean profileReady(ItemStack stack,MultitoolMode mode) {
        if(MultitoolState.getActiveMode(stack)!=mode) return false;
        // ProfileSyncMixin deliberately replaces the screen after the server echo.
        // Continue driving the visible screen rather than the retired fixture reference.
        if(!(Minecraft.getInstance().screen instanceof MultitoolRadialScreen)) return false;
        screen=(MultitoolRadialScreen)Minecraft.getInstance().screen;
        panel=screen.inlineModifiers();
        return panel!=null && panel.panel.profile()==mode.ordinal();
    }
    private void selectProfile(MultitoolMode mode) {
        panel.tick(); // Consume the authoritative acknowledgement before Back navigation.
        press(findButton(screen.uiRuntime().root(),"Back"));
        double angle=-Math.PI/2+mode.ordinal()*(Math.PI*2/MultitoolMode.values().length);
        double x=screen.menuCenterX()+Math.cos(angle)*68, y=screen.menuCenterY()+Math.sin(angle)*68;
        if(!screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0)),false)) throw new IllegalStateException("general profile wheel unreachable");
        screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0)));
        if(screen.inlineModifiers()!=null) throw new IllegalStateException("profile controls exposed before server sync");
        screen.resize(screen.width,screen.height);
        if(screen.inlineModifiers()!=null) throw new IllegalStateException("resize exposed controls before profile sync");
    }
    private void verifyDraftResize() {
        TextField input=panel.panel.field(1);
        input.setValue("abc");
        if(panel.flush()) throw new IllegalStateException("nonnumeric draft silently accepted");
        input.setValue("-");
        if(panel.flush()) throw new IllegalStateException("invalid draft accepted");
        int w=screen.width,h=screen.height;
        screen.resize(320,240); panel=screen.inlineModifiers();
        verifyVisibleControlsAfterResize();
        if(!panel.panel.field(1).getValue().equals("-")) throw new IllegalStateException("resize lost numeric draft");
        screen.resize(w,h); panel=screen.inlineModifiers();
        verifyVisibleControlsAfterResize();
        panel.panel.field(1).setValue("3");
        if(!panel.flush()) throw new IllegalStateException("valid draft rejected after resize");
    }
    private void verifyVisibleControlsAfterResize() {
        if(findButton(screen.uiRuntime().root(),"Reset Array")==null)
            throw new IllegalStateException("resize disposed the visible modifier tree");
        if(!containsRangeSlider(screen.uiRuntime().root()))
            throw new IllegalStateException("resize disposed the visible range controls");
        if(!containsComponent(screen.uiRuntime().root(),panel.panel.field(1)))
            throw new IllegalStateException("resize detached the retained text field");
    }
    private boolean containsRangeSlider(UIComponent root) {
        if(root instanceof com.nstut.openui.controls.Slider) return true;
        for(UIComponent child:root.children()) if(containsRangeSlider(child)) return true;
        return false;
    }
    private boolean containsComponent(UIComponent root,UIComponent target) {
        if(root==target) return true;
        for(UIComponent child:root.children()) if(containsComponent(child,target)) return true;
        return false;
    }
    private void edit(int index,String value) {
        TextField input=panel.panel.field(index); panel.revealField(index); input.requestFocus(); panel.tick(); input.setValue(value);
        screen.keyPressed(new net.minecraft.client.input.KeyEvent(257,0,0));
        input.clearFocus();
        if(!panel.flush()) throw new IllegalStateException("valid numeric input rejected");
    }
    private ButtonWidget findButton(UIComponent root,String label) {
        if(root instanceof ButtonWidget button && button.getLabel().getString().equals(label)) return button;
        for(UIComponent child:root.children()) { ButtonWidget found=findButton(child,label); if(found!=null) return found; }
        return null;
    }
    private void press(ButtonWidget button) {
        if(button==null) throw new IllegalStateException("missing OpenUI control");
        button.requestFocus(); renderFrame();
        double x=button.getX()+button.getWidth()/2.0, y=button.getY()+button.getHeight()/2.0;
        if(!screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0)),false)) throw new IllegalStateException("OpenUI button unreachable: "+button.getLabel().getString());
        screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(0,0)));
    }
    private void renderFrame() {
        screen.uiRuntime().root().layoutTree(Minecraft.getInstance().font,0,0,screen.width,screen.height);
    }
    public static void captureCurrent(String name) {
        ModifierScreenRoundTrip fixture=new ModifierScreenRoundTrip();
        fixture.screen=(MultitoolRadialScreen)Minecraft.getInstance().screen;
        fixture.capture(name);
    }
    private void capture(String name) { renderFrame(); }
}
