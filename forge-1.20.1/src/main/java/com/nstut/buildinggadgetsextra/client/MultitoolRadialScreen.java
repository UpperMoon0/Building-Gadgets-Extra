package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.DestructionGUI;
import com.direwolf20.buildinggadgets2.client.screen.MaterialListGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.network.PacketHandler;
import com.direwolf20.buildinggadgets2.common.network.packets.*;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolMenuState;
import com.nstut.buildinggadgetsextra.common.RadialButtonPolicy;
import com.nstut.buildinggadgetsextra.common.RadialIconLayout;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.network.MirrorPacket;
import com.nstut.buildinggadgetsextra.network.MultitoolCutPacket;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.MultitoolSelectionPacket;
import com.direwolf20.buildinggadgets2.client.screen.PasteGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Constructor;

public final class MultitoolRadialScreen extends com.nstut.openui.minecraft.UiScreen {
    private static final int TOOL_RADIUS = 68;
    private static final int ACTION_RADIUS = 82;
    private static final int BACK_RADIUS = 28;
    private static MultitoolMenuState.Page rememberedPage = MultitoolMenuState.Page.GENERAL;

    private final ItemStack stack;
    private com.nstut.buildinggadgetsextra.common.ProfileSettingsLayout settingsLayout;
    private InlineModifiers inlineModifiers;
    private final com.nstut.buildinggadgetsextra.client.ui.ProfileControls profileControls=new com.nstut.buildinggadgetsextra.client.ui.ProfileControls();
    private com.nstut.openui.api.UIComponent profileRange;

    public int menuCenterX() { return settingsLayout == null ? width / 2 : settingsLayout.centerX; }
    public int menuCenterY() { return height / 2; }
    private int backRadius() { return settingsLayout==null?BACK_RADIUS:settingsLayout.backRadius; }
    private int actionRadius() { return settingsLayout == null ? 82 : settingsLayout.radius; }
    public InlineModifiers inlineModifiers() { return inlineModifiers; }
    public void openActiveProfile() {
        navigation.openTool(MultitoolState.getActiveMode(stack));
        rebuildContextButtons();
    }
    private ItemStack authoritativeStack() {
        return minecraft.player == null ? stack : com.direwolf20.buildinggadgets2.common.items.BaseGadget.getGadget(minecraft.player);
    }
    private void addContext(net.minecraft.client.gui.components.AbstractWidget widget) {
        if(widget!=null) addRenderableWidget(widget);
    }
    private void closeNow() { super.onClose(); }
    @Override public void onClose() {
        if(inlineModifiers != null) inlineModifiers.navigate(this::closeNow, true); else closeNow();
    }
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

    @Override public void tick() { super.tick(); if(inlineModifiers != null) inlineModifiers.tick(); profileControls.tick(); }

    @Override
    protected void init() {
        // UiScreen replaces the runtime; do not mount fresh controls into the retiring one.
        populateContextButtons();
        super.init();
    }

    private void rebuildContextButtons() { populateContextButtons(); rebuildUI(); }
    private void populateContextButtons() {
        profileControls.clear(); profileRange=null;

        InlineModifiers previous = inlineModifiers;
        clearWidgets();
        inlineModifiers = null; settingsLayout = null;
        if (navigation.page() == MultitoolMenuState.Page.SUBMENU && (selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING)) {
            settingsLayout = new com.nstut.buildinggadgetsextra.common.ProfileSettingsLayout(width,height);
        }
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return;
        // Resize initializes directly while the profile-sync mixin may be awaiting an echo.
        if (MultitoolState.getActiveMode(authoritativeStack()) != selectedTool()) return;

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
        boolean hasCutBuffer = cutTool && GadgetNBT.hasCopyUUID(stack);
        String effectiveMode = cutTool ? (hasCutBuffer ? "paste" : "cut") : mode;

        addUpstream(false, rightY, "raytrace_fluid",
                Component.translatable("buildinggadgets2.radialmenu.raytracefluids"), true,
                "raytracefluid", null);
        addUpstream(true, leftY, "anchor", Component.translatable("buildinggadgets2.radialmenu.anchor"), true,
                null, () -> PacketHandler.sendToServer(new PacketAnchor()));
        addRenderTypeButton(leftY);

        if (!cutTool) {
            addUpstream(true, leftY, "undo", Component.translatable("buildinggadgets2.radialmenu.undo"), false,
                    null, () -> PacketHandler.sendToServer(new PacketUndo()));
            addUpstream(true, leftY, "building_place_atop", Component.translatable("buildinggadgets2.radialmenu.bind"), true,
                    "bind", null);
        }

        if (selectedTool() == MultitoolMode.BUILD) {
            addUpstream(false, rightY, "building_place_atop", Component.translatable("buildinggadgets2.screen.placeatop"), true,
                    "placeontop", null);
            if ("surface".equals(effectiveMode)) {
                addUpstream(false, rightY, "fuzzy", Component.translatable("buildinggadgets2.radialmenu.fuzzy"), true,
                        "fuzzy", null);
                addUpstream(false, rightY, "connected_area", Component.translatable("buildinggadgets2.radialmenu.connected_area"), true,
                        "connected", null);
            }
        }

        if (selectedTool() == MultitoolMode.EXCHANGING) {
            addUpstream(false, rightY, "affecttiles", Component.translatable("buildinggadgets2.screen.affecttiles"), true,
                    "affecttiles", null);
            addUpstream(false, rightY, "fuzzy", Component.translatable("buildinggadgets2.radialmenu.fuzzy"), true,
                    "fuzzy", null);
            addUpstream(false, rightY, "connected_area", Component.translatable("buildinggadgets2.radialmenu.connected_area"), true,
                    "connected", null);
        }

        if (selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING) {
            profileRange=com.nstut.buildinggadgetsextra.client.ui.ProfileControls.range(GadgetNBT.getToolRange(authoritativeStack()),
                    ExtraConfig.multitoolMaxRange(),next->PacketHandler.sendToServer(new PacketRangeChange(next)));
        }

        if (selectedTool() == MultitoolMode.COPY_PASTE || cutTool) {
            addUpstream(false, rightY, "paste_replace", Component.translatable("buildinggadgets2.screen.paste_replace"), true,
                    "pastereplace", null);
            addUpstream(false, rightY, "copypaste_opengui", Component.translatable("buildinggadgets2.radialmenu.copypastemenu"), false,
                    null, () -> Minecraft.getInstance().setScreen("paste".equals(effectiveMode)
                            ? createPasteGUI(stack) : new CopyGUI(stack)));
            if (RadialButtonPolicy.showRotateButton(effectiveMode)) {
                addUpstream(true, leftY, "rotate", Component.translatable("buildinggadgets2.radialmenu.rotate"), false,
                        null, () -> PacketHandler.sendToServer(new PacketRotate()));
            }
            if (!cutTool && "paste".equals(effectiveMode)) {
                addUpstream(false, rightY, "copypaste_materiallist", Component.translatable("buildinggadgets2.radialmenu.materiallist"), false,
                        null, () -> {
                            if (GadgetNBT.hasCopyUUID(stack)) Minecraft.getInstance().setScreen(new MaterialListGUI(stack));
                        });
            }
        }

        boolean liveMirrorTool = selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING;
        boolean templateMirrorTool = RadialButtonPolicy.showMirrorButtons(effectiveMode)
                && (selectedTool() == MultitoolMode.COPY_PASTE || selectedTool() == MultitoolMode.CUT_PASTE);
        if (templateMirrorTool) {
            addRenderableWidget(new MirrorIconButton(menuCenterX() - 136, next(leftY),
                    MirrorIconButton.addonSettingIcon("mirror_horizontal"), RadialIconLayout.SOURCE_TEXTURE_SIZE,
                    Component.translatable(liveMirrorTool
                            ? ExtraConstants.LIVE_MIRROR_HORIZONTAL : ExtraConstants.MIRROR_HORIZONTAL),
                    () -> ExtraNetwork.sendToServer(new MirrorPacket(false))));
            addRenderableWidget(new MirrorIconButton(menuCenterX() - 136, next(leftY),
                    MirrorIconButton.addonSettingIcon("mirror_vertical"), RadialIconLayout.SOURCE_TEXTURE_SIZE,
                    Component.translatable(liveMirrorTool
                            ? ExtraConstants.LIVE_MIRROR_VERTICAL : ExtraConstants.MIRROR_VERTICAL),
                    () -> ExtraNetwork.sendToServer(new MirrorPacket(true))));
        }

        RadialButtonPolicy.FileAction fileAction = RadialButtonPolicy.fileAction(cutTool, effectiveMode);
        if (fileAction != RadialButtonPolicy.FileAction.NONE) {
            boolean load = fileAction == RadialButtonPolicy.FileAction.LOAD;
            ResourceLocation fileIcon = MirrorIconButton.addonSettingIcon(load ? "load" : "save");
            addRenderableWidget(new MirrorIconButton(menuCenterX() + 112, next(rightY), fileIcon,
                    RadialIconLayout.SOURCE_TEXTURE_SIZE,
                    Component.translatable(load ? ExtraConstants.LOAD_STRUCTURE : ExtraConstants.SAVE_STRUCTURE),
                    load ? ClientStructureFiles::chooseLoad : ClientStructureFiles::chooseSave));
        }

        if (cutTool && !hasCutBuffer) {
            addRenderableWidget(new MirrorIconButton(menuCenterX() + 112, next(rightY),
                    MirrorIconButton.upstreamSettingIcon("cut"), RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                    Component.translatable("buildinggadgets2.radialmenu.cut"),
                    () -> ExtraNetwork.sendToServer(new MultitoolCutPacket())));
        }
        if(settingsLayout != null) {
            inlineModifiers = new InlineModifiers(this::authoritativeStack);
            inlineModifiers.restoreFrom(previous);
        }
    }

    private void addUpstream(boolean left, int[] y, String icon, Component tooltip, boolean selectable,
                             String setting, Runnable action) {
        int x = left ? menuCenterX() - 136 : menuCenterX() + 112;
        addContext(contextIcon(x, next(y), icon, tooltip, selectable, send -> {
            if (send) {
                if (setting != null) PacketHandler.sendToServer(new PacketToggleSetting(setting));
                if (action != null) action.run();
            }
            if (setting != null) return GadgetNBT.getSetting(authoritativeStack(), setting);
            if ("anchor".equals(icon)) return !GadgetNBT.getAnchorPos(authoritativeStack()).equals(GadgetNBT.nullPos);
            return false;
        }));
    }

    private void addRenderTypeButton(int[] y) {
        GadgetNBT.RenderTypes[] current = {GadgetNBT.getRenderType(stack)};
        GuiIconActionable[] holder = new GuiIconActionable[1];
        holder[0] = contextIcon(menuCenterX() - 136, next(y), "raytrace_fluid",
                Component.translatable(current[0].getLang()), false, send -> {
            if (send) {
                current[0] = current[0].next();
                if(holder[0]!=null) holder[0].setMessage(Component.translatable(current[0].getLang()));
                PacketHandler.sendToServer(new PacketRenderChange(current[0].getPosition()));
            }
            return false;
        });
        addContext(holder[0]);
    }

    private GuiIconActionable contextIcon(int x,int y,String icon,net.minecraft.network.chat.Component tooltip,boolean selectable,java.util.function.Predicate<Boolean> action) {
        if(settingsLayout!=null) {
            profileControls.icon("buildinggadgets2","textures/gui/setting/"+icon+".png",15,tooltip.getString(),
                    ()->selectable && action.test(false),()->{ if(inlineModifiers!=null) inlineModifiers.navigate(()->action.test(true),false); });
            return null;
        }
        return new GuiIconActionable(x,y,icon,tooltip,selectable,action);
    }
    private static int next(int[] y) {
        int result = y[0];
        y[0] += 30;
        return result;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Radial menus are sharp overlays, matching BG2's ModeRadialMenu.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(settingsLayout!=null && inlineModifiers!=null) { super.render(graphics,mouseX,mouseY,partialTick); return; }
        int cx = menuCenterX();
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
            hoveredBack = Math.sqrt(dx * dx + dy * dy) <= backRadius();
            renderSubmenu(graphics, cx, cy, actions);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.flush();
    }

    private void renderGeneral(GuiGraphics graphics, int cx, int cy) {
        graphics.fill(cx - 34, cy - 22, cx + 34, cy + 22, 0xAA20252A);
        graphics.drawCenteredString(font, title, cx, cy - 4, 0xFFFFFF);

        MultitoolMode[] tools = MultitoolMode.values();
        for (int i = 0; i < tools.length; i++) {
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / tools.length);
            int x = cx + (int) (Math.cos(angle) * TOOL_RADIUS);
            int y = cy + (int) (Math.sin(angle) * TOOL_RADIUS);
            renderToolButton(graphics, tools[i], x, y, tools[i] == selectedTool(), tools[i] == hoveredTool);
        }

        if (hoveredTool != null) {
            graphics.drawCenteredString(font, Component.translatable(hoveredTool.translationKey()),
                    cx, cy + 31, hoveredTool.accentColor());
        }
    }

    private void renderSubmenu(GuiGraphics graphics, int cx, int cy, List<BaseMode> actions) {
        graphics.drawCenteredString(font, Component.translatable(selectedTool().translationKey()),
                cx, cy - (settingsLayout == null ? 122 : actionRadius()+40), selectedTool().accentColor());
        graphics.fill(cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(), 0xCC20252A);
        drawOutline(graphics, cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(),
                hoveredBack ? 0xFF3598FF : 0);
        graphics.flush();
        graphics.drawCenteredString(font, Component.translatable("buildinggadgetsextra.multitool.back"),
                cx, cy - 4, 0xFFFFFF);

        for (int i = 0; i < actions.size(); i++) {
            BaseMode action = actions.get(i);
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / actions.size());
            int x = cx + (int) (Math.cos(angle) * actionRadius());
            int y = cy + (int) (Math.sin(angle) * actionRadius());
            renderActionButton(graphics, action, x, y, action.equals(selectedAction), action == hoveredAction);
        }

        BaseMode label = hoveredAction == null ? selectedAction : hoveredAction;
        if (label != null) {
            graphics.drawCenteredString(font, Component.translatable(label.i18n()),
                    cx, cy + (settingsLayout != null && actionRadius() < 82 ? actionRadius()+24 : 34), selectedTool().accentColor());
        }
    }

    private void renderToolButton(GuiGraphics graphics, MultitoolMode tool, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        int iconSize = RadialIconLayout.MULTITOOL_MODE_ICON_SIZE;
        graphics.fill(x - half, y - half, x + half, y + half, 0xAA394149);
        drawOutline(graphics, x - half, y - half, x + half, y + half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(actionIconNamespace(tool), actionIconPath(tool));
        int sourceSize = actionIconSourceSize(tool);
        // Explicit source rectangle prevents the 15x15 action textures from tiling.
        graphics.flush();
        graphics.blit(icon, x - iconSize / 2, y - iconSize / 2, iconSize, iconSize,
                0, 0, sourceSize, sourceSize, sourceSize, sourceSize);
    }

    private void renderActionButton(GuiGraphics graphics, BaseMode action, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        int iconSize = RadialIconLayout.MULTITOOL_MODE_ICON_SIZE;
        graphics.fill(x - half, y - half, x + half, y + half, 0xAA394149);
        drawOutline(graphics, x - half, y - half, x + half, y + half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("buildinggadgets2",
                "textures/gui/mode/" + action.getId().getPath() + ".png");
        graphics.flush();
        graphics.blit(icon, x - iconSize / 2, y - iconSize / 2, iconSize, iconSize,
                0, 0, RadialIconLayout.MODERN_SETTING_ICON_SIZE, RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                RadialIconLayout.MODERN_SETTING_ICON_SIZE, RadialIconLayout.MODERN_SETTING_ICON_SIZE);
    }

    private static void drawOutline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        if (color == 0) return;
        graphics.fill(left, top, right, top + 2, color);
        graphics.fill(left, bottom - 2, right, bottom, color);
        graphics.fill(left, top + 2, left + 2, bottom - 2, color);
        graphics.fill(right - 2, top + 2, right, bottom - 2, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(inlineModifiers!=null && inlineModifiers.needsCommitBeforeClick(mouseX,mouseY)) {
            inlineModifiers.navigate(() -> mouseClicked(mouseX,mouseY,button),false); return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if(inlineModifiers != null) { inlineModifiers.navigate(() -> handleWheel(mouseX, mouseY, button), false); return true; }
        return handleWheel(mouseX, mouseY, button);
    }
    private boolean handleWheel(double mouseX, double mouseY, int button) {
        if(settingsLayout!=null) return false;
        double dx = mouseX - (double)menuCenterX();
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

        if (Math.sqrt(dx * dx + dy * dy) <= backRadius()) {
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
        String modeStr;
        if (selectedTool() == MultitoolMode.CUT_PASTE) {
            modeStr = GadgetNBT.hasCopyUUID(stack) ? "buildinggadgets2:paste" : "buildinggadgets2:cut";
        } else {
            modeStr = selectedAction == null ? "" : selectedAction.getId().toString();
        }
        ExtraNetwork.sendToServer(new MultitoolSelectionPacket(selectedTool().ordinal(), modeStr));
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
        if (distance < (settingsLayout == null ? 54 : actionRadius()-18) || distance > (settingsLayout == null ? 108 : actionRadius()+18) || actions.isEmpty()) return null;
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
        ResourceLocation saved = MultitoolState.getProfileMode(stack, tool);
        if (tool == MultitoolState.getActiveMode(stack) && tool != MultitoolMode.DESTRUCTION) {
            saved = GadgetNBT.getMode(stack).getId();
        }
        ResourceLocation wanted = saved;
        return actions.stream().filter(action -> wanted != null && action.getId().equals(wanted))
                .findFirst().orElse(actions.get(0));
    }

    private static List<BaseMode> actionsFor(MultitoolMode tool) {
        if (tool == MultitoolMode.DESTRUCTION || tool == MultitoolMode.CUT_PASTE) return List.of();
        return new ArrayList<>(GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(tool)));
    }


    @Override protected com.nstut.openui.api.UIComponent buildUI() {
        if(settingsLayout==null || inlineModifiers==null) return com.nstut.openui.api.Ui.column();
        com.nstut.openui.api.Radial wheel=new com.nstut.openui.api.Radial(settingsLayout.radius,36);
        for(BaseMode action:actionsFor(selectedTool())) {
            com.nstut.openui.api.ButtonWidget button=new com.nstut.openui.api.ButtonWidget(Component.translatable(action.i18n())).radius(2)
                    .content(com.nstut.openui.controls.IconWidget.texture("buildinggadgets2","textures/gui/mode/"+action.getId().getPath()+".png",15,15).size(24))
                    .onPress(()->inlineModifiers.navigate(()->{ selectedAction=action; sendSelection(); rebuildContextButtons(); },false));
            button.setActive(action.equals(selectedAction)); button.tooltip(Component.translatable(action.i18n())); wheel.item(button);
        }
        String label=selectedAction==null?"":Component.translatable(selectedAction.i18n()).getString();
        wheel.center(com.nstut.buildinggadgetsextra.client.ui.ModifierPanel.button("Back",()->inlineModifiers.navigate(()->{
            navigation.back(); rememberedPage=MultitoolMenuState.Page.GENERAL; rebuildContextButtons();
        },false)),settingsLayout.backRadius*2);
        return profileControls.root(Component.translatable(selectedTool().translationKey()).getString(),
                wheel,label,settingsLayout.panelWidth,profileRange,inlineModifiers.panel);
    }

    private MultitoolMode selectedTool() {
        return navigation.selectedTool();
    }

    private static String actionIconPath(MultitoolMode tool) {
        return switch (tool) {
            case BUILD -> "textures/gui/setting/build.png";
            case EXCHANGING -> "textures/gui/setting/exchange.png";
            case COPY_PASTE -> "textures/gui/mode/copy.png";
            case CUT_PASTE -> "textures/gui/mode/cut.png";
            case DESTRUCTION -> "textures/gui/setting/delete.png";
        };
    }

    private static String actionIconNamespace(MultitoolMode tool) {
        return switch (tool) {
            case BUILD, EXCHANGING, DESTRUCTION -> ExtraConstants.MOD_ID;
            case COPY_PASTE, CUT_PASTE -> "buildinggadgets2";
        };
    }

    private static int actionIconSourceSize(MultitoolMode tool) {
        return switch (tool) {
            case BUILD, EXCHANGING, DESTRUCTION -> RadialIconLayout.SOURCE_TEXTURE_SIZE;
            case COPY_PASTE, CUT_PASTE -> RadialIconLayout.MODERN_SETTING_ICON_SIZE;
        };
    }

    private static PasteGUI createPasteGUI(ItemStack stack) {
        try {
            Constructor<PasteGUI> ctor = PasteGUI.class.getDeclaredConstructor(ItemStack.class);
            ctor.setAccessible(true);
            return ctor.newInstance(stack);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create PasteGUI", e);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==257 && inlineModifiers!=null && inlineModifiers.enter()) return true;
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        return super.mouseScrolled(x,y,delta);
    }
}
