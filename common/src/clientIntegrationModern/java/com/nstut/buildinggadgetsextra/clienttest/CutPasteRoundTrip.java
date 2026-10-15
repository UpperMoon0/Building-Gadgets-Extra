package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.openui.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import java.util.*;
import java.util.function.Consumer;

/** The same real network/GUI lifecycle runs on integrated and dedicated servers on every BG2 port. */
public final class CutPasteRoundTrip {
    private enum Stage { START, TERRAIN, FIRST_CORNER, SECOND_CORNER, PASTE_MENU, FULLSCREEN, WINDOWED, REOPEN,
        BUILD_PROFILE, CUT_PROFILE, DESTINATION, PASTED, FINISHED }
    private Stage stage=Stage.START;
    private int ticks,settle;
    private UUID gadget,buffer;
    private final List<String> steps=new ArrayList<>();
    private Consumer<ButtonWidget> click;
    private Consumer<String> capture;
    private Minecraft mc() { return Minecraft.getInstance(); }
    private net.minecraft.world.item.ItemStack held() { return mc().player.getMainHandItem(); }
    private MultitoolRadialScreen screen() {
        if(!(mc().screen instanceof MultitoolRadialScreen s)) throw new IllegalStateException("Cut menu is missing at "+stage);
        return s;
    }
    public boolean tick(Consumer<ButtonWidget> click,Consumer<String> capture) {
        this.click=click;this.capture=capture;
        try {
            if(++ticks>400) throw new IllegalStateException("timeout at "+stage+"; mode="+GadgetNBT.getMode(held()).getId()
                    +" buffer="+GadgetNBT.hasCopyUUID(held())+" start="+GadgetNBT.getCopyStartPos(held())+" end="+GadgetNBT.getCopyEndPos(held())+" position="+mc().player.position()+" pitch="+mc().player.getXRot());
            switch(stage) {
                case START:
                    CutFlowEvidence.start("client");gadget=GadgetNBT.getUUID(held());
                    require(!button("Cut selection").isEnabled(),"Cut enabled with no selection");
                    steps.add("empty_selection_disabled");screen().onClose();next(Stage.TERRAIN);break;
                case TERRAIN:
                    if(!aimedAt(CutPasteServerScenario.SOURCE)) break;
                    CutPasteNativeInput.aimDown();if(++settle<3) break;
                    use(false);next(Stage.FIRST_CORNER);break;
                case FIRST_CORNER:
                    if(!GadgetNBT.getCopyStartPos(held()).equals(CutPasteServerScenario.SOURCE)) break;
                    require(GadgetNBT.getCopyEndPos(held()).equals(GadgetNBT.nullPos),"first click selected both corners");
                    open();require(!button("Cut selection").isEnabled(),"Cut enabled with only one corner");
                    geometry();capture.accept("cut-first-corner.png");steps.add("first_corner_disabled");
                    screen().onClose();use(true);next(Stage.SECOND_CORNER);break;
                case SECOND_CORNER:
                    if(!GadgetNBT.getCopyEndPos(held()).equals(CutPasteServerScenario.SOURCE)) break;
                    open();require(button("Cut selection").isEnabled(),"Cut disabled after both corners arrived");geometry();
                    steps.add("selection_received");capture.accept("cut-ready.png");click.accept(button("Cut selection"));next(Stage.PASTE_MENU);break;
                case PASTE_MENU:
                    if(!GadgetNBT.hasCopyUUID(held()) || screen().toolSettings()==null || !screen().toolSettings().kind().equals("paste")) break;
                    buffer=GadgetNBT.getCopyUUID(held());pasteMenu();steps.add("automatic_paste_menu");
                    capture.accept("cut-paste-ready.png");
                    // Resize while holding an unfinished draft: the server echo must not resurrect old selection fields.
                    screen().toolSettings().field(0).setValue("-");
                    int w=screen().width,h=screen().height;
                    for(int[] size:new int[][]{{320,240},{640,360},{w,h}}) {
                        CutPasteNativeInput.resize(screen(),size[0],size[1]);pasteMenu();
                        require(screen().toolSettings().field(0).getValue().equals("-"),"resize lost Paste draft");
                    }
                    screen().toolSettings().field(0).setValue("0");require(screen().toolSettings().flush(),"restored draft rejected");
                    steps.add("resize_preserved_paste");mc().getWindow().toggleFullScreen();next(Stage.FULLSCREEN);break;
                case FULLSCREEN:
                    if(++settle<20) break;pasteMenu();mc().getWindow().toggleFullScreen();next(Stage.WINDOWED);break;
                case WINDOWED:
                    if(++settle<20) break;pasteMenu();steps.add("fullscreen_preserved_paste");screen().onClose();open();
                    pasteMenu();steps.add("reopen_preserved_paste");next(Stage.REOPEN);break;
                case REOPEN:
                    select(MultitoolMode.BUILD);next(Stage.BUILD_PROFILE);break;
                case BUILD_PROFILE:
                    if(MultitoolState.getActiveMode(held())!=MultitoolMode.BUILD || screen().inlineModifiers()==null) break;
                    select(MultitoolMode.CUT_PASTE);next(Stage.CUT_PROFILE);break;
                case CUT_PROFILE:
                    if(MultitoolState.getActiveMode(held())!=MultitoolMode.CUT_PASTE || screen().toolSettings()==null
                            || !screen().toolSettings().kind().equals("paste")) break;
                    pasteMenu();steps.add("profile_preserved_buffer");screen().onClose();next(Stage.DESTINATION);break;
                case DESTINATION:
                    if(!aimedAt(CutPasteServerScenario.DESTINATION)) break;
                    CutPasteNativeInput.aimDown();if(++settle<3) break;
                    var preview=com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient.getLookupFromUUID(gadget);
                    if(preview==null || !buffer.equals(com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient.getCopyUUID(gadget))) break;
                    require(preview.size()==1 && preview.get(0).pos.equals(BlockPos.ZERO)
                            && preview.get(0).state.equals(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),"Paste preview contents do not match the cut block");
                    steps.add("preview_received");use(false);next(Stage.PASTED);break;
                case PASTED:
                    if(GadgetNBT.hasCopyUUID(held())) break;
                    if(!mc().level.getBlockState(CutPasteServerScenario.DESTINATION).is(net.minecraft.world.level.block.Blocks.STONE)
                            || !mc().level.isEmptyBlock(CutPasteServerScenario.SOURCE)) break;
                    steps.add("world_move_received");
                    require(GadgetNBT.getMode(held()).getId().getPath().equals("cut"),"consumed buffer still previews Paste");
                    open();require(screen().toolSettings().kind().equals("copy"),"Paste did not restore selection controls");
                    require(!button("Cut selection").isEnabled(),"Cut remained enabled after Paste");geometry();
                    steps.add("buffer_consumed");steps.add("returned_to_selection");capture.accept("cut-after-paste.png");
                    CutFlowEvidence.pass("client",gadget,buffer,mc().getSingleplayerServer()==null,steps);next(Stage.FINISHED);break;
                case FINISHED:return true;
            }
            return false;
        } catch(RuntimeException failure) {
            try { if(mc().screen instanceof MultitoolRadialScreen) capture.accept("cut-failure-"+stage+".png"); }
            catch(RuntimeException captureFailure) { failure.addSuppressed(captureFailure); }
            CutFlowEvidence.fail("client",stage+": "+failure);throw new IllegalStateException("Cut/Paste lifecycle failed at "+stage,failure);
        }
    }
    private void next(Stage next) { System.out.println("[BGE cut flow] "+stage+" -> "+next);stage=next;ticks=0;settle=0; }
    private boolean aimedAt(BlockPos at) {
        return Math.abs(mc().player.getX()-(at.getX()+0.5))<0.05 && mc().player.getY()>=at.getY()-0.05 && mc().player.getY()<=at.getY()+2.1
                && Math.abs(mc().player.getZ()-(at.getZ()+0.5))<0.05;
    }
    private void use(boolean shift) { CutPasteNativeInput.use(shift); }
    private void open() { MultitoolRadialScreen s=new MultitoolRadialScreen(held());mc().setScreen(s);s.openActiveProfile(); }
    private ButtonWidget button(String label) {
        ButtonWidget result=find(screen().uiRuntime().root(),label);
        require(result!=null,"missing control "+label);return result;
    }
    private void select(MultitoolMode tool) {
        click.accept(button("Back"));
        // The native general wheel uses the same entry path as a player choosing a profile.
        MultitoolRadialScreen s=screen();double angle=-Math.PI/2+tool.ordinal()*Math.PI*2/MultitoolMode.values().length;
        ModernClientRangeAdapter.clickGeneral(s,s.width/2.0+Math.cos(angle)*68,s.height/2.0+Math.sin(angle)*68);
    }
    private void pasteMenu() {
        require(screen().toolSettings()!=null && screen().toolSettings().kind().equals("paste"),"Paste controls disappeared");
        require(find(screen().uiRuntime().root(),"Cut selection")==null,"Cut action remains while a buffer is pending");
        require(GadgetNBT.hasCopyUUID(held()) && (buffer==null || buffer.equals(GadgetNBT.getCopyUUID(held()))),"pending contents were replaced");
        require(GadgetNBT.getMode(held()).getId().getPath().equals("paste"),"preview/action is not Paste");geometry();
    }
    private void geometry() {
        MultitoolRadialScreen s=screen();s.uiRuntime().root().layoutTree(mc().font,0,0,s.width,s.height);
        int count=s.toolSettings().kind().equals("paste")?3:6;
        for(int i=0;i<count;i++) {
            UIComponent a=s.toolSettings().field(i);require(a.getWidth()>0 && a.getHeight()>0,"unlaid selection/offset field");
            for(int j=0;j<i;j++) {
                UIComponent b=s.toolSettings().field(j);
                require(a.getX()>=b.getX()+b.getWidth() || b.getX()>=a.getX()+a.getWidth()
                        || a.getY()>=b.getY()+b.getHeight() || b.getY()>=a.getY()+a.getHeight(),"fields overlap after layout");
            }
        }
        ButtonWidget cut=find(s.uiRuntime().root(),"Cut selection");
        if(cut!=null) require(cut.getWidth()>0 && cut.getHeight()>0 && cut.getY()>=0 && cut.getY()+cut.getHeight()<=s.height,"primary Cut button clipped");
    }
    private static ButtonWidget find(UIComponent root,String label) {
        if(root instanceof ButtonWidget b && b.getLabel().getString().equals(label)) return b;
        for(UIComponent child:root.children()) { ButtonWidget result=find(child,label);if(result!=null)return result; }return null;
    }
    private static void require(boolean ok,String message) { if(!ok) throw new IllegalStateException(message); }
}
