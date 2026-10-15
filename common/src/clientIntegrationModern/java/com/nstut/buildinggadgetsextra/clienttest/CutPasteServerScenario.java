package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Server prepares isolated terrain, then observes actual network-driven player actions. */
final class CutPasteServerScenario {
    static final BlockPos SOURCE=new BlockPos(5,100,5), DESTINATION=new BlockPos(9,100,5);
    private enum Phase { PREPARE, TYPED, CLEARED, FIRST_CORNER, SECOND_CORNER, CUT, PASTED, RESTORE, FINISHED }
    private static Phase phase=Phase.PREPARE;
    private static int ticks;
    private static UUID gadget,buffer;
    private static GameType originalMode;
    private static boolean originalFlying;
    private static double x,y,z;
    private static float yaw,pitch;
    private static final List<String> steps=new ArrayList<>();
    static void reset() { phase=Phase.PREPARE;ticks=0;gadget=null;buffer=null;steps.clear();CutFlowEvidence.start("server"); }
    static boolean complete() { return phase==Phase.FINISHED; }
    static void tick(ServerPlayer player) {
        if(complete()) return;
        ItemStack held=player.getMainHandItem();
        if(phase==Phase.PREPARE && MultitoolState.getActiveMode(held)!=MultitoolMode.CUT_PASTE) return;
        if(++ticks>1200) fail("timeout phase="+phase);
        // Keep both chunks loaded until the client has observed the move and leaves this profile.
        // GameTest worlds can spawn millions of blocks away; restoring early loses the final updates.
        if(phase==Phase.RESTORE) {
            if(MultitoolState.getActiveMode(held)==MultitoolMode.CUT_PASTE) return;
            player.connection.teleport(x,y,z,yaw,pitch);player.setGameMode(originalMode);
            player.getAbilities().flying=originalFlying;player.onUpdateAbilities();phase=Phase.FINISHED;return;
        }
        ServerLevel world=(ServerLevel)player.level();
        if(phase==Phase.PREPARE) {
            gadget=GadgetNBT.getUUID(held);originalMode=player.gameMode.getGameModeForPlayer();
            x=player.getX();y=player.getY();z=player.getZ();yaw=player.getYRot();pitch=player.getXRot();originalFlying=player.getAbilities().flying;
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            for(int dx=-1;dx<=5;dx++) for(int dy=0;dy<=5;dy++) for(int dz=-1;dz<=1;dz++)
                world.setBlock(SOURCE.offset(dx,dy,dz),Blocks.AIR.defaultBlockState(),3);
            world.setBlock(SOURCE,Blocks.STONE.defaultBlockState(),3);
            world.setBlock(DESTINATION.below(),Blocks.DIRT.defaultBlockState(),3);
            // Selection is deliberately NOT seeded: the real client must send both clicks.
            if(GadgetNBT.hasCopyUUID(held) || !GadgetNBT.getCopyStartPos(held).equals(GadgetNBT.nullPos)
                    || !GadgetNBT.getCopyEndPos(held).equals(GadgetNBT.nullPos)) fail("fixture requires a fresh cut profile");
            aim(player,SOURCE);steps.add("terrain_ready");phase=Phase.TYPED;return;
        }
        if(phase==Phase.TYPED) {
            if(!GadgetNBT.getCopyStartPos(held).equals(SOURCE) || !GadgetNBT.getCopyEndPos(held).equals(SOURCE)) return;
            selectionOnly(held,world);steps.add("typed_coordinates_selection_only");phase=Phase.CLEARED;return;
        }
        if(phase==Phase.CLEARED) {
            if(!GadgetNBT.getCopyStartPos(held).equals(GadgetNBT.nullPos) || !GadgetNBT.getCopyEndPos(held).equals(GadgetNBT.nullPos)) return;
            selectionOnly(held,world);steps.add("clear_selection_only");phase=Phase.FIRST_CORNER;return;
        }
        if(phase==Phase.FIRST_CORNER) {
            if(!GadgetNBT.getCopyStartPos(held).equals(SOURCE)) return;
            if(!GadgetNBT.getCopyEndPos(held).equals(GadgetNBT.nullPos)) fail("second corner arrived before first-corner observation");
            steps.add("first_corner_received");phase=Phase.SECOND_CORNER;return;
        }
        if(phase==Phase.SECOND_CORNER) {
            if(!GadgetNBT.getCopyEndPos(held).equals(SOURCE)) return;
            steps.add("second_corner_received");phase=Phase.CUT;return;
        }
        if(phase==Phase.CUT) {
            if(!GadgetNBT.hasCopyUUID(held)) return;
            if(buffer==null) { buffer=GadgetNBT.getCopyUUID(held);steps.add("cut_accepted"); }
            if(!buffer.equals(GadgetNBT.getCopyUUID(held))) fail("pending cut contents were replaced");
            if(!GadgetNBT.getMode(held).getId().getPath().equals("paste")) fail("server did not activate Paste");
            if(ServerTickHandler.gadgetWorking(gadget) || !world.isEmptyBlock(SOURCE)) return;
            steps.add("source_removed");aim(player,DESTINATION);phase=Phase.PASTED;return;
        }
        if(phase==Phase.PASTED) {
            // A profile round trip must retain the same cut contents.
            if(MultitoolState.getActiveMode(held)!=MultitoolMode.CUT_PASTE) return;
            if(GadgetNBT.hasCopyUUID(held)) {
                if(!buffer.equals(GadgetNBT.getCopyUUID(held))) fail("profile navigation replaced cut contents");
                return;
            }
            if(!world.getBlockState(DESTINATION).is(Blocks.STONE)) return; // Wait for the native render block.
            if(!world.isEmptyBlock(SOURCE)) fail("original block remains after Paste");
            if(!GadgetNBT.getMode(held).getId().getPath().equals("cut")) fail("Paste did not return to Cut");
            steps.add("destination_placed");steps.add("buffer_consumed");steps.add("returned_to_selection");
            CutFlowEvidence.pass("server",gadget,buffer,world.getServer().isDedicatedServer(),steps);phase=Phase.RESTORE;
        }
    }
    private static void selectionOnly(ItemStack held,ServerLevel world) {
        if(GadgetNBT.hasCopyUUID(held) || !GadgetNBT.getMode(held).getId().getPath().equals("cut"))
            fail("coordinate edit/Clear created a Paste buffer without a cut");
        if(!world.getBlockState(SOURCE).is(Blocks.STONE)) fail("coordinate edit/Clear changed the source block");
    }
    private static void aim(ServerPlayer player,BlockPos at) {
        player.setYRot(0);player.setXRot(90);player.yRotO=0;player.xRotO=90;
        player.connection.teleport(at.getX()+0.5,at.getY()+2,at.getZ()+0.5,0,90);
    }
    private static void fail(String detail) { CutFlowEvidence.fail("server",detail);throw new IllegalStateException("Cut flow server: "+detail); }
}
