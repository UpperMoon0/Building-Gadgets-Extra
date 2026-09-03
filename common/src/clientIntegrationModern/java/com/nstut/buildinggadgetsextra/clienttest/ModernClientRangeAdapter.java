package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Shared client driver for BG2-based ports (1.20.1+). */
public final class ModernClientRangeAdapter implements ClientRangeRoundTripScenario.Adapter {
    @FunctionalInterface
    public interface ScreenClicker {
        void click(Screen screen, double x, double y);
    }

    private final ScreenClicker clicker;

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
        return held().getItem() instanceof BuildersMultitool;
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
        Screen screen = minecraft().screen;
        if (!(screen instanceof MultitoolRadialScreen)) return -1;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof IncrementalSliderWidget) {
                return ((IncrementalSliderWidget) child).getValueInt();
            }
        }
        return -1;
    }

    @Override
    public void enterBuildSubmenu() {
        Screen screen = radialScreen();
        clicker.click(screen, screen.width / 2.0, screen.height / 2.0 - 68.0);
    }

    @Override
    public void clickRangePlus() {
        Screen screen = radialScreen();
        // Actual + component created by IncrementalSliderWidget.getComponents() for the range rail.
        clicker.click(screen, screen.width / 2.0 + 206.0, screen.height / 2.0 - 37.0);
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
        clickSliderPlus(destructionSlider(-145, -7));
    }

    @Override
    public void clickDestructionDepthPlus() {
        clickSliderPlus(destructionSlider(-35, -7));
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
        clicker.click(screen, screen.width / 2.0, screen.height / 2.0);
        double angle = -Math.PI / 2.0 + tool.ordinal() * (Math.PI * 2.0 / MultitoolMode.values().length);
        clicker.click(screen,
                screen.width / 2.0 + Math.cos(angle) * 68.0,
                screen.height / 2.0 + Math.sin(angle) * 68.0);
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

    private void clickSliderPlus(IncrementalSliderWidget slider) {
        Screen screen = destructionScreen();
        clicker.click(screen,
                slider.getX() + slider.getWidth() + 5.0 + slider.getHeight() / 2.0,
                slider.getY() + slider.getHeight() / 2.0);
    }

    @Override
    public void pass(String detail) {
        finish("client-pass.txt", detail, null, 0);
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
