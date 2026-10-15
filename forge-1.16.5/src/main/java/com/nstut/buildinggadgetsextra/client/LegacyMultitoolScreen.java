package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
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
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
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

public final class LegacyMultitoolScreen extends com.nstut.openui.minecraft.UiScreen {
    private static final int TOOL_RADIUS = 68, BACK_RADIUS = 28;
    private static final MultitoolMode[] LEGACY_TOOLS = {
            MultitoolMode.BUILD, MultitoolMode.EXCHANGING,
            MultitoolMode.COPY_PASTE, MultitoolMode.CUT_PASTE, MultitoolMode.DESTRUCTION
    };
    private static MultitoolMenuState.Page rememberedPage = MultitoolMenuState.Page.GENERAL;
    private ItemStack stack;
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
        rebuildSettings();
    }
    private ItemStack authoritativeStack() {
        return minecraft.player == null ? stack : AbstractGadget.getGadget(minecraft.player);
    }
    private void addContext(net.minecraft.client.gui.widget.Widget widget) {
        if(widget!=null) addButton(widget);
    }
    private void closeNow() { super.onClose(); }
    @Override public void onClose() {
        if(inlineModifiers != null) inlineModifiers.navigate(this::closeNow, true); else closeNow();
    }
    private final MultitoolMenuState navigation;
    private int selectedAction;
    private MultitoolMode hoveredTool;
    private int hoveredAction = -1;
    private boolean hoveredBack;

    public LegacyMultitoolScreen(ItemStack stack) {
        super(new TranslationTextComponent("buildinggadgetsextra.multitool.menu"));
        this.stack = stack;
        this.navigation = new MultitoolMenuState(MultitoolState.getActiveMode(stack));
        if (rememberedPage == MultitoolMenuState.Page.SUBMENU) navigation.openTool(MultitoolState.getActiveMode(stack));
        selectedAction = MultitoolState.getProfile(stack, selectedTool());
    }

    @Override protected void init() { populateContextButtons(); super.init(); }

    @Override public void tick() {
        if (minecraft.player == null) return;
        ItemStack held = AbstractGadget.getGadget(minecraft.player);
        if (!held.isEmpty()) this.stack = held;
        if(inlineModifiers != null) inlineModifiers.tick(); profileControls.tick();
    }

    private void rebuildSettings() { populateContextButtons(); rebuildUI(); }
    private void populateContextButtons() {
        com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel previousSettings=toolSettings;
        MultitoolMode previousProfile=settingsProfile;
        toolSettings=null;settingsProfile=selectedTool();
        profileControls.clear(); profileRange=null;

        InlineModifiers previous = inlineModifiers;
        buttons.clear(); children.clear();
        inlineModifiers = null; settingsLayout = null;
        if (navigation.page() == MultitoolMenuState.Page.SUBMENU) {
            settingsLayout = new com.nstut.buildinggadgetsextra.common.ProfileSettingsLayout(width,height);
        }
        if (navigation.page() != MultitoolMenuState.Page.SUBMENU) return;
        int[] left = {height / 2 - 104}, right = {height / 2 - 104};
        MultitoolMode tool = selectedTool();
        if (tool == MultitoolMode.DESTRUCTION) {
            toolSettings=createToolSettings("destruction");
            if(previousProfile==settingsProfile) toolSettings.restoreFrom(previousSettings);
            setting(false,right,"anchor","buildinggadgets.radialmenu.anchor",false,()->false,LegacyMultitoolPacket.ANCHOR);
            setting(false,right,"raytrace_fluid","buildinggadgets.radialmenu.raytrace_fluid",true,()->AbstractGadget.shouldRayTraceFluid(stack),LegacyMultitoolPacket.RAYTRACE);
            setting(false,right,"undo","buildinggadgets.radialmenu.undo",false,()->false,LegacyMultitoolPacket.UNDO);
            setting(false,right,"fuzzy","buildinggadgets.screen.overlay",true,()->GadgetDestruction.getOverlay(stack),LegacyMultitoolPacket.DESTROY_OVERLAY);
            setting(false,right,"raytrace_fluid","buildinggadgets.screen.fluidonly",true,()->GadgetDestruction.getIsFluidOnly(stack),LegacyMultitoolPacket.FLUID_ONLY);
            return;
        }
        setting(false, right, "raytrace_fluid", "buildinggadgets.radialmenu.raytrace_fluid", true,
                () -> AbstractGadget.shouldRayTraceFluid(stack), LegacyMultitoolPacket.RAYTRACE);
        setting(true, left, "anchor", "buildinggadgets.radialmenu.anchor", false, () -> false, LegacyMultitoolPacket.ANCHOR);
        if (tool != MultitoolMode.EXCHANGING) {
            setting(true, left, "undo", "buildinggadgets.radialmenu.undo", false, () -> false, LegacyMultitoolPacket.UNDO);
        }

        if (tool == MultitoolMode.BUILD) {
            setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false,
                    () -> false, LegacyMultitoolPacket.ROTATE);
            setting(true, left, "mirror", "buildinggadgets.radialmenu.mirror", false,
                    () -> false, LegacyMultitoolPacket.MIRROR);
            setting(false, right, "fuzzy", "buildinggadgets.radialmenu.fuzzy", true,
                    () -> AbstractGadget.getFuzzy(stack), LegacyMultitoolPacket.FUZZY);
            setting(false, right, "connected_area", "buildinggadgets.radialmenu.connected_surface", true,
                    () -> AbstractGadget.getConnectedArea(stack), LegacyMultitoolPacket.CONNECTED);
            setting(false, right, "building_place_atop", "buildinggadgets.radialmenu.place_on_top", true,
                    () -> GadgetBuilding.shouldPlaceAtop(stack), LegacyMultitoolPacket.PLACE_ATOP);
            addRange(right);
        }

        if (tool == MultitoolMode.EXCHANGING) {
            setting(false, right, "fuzzy", "buildinggadgets.radialmenu.fuzzy", true,
                    () -> AbstractGadget.getFuzzy(stack), LegacyMultitoolPacket.FUZZY);
            setting(false, right, "connected_area", "buildinggadgets.radialmenu.connected_surface", true,
                    () -> AbstractGadget.getConnectedArea(stack), LegacyMultitoolPacket.CONNECTED);
            addRange(right);
        }

        if (tool == MultitoolMode.COPY_PASTE) {
            String mode = selectedAction == 1 ? "paste" : "copy";
            toolSettings=createToolSettings(mode);
            if(previousProfile==settingsProfile) toolSettings.restoreFrom(previousSettings);
            if ("paste".equals(mode)) {
                clientSetting(false, right, "copypaste_materiallist", "buildinggadgets.radialmenu.materiallist",
                        () -> minecraft.setScreen(new MaterialListGUI(stack)));
            }
            if (RadialButtonPolicy.showRotateButton(mode)) {
                setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false, () -> false, LegacyMultitoolPacket.ROTATE);
            }
            if (RadialButtonPolicy.showMirrorButtons(mode)) {
                addAddonIcon( "mirror_horizontal",
                        new TranslationTextComponent(ExtraConstants.MIRROR_HORIZONTAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(false)));
                addAddonIcon( "mirror_vertical",
                        new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(true)));
            }
            RadialButtonPolicy.FileAction file = RadialButtonPolicy.fileAction(false, mode);
            if (file == RadialButtonPolicy.FileAction.SAVE) {
                addAddonIcon( "save",
                        new TranslationTextComponent(ExtraConstants.SAVE_STRUCTURE),
                        ClientStructureFiles::chooseSave);
            } else if (file == RadialButtonPolicy.FileAction.LOAD) {
                addAddonIcon( "load",
                        new TranslationTextComponent(ExtraConstants.LOAD_STRUCTURE),
                        ClientStructureFiles::chooseLoad);
            }
        }

        if (tool == MultitoolMode.CUT_PASTE) {
            boolean hasCutBuffer = stack.getOrCreateTag().getBoolean("cutBufferActive")
                    && GadgetCopyPaste.getSelectedRegion(stack).isPresent();
            toolSettings=createToolSettings(hasCutBuffer?"paste":"copy");
            if(previousProfile==settingsProfile) toolSettings.restoreFrom(previousSettings);
            if (!hasCutBuffer) {
                addAddonIcon( "cut", RadialIconLayout.MODERN_SETTING_ICON_SIZE,
                        new TranslationTextComponent(ExtraConstants.CUT_SELECTION),
                        () -> ExtraNetwork.sendToServer(new CutSelectionPacket()));
                addAddonIcon( "save",
                        new TranslationTextComponent(ExtraConstants.SAVE_STRUCTURE),
                        ClientStructureFiles::chooseSave);
            } else {
                    setting(true, left, "rotate", "buildinggadgets.radialmenu.rotate", false, () -> false, LegacyMultitoolPacket.ROTATE);
                addAddonIcon( "mirror_horizontal",
                        new TranslationTextComponent(ExtraConstants.MIRROR_HORIZONTAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(false)));
                addAddonIcon( "mirror_vertical",
                        new TranslationTextComponent(ExtraConstants.MIRROR_VERTICAL),
                        () -> ExtraNetwork.sendToServer(new MirrorPacket(true)));
                addAddonIcon( "load",
                        new TranslationTextComponent(ExtraConstants.LOAD_STRUCTURE),
                        ClientStructureFiles::chooseLoad);
            }
        }
        if(selectedTool() == MultitoolMode.BUILD || selectedTool() == MultitoolMode.EXCHANGING) {
            inlineModifiers = new InlineModifiers(this::authoritativeStack);
            inlineModifiers.restoreFrom(previous);
        }
    }

    private boolean currentProfile(Object connection,MultitoolMode profile) {
        return connection!=null && minecraft.player!=null && minecraft.getConnection()==connection
                && authoritativeStack().getItem() instanceof BuildersMultitool
                && MultitoolState.getActiveMode(authoritativeStack())==profile;
    }

    private com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel createToolSettings(String kind) {
        ItemStack held=authoritativeStack();final Object connection=minecraft.getConnection(); final MultitoolMode profile=selectedTool();
        if(kind.equals("destruction")) {
            int[] initial=new int[5];String[] names={"left","right","up","down","depth"};
            for(int i=0;i<5;i++) initial[i]=GadgetDestruction.getToolValue(held,names[i]);
            return new com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel(kind,initial,0,com.nstut.buildinggadgetsextra.setup.ExtraConfig.multitoolDestructionSide(),
                v->com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy.validDestruction(v[0],v[1],v[2],v[3],v[4],com.nstut.buildinggadgetsextra.setup.ExtraConfig.multitoolDestructionSide(),com.nstut.buildinggadgetsextra.setup.ExtraConfig.multitoolDestructionSpan()),
                v->{if(currentProfile(connection,profile)) com.direwolf20.buildinggadgets.common.network.PacketHandler.sendToServer(new com.direwolf20.buildinggadgets.common.network.packets.PacketDestructionGUI(v[0],v[1],v[2],v[3],v[4]));});
        }
        com.direwolf20.buildinggadgets.common.tainted.building.Region region=GadgetCopyPaste.getSelectedRegion(held).orElse(com.direwolf20.buildinggadgets.common.tainted.building.Region.singleZero());
        net.minecraft.util.math.BlockPos start=kind.equals("paste")?GadgetCopyPaste.getRelativeVector(held):region.getMin();
        net.minecraft.util.math.BlockPos end=region.getMax();
        int[] initial=kind.equals("paste")?new int[]{start.getX(),start.getY(),start.getZ()}:new int[]{start.getX(),start.getY(),start.getZ(),end.getX(),end.getY(),end.getZ()};
        com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel result=new com.nstut.buildinggadgetsextra.client.ui.ToolSettingsPanel(kind,initial,kind.equals("paste")?-16:-30000000,kind.equals("paste")?16:30000000,v->true,v->{
            if(!currentProfile(connection,profile)) return;
            if(kind.equals("paste")) com.direwolf20.buildinggadgets.common.network.PacketHandler.sendToServer(new com.direwolf20.buildinggadgets.common.network.packets.PacketPasteGUI(v[0],v[1],v[2]));
            else com.direwolf20.buildinggadgets.common.network.PacketHandler.sendToServer(new com.direwolf20.buildinggadgets.common.network.packets.PacketCopyCoords(new net.minecraft.util.math.BlockPos(v[0],v[1],v[2]),new net.minecraft.util.math.BlockPos(v[3],v[4],v[5])));
        });
        if(kind.equals("copy") && !com.direwolf20.buildinggadgets.common.config.Config.GENERAL.allowAbsoluteCoords.get()) result.relativeOnly();
        if(kind.equals("copy") && !GadgetCopyPaste.getSelectedRegion(held).isPresent()) result.emptySelection();
        return result;
    }

    private void navigate(Runnable action) { if(toolSettings!=null && !toolSettings.flush()) return; if(inlineModifiers!=null) inlineModifiers.navigate(action,false); else action.run(); }
    private void addAddonIcon(String name,ITextComponent label,Runnable action) { addAddonIcon(name,44,label,action); }
    private void addAddonIcon(String name,int source,ITextComponent label,Runnable action) {
        profileControls.icon(ExtraConstants.MOD_ID,"textures/gui/setting/"+name+".png",source,label.getString(),()->false,()->navigate(action));
    }

    private void setting(boolean leftSide, int[] y, String icon, String translation, boolean selectable,
                         BooleanSupplier selected, int operation) {
        int x = leftSide ? menuCenterX() - 136 : menuCenterX() + 112;
        addContext(contextIcon(x, next(y), icon, new TranslationTextComponent(translation), selectable, send -> {
            if (send) ExtraNetwork.sendToServer(new LegacyMultitoolPacket(operation, 0));
            return selected.getAsBoolean();
        }));
    }

    private void clientSetting(boolean leftSide, int[] y, String icon, String translation, Runnable action) {
        int x = leftSide ? menuCenterX() - 136 : menuCenterX() + 112;
        addContext(contextIcon(x, next(y), icon, new TranslationTextComponent(translation), false, send -> {
            if (send) action.run();
            return false;
        }));
    }

    private void addRange(int[] right) {
        final int[] range={GadgetUtils.getToolRange(authoritativeStack())};
        profileRange=com.nstut.buildinggadgetsextra.client.ui.ProfileControls.range(GadgetUtils.getToolRange(authoritativeStack()),
                com.nstut.buildinggadgetsextra.setup.ExtraConfig.multitoolMaxRange(), next->{
                    int current=range[0]; range[0]=next;
                    ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.RANGE,next-current));
                });
    }

    private GuiIconActionable contextIcon(int x,int y,String icon,net.minecraft.util.text.ITextComponent tooltip,boolean selectable,java.util.function.Predicate<Boolean> action) {
        if(settingsLayout!=null) {
            profileControls.icon("buildinggadgets","textures/gui/setting/"+icon+".png",16,tooltip.getString(),
                    ()->selectable && action.test(false),()->navigate(()->action.test(true)));
            return null;
        }
        return new GuiIconActionable(x,y,icon,tooltip,selectable,action);
    }
    private static int next(int[] value) { int result = value[0]; value[0] += 30; return result; }

    @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTick) {
        if(settingsLayout!=null) { super.render(matrices,mouseX,mouseY,partialTick); return; }
        int cx = menuCenterX(), cy = height / 2;
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
            hoveredBack = Math.sqrt(dx * dx + dy * dy) <= backRadius();
            fill(matrices, cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(), 0xCC20252A);
            outline(matrices, cx - backRadius(), cy - backRadius(), cx + backRadius(), cy + backRadius(), hoveredBack ? 0xFF3598FF : 0);
            drawCenteredString(matrices, font, new TranslationTextComponent("buildinggadgetsextra.multitool.back").getString(), cx, cy - 4, 0xFFFFFF);
            drawCenteredString(matrices, font, new TranslationTextComponent(selectedTool().translationKey()).getString(), cx, cy - (settingsLayout == null ? 122 : actionRadius()+40), selectedTool().accentColor());
            for (int i = 0; i < actions.size(); i++) {
                double angle = -Math.PI / 2 + i * Math.PI * 2 / actions.size();
                drawAction(matrices, actions.get(i), cx + (int)(Math.cos(angle) * actionRadius()), cy + (int)(Math.sin(angle) * actionRadius()), i == selectedAction, i == hoveredAction);
            }
            if (!actions.isEmpty()) {
                int label = hoveredAction >= 0 ? hoveredAction : Math.min(selectedAction, actions.size() - 1);
                drawCenteredString(matrices, font, new TranslationTextComponent(actions.get(label).translation).getString(), cx, cy + (settingsLayout != null && actionRadius() < 82 ? actionRadius()+24 : 34), selectedTool().accentColor());
            }
        }

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
        minecraft.getTextureManager().bind(new ResourceLocation(action.namespace, action.icon));
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
        if(inlineModifiers!=null && inlineModifiers.needsCommitBeforeClick(mouseX,mouseY)) {
            inlineModifiers.navigate(() -> mouseClicked(mouseX,mouseY,button),false); return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if(inlineModifiers != null) { inlineModifiers.navigate(() -> handleWheel(mouseX, mouseY, button), false); return true; }
        return handleWheel(mouseX, mouseY, button);
    }
    private boolean handleWheel(double mouseX, double mouseY, int button) {
        if(settingsLayout!=null) return false;
        double dx = mouseX-(double)menuCenterX(), dy=mouseY-height/2.0;
        if (navigation.page() == MultitoolMenuState.Page.GENERAL) {
            MultitoolMode mode = findTool(dx,dy); if (mode == null) return false;
            navigation.openTool(mode); rememberedPage=MultitoolMenuState.Page.SUBMENU;
            if (mode == MultitoolMode.CUT_PASTE) {
                boolean hasCutBuffer = stack.getOrCreateTag().getBoolean("cutBufferActive")
                        && GadgetCopyPaste.getSelectedRegion(stack).isPresent();
                selectedAction = hasCutBuffer ? 1 : 0;
                MultitoolState.selectTool(stack, mode);
                ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_TOOL, mode.ordinal()));
            } else {
                selectedAction = MultitoolState.getProfile(stack, mode);
                MultitoolState.selectTool(stack, mode);
                ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_TOOL, mode.ordinal()));
            }
            rebuildSettings(); return true;
        }
        if (Math.sqrt(dx*dx+dy*dy)<=backRadius()) { navigation.back(); rememberedPage=MultitoolMenuState.Page.GENERAL; rebuildSettings(); return true; }
        List<LegacyAction> actions=actions(selectedTool()); int action=findAction(actions.size(),dx,dy); if(action<0)return false;
        selectedAction=action; MultitoolState.applyProfile(stack,selectedTool(),action);
        ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_ACTION,action)); rebuildSettings(); return true;
    }

    private MultitoolMode findTool(double dx,double dy){double d=Math.sqrt(dx*dx+dy*dy);if(d<42||d>94)return null;int n=LEGACY_TOOLS.length;double s=Math.PI*2/n;int i=(int)Math.floor((angle(dx,dy)+s/2)/s)%n;return LEGACY_TOOLS[i];}
    private int findAction(int n,double dx,double dy){double d=Math.sqrt(dx*dx+dy*dy);if(n==0||d<(settingsLayout==null?54:actionRadius()-18)||d>(settingsLayout==null?108:actionRadius()+18))return-1;double s=Math.PI*2/n;return(int)Math.floor((angle(dx,dy)+s/2)/s)%n;}
    private static double angle(double dx,double dy){double a=Math.atan2(dy,dx)+Math.PI/2;return a<0?a+Math.PI*2:a;}

    @Override protected com.nstut.openui.api.UIComponent buildUI() {
        if(settingsLayout==null) return com.nstut.openui.api.Ui.column();
        com.nstut.openui.api.Radial wheel=new com.nstut.openui.api.Radial(settingsLayout.radius,36);
        List<LegacyAction> actions=actions(selectedTool());
        for(int i=0;i<actions.size();i++) {
            final int index=i; LegacyAction action=actions.get(i);
            com.nstut.openui.api.ButtonWidget button=new com.nstut.openui.api.ButtonWidget(new TranslationTextComponent(action.translation)).radius(2)
                    .content(com.nstut.openui.controls.IconWidget.texture(action.namespace,action.icon,16,16).size(24))
                    .onPress(()->navigate(()->{
                        selectedAction=index; MultitoolState.applyProfile(stack,selectedTool(),index);
                        ExtraNetwork.sendToServer(new LegacyMultitoolPacket(LegacyMultitoolPacket.SELECT_ACTION,index)); rebuildSettings();
                    }));
            button.setActive(i==selectedAction); button.tooltip(new TranslationTextComponent(action.translation)); wheel.item(button);
        }
        String label=actions.isEmpty()?"":new TranslationTextComponent(actions.get(Math.min(selectedAction,actions.size()-1)).translation).getString();
        wheel.center(com.nstut.buildinggadgetsextra.client.ui.ModifierPanel.button("Back",()->navigate(()->{
            navigation.back(); rememberedPage=MultitoolMenuState.Page.GENERAL; rebuildSettings();
        })),settingsLayout.backRadius*2);
        return profileControls.root(new TranslationTextComponent(selectedTool().translationKey()).getString(),
                wheel,label,settingsLayout.panelWidth,profileRange,inlineModifiers==null?null:inlineModifiers.panel,toolSettings);
    }

    private MultitoolMode selectedTool(){return navigation.selectedTool();}

    private static List<LegacyAction> actions(MultitoolMode tool) {
        List<LegacyAction> result=new ArrayList<>();
        if(tool==MultitoolMode.BUILD)for(BuildingModes m:BuildingModes.values())result.add(new LegacyAction(m.getTranslationKey(),m.getIcon()));
        else if(tool==MultitoolMode.EXCHANGING)for(ExchangingModes m:ExchangingModes.values())result.add(new LegacyAction(m.getTranslationKey(),m.getIcon()));
        else if(tool==MultitoolMode.COPY_PASTE){result.add(new LegacyAction("buildinggadgets.modes.copy","textures/gui/mode/copy.png"));result.add(new LegacyAction("buildinggadgets.modes.paste","textures/gui/mode/paste.png"));}
        return result;
    }
    private static final class LegacyAction { final String translation, icon, namespace; LegacyAction(String translation, String icon, String namespace){this.translation=translation;this.icon=icon;this.namespace=namespace;} LegacyAction(String translation, String icon){this(translation, icon, "buildinggadgets");} }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==257 && inlineModifiers!=null && inlineModifiers.enter()) return true;
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        return super.mouseScrolled(x,y,delta);
    }
}
