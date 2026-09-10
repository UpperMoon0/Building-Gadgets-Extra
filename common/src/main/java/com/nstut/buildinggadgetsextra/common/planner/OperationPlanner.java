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

    private static LinkedHashMap<PlanPos, PlannedPosition> mirrorStage(
            LinkedHashMap<PlanPos, PlannedPosition> input,
            PlanPos anchor,
            MirrorAxis axis,
            int maxPositions) {
        LinkedHashMap<PlanPos, PlannedPosition> output = new LinkedHashMap<PlanPos, PlannedPosition>();
        for (Map.Entry<PlanPos, PlannedPosition> entry : input.entrySet()) {
            putBounded(output, entry.getKey(), entry.getValue(), maxPositions);
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
