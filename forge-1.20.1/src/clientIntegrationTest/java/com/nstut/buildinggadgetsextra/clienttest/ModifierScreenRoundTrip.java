package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.InlineModifiers;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import net.minecraft.world.item.ItemStack;

/** Real inline controls, field commits, stage resets and authoritative inventory echo. */
public final class ModifierScreenRoundTrip {
    private int phase;
    private boolean preparedCapture;
    private int captureTicks;
    private MultitoolRadialScreen screen;
    private InlineModifiers panel;
    public boolean tick(ItemStack stack) {
        try {
            LiveModifiers current=MultitoolState.getLiveModifiers(stack);
            if(phase==0) {
                if(MultitoolState.getActiveMode(stack)!=com.nstut.buildinggadgetsextra.common.MultitoolMode.BUILD) return false;
                Minecraft.getInstance().options.guiScale().set(4); Minecraft.getInstance().resizeDisplay();
                screen=new MultitoolRadialScreen(stack); Minecraft.getInstance().setScreen(screen); screen.openActiveProfile(); panel=screen.inlineModifiers();
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
                Minecraft.getInstance().setScreen(null); phase=5;
            }
            return true;
        } catch(RuntimeException failure) { throw new IllegalStateException("inline editor fixture phase="+phase,failure); }
    }
    private void verifyDraftResize() {
        TextField input=panel.panel.field(1); input.setValue("-");
        if(panel.flush()) throw new IllegalStateException("invalid draft accepted");
        int w=screen.width,h=screen.height;
        screen.resize(Minecraft.getInstance(),320,240); panel=screen.inlineModifiers();
        if(!panel.panel.field(1).getValue().equals("-")) throw new IllegalStateException("resize lost numeric draft");
        screen.resize(Minecraft.getInstance(),w,h); panel=screen.inlineModifiers();
        panel.panel.field(1).setValue("3");
        if(!panel.flush()) throw new IllegalStateException("valid draft rejected after resize");
    }
    private void edit(int index,String value) {
        TextField input=panel.panel.field(index); panel.revealField(index); input.requestFocus(); panel.tick(); input.setValue(value);
        screen.keyPressed(257,0,0);
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
        if(!screen.mouseClicked(x,y,0)) throw new IllegalStateException("OpenUI button unreachable: "+button.getLabel().getString());
        screen.mouseReleased(x,y,0);
    }
    private void renderFrame() {
        // Render the current screen completely; tick-time framebuffer reads can catch an older frame.
        Minecraft mc=Minecraft.getInstance();
        org.joml.Matrix4f previous=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,screen.width,screen.height,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
        com.mojang.blaze3d.vertex.PoseStack modelView=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        modelView.pushPose(); modelView.setIdentity(); modelView.translate(0,0,-11000);
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(true);
        net.minecraft.client.gui.GuiGraphics graphics=new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource());
        graphics.drawManaged(()->screen.render(graphics,0,0,0)); graphics.flush();
        modelView.popPose(); com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(previous,com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
    }
    private void capture(String name) {
        renderFrame();
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            java.nio.file.Path path=java.nio.file.Paths.get(System.getProperty("bge.clientIntegrationResultDir","build/client-integration"));
            java.nio.file.Files.createDirectories(path); image.writeToFile(path.resolve(name));
        } catch(java.io.IOException failure) { throw new IllegalStateException("screenshot",failure); }
    }
}
