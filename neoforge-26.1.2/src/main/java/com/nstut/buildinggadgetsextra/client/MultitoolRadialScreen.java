package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets2.client.screen.MaterialListGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.network.data.*;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolMenuState;
import com.nstut.buildinggadgetsextra.common.RadialButtonPolicy;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.MirrorPayload;
import com.nstut.buildinggadgetsextra.network.MultitoolCutPayload;
import com.nstut.buildinggadgetsextra.network.MultitoolSelectionPayload;
import com.nstut.buildinggadgetsextra.mixin.PasteGUIInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MultitoolRadialScreen extends Screen {
    private static final int TOOL_RADIUS = 68;
    private static final int ACTION_RADIUS = 82;
    private static final int BACK_RADIUS = 28;
    private static MultitoolMenuState.Page rememberedPage = MultitoolMenuState.Page.GENERAL;

    private final ItemStack stack;
    private final MultitoolMenuState navigation;
    private BaseMode selectedAction;
    private MultitoolMode hoveredTool;
    private BaseMode hoveredAction;
    private boolean hoveredBack;

    public MultitoolRadialScreen(ItemStack stack) {
        super(Component.translatable("buildinggadgetsextra.multitool.menu"));
        this.stack = stack;
        this.navigation = new MultitoolMenuState(MultitoolState.getActiveMode(stack));
        if (rememberedPage == MultitoolMenuState.Page.SUBMENU) {
            this.navigation.openTool(MultitoolState.getActiveMode(stack));
        }
        this.selectedAction = rememberedAction(selectedTool());
    }

    @Override
    protected void init() {
        rebuildContextButtons();
    }

    private void rebuildContextButtons() {
        clearWidgets();
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return;

        int[] leftY = {height / 2 - 104};
        int[] rightY = {height / 2 - 104};

        if (selectedTool() == MultitoolMode.DESTRUCTION) {
            addUpstream(false, rightY, "copypaste_opengui",
                    Component.translatable("buildinggadgetsextra.multitool.configure_destruction"), false,
                    null, () -> Minecraft.getInstance().setScreen(new DestructionGUI(stack, false)));
            return;
        }

        String mode = selectedAction == null ? "" : selectedAction.getId().getPath();
        boolean cutTool = selectedTool() == MultitoolMode.CUT_PASTE;

        addUpstream(false, rightY, "raytrace_fluid",
                Component.translatable("buildinggadgets2.radialmenu.raytracefluids"), true,
                GadgetNBT.ToggleableSettings.RAYTRACE_FLUID, null);
        addUpstream(true, leftY, "anchor", Component.translatable("buildinggadgets2.radialmenu.anchor"), true,
                null, () -> ClientPacketDistributor.sendToServer(new AnchorPayload()));
        addRenderTypeButton(leftY);

        if (!cutTool) {
            addUpstream(true, leftY, "undo", Component.translatable("buildinggadgets2.radialmenu.undo"), false,
                    null, () -> ClientPacketDistributor.sendToServer(new UndoPayload()));
            addUpstream(true, leftY, "building_place_atop", Component.translatable("buildinggadgets2.radialmenu.bind"), true,
                    GadgetNBT.ToggleableSettings.BIND, null);
        }

        if (selectedTool() == MultitoolMode.BUILD) {
            addUpstream(false, rightY, "building_place_atop", Component.translatable("buildinggadgets2.screen.placeatop"), true,
                    GadgetNBT.ToggleableSettings.PLACE_ON_TOP, null);
            if ("surface".equals(mode)) {
                addUpstream(false, rightY, "fuzzy", Component.translatable("buildinggadgets2.radialmenu.fuzzy"), true,
                        GadgetNBT.ToggleableSettings.FUZZY, null);
                addUpstream(false, rightY, "connected_area", Component.translatable("buildinggadgets2.radialmenu.connected_area"), true,
                        GadgetNBT.ToggleableSettings.CONNECTED_AREA, null);
            }
        }

        if (selectedTool() == MultitoolMode.EXCHANGING) {
            addUpstream(false, rightY, "affecttiles", Component.translatable("buildinggadgets2.screen.affecttiles"), true,
                    GadgetNBT.ToggleableSettings.AFFECT_TILES, null);
            addUpstream(false, rightY, "fuzzy", Component.translatable("buildinggadgets2.radialmenu.fuzzy"), true,
                    GadgetNBT.ToggleableSettings.FUZZY, null);
            addUpstream(false, rightY, "connected_area", Component.translatable("buildinggadgets2.radialmenu.connected_area"), true,
                    GadgetNBT.ToggleableSettings.CONNECTED_AREA, null);
        }

        if (selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING) {
            // Range is a build/exchange setting, so keep it in the right-hand settings rail.
            IncrementalSliderWidget range = new IncrementalSliderWidget(width / 2 + 112, next(rightY),
                    82, 14, 1, 15, Component.translatable("buildinggadgets2.gui.range").append(": "),
                    GadgetNBT.getToolRange(stack), slider ->
                    ClientPacketDistributor.sendToServer(new RangeChangePayload(slider.getValueInt())));
            range.getComponents().forEach(this::addRenderableWidget);
        }

        if (selectedTool() == MultitoolMode.COPY_PASTE || cutTool) {
            addUpstream(false, rightY, "paste_replace", Component.translatable("buildinggadgets2.screen.paste_replace"), true,
                    GadgetNBT.ToggleableSettings.PASTE_REPLACE, null);
            addUpstream(false, rightY, "copypaste_opengui", Component.translatable("buildinggadgets2.radialmenu.copypastemenu"), false,
                    null, () -> Minecraft.getInstance().setScreen("paste".equals(mode)
                            ? PasteGUIInvoker.buildingGadgetsExtra$create(stack) : new CopyGUI(stack)));
            if (RadialButtonPolicy.showRotateButton(mode)) {
                addUpstream(true, leftY, "rotate", Component.translatable("buildinggadgets2.radialmenu.rotate"), false,
                        null, () -> ClientPacketDistributor.sendToServer(new RotatePayload()));
            }
            if (!cutTool && "paste".equals(mode)) {
                addUpstream(false, rightY, "copypaste_materiallist", Component.translatable("buildinggadgets2.radialmenu.materiallist"), false,
                        null, () -> {
                            if (GadgetNBT.hasCopyUUID(stack)) Minecraft.getInstance().setScreen(new MaterialListGUI(stack));
                        });
            }
        }

        if (RadialButtonPolicy.showMirrorButtons(mode)
                && (selectedTool() == MultitoolMode.COPY_PASTE || selectedTool() == MultitoolMode.CUT_PASTE)) {
            addRenderableWidget(new MirrorIconButton(width / 2 - 136, next(leftY), "mirror_horizontal",
                    Component.translatable(ExtraConstants.MIRROR_HORIZONTAL),
                    () -> ClientPacketDistributor.sendToServer(new MirrorPayload(false))));
            addRenderableWidget(new MirrorIconButton(width / 2 - 136, next(leftY), "mirror_vertical",
                    Component.translatable(ExtraConstants.MIRROR_VERTICAL),
                    () -> ClientPacketDistributor.sendToServer(new MirrorPayload(true))));
        }

        RadialButtonPolicy.FileAction fileAction = RadialButtonPolicy.fileAction(
                selectedTool() == MultitoolMode.CUT_PASTE, mode);
        if (fileAction != RadialButtonPolicy.FileAction.NONE) {
            boolean load = fileAction == RadialButtonPolicy.FileAction.LOAD;
            addRenderableWidget(new MirrorIconButton(width / 2 + 112, next(rightY), load ? "load" : "save",
                    Component.translatable(load ? ExtraConstants.LOAD_STRUCTURE : ExtraConstants.SAVE_STRUCTURE),
                    load ? ClientStructureFiles::chooseLoad : ClientStructureFiles::chooseSave));
        }

        if (selectedTool() == MultitoolMode.CUT_PASTE && "cut".equals(mode)) {
            addRenderableWidget(new MirrorIconButton(width / 2 + 112, next(rightY),
                    "cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                    Component.translatable("buildinggadgets2.radialmenu.cut"),
                    () -> ClientPacketDistributor.sendToServer(MultitoolCutPayload.INSTANCE)));
        }
    }

    private void addUpstream(boolean left, int[] y, String icon, Component tooltip, boolean selectable,
                             GadgetNBT.ToggleableSettings setting, Runnable action) {
        int x = left ? width / 2 - 136 : width / 2 + 112;
        addRenderableWidget(new GuiIconActionable(x, next(y), icon, tooltip, selectable, send -> {
            if (send) {
                if (setting != null) ClientPacketDistributor.sendToServer(new ToggleSettingPayload(setting.getName()));
                if (action != null) action.run();
            }
            if (setting != null) return GadgetNBT.getSetting(stack, setting.getName());
            if ("anchor".equals(icon)) return !GadgetNBT.getAnchorPos(stack).equals(GadgetNBT.nullPos);
            return false;
        }));
    }

    private void addRenderTypeButton(int[] y) {
        GadgetNBT.RenderTypes[] current = {GadgetNBT.getRenderType(stack)};
        GuiIconActionable[] holder = new GuiIconActionable[1];
        holder[0] = new GuiIconActionable(width / 2 - 136, next(y), "raytrace_fluid",
                Component.translatable(current[0].getLang()), false, send -> {
            if (send) {
                current[0] = current[0].next();
                holder[0].setMessage(Component.translatable(current[0].getLang()));
                ClientPacketDistributor.sendToServer(new RenderChangePayload(current[0].getPosition()));
            }
            return false;
        });
        addRenderableWidget(holder[0]);
    }

    private static int next(int[] y) {
        int result = y[0];
        y[0] += 30;
        return result;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Radial menus are sharp overlays, matching BG2's ModeRadialMenu.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cx = width / 2;
        int cy = height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;

        if (navigation.page() == MultitoolMenuState.Page.GENERAL) {
            hoveredTool = findTool(dx, dy);
            hoveredAction = null;
            hoveredBack = false;
            renderGeneral(graphics, cx, cy);
        } else {
            hoveredTool = null;
            List<BaseMode> actions = actionsFor(selectedTool());
            hoveredAction = findAction(actions, dx, dy);
            hoveredBack = Math.sqrt(dx * dx + dy * dy) <= BACK_RADIUS;
            renderSubmenu(graphics, cx, cy, actions);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGeneral(GuiGraphicsExtractor graphics, int cx, int cy) {
        graphics.fill(cx - 34, cy - 22, cx + 34, cy + 22, 0xAA20252A);
        graphics.centeredText(font, title, cx, cy - 4, 0xFFFFFF);

        MultitoolMode[] tools = MultitoolMode.values();
        for (int i = 0; i < tools.length; i++) {
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / tools.length);
            int x = cx + (int) (Math.cos(angle) * TOOL_RADIUS);
            int y = cy + (int) (Math.sin(angle) * TOOL_RADIUS);
            renderToolButton(graphics, tools[i], x, y, tools[i] == selectedTool(), tools[i] == hoveredTool);
        }

        if (hoveredTool != null) {
            graphics.centeredText(font, Component.translatable(hoveredTool.translationKey()),
                    cx, cy + 31, hoveredTool.accentColor());
        }
    }

    private void renderSubmenu(GuiGraphicsExtractor graphics, int cx, int cy, List<BaseMode> actions) {
        graphics.centeredText(font, Component.translatable(selectedTool().translationKey()),
                cx, cy - 122, selectedTool().accentColor());
        graphics.fill(cx - BACK_RADIUS, cy - BACK_RADIUS, cx + BACK_RADIUS, cy + BACK_RADIUS, 0xCC20252A);
        drawOutline(graphics, cx - BACK_RADIUS, cy - BACK_RADIUS, cx + BACK_RADIUS, cy + BACK_RADIUS,
                hoveredBack ? 0xFF3598FF : 0);
        graphics.centeredText(font, Component.translatable("buildinggadgetsextra.multitool.back"),
                cx, cy - 4, 0xFFFFFF);

        for (int i = 0; i < actions.size(); i++) {
            BaseMode action = actions.get(i);
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / actions.size());
            int x = cx + (int) (Math.cos(angle) * ACTION_RADIUS);
            int y = cy + (int) (Math.sin(angle) * ACTION_RADIUS);
            renderActionButton(graphics, action, x, y, action.equals(selectedAction), action == hoveredAction);
        }

        BaseMode label = hoveredAction == null ? selectedAction : hoveredAction;
        if (label != null) {
            graphics.centeredText(font, Component.translatable(label.i18n()),
                    cx, cy + 34, selectedTool().accentColor());
        }
    }

    private void renderToolButton(GuiGraphicsExtractor graphics, MultitoolMode tool, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        int iconSize = RadialIconLayout.MULTITOOL_MODE_ICON_SIZE;
        graphics.fill(x - half, y - half, x + half, y + half, 0xAA394149);
        drawOutline(graphics, x - half, y - half, x + half, y + half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        Identifier icon = Identifier.fromNamespaceAndPath(actionIconNamespace(tool), actionIconPath(tool));
        int sourceSize = actionIconSourceSize(tool);
        // Explicit source rectangle prevents the 15x15 action textures from tiling.
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x - iconSize / 2, y - iconSize / 2,
                0, 0, iconSize, iconSize, sourceSize, sourceSize, sourceSize, sourceSize, 0xFFFFFFFF);
    }

    private void renderActionButton(GuiGraphicsExtractor graphics, BaseMode action, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        int iconSize = RadialIconLayout.MULTITOOL_MODE_ICON_SIZE;
        graphics.fill(x - half, y - half, x + half, y + half, 0xAA394149);
        drawOutline(graphics, x - half, y - half, x + half, y + half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        Identifier icon = Identifier.fromNamespaceAndPath("buildinggadgets2",
                "textures/gui/mode/" + action.getId().getPath() + ".png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x - iconSize / 2, y - iconSize / 2,
                0, 0, iconSize, iconSize,
                RadialIconLayout.MODERN_SETTING_ICON_SIZE, RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                RadialIconLayout.MODERN_SETTING_ICON_SIZE, RadialIconLayout.MODERN_SETTING_ICON_SIZE, 0xFFFFFFFF);
    }

    private static void drawOutline(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        if (color == 0) return;
        graphics.fill(left, top, right, top + 2, color);
        graphics.fill(left, bottom - 2, right, bottom, color);
        graphics.fill(left, top + 2, left + 2, bottom - 2, color);
        graphics.fill(right - 2, top + 2, right, bottom - 2, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mouseX = event.x();
        double mouseY = event.y();
        double dx = mouseX - width / 2.0;
        double dy = mouseY - height / 2.0;

        if (navigation.page() == MultitoolMenuState.Page.GENERAL) {
            MultitoolMode tool = findTool(dx, dy);
            if (tool == null) return false;
            navigation.openTool(tool);
            rememberedPage = MultitoolMenuState.Page.SUBMENU;
            selectedAction = rememberedAction(tool);
            sendSelection();
            rebuildContextButtons();
            return true;
        }

        if (Math.sqrt(dx * dx + dy * dy) <= BACK_RADIUS) {
            navigation.back();
            rememberedPage = MultitoolMenuState.Page.GENERAL;
            rebuildContextButtons();
            return true;
        }

        BaseMode action = findAction(actionsFor(selectedTool()), dx, dy);
        if (action == null) return false;
        selectedAction = action;
        sendSelection();
        rebuildContextButtons();
        return true;
    }

    private void sendSelection() {
        ClientPacketDistributor.sendToServer(new MultitoolSelectionPayload(selectedTool().ordinal(),
                selectedAction == null ? "" : selectedAction.getId().toString()));
    }

    private MultitoolMode findTool(double dx, double dy) {
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 42 || distance > 94) return null;
        double angle = normalizedAngle(dx, dy);
        int count = MultitoolMode.values().length;
        double slice = Math.PI * 2 / count;
        int index = (int) Math.floor((angle + slice / 2) / slice) % count;
        return MultitoolMode.values()[index];
    }

    private BaseMode findAction(List<BaseMode> actions, double dx, double dy) {
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 54 || distance > 108 || actions.isEmpty()) return null;
        double angle = normalizedAngle(dx, dy);
        int index = (int) Math.floor((angle + Math.PI / actions.size())
                / (Math.PI * 2 / actions.size())) % actions.size();
        return actions.get(index);
    }

    private static double normalizedAngle(double dx, double dy) {
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        return angle < 0 ? angle + Math.PI * 2 : angle;
    }

    private BaseMode rememberedAction(MultitoolMode tool) {
        List<BaseMode> actions = actionsFor(tool);
        if (actions.isEmpty()) return null;
        Identifier saved = MultitoolState.getProfileMode(stack, tool);
        if (tool == MultitoolState.getActiveMode(stack) && tool != MultitoolMode.DESTRUCTION) {
            saved = GadgetNBT.getMode(stack).getId();
        }
        Identifier wanted = saved;
        return actions.stream().filter(action -> wanted != null && action.getId().equals(wanted))
                .findFirst().orElse(actions.get(0));
    }

    private static List<BaseMode> actionsFor(MultitoolMode tool) {
        if (tool == MultitoolMode.DESTRUCTION) return List.of();
        return new ArrayList<>(GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(tool)));
    }

    private MultitoolMode selectedTool() {
        return navigation.selectedTool();
    }

    private static String actionIconPath(MultitoolMode tool) {
        return switch (tool) {
            case BUILD -> "textures/gui/setting/build.png";
            case EXCHANGING -> "textures/item/gadget_exchanging.png";
            case COPY_PASTE -> "textures/gui/mode/copy.png";
            case CUT_PASTE -> "textures/gui/mode/cut.png";
            case DESTRUCTION -> "textures/gui/setting/delete.png";
        };
    }

    private static String actionIconNamespace(MultitoolMode tool) {
        return switch (tool) {
            case BUILD, DESTRUCTION -> ExtraConstants.MOD_ID;
            case EXCHANGING, COPY_PASTE, CUT_PASTE -> "buildinggadgets2";
        };
    }

    private static int actionIconSourceSize(MultitoolMode tool) {
        return switch (tool) {
            case BUILD, DESTRUCTION -> RadialIconLayout.SOURCE_TEXTURE_SIZE;
            case EXCHANGING -> 16;
            case COPY_PASTE, CUT_PASTE -> RadialIconLayout.MODERN_SETTING_ICON_SIZE;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}



