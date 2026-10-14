package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.InlineModifiers;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Field;
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
                if(panel==null) throw new IllegalStateException("inline Build settings missing");
                captureTicks=0; phase=8; return false;
            }
            if(phase==8) {
                if(++captureTicks<20) return false;
                capture("inline-collapsed-scale4.png");
                press(((Button[])field("toggles"))[0]); panel.expand(0);
                edit(0,"2"); edit(1,"3"); edit(3,"-2"); phase=1; return false;
            }
            if(phase==1) {
                if(current.value(0)!=2 || current.value(1)!=3 || current.value(3)!=-2) return false;
                if(!preparedCapture) {
                    verifyDraftResize(); panel.expand(0); panel.expand(0); preparedCapture=true; captureTicks=0; return false;
                }
                if(++captureTicks<20) return false;
                capture("inline-array.png"); preparedCapture=false;
                press(((Button[])field("toggles"))[1]); panel.expand(1);
                edit(4,"4"); edit(5,"1"); edit(6,"-1"); phase=2; return false;
            }
            if(phase==2) {
                if(current.value(4)!=4 || current.value(5)!=1 || current.value(6)!=-1) return false;
                if(!preparedCapture) { panel.expand(1); panel.expand(1); preparedCapture=true; captureTicks=0; return false; }
                if(++captureTicks<20) return false;
                capture("inline-radial.png"); preparedCapture=false;
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
        EditBox input=((EditBox[])field("fields"))[1]; input.setValue("-");
        if(panel.flush()) throw new IllegalStateException("invalid draft accepted");
        int w=screen.width,h=screen.height;
        screen.resize(Minecraft.getInstance(),320,240); panel=screen.inlineModifiers();
        if(!((EditBox[])field("fields"))[1].getValue().equals("-")) throw new IllegalStateException("resize lost numeric draft");
        screen.resize(Minecraft.getInstance(),w,h); panel=screen.inlineModifiers();
        ((EditBox[])field("fields"))[1].setValue("3");
        if(!panel.flush()) throw new IllegalStateException("valid draft rejected after resize");
    }
    private Object field(String name) throws ReflectiveOperationException { Field f=InlineModifiers.class.getDeclaredField(name); f.setAccessible(true); return f.get(panel); }
    private void edit(int index,String value) throws ReflectiveOperationException {
        EditBox input=((EditBox[])field("fields"))[index]; panel.revealField(index); input.setFocused(true); panel.tick(); input.setValue(value);
        screen.keyPressed(257,0,0);
        input.setFocused(false);
        if(!panel.flush()) throw new IllegalStateException("valid numeric input rejected");
    }
    private void press(Button button) {
        for(int i=0;i<20 && !button.visible;i++) panel.scroll(36);
        if(!button.visible) { for(int i=0;i<20 && !button.visible;i++) panel.scroll(-36); }
        if(!button.visible) throw new IllegalStateException("inline control unreachable: "+button.getMessage().getString());
        button.onPress();
    }
    private void capture(String name) {
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
        try(var image=net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            java.nio.file.Path path=java.nio.file.Paths.get(System.getProperty("bge.clientIntegrationResultDir","build/client-integration"));
            java.nio.file.Files.createDirectories(path); image.writeToFile(path.resolve(name));
        } catch(java.io.IOException failure) { throw new IllegalStateException("screenshot",failure); }
    }
}
