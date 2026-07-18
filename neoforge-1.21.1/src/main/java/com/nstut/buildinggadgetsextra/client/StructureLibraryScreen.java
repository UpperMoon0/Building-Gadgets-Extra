package com.nstut.buildinggadgetsextra.client;

import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class StructureLibraryScreen extends Screen {
    private final Screen parent;
    public StructureLibraryScreen(Screen parent){super(Component.translatable(ExtraConstants.STRUCTURE_LIBRARY));this.parent=parent;}
    @Override protected void init(){int left=width/2-100,top=height/2-10;addRenderableWidget(Button.builder(Component.translatable(ExtraConstants.SAVE_STRUCTURE),button->ClientStructureFiles.chooseSave()).bounds(left,top,98,20).build());addRenderableWidget(Button.builder(Component.translatable(ExtraConstants.LOAD_STRUCTURE),button->ClientStructureFiles.chooseLoad()).bounds(left+102,top,98,20).build());}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics,mouseX,mouseY,partialTick);graphics.drawCenteredString(font,title,width/2,height/2-35,0xFFFFFFFF);super.render(graphics,mouseX,mouseY,partialTick);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
