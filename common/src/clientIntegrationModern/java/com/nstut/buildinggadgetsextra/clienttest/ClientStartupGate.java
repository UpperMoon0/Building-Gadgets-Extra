package com.nstut.buildinggadgetsextra.clienttest;

import net.minecraft.client.Minecraft;

/** Do not let initial resource loading replace an in-progress test connection screen. */
final class ClientStartupGate {
    private Object previousScreen;
    private int stableTicks;
    boolean ready(Minecraft minecraft) {
        if(minecraft.getOverlay()!=null || minecraft.screen==null) { previousScreen=null;stableTicks=0;return false; }
        if(previousScreen!=minecraft.screen) { previousScreen=minecraft.screen;stableTicks=0; }
        return ++stableTicks>=20;
    }
    static String describe(Minecraft minecraft) {
        return "; screen="+(minecraft.screen==null?"none":minecraft.screen.getClass().getSimpleName())
                +" overlay="+(minecraft.getOverlay()==null?"none":minecraft.getOverlay().getClass().getSimpleName());
    }
}
