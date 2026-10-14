package com.nstut.buildinggadgetsextra.common.planner;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LiveModifiersTest {
    private List<PlannedPosition> plan(PlanPos source, boolean mirror, LiveModifiers settings, int cap) {
        return OperationPlanner.compose(Collections.singletonList(source),PlanPos.ZERO,MirrorAxis.X,mirror,false,settings,cap);
    }
    @Test void arrayPreservesSourceAndNegativeOffsetsWithoutDuplicateCosts() {
        List<PlannedPosition> p=plan(new PlanPos(2,3,4),false,new LiveModifiers(3,-5,2,0,1,0,0,0),10);
        assertEquals(Arrays.asList(new PlanPos(2,3,4),new PlanPos(-3,5,4),new PlanPos(-8,7,4)),positions(p));
        assertFalse(p.get(0).generated()); assertTrue(p.get(1).generated());
        assertEquals(0,p.get(2).sourceIndex()); assertEquals(Collections.emptyList(),p.get(2).stateTransforms());
        assertEquals(1,plan(new PlanPos(2,3,4),false,new LiveModifiers(16,0,0,0,1,0,0,0),1).size());
    }
    @Test void quarterTurnRadialProducesCardinalCopiesAndFacingTransforms() {
        List<PlannedPosition> p=plan(new PlanPos(2,3,0),false,new LiveModifiers(1,0,0,0,4,0,0,0),4);
        assertEquals(Arrays.asList(new PlanPos(2,3,0),new PlanPos(0,3,2),new PlanPos(-2,3,0),new PlanPos(0,3,-2)),positions(p));
        assertEquals(Collections.singletonList(StateTransform.ROTATE_90),p.get(1).stateTransforms());
        assertEquals(Collections.singletonList(StateTransform.ROTATE_180),p.get(2).stateTransforms());
        assertEquals(Collections.singletonList(StateTransform.ROTATE_270),p.get(3).stateTransforms());
    }
    @Test void pivotAndArbitraryRadialCountsAreDeterministicAndDeduplicated() {
        LiveModifiers settings=new LiveModifiers(1,0,0,0,7,-2,3,0);
        List<PlannedPosition> p=plan(new PlanPos(5,0,-4),false,settings,20);
        assertEquals(7,p.size()); assertEquals(positions(p),positions(plan(new PlanPos(5,0,-4),false,settings,20)));
        assertEquals(p.size(),new HashSet<PlanPos>(positions(p)).size());
        assertEquals(1,plan(new PlanPos(-2,1,3),false,settings,1).size());
    }
    @Test void stageOrderChangesGeometryAndPreservesOrderedStateProvenance() {
        List<PlannedPosition> a=plan(new PlanPos(2,0,0),true,new LiveModifiers(2,5,0,0,1,0,0,0),8);
        List<PlannedPosition> b=plan(new PlanPos(2,0,0),true,new LiveModifiers(2,5,0,0,1,0,0,2),8);
        assertTrue(positions(a).contains(new PlanPos(3,0,0))); assertFalse(positions(b).contains(new PlanPos(3,0,0)));
        for(int order=0;order<6;order++) {
            List<PlannedPosition> p=plan(new PlanPos(2,1,3),true,new LiveModifiers(2,7,0,0,4,0,0,order),64);
            assertEquals(p.size(),new HashSet<PlanPos>(positions(p)).size());
            assertEquals(new PlanPos(2,1,3),p.get(0).position());
            assertTrue(p.stream().anyMatch(e->e.stateTransforms().contains(StateTransform.MIRROR_X)));
            assertTrue(p.stream().anyMatch(e->e.stateTransforms().contains(StateTransform.ROTATE_90)));
        }
    }
    @Test void overlapRetainsOriginalSourceStateBeforeGeneratedCopies() {
        List<PlannedPosition> p=OperationPlanner.compose(Arrays.asList(new PlanPos(2,0,0),new PlanPos(-2,0,0)),PlanPos.ZERO,MirrorAxis.X,true,false,new LiveModifiers(1,0,0,0,4,0,0,0),4);
        assertEquals(1,p.get(1).sourceIndex()); assertEquals(Collections.emptyList(),p.get(1).stateTransforms());
    }
    @Test void rejectsOverflowAndExcessiveOutputBeforeReturningPartialPlans() {
        assertThrows(IllegalArgumentException.class,()->plan(new PlanPos(Integer.MAX_VALUE,0,0),false,new LiveModifiers(2,1,0,0,1,0,0,0),10));
        assertThrows(IllegalArgumentException.class,()->plan(new PlanPos(Integer.MIN_VALUE,0,0),false,new LiveModifiers(1,0,0,0,4,64,0,0),10));
        assertThrows(OperationPlanner.PlanLimitExceededException.class,()->plan(new PlanPos(5,1,2),true,new LiveModifiers(16,7,0,0,16,0,0,0),10));
    }
    @Test void rejectsHostileParameters() {
        assertThrows(IllegalArgumentException.class,()->new LiveModifiers(0,0,0,0,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new LiveModifiers(17,0,0,0,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new LiveModifiers(1,65,0,0,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new LiveModifiers(1,0,0,0,17,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new LiveModifiers(1,0,0,0,1,0,0,6));
        assertThrows(IllegalArgumentException.class,()->LiveModifiers.from(new int[100]));
    }
    private List<PlanPos> positions(List<PlannedPosition> plan) {
        List<PlanPos> result=new ArrayList<PlanPos>(); for(PlannedPosition entry:plan) result.add(entry.position()); return result;
    }
}
