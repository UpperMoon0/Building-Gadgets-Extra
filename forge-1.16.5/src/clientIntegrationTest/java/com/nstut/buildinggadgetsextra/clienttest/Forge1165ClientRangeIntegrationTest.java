package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets.client.screen.components.GuiSliderInt;
import net.minecraft.client.gui.widget.button.Button;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.nstut.buildinggadgetsextra.client.LegacyMultitoolScreen;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod.EventBusSubscriber(modid = ExtraConstants.MOD_ID, value = Dist.CLIENT)
public final class Forge1165ClientRangeIntegrationTest {
    private static final boolean ENABLED = Boolean.getBoolean(ClientRangeRoundTripScenario.ENABLE_PROPERTY);
    private static final ClientRangeRoundTripScenario SCENARIO = new ClientRangeRoundTripScenario(new Adapter());

    private Forge1165ClientRangeIntegrationTest() {}

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
            return minecraft().player == null ? ItemStack.EMPTY : AbstractGadget.getGadget(minecraft().player);
        }

        @Override
        public boolean hasReadyMultitool() {
            return held().getItem() instanceof BuildersMultitool;
        }

        @Override
        public int clientRange() {
            return GadgetUtils.getToolRange(held());
        }

        @Override
        public void openMultitoolScreen() {
            minecraft().setScreen(new LegacyMultitoolScreen(held()));
        }

        @Override
        public boolean isMultitoolScreenOpen() {
            return minecraft().screen instanceof LegacyMultitoolScreen;
        }

        @Override
        public int visibleScreenRange() {
            if (!(minecraft().screen instanceof LegacyMultitoolScreen)) return -1;
            try {
                Field stack = LegacyMultitoolScreen.class.getDeclaredField("stack");
                stack.setAccessible(true);
                return GadgetUtils.getToolRange((ItemStack) stack.get(minecraft().screen));
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("cannot read legacy screen stack", error);
            }
        }

        @Override
        public void enterBuildSubmenu() {
            Screen screen = minecraft().screen;
            if (!(screen instanceof LegacyMultitoolScreen)) throw new IllegalStateException("legacy range screen missing");
            screen.mouseClicked(screen.width / 2.0, screen.height / 2.0 - 68.0, 0);
        }

        @Override
        public void clickRangePlus() {
            Screen screen = minecraft().screen;
            if (!(screen instanceof LegacyMultitoolScreen)) throw new IllegalStateException("legacy range screen missing");
            // Real + Button created by LegacyMultitoolScreen.addRange for the Build profile.
            screen.mouseClicked(screen.width / 2.0 + 185.0, screen.height / 2.0 + 26.0, 0);
        }

        @Override
        public void closeScreen() {
            minecraft().setScreen(null);
        }

        @Override
        public void pass(String detail) {
            try {
                verifyDestructionSlider(held(), 17);
                verifyDestructionSlider(new ItemStack(OurItems.DESTRUCTION_GADGET_ITEM.get()), 16);
                finish("client-pass.txt", detail + "; destruction slider/plus exceeds 16 only for multitool", null, 0);
            } catch (Throwable error) {
                finish("client-fail.txt", "legacy destruction range regression", error, 1);
            }
        }

        private void verifyDestructionSlider(ItemStack stack, int expected) throws ReflectiveOperationException {
            DestructionGUI screen = new DestructionGUI(stack);
            minecraft().setScreen(screen);
            Field field = DestructionGUI.class.getDeclaredField("depth");
            field.setAccessible(true);
            GuiSliderInt depth = (GuiSliderInt) field.get(screen);
            depth.setValue(16);
            for (Button button : depth.getComponents()) {
                if (button.getMessage().getString().equals("+")) button.onPress();
            }
            if (depth.getValueInt() != expected) {
                throw new IllegalStateException("destruction depth expected " + expected + " got " + depth.getValueInt());
            }
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
