package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiTextFieldBase;
import com.nstut.buildinggadgetsextra.mixin.GuiIncrementerAccessor;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Shared client driver for BG2-based ports (1.20.1+). */
public final class ModernClientRangeAdapter implements ClientRangeRoundTripScenario.Adapter {
    @FunctionalInterface
    public interface ScreenClicker {
        void click(Screen screen, double x, double y);
    }

    private final ScreenClicker clicker;
    private static ScreenClicker generalClicker;
    static void clickGeneral(Screen screen,double x,double y) { generalClicker.click(screen,x,y); }
    private final ModifierScreenRoundTrip modifierRoundTrip = new ModifierScreenRoundTrip();
    private String pendingPassDetail;
    private int copyTransportTicks;
    private boolean copyTransportVerified;

    /** Called even after the UI scenario is DONE so a missing/truncated delivery cannot pass. */
    public void tickCopyTransport() {
        try {
            if (!copyTransportVerified) {
                copyTransportTicks++;
                ArrayList<StatePos> received = BG2DataClient.getLookupFromUUID(ModernCopyTransportFixture.GADGET_UUID);
                if (received != null) {
                    if (!ModernCopyTransportFixture.COPY_UUID.equals(
                            BG2DataClient.getCopyUUID(ModernCopyTransportFixture.GADGET_UUID))) {
                        throw new IllegalStateException("copy transport cache UUID mismatch");
                    }
                    if (received.size() != ModernCopyTransportFixture.POSITION_COUNT) {
                        throw new IllegalStateException("copy transport position count=" + received.size());
                    }
                    // Check every coordinate and alternating air/stone entry, including both corners.
                    // This also rejects duplicate positions, missing air, reordered chunks and bad bounds.
                    for (int index = 0; index < received.size(); index++) {
                        StatePos entry = received.get(index);
                        if (entry.pos.getX() != index % 100
                                || entry.pos.getY() != (index / 100) % 100
                                || entry.pos.getZ() != index / 10_000
                                || !entry.state.equals((index % 2 == 0 ? Blocks.AIR : Blocks.STONE).defaultBlockState())) {
                            throw new IllegalStateException("copy transport content mismatch at index=" + index);
                        }
                    }
                    copyTransportVerified = true;
                    System.out.println("[BGE client integration] million-position upstream copy delivery verified");
                } else if (copyTransportTicks > ClientRangeRoundTripScenario.TIMEOUT_TICKS) {
                    throw new IllegalStateException("timeout waiting for million-position upstream copy delivery");
                }
            }
            if (copyTransportVerified && pendingPassDetail != null) {
                finish("client-pass.txt", pendingPassDetail
                        + "; million-position upstream copy delivery verified (UUIDs, count, air, bounds)", null, 0);
            }
        } catch (Throwable error) {
            fail("million-position upstream copy delivery failed", error);
        }
    }

    public ModernClientRangeAdapter(ScreenClicker clicker) {
        this.clicker = clicker; generalClicker=clicker;
    }

    private Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    private ItemStack held() {
        return minecraft().player == null ? ItemStack.EMPTY : BaseGadget.getGadget(minecraft().player);
    }

    @Override
    public boolean hasReadyMultitool() {
        return held().getItem() instanceof BuildersMultitool && modifierRoundTrip.tick(held());
    }

    @Override
    public int clientRange() {
        return GadgetNBT.getToolRange(held());
    }

    @Override
    public void openMultitoolScreen() {
        minecraft().setScreen(new MultitoolRadialScreen(held()));
    }

    @Override
    public boolean isMultitoolScreenOpen() {
        return minecraft().screen instanceof MultitoolRadialScreen;
    }

    @Override
    public int visibleScreenRange() {
        com.nstut.openui.api.UIComponent root=profileRoot();
        com.nstut.openui.api.TextWidget label=findRange(root);
        return label==null?-1:Integer.parseInt(label.getText().getString().substring("Range: ".length()));
    }

    @Override
    public void enterBuildSubmenu() {
        Screen screen = radialScreen();
        clicker.click(screen, screen.width / 2.0, screen.height / 2.0 - 68.0);
    }

    @Override
    public void clickRangePlus() {
        MultitoolRadialScreen screen=(MultitoolRadialScreen)radialScreen();
        com.nstut.openui.api.ButtonWidget plus=findPlus(profileRoot());
        if(plus==null) throw new IllegalStateException("OpenUI range step missing");
        // Server echo can rebuild the screen during a tick before the next render on slow CI GPUs.
        screen.uiRuntime().root().layoutTree(minecraft().font,0,0,screen.width,screen.height);
        plus.requestFocus();
        screen.uiRuntime().root().layoutTree(minecraft().font,0,0,screen.width,screen.height);
        clicker.click(screen,plus.getX()+plus.getWidth()/2.0,plus.getY()+plus.getHeight()/2.0);
    }

    @Override
    public boolean supportsExtendedStateRoundTrip() {
        return true;
    }

    @Override
    public int clientActiveToolOrdinal() {
        return MultitoolState.getActiveMode(held()).ordinal();
    }

    @Override
    public void switchToBuildSubmenu() {
        switchToTool(MultitoolMode.BUILD);
    }

    @Override
    public void switchToExchangeSubmenu() {
        switchToTool(MultitoolMode.EXCHANGING);
    }

    @Override
    public void switchToCopySubmenu() {
        switchToTool(MultitoolMode.COPY_PASTE);
    }

    @Override
    public void switchToDestructionSubmenu() {
        switchToTool(MultitoolMode.DESTRUCTION);
    }

    private com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel toolEditor(String kind) {
        MultitoolRadialScreen screen=(MultitoolRadialScreen)radialScreen();
        if(screen.toolSettings()==null || !screen.toolSettings().kind().equals(kind)) throw new IllegalStateException("inline "+kind+" settings missing");
        return screen.toolSettings();
    }
    @Override public boolean isCopyScreenOpen() { return minecraft().screen instanceof MultitoolRadialScreen && ((MultitoolRadialScreen)minecraft().screen).toolSettings()!=null && ((MultitoolRadialScreen)minecraft().screen).toolSettings().kind().equals("copy"); }
    @Override public void openCopyConfig() { toolEditor("copy"); captureInline("copy-settings.png"); }
    @Override public int visibleCopyStartX() { return Integer.parseInt(toolEditor("copy").field(0).getValue()); }
    @Override public void clickCopyStartXPlus() { var editor=toolEditor("copy");editor.field(0).setValue(Integer.toString(visibleCopyStartX()+1));if(!editor.flush()) throw new IllegalStateException("copy edit rejected"); }
    @Override public void confirmCopyConfig() { if(!toolEditor("copy").flush()) throw new IllegalStateException("copy commit rejected");closeScreen(); }

    @Override
    public int clientCopyStartX() {
        return GadgetNBT.getCopyStartPos(held()).getX();
    }

    // Drive the actual upstream BG2 CopyGUI. The native screen owns the buttons,
    // incrementers and packet path; the dedicated server independently observes the result.
    @Override public void openNativeCopyGui() {
        minecraft().setScreen(new CopyGUI(held()));
        requireCopyGui();
        if (GadgetNBT.getCopyStartPos(held()).getX() != 1)
            throw new IllegalStateException("native CopyGUI fixture did not receive the prior selection");
    }
    private CopyGUI requireCopyGui() {
        if (!(minecraft().screen instanceof CopyGUI screen))
            throw new IllegalStateException("BG2 CopyGUI not open");
        return screen;
    }
    private List<GuiIncrementer> copyGuiFields() {
        List<GuiIncrementer> result = new ArrayList<>();
        for (GuiEventListener child : requireCopyGui().children())
            if (child instanceof GuiIncrementer incrementer) result.add(incrementer);
        if (result.size() != 6) throw new IllegalStateException("expected six BG2 CopyGUI incrementers; got " + result.size());
        return result;
    }
    private GuiTextFieldBase copyGuiText(GuiIncrementer incrementer) {
        return ((GuiIncrementerAccessor) (Object) incrementer).buildingGadgetsExtra$getField();
    }
    @Override public void editNativeCopyGuiFields() {
        List<GuiIncrementer> fields = copyGuiFields();
        int[] indices = {0, 1, 3, 5};
        String[] values = {"3", "-2", "5", "7"};
        for (int i = 0; i < indices.length; i++) {
            GuiIncrementer field = fields.get(indices[i]);
            field.setFocused(true);
            GuiTextFieldBase text = copyGuiText(field);
            text.setValue("");
            text.insertText(values[i]); // actual EditBox text-input/filter path
            if (!values[i].equals(text.getValue())) throw new IllegalStateException("typed CopyGUI draft rejected");
            field.setFocused(false); // verify edits survive focus changes before Confirm
        }
        for (int i = 0; i < indices.length; i++)
            if (!values[i].equals(copyGuiText(fields.get(indices[i])).getValue()))
                throw new IllegalStateException("BG2 CopyGUI lost a draft after focus changed");
    }
    private void clickCopyGuiButton(boolean modeToggle) {
        CopyGUI screen = requireCopyGui();
        AbstractButton selected = null;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractButton button && button.getWidth() == (modeToggle ? 120 : 50)) {
                selected = button;
                break; // upstream adds Confirm first, then Close, Clear, and mode switch
            }
        }
        if (selected == null) throw new IllegalStateException("BG2 CopyGUI button not found");
        clicker.click(screen, selected.getX() + selected.getWidth() / 2.0, selected.getY() + selected.getHeight() / 2.0);
    }
    @Override public void toggleNativeCopyGuiModeAndCheck(boolean absolute) {
        clickCopyGuiButton(true);
        int[] expected = absolute ? new int[]{4, -2, 0, 6, 2, 7} : new int[]{3, -2, 0, 5, 2, 7};
        List<GuiIncrementer> fields = copyGuiFields();
        for (int i = 0; i < 6; i++) {
            int actual = fields.get(i).getValue();
            if (actual != expected[i] || copyGuiText(fields.get(i)).getInt() != expected[i])
                throw new IllegalStateException("BG2 CopyGUI mode switch lost field " + i + ": " + actual + " expected " + expected[i]);
        }
    }
    @Override public void confirmNativeCopyGui() {
        clickCopyGuiButton(false);
        if (minecraft().screen instanceof CopyGUI) throw new IllegalStateException("BG2 Confirm did not close CopyGUI");
    }
    @Override public boolean nativeCopyGuiServerResultObserved() {
        return GadgetNBT.getCopyStartPos(held()).equals(new BlockPos(4, -2, 0))
                && GadgetNBT.getCopyEndPos(held()).equals(new BlockPos(9, 0, 7));
    }

    @Override public boolean isDestructionScreenOpen() { return minecraft().screen instanceof MultitoolRadialScreen && ((MultitoolRadialScreen)minecraft().screen).toolSettings()!=null && ((MultitoolRadialScreen)minecraft().screen).toolSettings().kind().equals("destruction"); }
    @Override public void openDestructionConfig() { toolEditor("destruction");captureInline("destruction-settings.png"); }
    @Override public int visibleDestructionLeft() { return Integer.parseInt(toolEditor("destruction").field(0).getValue()); }
    @Override public int visibleDestructionDepth() { return Integer.parseInt(toolEditor("destruction").field(4).getValue()); }
    @Override public void clickDestructionLeftPlus() { incrementDestruction(0); }
    @Override public void clickDestructionDepthPlus() { incrementDestruction(4); }
    private void incrementDestruction(int index) { var editor=toolEditor("destruction");editor.field(index).setValue(Integer.toString(Integer.parseInt(editor.field(index).getValue())+1));if(!editor.flush()) throw new IllegalStateException("destruction edit rejected"); }
    private void captureInline(String name) {
        ModifierScreenRoundTrip.captureCurrent(name);
    }

    @Override
    public int clientDestructionLeft() {
        return GadgetNBT.getToolValue(held(), "left");
    }

    @Override
    public int clientDestructionDepth() {
        return GadgetNBT.getToolValue(held(), "depth");
    }

    @Override
    public void closeScreen() {
        minecraft().setScreen(null);
    }

    private Screen radialScreen() {
        Screen screen = minecraft().screen;
        if (!(screen instanceof MultitoolRadialScreen)) throw new IllegalStateException("multitool radial screen missing");
        return screen;
    }

    private void switchToTool(MultitoolMode tool) {
        Screen screen = radialScreen();
        // Center is Back in a submenu and harmless in the general page, so this normalizes navigation.
        clicker.click(screen, ((MultitoolRadialScreen)screen).menuCenterX(), ((MultitoolRadialScreen)screen).menuCenterY());
        double angle = -Math.PI / 2.0 + tool.ordinal() * (Math.PI * 2.0 / MultitoolMode.values().length);
        clicker.click(screen,
                screen.width / 2.0 + Math.cos(angle) * 68.0,
                screen.height / 2.0 + Math.sin(angle) * 68.0);
    }

    private com.nstut.openui.api.UIComponent profileRoot() {
        if(!(minecraft().screen instanceof MultitoolRadialScreen screen) || screen.uiRuntime()==null) return null;
        return screen.uiRuntime().root();
    }
    private com.nstut.openui.api.TextWidget findRange(com.nstut.openui.api.UIComponent root) {
        if(root==null) return null;
        if(root instanceof com.nstut.openui.api.TextWidget text && text.getText().getString().startsWith("Range: ")) return text;
        for(com.nstut.openui.api.UIComponent child:root.children()) { com.nstut.openui.api.TextWidget found=findRange(child); if(found!=null) return found; }
        return null;
    }
    private com.nstut.openui.api.ButtonWidget findPlus(com.nstut.openui.api.UIComponent root) {
        if(root==null) return null;
        if(root instanceof com.nstut.openui.api.ButtonWidget button && button.getLabel().getString().equals("+")) return button;
        for(com.nstut.openui.api.UIComponent child:root.children()) { com.nstut.openui.api.ButtonWidget found=findPlus(child); if(found!=null) return found; }
        return null;
    }

    @Override
    public void pass(String detail) {
        pendingPassDetail = detail;
    }

    @Override
    public void fail(String detail, Throwable error) {
        finish("client-fail.txt", detail, error, 1);
    }

    private void finish(String file, String detail, Throwable error, int code) {
        try {
            Path dir = Paths.get(System.getProperty("bge.clientIntegrationResultDir", "build/client-integration"));
            Files.createDirectories(dir);
            String text = detail + (error == null ? "" : "\n" + error.toString()) + "\n";
            Files.write(dir.resolve(file), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException io) {
            io.printStackTrace();
            code = 1;
        }
        System.out.println("[BGE client integration] " + detail);
        if (error != null) error.printStackTrace();
        System.exit(code);
    }
}
