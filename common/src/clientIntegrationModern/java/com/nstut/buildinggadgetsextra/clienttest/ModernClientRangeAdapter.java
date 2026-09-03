package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
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
        Screen screen = minecraft().screen;
        if (!(screen instanceof MultitoolRadialScreen)) throw new IllegalStateException("range screen missing");
        clicker.click(screen, screen.width / 2.0, screen.height / 2.0 - 68.0);
    }

    @Override
    public void clickRangePlus() {
        Screen screen = minecraft().screen;
        if (!(screen instanceof MultitoolRadialScreen)) throw new IllegalStateException("range screen missing");
        // Actual + component created by IncrementalSliderWidget.getComponents() for the Build range rail.
        clicker.click(screen, screen.width / 2.0 + 206.0, screen.height / 2.0 - 37.0);
    }

    @Override
    public void closeScreen() {
        minecraft().setScreen(null);
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
