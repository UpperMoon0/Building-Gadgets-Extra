package com.nstut.buildinggadgetsextra.clienttest;

import net.minecraft.client.Minecraft;

/** Do not let initial resource loading replace an in-progress test connection screen. */
final class ClientStartupGate {
    private Object previousScreen;
    private int stableTicks;
    private boolean copyGuiVerified;
    boolean ready(Minecraft minecraft) {
        if(minecraft.getOverlay()!=null || minecraft.screen==null) { previousScreen=null;stableTicks=0;return false; }
        if(previousScreen!=minecraft.screen) { previousScreen=minecraft.screen;stableTicks=0; }
        if(++stableTicks<20) return false;
        if(!copyGuiVerified) {
            // Force BG2's CopyGUI class through the Mixin transformer during real client integration.
            // Without this gate a broken injection can remain dormant until a player opens that GUI.
            try {
                Class.forName("com.direwolf20.buildinggadgets2.client.screen.CopyGUI", true,
                        ClientStartupGate.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Building Gadgets 2 CopyGUI missing", failure);
            }
            copyGuiVerified=true;
        }
        return true;
    }
    static String describe(Minecraft minecraft) {
        return "; screen="+(minecraft.screen==null?"none":minecraft.screen.getClass().getSimpleName())
                +" overlay="+(minecraft.getOverlay()==null?"none":minecraft.getOverlay().getClass().getSimpleName());
    }
}
