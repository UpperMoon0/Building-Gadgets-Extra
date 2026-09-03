package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1201ClientRangeIntegrationTest {
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(new Adapter());

    private Forge1201ClientRangeIntegrationTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        SCENARIO.tick();
    }

    private static final class Adapter implements ClientRangeRoundTripScenario.Adapter {
        private Minecraft minecraft() {
            return Minecraft.getInstance();
        }

        private ItemStack held() {
            return minecraft().player == null ? ItemStack.EMPTY : BaseGadget.getGadget(minecraft().player);
        }

        @Override
        public boolean hasReadyMultitool() {
            ItemStack stack = held();
            return stack.getItem() instanceof BuildersMultitool;
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
            screen.mouseClicked(screen.width / 2.0, screen.height / 2.0 - 68.0, 0);
        }

        @Override
        public void clickRangePlus() {
            Screen screen = minecraft().screen;
            if (!(screen instanceof MultitoolRadialScreen)) throw new IllegalStateException("range screen missing");
            // Real + button for the Build-profile IncrementalSliderWidget.
            screen.mouseClicked(screen.width / 2.0 + 206.0, screen.height / 2.0 - 37.0, 0);
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
}
