package com.nstut.buildinggadgetsextra.common.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One resolved position plus enough provenance to reproduce state transforms in a loader adapter. */
public final class PlannedPosition {
    private final int sourceIndex;
    private final PlanPos position;
    private final List<MirrorAxis> stateMirrors;
    private final List<StateTransform> stateTransforms;
    private final boolean generated;

    PlannedPosition(int sourceIndex, PlanPos position, List<MirrorAxis> stateMirrors) {
        this(sourceIndex, position, stateMirrors, Collections.<StateTransform>emptyList(), false);
    }

    private PlannedPosition(int sourceIndex, PlanPos position, List<MirrorAxis> stateMirrors,
                            List<StateTransform> stateTransforms, boolean generated) {
        this.sourceIndex = sourceIndex;
        this.position = position;
        this.stateMirrors = Collections.unmodifiableList(new ArrayList<MirrorAxis>(stateMirrors));
        this.stateTransforms = Collections.unmodifiableList(new ArrayList<StateTransform>(stateTransforms));
        this.generated = generated;
    }

    static PlannedPosition source(int sourceIndex, PlanPos position) {
        return new PlannedPosition(sourceIndex, position, Collections.<MirrorAxis>emptyList());
    }

    PlannedPosition mirrored(PlanPos newPosition, MirrorAxis axis) {
        ArrayList<MirrorAxis> mirrors = new ArrayList<MirrorAxis>(stateMirrors);
        mirrors.add(axis);
        ArrayList<StateTransform> transforms = new ArrayList<StateTransform>(stateTransforms);
        transforms.add(StateTransform.values()[axis.ordinal()]);
        return new PlannedPosition(sourceIndex, newPosition, mirrors, transforms, true);
    }

    PlannedPosition copied(PlanPos newPosition, int quarterTurns) {
        ArrayList<StateTransform> transforms = new ArrayList<StateTransform>(stateTransforms);
        if (quarterTurns != 0) transforms.add(StateTransform.values()[2 + quarterTurns]);
        return new PlannedPosition(sourceIndex, newPosition, stateMirrors, transforms, true);
    }

    public int sourceIndex() { return sourceIndex; }
    public PlanPos position() { return position; }
    public List<MirrorAxis> stateMirrors() { return stateMirrors; }
    public List<StateTransform> stateTransforms() { return stateTransforms; }
    public boolean generated() { return generated; }
}
