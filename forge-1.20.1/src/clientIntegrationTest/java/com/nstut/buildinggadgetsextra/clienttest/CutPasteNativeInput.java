package com.nstut.buildinggadgetsextra.clienttest;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
final class CutPasteNativeInput {
    static void resize(MultitoolRadialScreen screen,int w,int h) { screen.resize(Minecraft.getInstance(),w,h); }
    static void aimDown() {
        Minecraft mc=Minecraft.getInstance();
        mc.player.setYRot(0);mc.player.setXRot(90);mc.player.yRotO=0;mc.player.xRotO=90;
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(0,90,mc.player.onGround()));
    }
    static void use(boolean shift) {
        Minecraft mc=Minecraft.getInstance();
        aimDown();sendShift(shift);
        mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        if(shift) sendShift(false);
    }
    private static void sendShift(boolean shift) {
        Minecraft mc=Minecraft.getInstance();
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(mc.player,
                shift?net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY
                :net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
    }
}
