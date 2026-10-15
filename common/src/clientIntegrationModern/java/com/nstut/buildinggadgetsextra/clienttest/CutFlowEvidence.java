package com.nstut.buildinggadgetsextra.clienttest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;

/** Machine-readable completion proof, separate from the generic range test pass marker. */
final class CutFlowEvidence {
    private CutFlowEvidence() {}
    static Path directory() { return Paths.get(System.getProperty("bge.clientIntegrationResultDir","build/client-integration")); }
    static void start(String role) {
        try {
            Files.createDirectories(directory());
            Files.deleteIfExists(directory().resolve(role+"-pass.txt"));
            Files.deleteIfExists(directory().resolve(role+"-fail.txt"));
            Files.deleteIfExists(directory().resolve("cut-flow-"+role+".json"));
            Files.deleteIfExists(directory().resolve("cut-flow-"+role+"-fail.txt"));
            if(role.equals("client")) for(String name:new String[]{"cut-first-corner.png","cut-ready.png","cut-paste-ready.png","cut-after-paste.png"})
                Files.deleteIfExists(directory().resolve(name));
        } catch(java.io.IOException e) { throw new IllegalStateException("Cannot reset cut flow evidence",e); }
    }
    static void fail(String role,String detail) {
        try { Files.writeString(directory().resolve("cut-flow-"+role+"-fail.txt"),detail); }
        catch(java.io.IOException e) { throw new IllegalStateException("Cannot save cut flow failure",e); }
    }
    static void pass(String role,UUID gadget,UUID buffer,boolean dedicated,List<String> steps) {
        JsonObject proof=new JsonObject();proof.addProperty("schema",1);proof.addProperty("scenario","cut-paste");
        proof.addProperty("role",role);proof.addProperty("gadget_id",gadget.toString());proof.addProperty("buffer_id",buffer.toString());
        proof.addProperty("mode",dedicated?"dedicated":"integrated");
        JsonArray done=new JsonArray();for(String step:steps) done.add(step);proof.add("steps",done);
        try { Files.writeString(directory().resolve("cut-flow-"+role+".json"),proof.toString()); }
        catch(java.io.IOException e) { throw new IllegalStateException("Cannot save cut flow proof",e); }
    }
}
