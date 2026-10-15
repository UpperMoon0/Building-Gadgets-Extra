package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.api.gadgets.GadgetModes;
import com.direwolf20.buildinggadgets2.client.screen.MaterialListGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIconActionable;
import com.direwolf20.buildinggadgets2.client.screen.widgets.IncrementalSliderWidget;
import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.data.*;
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
import com.nstut.buildinggadgetsextra.network.MirrorPayload;
import com.nstut.buildinggadgetsextra.network.MultitoolCutPayload;
import com.nstut.buildinggadgetsextra.network.MultitoolSelectionPayload;
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

public final class MultitoolRadialScreen extends com.nstut.openui.minecraft.UiScreen {
    private static final int TOOL_RADIUS = 68;
    private static final int ACTION_RADIUS = 82;
    private static final int BACK_RADIUS = 28;
    private static MultitoolMenuState.Page rememberedPage = MultitoolMenuState.Page.GENERAL;

    private ItemStack stack;
    private String cutSnapshot="";
    private boolean refreshingCut;
    private String cutSnapshot() {
        ItemStack held=authoritativeStack();
        return GadgetNBT.hasCopyUUID(held)+":"+GadgetNBT.getCopyStartPos(held)+":"+GadgetNBT.getCopyEndPos(held);
    }
    private com.nstut.buildinggadgetsextra.common.ProfileSettingsLayout settingsLayout;
    private InlineModifiers inlineModifiers;
    private com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel toolSettings;
    private MultitoolMode settingsProfile;
    public com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel toolSettings() { return toolSettings; }
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
        if(toolSettings!=null && !toolSettings.flush()) return;
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

    @Override
    protected void init() {
        // UiScreen replaces the runtime; do not mount fresh controls into the retiring one.
        populateContextButtons();
        super.init();
    }

    @Override
    public void tick() {
        super.tick();
        if(settingsLayout!=null && selectedTool()==MultitoolMode.CUT_PASTE
                && MultitoolState.getActiveMode(authoritativeStack())==selectedTool()
                && !cutSnapshot.equals(cutSnapshot())) {
            refreshingCut=true;
            try { rebuildContextButtons(); } finally { refreshingCut=false; }
        }
        if(inlineModifiers != null) inlineModifiers.tick(); profileControls.tick();
        if (minecraft.player != null) {
            ItemStack held = BaseGadget.getGadget(minecraft.player);
            if (!held.isEmpty()) this.stack = held;
        }
    }

    private void rebuildContextButtons() { populateContextButtons(); rebuildUI(); }
    private void populateContextButtons() {
        com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel previousSettings=toolSettings;
        MultitoolMode previousProfile=settingsProfile;
        boolean sameCutState=cutSnapshot.equals(cutSnapshot());
        toolSettings=null; settingsProfile=selectedTool();
        profileControls.clear(); profileRange=null;

        InlineModifiers previous = inlineModifiers;
        clearWidgets();
        inlineModifiers = null; settingsLayout = null;
        if (navigation.page() == MultitoolMenuState.Page.SUBMENU) {
            settingsLayout = new com.nstut.buildinggadgetsextra.common.ProfileSettingsLayout(width,height);
        }
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return;
        // Resize initializes directly while the profile-sync mixin may be awaiting an echo.
        if (MultitoolState.getActiveMode(authoritativeStack()) != selectedTool()) return;

        int[] leftY = {height / 2 - 104};
        int[] rightY = {height / 2 - 104};

        if (selectedTool() == MultitoolMode.DESTRUCTION) {
            toolSettings=createToolSettings("destruction");
            if(previousProfile==settingsProfile && !refreshingCut && (settingsProfile!=MultitoolMode.CUT_PASTE || sameCutState)) toolSettings.restoreFrom(previousSettings);
            addUpstream(false,rightY,"anchor",Component.translatable("buildinggadgets2.radialmenu.anchor"),true,null,()->ClientPacketDistributor.sendToServer(new AnchorPayload()));
            addUpstream(false,rightY,"affecttiles",Component.translatable("buildinggadgets2.screen.affecttiles"),true,GadgetNBT.ToggleableSettings.AFFECT_TILES,null);
            addUpstream(false,rightY,"raytrace_fluid",Component.translatable("buildinggadgets2.radialmenu.raytracefluids"),true,GadgetNBT.ToggleableSettings.RAYTRACE_FLUID,null);
            addRenderTypeButton(leftY);
            addUpstream(false,rightY,"undo",Component.translatable("buildinggadgets2.radialmenu.undo"),false,null,()->ClientPacketDistributor.sendToServer(new UndoPayload()));
            return;
        }

        String mode = selectedAction == null ? "" : selectedAction.getId().getPath();
        boolean cutTool = selectedTool() == MultitoolMode.CUT_PASTE;
        boolean hasCutBuffer = cutTool && GadgetNBT.hasCopyUUID(authoritativeStack());
        if(cutTool) {
            cutSnapshot=cutSnapshot();
            ItemStack held=authoritativeStack();
            com.nstut.buildinggadgetsextra.common.CutPasteStep step=com.nstut.buildinggadgetsextra.common.CutPasteStep.of(hasCutBuffer,
                    !GadgetNBT.getCopyStartPos(held).equals(GadgetNBT.nullPos),!GadgetNBT.getCopyEndPos(held).equals(GadgetNBT.nullPos));
            profileControls.workflow(com.nstut.buildinggadgetsextra.client.ui.CutPasteControls.create(step,
                    ()->navigate(()->ClientPacketDistributor.sendToServer(new MultitoolCutPayload()))));
        }
        String effectiveMode = cutTool ? (hasCutBuffer ? "paste" : "cut") : mode;

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
            if ("surface".equals(effectiveMode)) {
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
            profileRange=com.nstut.buildinggadgetsextra.client.ui.ProfileControls.range(GadgetNBT.getToolRange(authoritativeStack()),
                    ExtraConfig.multitoolMaxRange(),next->ClientPacketDistributor.sendToServer(new RangeChangePayload(next)));
        }

        if (selectedTool() == MultitoolMode.COPY_PASTE || cutTool) {
            addUpstream(false, rightY, "paste_replace", Component.translatable("buildinggadgets2.screen.paste_replace"), true,
                    GadgetNBT.ToggleableSettings.PASTE_REPLACE, null);
            toolSettings=createToolSettings("paste".equals(effectiveMode)?"paste":"copy");
            if(previousProfile==settingsProfile && !refreshingCut && (settingsProfile!=MultitoolMode.CUT_PASTE || sameCutState)) toolSettings.restoreFrom(previousSettings);
            if (RadialButtonPolicy.showRotateButton(effectiveMode)) {
                addUpstream(true, leftY, "rotate", Component.translatable("buildinggadgets2.radialmenu.rotate"), false,
                        null, () -> ClientPacketDistributor.sendToServer(new RotatePayload()));
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
            addAddonIcon( "mirror_horizontal",
                    Component.translatable(liveMirrorTool
                            ? ExtraConstants.LIVE_MIRROR_HORIZONTAL : ExtraConstants.MIRROR_HORIZONTAL),
                    () -> ClientPacketDistributor.sendToServer(new MirrorPayload(false)));
            addAddonIcon( "mirror_vertical",
                    Component.translatable(liveMirrorTool
                            ? ExtraConstants.LIVE_MIRROR_VERTICAL : ExtraConstants.MIRROR_VERTICAL),
                    () -> ClientPacketDistributor.sendToServer(new MirrorPayload(true)));
        }

        RadialButtonPolicy.FileAction fileAction = RadialButtonPolicy.fileAction(cutTool, effectiveMode);
        if (fileAction != RadialButtonPolicy.FileAction.NONE) {
            boolean load = fileAction == RadialButtonPolicy.FileAction.LOAD;
            addAddonIcon( load ? "load" : "save",
                    Component.translatable(load ? ExtraConstants.LOAD_STRUCTURE : ExtraConstants.SAVE_STRUCTURE),
                    load ? ClientStructureFiles::chooseLoad : ClientStructureFiles::chooseSave);
        }

        if(selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING) {
            inlineModifiers = new InlineModifiers(this::authoritativeStack);
            inlineModifiers.restoreFrom(previous);
        }
    }

    private void addAddonIcon(String name,Component label,Runnable action) { addAddonIcon(MirrorIconButton.addonSettingIcon(name),44,label,action); }
    private void addAddonIcon(String name,int source,Component label,Runnable action) { addAddonIcon(MirrorIconButton.upstreamSettingIcon(name),source,label,action); }

    private void addAddonIcon(Identifier icon,int source,Component label,Runnable action) {
        profileControls.icon(icon.getNamespace(),icon.getPath(),source,label.getString(),()->false,()->navigate(action));
    }

    private boolean currentProfile(Object connection,MultitoolMode profile) {
        return connection!=null && minecraft.player!=null && minecraft.getConnection()==connection
                && authoritativeStack().getItem() instanceof BuildersMultitool
                && MultitoolState.getActiveMode(authoritativeStack())==profile;
    }

    private com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel createToolSettings(String kind) {
        ItemStack held=authoritativeStack(); final Object connection=minecraft.getConnection(); final MultitoolMode profile=selectedTool();
        if(kind.equals("destruction")) {
            int[] initial=new int[5];String[] names={"left","right","up","down","depth"};
            for(int i=0;i<5;i++) initial[i]=GadgetNBT.getToolValue(held,names[i]);
            return new com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel(kind,initial,0,ExtraConfig.multitoolDestructionSide(),
                v->com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy.validDestruction(v[0],v[1],v[2],v[3],v[4],ExtraConfig.multitoolDestructionSide(),ExtraConfig.multitoolDestructionSpan()),
                v->{if(currentProfile(connection,profile)) ClientPacketDistributor.sendToServer(new DestructionRangesPayload(v[0],v[1],v[2],v[3],v[4]));});
        }
        net.minecraft.core.BlockPos start=kind.equals("paste")?GadgetNBT.getRelativePaste(held):GadgetNBT.getCopyStartPos(held);
        net.minecraft.core.BlockPos end=GadgetNBT.getCopyEndPos(held);
        int[] initial=kind.equals("paste")?new int[]{start.getX(),start.getY(),start.getZ()}:new int[]{start.getX(),start.getY(),start.getZ(),end.getX(),end.getY(),end.getZ()};
        com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel result=new com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel(kind,initial,kind.equals("paste")?-16:-30000000,kind.equals("paste")?16:30000000,v->true,v->{
            if(!currentProfile(connection,profile)) return;
            if(kind.equals("paste")) ClientPacketDistributor.sendToServer(new RelativePastePayload(new net.minecraft.core.BlockPos(v[0],v[1],v[2])));
            else ClientPacketDistributor.sendToServer(new CopyCoordsPayload(new net.minecraft.core.BlockPos(v[0],v[1],v[2]),new net.minecraft.core.BlockPos(v[3],v[4],v[5])));
        });
        if(kind.equals("copy")) result.clearTo(new int[]{GadgetNBT.nullPos.getX(),GadgetNBT.nullPos.getY(),GadgetNBT.nullPos.getZ(),GadgetNBT.nullPos.getX(),GadgetNBT.nullPos.getY(),GadgetNBT.nullPos.getZ()});
        if(kind.equals("copy") && (start.equals(GadgetNBT.nullPos) || end.equals(GadgetNBT.nullPos))) result.emptySelection();
        return result;
    }

    private void addUpstream(boolean left, int[] y, String icon, Component tooltip, boolean selectable,
                             GadgetNBT.ToggleableSettings setting, Runnable action) {
        int x = left ? menuCenterX() - 136 : menuCenterX() + 112;
        addContext(contextIcon(x, next(y), icon, tooltip, selectable, send -> {
            if (send) {
                if (setting != null) ClientPacketDistributor.sendToServer(new ToggleSettingPayload(setting.getName()));
                if (action != null) action.run();
            }
            if (setting != null) return GadgetNBT.getSetting(stack, setting.getName());
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
                ClientPacketDistributor.sendToServer(new RenderChangePayload(current[0].getPosition()));
            }
            return false;
        });
        addContext(holder[0]);
    }

    private GuiIconActionable contextIcon(int x,int y,String icon,net.minecraft.network.chat.Component tooltip,boolean selectable,java.util.function.Predicate<Boolean> action) {
        if(settingsLayout!=null) {
            profileControls.icon("buildinggadgets2","textures/gui/setting/"+icon+".png",15,tooltip.getString(),
                    ()->selectable && action.test(false),()->navigate(()->action.test(true)));
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Radial menus are sharp overlays, matching BG2's ModeRadialMenu.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if(settingsLayout!=null) { super.extractRenderState(graphics,mouseX,mouseY,partialTick); return; }
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
                cx, cy - (settingsLayout == null ? 122 : actionRadius()+40), selectedTool().accentColor());
        graphics.fill(cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(), 0xCC20252A);
        drawOutline(graphics, cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(),
                hoveredBack ? 0xFF3598FF : 0);
        graphics.centeredText(font, Component.translatable("buildinggadgetsextra.multitool.back"),
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
            graphics.centeredText(font, Component.translatable(label.i18n()),
                    cx, cy + (settingsLayout != null && actionRadius() < 82 ? actionRadius()+24 : 34), selectedTool().accentColor());
        }
    }

    private void renderToolButton(GuiGraphicsExtractor graphics, MultitoolMode tool, int x, int y, boolean selected, boolean hovered) {
        int half = RadialIconLayout.MULTITOOL_MODE_BUTTON_SIZE / 2;
        int iconSize = RadialIconLayout.MULTITOOL_MODE_ICON_SIZE;
        graphics.fill(x - half, y - half, x + half, y + half, 0xAA394149);
        drawOutline(graphics, x - half, y - half, x + half, y + half, hovered ? 0xFF3598FF : selected ? 0xFF00E640 : 0);
        Identifier icon = Identifier.fromNamespaceAndPath(actionIconNamespace(tool), actionIconPath(tool));
        int sourceSize = actionIconSourceSize(tool);
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
        if(inlineModifiers!=null && inlineModifiers.needsCommitBeforeClick(event.x(),event.y())) {
            inlineModifiers.navigate(() -> mouseClicked(event,doubleClick),false); return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        double mouseX = event.x();
        double mouseY = event.y();
        if(inlineModifiers != null) { inlineModifiers.navigate(() -> handleWheel(event, doubleClick), false); return true; }
        return handleWheel(event, doubleClick);
    }
    private boolean handleWheel(MouseButtonEvent event, boolean doubleClick) {
        if(settingsLayout!=null) return false;
        double mouseX=event.x(), mouseY=event.y();
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
            modeStr = GadgetNBT.hasCopyUUID(authoritativeStack()) ? "buildinggadgets2:paste" : "buildinggadgets2:cut";
        } else {
            modeStr = selectedAction == null ? "" : selectedAction.getId().toString();
        }
        ClientPacketDistributor.sendToServer(new MultitoolSelectionPayload(selectedTool().ordinal(), modeStr));
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
        Identifier saved = MultitoolState.getProfileMode(stack, tool);
        if (tool == MultitoolState.getActiveMode(stack) && tool != MultitoolMode.DESTRUCTION) {
            saved = GadgetNBT.getMode(stack).getId();
        }
        Identifier wanted = saved;
        return actions.stream().filter(action -> wanted != null && action.getId().equals(wanted))
                .findFirst().orElse(actions.get(0));
    }

    private static List<BaseMode> actionsFor(MultitoolMode tool) {
        if (tool == MultitoolMode.DESTRUCTION || tool == MultitoolMode.CUT_PASTE) return List.of();
        return new ArrayList<>(GadgetModes.INSTANCE.getModesForGadget(BuildersMultitool.target(tool)));
    }


    @Override protected com.nstut.openui.api.UIComponent buildUI() {
        if(settingsLayout==null) return com.nstut.openui.api.Ui.column();
        if(MultitoolState.getActiveMode(authoritativeStack()) != selectedTool()) return com.nstut.openui.api.Ui.column(
                new com.nstut.openui.api.TextWidget("Waiting for profile sync..."));
        com.nstut.openui.api.Radial wheel=new com.nstut.openui.api.Radial(settingsLayout.radius,36);
        for(BaseMode action:actionsFor(selectedTool())) {
            com.nstut.openui.api.ButtonWidget button=new com.nstut.openui.api.ButtonWidget(Component.translatable(action.i18n())).radius(2)
                    .content(com.nstut.openui.controls.IconWidget.texture("buildinggadgets2","textures/gui/mode/"+action.getId().getPath()+".png",15,15).size(24))
                    .onPress(()->navigate(()->{ selectedAction=action; sendSelection(); rebuildContextButtons(); }));
            button.setActive(action.equals(selectedAction)); button.tooltip(Component.translatable(action.i18n())); wheel.item(button);
        }
        String label=selectedAction==null?"":Component.translatable(selectedAction.i18n()).getString();
        if(selectedTool()==MultitoolMode.CUT_PASTE) label=GadgetNBT.hasCopyUUID(authoritativeStack())?"Paste":"Select area";
        wheel.center(com.nstut.buildinggadgetsextra.client.ui.ModifierPanel.button("Back",()->navigate(()->{
            navigation.back(); rememberedPage=MultitoolMenuState.Page.GENERAL; rebuildContextButtons();
        })),settingsLayout.backRadius*2);
        return profileControls.root(Component.translatable(selectedTool().translationKey()).getString(),
                wheel,label,settingsLayout.panelWidth,profileRange,inlineModifiers==null?null:inlineModifiers.panel,toolSettings);
    }

    private void navigate(Runnable action) { if(toolSettings!=null && !toolSettings.flush()) return; if(inlineModifiers!=null) inlineModifiers.navigate(action,false); else action.run(); }

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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if(event.key()==257 && inlineModifiers!=null && inlineModifiers.enter()) return true;
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double delta) {
        return super.mouseScrolled(x,y,horizontal,delta);
    }
}
