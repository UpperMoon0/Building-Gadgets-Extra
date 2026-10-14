package com.nstut.buildinggadgetsextra.common.planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared, Minecraft-independent operation planner. The first release uses the mirror stage;
 * later modifiers (array/radial/masks/palettes) can extend the same deterministic pipeline.
 */
public final class OperationPlanner {
    private OperationPlanner() {}

    public static List<PlannedPosition> mirrorCopies(
            List<PlanPos> source,
            PlanPos anchor,
            MirrorAxis horizontalAxis,
            boolean horizontal,
            boolean vertical,
            int maxPositions) {
        if (source == null) throw new IllegalArgumentException("source cannot be null");
        if (anchor == null) throw new IllegalArgumentException("anchor cannot be null");
        if (maxPositions < 1) throw new IllegalArgumentException("maxPositions must be positive");
        if (horizontal && horizontalAxis != MirrorAxis.X && horizontalAxis != MirrorAxis.Z) {
            throw new IllegalArgumentException("horizontalAxis must be X or Z");
        }

        LinkedHashMap<PlanPos, PlannedPosition> current = new LinkedHashMap<PlanPos, PlannedPosition>();
        for (int i = 0; i < source.size(); i++) {
            PlanPos position = source.get(i);
            if (position == null) throw new IllegalArgumentException("source contains null position");
            putBounded(current, position, PlannedPosition.source(i, position), maxPositions);
        }

        if (horizontal) current = mirrorStage(current, anchor, horizontalAxis, maxPositions);
        if (vertical) current = mirrorStage(current, anchor, MirrorAxis.Y, maxPositions);
        return new ArrayList<PlannedPosition>(current.values());
    }

    /** Compose the three stages in the user's order. Existing positions always win overlaps. */
    public static List<PlannedPosition> compose(List<PlanPos> source, PlanPos anchor, MirrorAxis axis,
                                               boolean horizontal, boolean vertical, LiveModifiers settings,
                                               int maxPositions) {
        if (settings == null) throw new IllegalArgumentException("missing modifier settings");
        List<PlannedPosition> originals = mirrorCopies(source, anchor, axis, false, false, maxPositions);
        LinkedHashMap<PlanPos, PlannedPosition> current = new LinkedHashMap<PlanPos, PlannedPosition>();
        for (PlannedPosition entry : originals) current.put(entry.position(), entry);
        int[][] orders = {{0,1,2}, {0,2,1}, {1,0,2}, {1,2,0}, {2,0,1}, {2,1,0}};
        for (int stage : orders[settings.value(7)]) {
            if (stage == 0) {
                if (horizontal) {
                    if (axis != MirrorAxis.X && axis != MirrorAxis.Z) throw new IllegalArgumentException("invalid horizontal axis");
                    current = mirrorStage(current, anchor, axis, maxPositions);
                }
                if (vertical) current = mirrorStage(current, anchor, MirrorAxis.Y, maxPositions);
            } else {
                LinkedHashMap<PlanPos, PlannedPosition> output = new LinkedHashMap<PlanPos, PlannedPosition>(current);
                int count = settings.value(stage == 1 ? 0 : 4);
                for (int copy = 1; copy < count; copy++) {
                    for (PlannedPosition entry : current.values()) {
                        PlanPos pos = entry.position();
                        int turns = 0;
                        if (stage == 1) {
                            pos = new PlanPos(checked((long) pos.x() + (long) copy * settings.value(1)),
                                    checked((long) pos.y() + (long) copy * settings.value(2)),
                                    checked((long) pos.z() + (long) copy * settings.value(3)));
                        } else {
                            long pivotX = (long) anchor.x() + settings.value(5);
                            long pivotZ = (long) anchor.z() + settings.value(6);
                            long dx = (long) pos.x() - pivotX, dz = (long) pos.z() - pivotZ;
                            double angle = 2.0 * StrictMath.PI * copy / count;
                            // Minecraft clockwise Y rotation: (x,z) -> (-z,x).
                            double x = dx * StrictMath.cos(angle) - dz * StrictMath.sin(angle);
                            double z = dx * StrictMath.sin(angle) + dz * StrictMath.cos(angle);
                            pos = new PlanPos(checked(pivotX + StrictMath.round(x)), pos.y(),
                                    checked(pivotZ + StrictMath.round(z)));
                            turns = (int) StrictMath.floor(4.0 * copy / count + 0.5) % 4;
                        }
                        putBounded(output, pos, entry.copied(pos, turns), maxPositions);
                    }
                }
                current = output;
            }
        }
        return new ArrayList<PlannedPosition>(current.values());
    }

    private static int checked(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IllegalArgumentException("modifier coordinate overflow");
        return (int) value;
    }

    private static LinkedHashMap<PlanPos, PlannedPosition> mirrorStage(
            LinkedHashMap<PlanPos, PlannedPosition> input,
            PlanPos anchor,
            MirrorAxis axis,
            int maxPositions) {
        LinkedHashMap<PlanPos, PlannedPosition> output = new LinkedHashMap<PlanPos, PlannedPosition>();
        for (Map.Entry<PlanPos, PlannedPosition> entry : input.entrySet()) {
            putBounded(output, entry.getKey(), entry.getValue(), maxPositions);
        }
        // Preserve every existing candidate before adding copies, including its state provenance.
        for (Map.Entry<PlanPos, PlannedPosition> entry : input.entrySet()) {
            PlanPos mirroredPosition = mirrorPosition(entry.getKey(), anchor, axis);
            putBounded(output, mirroredPosition, entry.getValue().mirrored(mirroredPosition, axis), maxPositions);
        }
        return output;
    }

    private static PlanPos mirrorPosition(PlanPos position, PlanPos anchor, MirrorAxis axis) {
        long reflected = 2L * anchor.coordinate(axis) - position.coordinate(axis);
        if (reflected < Integer.MIN_VALUE || reflected > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("mirror coordinate overflow on " + axis);
        }
        return position.withCoordinate(axis, (int) reflected);
    }

    private static void putBounded(
            LinkedHashMap<PlanPos, PlannedPosition> output,
            PlanPos position,
            PlannedPosition value,
            int maxPositions) {
        if (output.containsKey(position)) return;
        if (output.size() >= maxPositions) {
            throw new PlanLimitExceededException(maxPositions);
        }
        output.put(position, value);
    }

    public static final class PlanLimitExceededException extends IllegalArgumentException {
        private final int maxPositions;

        public PlanLimitExceededException(int maxPositions) {
            super("operation plan exceeds maximum of " + maxPositions + " positions");
            this.maxPositions = maxPositions;
        }

        public int maxPositions() { return maxPositions; }
    }
}
