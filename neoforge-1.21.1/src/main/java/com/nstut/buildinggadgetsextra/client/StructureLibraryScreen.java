package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.network.StructureFilePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StructureLibraryScreen extends Screen {
    private final Screen parent;
    private EditBox name;

    public StructureLibraryScreen(Screen parent) {
        super(Component.translatable(ExtraConstants.STRUCTURE_LIBRARY));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = height / 2 - 35;
        name = new EditBox(font, left, top, 200, 20, Component.translatable(ExtraConstants.STRUCTURE_NAME));
        name.setMaxLength(StructureFileName.MAX_LENGTH);
        name.setHint(Component.translatable(ExtraConstants.STRUCTURE_NAME));
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable(ExtraConstants.SAVE_STRUCTURE),
                button -> send(false)).bounds(left, top + 28, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(ExtraConstants.LOAD_STRUCTURE),
                button -> send(true)).bounds(left + 102, top + 28, 98, 20).build());
        setInitialFocus(name);
    }

    private void send(boolean load) {
        String value=com.nstut.buildinggadgetsextra.common.StructureFileName.normalize(name.getValue());
        if(!com.nstut.buildinggadgetsextra.common.StructureFileName.isValid(value))return;
        if(load)ClientStructureFiles.upload(value);
        else PacketDistributor.sendToServer(new StructureFilePayload(false,value));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
