package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.worlddata.BG2DataClient;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/** Shared client driver for BG2-based ports (1.20.1+). */
public final class ModernClientRangeAdapter implements ClientRangeRoundTripScenario.Adapter {
    @FunctionalInterface
    public interface ScreenClicker {
        void click(Screen screen, double x, double y);
    }

    private final ScreenClicker clicker;
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
        this.clicker = clicker;
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

    @Override
    public boolean isCopyScreenOpen() {
        return minecraft().screen instanceof CopyGUI;
    }

    @Override
    public void openCopyConfig() {
        Screen screen = radialScreen();
        // Copy/Paste right rail: raytrace, paste-replace, then copy-selection GUI.
        clicker.click(screen, screen.width / 2.0 + 124.0, screen.height / 2.0 - 32.0);
    }

    @Override
    public int visibleCopyStartX() {
        return copyStartX().getValue();
    }

    @Override
    public void clickCopyStartXPlus() {
        GuiIncrementer startX = copyStartX();
        clicker.click(copyScreen(), startX.getX() + 60.0, startX.getY() + 7.0);
    }

    @Override
    public void confirmCopyConfig() {
        Screen screen = copyScreen();
        // Four centered buttons total 285 px; Confirm is the first 50 px button.
        clicker.click(screen, screen.width / 2.0 - 117.0, screen.height / 2.0 + 30.0);
    }

    @Override
    public int clientCopyStartX() {
        return GadgetNBT.getCopyStartPos(held()).getX();
    }

    @Override
    public boolean isDestructionScreenOpen() {
        return minecraft().screen instanceof DestructionGUI;
    }

    @Override
    public void openDestructionConfig() {
        Screen screen = radialScreen();
        // Destruction has a single right-rail configure button at y = center - 104.
        clicker.click(screen, screen.width / 2.0 + 124.0, screen.height / 2.0 - 92.0);
    }

    @Override
    public int visibleDestructionLeft() {
        return destructionSlider(-145, -7).getValueInt();
    }

    @Override
    public int visibleDestructionDepth() {
        return destructionSlider(-35, -7).getValueInt();
    }

    @Override
    public void clickDestructionLeftPlus() {
        clickSliderPlus(destructionScreen(), destructionSlider(-145, -7));
    }

    @Override
    public void clickDestructionDepthPlus() {
        clickSliderPlus(destructionScreen(), destructionSlider(-35, -7));
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

    private Screen copyScreen() {
        Screen screen = minecraft().screen;
        if (!(screen instanceof CopyGUI)) throw new IllegalStateException("copy coordinate screen missing");
        return screen;
    }

    private Screen destructionScreen() {
        Screen screen = minecraft().screen;
        if (!(screen instanceof DestructionGUI)) throw new IllegalStateException("destruction screen missing");
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

    private GuiIncrementer copyStartX() {
        Screen screen = copyScreen();
        GuiIncrementer result = null;
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof GuiIncrementer)) continue;
            GuiIncrementer current = (GuiIncrementer) child;
            if (result == null || current.getY() < result.getY()
                    || (current.getY() == result.getY() && current.getX() < result.getX())) {
                result = current;
            }
        }
        if (result == null) throw new IllegalStateException("copy Start X incrementer missing");
        return result;
    }

    private IncrementalSliderWidget destructionSlider(int relativeX, int relativeY) {
        Screen screen = destructionScreen();
        int expectedX = screen.width / 2 + relativeX;
        int expectedY = screen.height / 2 + relativeY;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof IncrementalSliderWidget) {
                IncrementalSliderWidget slider = (IncrementalSliderWidget) child;
                if (slider.getX() == expectedX && slider.getY() == expectedY) return slider;
            }
        }
        throw new IllegalStateException("destruction slider missing at " + expectedX + "," + expectedY);
    }

    private void clickSliderPlus(Screen screen, IncrementalSliderWidget slider) {
        clicker.click(screen,
                slider.getX() + slider.getWidth() + 5.0 + slider.getHeight() / 2.0,
                slider.getY() + slider.getHeight() / 2.0);
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
