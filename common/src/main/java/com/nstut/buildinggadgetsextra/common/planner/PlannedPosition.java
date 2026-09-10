package com.nstut.buildinggadgetsextra.common.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One resolved position plus enough provenance to reproduce state transforms in a loader adapter. */
public final class PlannedPosition {
    private final int sourceIndex;
    private final PlanPos position;
    private final List<MirrorAxis> stateMirrors;

    PlannedPosition(int sourceIndex, PlanPos position, List<MirrorAxis> stateMirrors) {
        this.sourceIndex = sourceIndex;
        this.position = position;
        this.stateMirrors = Collections.unmodifiableList(new ArrayList<MirrorAxis>(stateMirrors));
    }

    static PlannedPosition source(int sourceIndex, PlanPos position) {
        return new PlannedPosition(sourceIndex, position, Collections.<MirrorAxis>emptyList());
    }

    PlannedPosition mirrored(PlanPos newPosition, MirrorAxis axis) {
        ArrayList<MirrorAxis> mirrors = new ArrayList<MirrorAxis>(stateMirrors);
        mirrors.add(axis);
        return new PlannedPosition(sourceIndex, newPosition, mirrors);
    }

    public int sourceIndex() { return sourceIndex; }
    public PlanPos position() { return position; }
    public List<MirrorAxis> stateMirrors() { return stateMirrors; }
}
