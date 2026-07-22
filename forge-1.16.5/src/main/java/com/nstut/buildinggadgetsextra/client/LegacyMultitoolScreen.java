package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
import com.direwolf20.buildinggadgets.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets.client.screen.MaterialListGUI;
import com.direwolf20.buildinggadgets.common.items.*;
import com.direwolf20.buildinggadgets.common.items.modes.BuildingModes;
import com.direwolf20.buildinggadgets.common.items.modes.ExchangingModes;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMenuState;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.RadialButtonPolicy;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.mixin.PasteGUIInvoker;
import com.nstut.buildinggadgetsextra.network.CutSelectionPacket;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.LegacyMultitoolPacket;
import com.nstut.buildinggadgetsextra.network.MirrorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class LegacyMultitoolScreen extends Screen {
    private static final int TOOL_RADIUS = 68, ACTION_RADIUS = 82, BACK_RADIUS = 28;
    private static final MultitoolMode[] LEGACY_TOOLS = {
            MultitoolMode.BUILD, MultitoolMode.EXCHANGING,
            MultitoolMode.COPY_PASTE, MultitoolMode.CUT_PASTE, MultitoolMode.DESTRUCTION
    };
    private static MultitoolMenuState.Page rememberedPage = MultitoolMenuState.Page.GENERAL;
    private ItemStack stack;
    private final MultitoolMenuState navigation;
    private int selectedAction;
    private MultitoolMode hoveredTool;
    private int hoveredAction = -1;
    private boolean hoveredBack;
    private int rangeLabelY = -1;

    public LegacyMultitoolScreen(ItemStack stack) {
        super(new TranslationTextComponent("buildinggadgetsextra.multitool.menu"));
        this.stack = stack;
        this.navigation = new MultitoolMenuState(MultitoolState.getActiveMode(stack));
        if (rememberedPage == MultitoolMenuState.Page.SUBMENU) navigation.openTool(MultitoolState.getActiveMode(stack));
        selectedAction = MultitoolState.getProfile(stack, selectedTool());
    }

    @Override protected void init() { rebuildSettings(); }

    @Override public void tick() {
        if (minecraft.player == null) return;
        ItemStack held = AbstractGadget.getGadget(minecraft.player);
        if (!held.isEmpty()) this.stack = held;
    }

    private void rebuildSettings() {
        buttons.clear(); children.clear(); rangeLabelY = -1;
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return;
        int[] left = {height / 2 - 104}, right = {height / 2 - 104};
        MultitoolMode tool = selectedTool();
        if (tool == MultitoolMode.DESTRUCTION) {
            setting(false, right, "destroy_overlay", "buildinggadgets.radialmenu.destruction_overlay", true,
                    () -> GadgetDestruction.getOverlay(stack), LegacyMultitoolPacket.DESTROY_OVERLAY);
            setting(false, right, "fluid_only", "buildinggadgets.radialmenu.fluid_only", true,
                    () -> GadgetDestruction.getIsFluidOnly(stack), LegacyMultitoolPacket.FLUID_ONLY);
            return;
        }
        setting(false, right, "raytrace_fluid", "buildinggadgets.radialmenu.raytracefluids", true,
                () -> AbstractGadget.shouldRayTraceFluid(stack), LegacyMultitoolPacket.RAYTRACE);
        setting(true, left, "anchor", "buildinggadgets.radialmenu.anchor", false, () -> false, LegacyMultitoolPacket.ANCHOR);
        if (tool != MultitoolMode.EXCHANGING) {
            setting(true, left, "undo", "buildinggadgets.radialmenu.undo", false, () -> false, LegacyMultitoolPacket.UNDO);
        }

        if (tool == MultitoolMode.BUILD || tool == MultitoolMode.EXCHANGING) {
            setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false,
                    () -> false, LegacyMultitoolPacket.ROTATE);
            setting(true, left, "mirror", "buildinggadgets.radialmenu.mirror", false,
                    () -> false, LegacyMultitoolPacket.MIRROR);
            setting(false, right, "fuzzy", "buildinggadgets.radialmenu.fuzzy", true,
                    () -> AbstractGadget.getFuzzy(stack), LegacyMultitoolPacket.FUZZY);
            setting(false, right, "connected_area", "buildinggadgets.radialmenu.connected_surface", true,
                    () -> AbstractGadget.getConnectedArea(stack), LegacyMultitoolPacket.CONNECTED);
            if (tool == MultitoolMode.BUILD) {
                setting(false, right, "building_place_atop", "buildinggadgets.radialmenu.place_atop", true,
                        () -> GadgetBuilding.shouldPlaceAtop(stack), LegacyMultitoolPacket.PLACE_ATOP);
            }
            addRange(right);
        }

        if (tool == MultitoolMode.COPY_PASTE) {
            String mode = selectedAction == 1 ? "paste" : "copy";
            clientSetting(false, right, "copypaste_opengui", "buildinggadgets.radialmenu.copypastemenu",
                    () -> minecraft.setScreen("paste".equals(mode)
                            ? PasteGUIInvoker.buildingGadgetsExtra$create(stack) : new CopyGUI(stack)));
            if ("paste".equals(mode)) {
                clientSetting(false, right, "copypaste_materiallist", "buildinggadgets.radialmenu.materiallist",
                        () -> minecraft.setScreen(new MaterialListGUI(stack)));
            }
            if (RadialButtonPolicy.showRotateButton(mode)) {
                setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false, () -> false, LegacyMultitoolPacket.ROTATE);
            }
            if (RadialButtonPolicy.showMirrorButtons(mode)) {
                addButton(new MirrorIconButton(width / 2 - 136, next(left), "mirror_horizontal",
                        new TranslationTextComponent(ExtraConstants.MIRROR_HORIZONTAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(false))));
                addButton(new MirrorIconButton(width / 2 - 136, next(left), "mirror_vertical",
                        new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
            }
            if (RadialButtonPolicy.showLegacyCutAction(false, mode)) {
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                        new TranslationTextComponent(ExtraConstants.CUT_SELECTION),
                        () -> ExtraNetwork.sendToServer(new CutSelectionPacket())));
            }
            RadialButtonPolicy.FileAction file = RadialButtonPolicy.fileAction(false, mode);
            if (file == RadialButtonPolicy.FileAction.SAVE) {
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "save",
                        new TranslationTextComponent(ExtraConstants.SAVE_STRUCTURE),
                        ClientStructureFiles::chooseSave));
            } else if (file == RadialButtonPolicy.FileAction.LOAD) {
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "load",
                        new TranslationTextComponent(ExtraConstants.LOAD_STRUCTURE),
                        ClientStructureFiles::chooseLoad));
            }
        }

        if (tool == MultitoolMode.CUT_PASTE) {
            String mode = selectedAction == 1 ? "paste" : "cut";
            clientSetting(false, right, "copypaste_opengui", "buildinggadgets.radialmenu.copypastemenu",
                    () -> minecraft.setScreen("paste".equals(mode)
                            ? PasteGUIInvoker.buildingGadgetsExtra$create(stack) : new CopyGUI(stack)));
            if ("paste".equals(mode)) {
                clientSetting(false, right, "copypaste_materiallist", "buildinggadgets.radialmenu.materiallist",
                        () -> minecraft.setScreen(new MaterialListGUI(stack)));
            }
            if ("cut".equals(mode)) {
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                        new TranslationTextComponent(ExtraConstants.CUT_SELECTION),
                        () -> ExtraNetwork.sendToServer(new CutSelectionPacket())));
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "save",
                        new TranslationTextComponent(ExtraConstants.SAVE_STRUCTURE),
                        ClientStructureFiles::chooseSave));
            }
            if ("paste".equals(mode)) {
                setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false, () -> false, LegacyMultitoolPacket.ROTATE);
                addButton(new MirrorIconButton(width / 2 - 136, next(left), "mirror_horizontal",
                        new TranslationTextComponent(ExtraConstants.MIRROR_HORIZONTAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(false))));
                addButton(new MirrorIconButton(width / 2 - 136, next(left), "mirror_vertical",
                        new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
                addButton(new MirrorIconButton(width / 2 + 112, next(right), "load",
                        new TranslationTextComponent(ExtraConstants.LOAD_STRUCTURE),
                        ClientStructureFiles::chooseLoad));
            }
        }
    }

    private void setting(boolean leftSide, int[] y, String icon, String translation, boolean selectable,
                         BooleanSupplier selected, int operation) {
        int x = leftSide ? width / 2 - 136 : width / 2 + 112;
        addButton(new GuiIconActionable(x, next(y), icon, new TranslationTextComponent(translation), selectable, send -> {
            if (send) ExtraNetwork.sendToServer(new LegacyMultitoolPacket(operation, 0));
            return selected.getAsBoolean();
        }));
    }

    private void clientSetting(boolean leftSide, int[] y, String icon, String translation, Runnable action) {
        int x = leftSide ? width / 2 - 136 : width / 2 + 112;
        addButton(new GuiIconActionable(x, next(y), icon, new TranslationTextComponent(translation), false, send -> {
            if (send) action.run();
            return false;
        }));
    }

    private void addRange(int[] right) {
        int y = next(right); rangeLabelY = y + 6;
        addButton(new Button(width / 2 + 112, y, 18, 20, new StringTextComponent("-"),
                b -> ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.RANGE, -1))));
        addButton(new Button(width / 2 + 176, y, 18, 20, new StringTextComponent("+"),
                b -> ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.RANGE, 1))));
    }

    private static int next(int[] value) { int result = value[0]; value[0] += 30; return result; }

    @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2, cy = height / 2;
        double dx = mouseX - cx, dy = mouseY - cy;
        if (navigation.page() == MultitoolMenuState.Page.GENERAL) {
            hoveredTool = findTool(dx, dy); hoveredAction = -1; hoveredBack = false;
            fill(matrices, cx - 34, cy - 22, cx + 34, cy + 22, 0xAA20252A);
            drawCenteredString(matrices, font, title.getString(), cx, cy - 4, 0xFFFFFF);
            MultitoolMode[] modes = LEGACY_TOOLS;
            for (int i = 0; i < modes.length; i++) {
                double angle = -Math.PI / 2 + i * Math.PI * 2 / modes.length;
                drawTool(matrices, modes[i], cx + (int)(Math.cos(angle) * TOOL_RADIUS), cy + (int)(Math.sin(angle) * TOOL_RADIUS), modes[i] == hoveredTool);
            }
            if (hoveredTool != null) drawCenteredString(matrices, font, new TranslationTextComponent(hoveredTool.translationKey()).getString(), cx, cy + 31, hoveredTool.accentColor());
        } else {
            List<LegacyAction> actions = actions(selectedTool());
            hoveredAction = findAction(actions.size(), dx, dy);
            hoveredBack = Math.sqrt(dx * dx + dy * dy) <= BACK_RADIUS;
            fill(matrices, cx - BACK_RADIUS, cy - BACK_RADIUS, cx + BACK_RADIUS, cy + BACK_RADIUS, 0xCC20252A);
            outline(matrices, cx - BACK_RADIUS, cy - BACK_RADIUS, cx + BACK_RADIUS, cy + BACK_RADIUS, hoveredBack ? 0xFF3598FF : 0);
            drawCenteredString(matrices, font, new TranslationTextComponent("buildinggadgetsextra.multitool.back").getString(), cx, cy - 4, 0xFFFFFF);
            drawCenteredString(matrices, font, new TranslationTextComponent(selectedTool().translationKey()).getString(), cx, cy - 122, selectedTool().accentColor());
            for (int i = 0; i < actions.size(); i++) {
                double angle = -Math.PI / 2 + i * Math.PI * 2 / actions.size();
                drawAction(matrices, actions.get(i), cx + (int)(Math.cos(angle) * ACTION_RADIUS), cy + (int)(Math.sin(angle) * ACTION_RADIUS), i == selectedAction, i == hoveredAction);
            }
            if (!actions.isEmpty()) {
                int label = hoveredAction >= 0 ? hoveredAction : Math.min(selectedAction, actions.size() - 1);
                drawCenteredString(matrices, font, new TranslationTextComponent(actions.get(label).translation).getString(), cx, cy + 34, selectedTool().accentColor());
            }
        }
        if (rangeLabelY >= 0) drawCenteredString(matrices, font, "Range: " + GadgetUtils.getToolRange(stack), width / 2 + 153, rangeLabelY, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, partialTick);
    }

    private void drawTool(MatrixStack matrices, MultitoolMode mode, int x, int y, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        fill(matrices, x-half, y-half, x+half, y+half, 0xAA394149);
        outline(matrices, x-half, y-half, x+half, y+half, hovered ? 0xFF3598FF : mode == selectedTool() ? 0xFF00E640 : 0);
        String namespace; String path; int source; int renderSize;
        if (mode == MultitoolMode.BUILD) { namespace = ExtraConstants.MOD_ID; path = "textures/gui/setting/build.png"; source = 44; renderSize = 24; }
        else if (mode == MultitoolMode.EXCHANGING) { namespace = ExtraConstants.MOD_ID; path = "textures/gui/setting/exchange.png"; source = 44; renderSize = 24; }
        else if (mode == MultitoolMode.DESTRUCTION) { namespace = ExtraConstants.MOD_ID; path = "textures/gui/setting/delete.png"; source = 44; renderSize = 24; }
        else if (mode == MultitoolMode.CUT_PASTE) { namespace = ExtraConstants.MOD_ID; path = "textures/gui/setting/cut.png"; source = 15; renderSize = 16; }
        else { namespace = "buildinggadgets"; path = "textures/gui/mode/copy.png"; source = 15; renderSize = 16; }
        minecraft.getTextureManager().bind(new ResourceLocation(namespace, path));
        blit(matrices, x - renderSize / 2, y - renderSize / 2, renderSize, renderSize, 0, 0, source, source, source, source);
    }

    private static final int BG1_ACTION_ICON_SIZE = 16;

    private void drawAction(MatrixStack matrices, LegacyAction action, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        fill(matrices, x-half, y-half, x+half, y+half, 0xAA394149);
        outline(matrices, x-half, y-half, x+half, y+half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        minecraft.getTextureManager().bind(new ResourceLocation("buildinggadgets", action.icon));
        blit(matrices, x - BG1_ACTION_ICON_SIZE / 2, y - BG1_ACTION_ICON_SIZE / 2,
                BG1_ACTION_ICON_SIZE, BG1_ACTION_ICON_SIZE,
                0, 0,
                15, 15, 15, 15);
    }

    private static void outline(MatrixStack matrices, int l, int t, int r, int b, int color) {
        if (color == 0) return;
        fill(matrices,l,t,r,t+2,color); fill(matrices,l,b-2,r,b,color); fill(matrices,l,t+2,l+2,b-2,color); fill(matrices,r-2,t+2,r,b-2,color);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        double dx = mouseX-width/2.0, dy=mouseY-height/2.0;
        if (navigation.page() == MultitoolMenuState.Page.GENERAL) {
            MultitoolMode mode = findTool(dx,dy); if (mode == null) return false;
            navigation.openTool(mode); rememberedPage=MultitoolMenuState.Page.SUBMENU;
            selectedAction=MultitoolState.getProfile(stack,mode); MultitoolState.selectTool(stack,mode);
            ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_TOOL,mode.ordinal())); rebuildSettings(); return true;
        }
        if (Math.sqrt(dx*dx+dy*dy)<=BACK_RADIUS) { navigation.back(); rememberedPage=MultitoolMenuState.Page.GENERAL; rebuildSettings(); return true; }
        List<LegacyAction> actions=actions(selectedTool()); int action=findAction(actions.size(),dx,dy); if(action<0)return false;
        selectedAction=action; MultitoolState.applyProfile(stack,selectedTool(),action);
        ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_ACTION,action)); rebuildSettings(); return true;
    }

    private MultitoolMode findTool(double dx,double dy){double d=Math.sqrt(dx*dx+dy*dy);if(d<42||d>94)return null;int n=LEGACY_TOOLS.length;double s=Math.PI*2/n;int i=(int)Math.floor((angle(dx,dy)+s/2)/s)%n;return LEGACY_TOOLS[i];}
    private int findAction(int n,double dx,double dy){double d=Math.sqrt(dx*dx+dy*dy);if(n==0||d<54||d>108)return-1;double s=Math.PI*2/n;return(int)Math.floor((angle(dx,dy)+s/2)/s)%n;}
    private static double angle(double dx,double dy){double a=Math.atan2(dy,dx)+Math.PI/2;return a<0?a+Math.PI*2:a;}
    private MultitoolMode selectedTool(){return navigation.selectedTool();}

    private static List<LegacyAction> actions(MultitoolMode tool) {
        List<LegacyAction> result=new ArrayList<>();
        if(tool==MultitoolMode.BUILD)for(BuildingModes m:BuildingModes.values())result.add(new LegacyAction(m.getTranslationKey(),m.getIcon()));
        else if(tool==MultitoolMode.EXCHANGING)for(ExchangingModes m:ExchangingModes.values())result.add(new LegacyAction(m.getTranslationKey(),m.getIcon()));
        else if(tool==MultitoolMode.COPY_PASTE){result.add(new LegacyAction("buildinggadgets.modes.copy","textures/gui/mode/copy.png"));result.add(new LegacyAction("buildinggadgets.modes.paste","textures/gui/mode/paste.png"));}
        else if(tool==MultitoolMode.CUT_PASTE){result.add(new LegacyAction("buildinggadgetsextra.multitool.action.cut","textures/gui/mode/copy.png"));result.add(new LegacyAction("buildinggadgets.modes.paste","textures/gui/mode/paste.png"));}
        return result;
    }
    private static final class LegacyAction { final String translation,icon; LegacyAction(String translation,String icon){this.translation=translation;this.icon=icon;} }
}
