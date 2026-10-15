package com.nstut.buildinggadgetsextra.common;
import java.util.concurrent.atomic.AtomicReferenceArray;
/** Client-only callers retain the last actual planner rejection for the profile settings view. */
public final class LivePlanFeedback {
    private static final AtomicReferenceArray<String> REASONS=new AtomicReferenceArray<String>(5);
    private LivePlanFeedback() {}
    public static boolean report(int profile,String reason) { return !reason.equals(REASONS.getAndSet(profile,reason)); }
    public static void clear(int profile) { REASONS.set(profile,null); }
    public static String reason(int profile) { return REASONS.get(profile); }
}
