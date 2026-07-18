package com.nstut.buildinggadgetsextra.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.StructureFileName;
import com.nstut.buildinggadgetsextra.network.ExtraNetwork;
import com.nstut.buildinggadgetsextra.network.StructureFilePacket;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.TranslationTextComponent;

public final class StructureLibraryScreen extends Screen {
    private final Screen parent;private TextFieldWidget name;
    public StructureLibraryScreen(Screen parent){super(new TranslationTextComponent(ExtraConstants.STRUCTURE_LIBRARY));this.parent=parent;}
    @Override protected void init(){int left=width/2-100,top=height/2-35;name=new TextFieldWidget(font,left,top,200,20,new TranslationTextComponent(ExtraConstants.STRUCTURE_NAME));name.setMaxLength(StructureFileName.MAX_LENGTH);name.setSuggestion(new TranslationTextComponent(ExtraConstants.STRUCTURE_NAME).getString());addButton(name);addButton(new Button(left,top+28,98,20,new TranslationTextComponent(ExtraConstants.SAVE_STRUCTURE),button->send(false)));addButton(new Button(left+102,top+28,98,20,new TranslationTextComponent(ExtraConstants.LOAD_STRUCTURE),button->send(true)));setInitialFocus(name);}
    private void send(boolean load){String value=StructureFileName.normalize(name.getValue());if(!StructureFileName.isValid(value))return;if(load)ClientStructureFiles.upload(value);else ExtraNetwork.sendToServer(new StructureFilePacket(false,value));}
    @Override public void render(MatrixStack stack,int mouseX,int mouseY,float partialTick){renderBackground(stack);drawCenteredString(stack,font,title,width/2,height/2-58,0xFFFFFFFF);super.render(stack,mouseX,mouseY,partialTick);}
    @Override public void onClose(){minecraft.setScreen(parent);}
}
