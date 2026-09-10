package com.nstut.buildinggadgetsextra.common.planner;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationPlannerTest {
    @Test
    void noMirrorDeduplicatesWithoutChangingSourceProvenance() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Arrays.asList(new PlanPos(1, 2, 3), new PlanPos(1, 2, 3), new PlanPos(2, 2, 3)),
                new PlanPos(0, 0, 0), MirrorAxis.X, false, false, 8);

        assertEquals(2, planned.size());
        assertEquals(0, planned.get(0).sourceIndex());
        assertEquals(2, planned.get(1).sourceIndex());
        assertEquals(Collections.emptyList(), planned.get(0).stateMirrors());
    }

    @Test
    void horizontalMirrorCopiesAcrossAnchorPlane() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Collections.singletonList(new PlanPos(12, 5, 20)),
                new PlanPos(10, 5, 20), MirrorAxis.X, true, false, 8);

        assertEquals(2, planned.size());
        assertEquals(new PlanPos(12, 5, 20), planned.get(0).position());
        assertEquals(new PlanPos(8, 5, 20), planned.get(1).position());
        assertEquals(Collections.singletonList(MirrorAxis.X), planned.get(1).stateMirrors());
    }

    @Test
    void verticalMirrorCopiesAcrossAnchorPlane() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Collections.singletonList(new PlanPos(4, 9, -2)),
                new PlanPos(4, 7, -2), MirrorAxis.Z, false, true, 8);

        assertEquals(new PlanPos(4, 5, -2), planned.get(1).position());
        assertEquals(Collections.singletonList(MirrorAxis.Y), planned.get(1).stateMirrors());
    }

    @Test
    void combinedMirrorsProduceAllQuadrantsAndPreserveTransformOrder() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Collections.singletonList(new PlanPos(2, 3, 0)),
                new PlanPos(0, 0, 0), MirrorAxis.X, true, true, 8);

        assertEquals(4, planned.size());
        assertEquals(new PlanPos(2, 3, 0), planned.get(0).position());
        assertEquals(new PlanPos(-2, 3, 0), planned.get(1).position());
        assertEquals(new PlanPos(2, -3, 0), planned.get(2).position());
        assertEquals(new PlanPos(-2, -3, 0), planned.get(3).position());
        assertEquals(Arrays.asList(MirrorAxis.X, MirrorAxis.Y), planned.get(3).stateMirrors());
    }

    @Test
    void positionsOnMirrorPlaneKeepOriginalStateTransform() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Collections.singletonList(new PlanPos(3, 4, 5)),
                new PlanPos(3, 4, 5), MirrorAxis.Z, true, true, 8);

        assertEquals(1, planned.size());
        assertEquals(Collections.emptyList(), planned.get(0).stateMirrors());
    }

    @Test
    void limitIsAppliedAfterDeduplication() {
        assertThrows(OperationPlanner.PlanLimitExceededException.class, () ->
                OperationPlanner.mirrorCopies(
                        Arrays.asList(new PlanPos(1, 0, 0), new PlanPos(2, 0, 0)),
                        new PlanPos(0, 0, 0), MirrorAxis.X, true, false, 3));
    }

    @Test
    void coordinateOverflowIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                OperationPlanner.mirrorCopies(
                        Collections.singletonList(new PlanPos(-1, 0, 0)),
                        new PlanPos(Integer.MAX_VALUE, 0, 0), MirrorAxis.X, true, false, 8));
    }

    @Test
    void negativeCoordinatesMirrorExactly() {
        List<PlannedPosition> planned = OperationPlanner.mirrorCopies(
                Collections.singletonList(new PlanPos(-7, 1, -5)),
                new PlanPos(-4, 1, -5), MirrorAxis.X, true, false, 8);

        assertEquals(new PlanPos(-1, 1, -5), planned.get(1).position());
    }
}
