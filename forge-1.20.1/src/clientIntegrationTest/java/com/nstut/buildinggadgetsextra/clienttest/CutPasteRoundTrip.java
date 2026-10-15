package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.client.MultitoolRadialScreen;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.openui.api.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.CompletableFuture;

/** Real cut button -> server queue -> synchronized paste controls -> native paste -> selection. */
final class CutPasteRoundTrip {
    private int phase,settleTicks;
    private CompletableFuture<Boolean> server;
    private boolean serverResult;
    private BlockPos source, destination;
    private GameType previousGameType;
    private double oldX, oldY, oldZ;
    private float oldYaw, oldPitch;

    boolean tick(MultitoolRadialScreen screen) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.getSingleplayerServer()==null) return true; // Dedicated runs retain their independent observer.
        if(phase==0) {
            ButtonWidget cut=find(screen.uiRuntime().root(),"Cut selection");
            if(cut==null || cut.isEnabled()) throw new IllegalStateException("Cut must be disabled before a selection");
            server=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                previousGameType=player.gameMode.getGameModeForPlayer();
                oldX=player.getX();oldY=player.getY();oldZ=player.getZ();oldYaw=player.getYRot();oldPitch=player.getXRot();
                player.setGameMode(GameType.CREATIVE);
                source=player.blockPosition().above(8); destination=source.offset(4,0,0);
                player.serverLevel().setBlock(source,Blocks.STONE.defaultBlockState(),3);
                player.serverLevel().setBlock(destination.below(),Blocks.DIRT.defaultBlockState(),3);
                GadgetNBT.setCopyStartPos(player.getMainHandItem(),source);
                GadgetNBT.setCopyEndPos(player.getMainHandItem(),source);
                player.containerMenu.broadcastChanges();
                return true;
            }); phase=1;return false;
        }
        if(server!=null && !server.isDone()) return false;
        if(server!=null) { serverResult=server.join();server=null; }
        if(phase==1) {
            if(!GadgetNBT.getCopyStartPos(mc.player.getMainHandItem()).equals(source)) return false;
            ButtonWidget cut=find(screen.uiRuntime().root(),"Cut selection");
            if(cut==null || !cut.isEnabled()) return false;
            if(++settleTicks<3) return false;settleTicks=0;
            ModifierScreenRoundTrip.captureCurrent("cut-ready.png");
            screen.uiRuntime().root().layoutTree(mc.font,0,0,screen.width,screen.height);
            double x=cut.getX()+cut.getWidth()/2.0,y=cut.getY()+cut.getHeight()/2.0;
            screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);
            phase=2;return false;
        }
        if(phase==2) {
            if(!GadgetNBT.hasCopyUUID(mc.player.getMainHandItem()) || !screen.toolSettings().kind().equals("paste")) return false;
            if(!GadgetNBT.getMode(mc.player.getMainHandItem()).getId().getPath().equals("paste")) throw new IllegalStateException("Cut did not activate Paste");
            if(find(screen.uiRuntime().root(),"Cut selection")!=null) throw new IllegalStateException("Cut action remains after acceptance");
            if(++settleTicks<3) return false;settleTicks=0;
            ModifierScreenRoundTrip.captureCurrent("cut-paste-ready.png");
            phase=3;return false;
        }
        if(phase==3) {
            server=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if(ServerTickHandler.gadgetWorking(GadgetNBT.getUUID(player.getMainHandItem())) || !player.serverLevel().isEmptyBlock(source)) return false;
                player.teleportTo(destination.getX()+0.5,destination.getY()+2,destination.getZ()+0.5);
                player.setYRot(0);player.setXRot(90);player.setShiftKeyDown(false);
                // Native BG2 raytraces at partialTick=0, so set the fixture's previous pose too.
                player.xo=player.getX();player.yo=player.getY();player.zo=player.getZ();player.xRotO=90;player.yRotO=0;
                var hit=com.direwolf20.buildinggadgets2.util.VectorHelper.getLookingAt(player,player.getMainHandItem());
                if(!hit.getBlockPos().equals(destination.below())) throw new IllegalStateException("Fixture aim missed destination: "+hit);
                ((BuildersMultitool)player.getMainHandItem().getItem()).use(player.serverLevel(),player,InteractionHand.MAIN_HAND);
                if(GadgetNBT.hasCopyUUID(player.getMainHandItem())) throw new IllegalStateException("Paste did not consume the cut buffer");
                player.containerMenu.broadcastChanges();
                return true;
            });phase=4;return false;
        }
        if(phase==4) {
            // A false result means the native cut animation is still running.
            server=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                return !GadgetNBT.hasCopyUUID(player.getMainHandItem());
            });phase=5;return false;
        }
        if(phase==5) {
            if(GadgetNBT.hasCopyUUID(mc.player.getMainHandItem())) { phase=3;return false; }
            server=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if(!player.serverLevel().getBlockState(destination).is(Blocks.STONE)) return false;
                if(!player.serverLevel().isEmptyBlock(source)) throw new IllegalStateException("Cut left the original block behind");
                if(!GadgetNBT.getMode(player.getMainHandItem()).getId().getPath().equals("cut")) throw new IllegalStateException("Paste did not return to selection");
                player.serverLevel().removeBlock(destination,false);player.serverLevel().removeBlock(destination.below(),false);
                player.teleportTo(oldX,oldY,oldZ);player.setYRot(oldYaw);player.setXRot(oldPitch);player.setGameMode(previousGameType);
                return true;
            });phase=6;return false;
        }
        if(phase==6) {
            if(!screen.toolSettings().kind().equals("copy")) return false;
            if(!serverResult) { phase=5;return false; }
            ButtonWidget cut=find(screen.uiRuntime().root(),"Cut selection");
            if(cut==null || cut.isEnabled()) throw new IllegalStateException("After paste, cut must wait for new corners");
            if(++settleTicks<3) return false;
            System.out.println("[BGE cut flow] actual block cut, pasted once, and selection restored");
            ModifierScreenRoundTrip.captureCurrent("cut-after-paste.png");return true;
        }
        return false;
    }
    private static ButtonWidget find(UIComponent root,String label) {
        if(root instanceof ButtonWidget b && b.getLabel().getString().equals(label)) return b;
        for(UIComponent child:root.children()) { ButtonWidget found=find(child,label);if(found!=null)return found; }
        return null;
    }
}
