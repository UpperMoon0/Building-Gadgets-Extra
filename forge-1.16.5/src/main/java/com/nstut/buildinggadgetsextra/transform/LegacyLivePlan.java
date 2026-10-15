package com.nstut.buildinggadgetsextra.transform;
import com.direwolf20.buildinggadgets.common.items.modes.AbstractMode;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.planner.*;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.mixin.LegacyModeAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Per-position state provenance survives collection, anchoring, preview and normal upstream placement. */
public final class LegacyLivePlan {
    private LegacyLivePlan() {}
    public static final class Position extends BlockPos {
        public final List<StateTransform> transforms;
        public Position(BlockPos pos, List<StateTransform> transforms) {
            super(pos.getX(), pos.getY(), pos.getZ());
            this.transforms=Collections.unmodifiableList(new ArrayList<StateTransform>(transforms));
        }
    }
    public static Iterator<BlockPos> trackingIterator(List<BlockPos> positions, java.util.function.Consumer<BlockPos> current) {
        final Iterator<BlockPos> iterator = positions.iterator();
        return new Iterator<BlockPos>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public BlockPos next() { BlockPos pos = iterator.next(); current.accept(pos); return pos; }
        };
    }
    public static BlockState state(BlockState state, BlockPos pos) {
        if (!(pos instanceof Position)) return state;
        for(StateTransform transform:((Position)pos).transforms) {
            switch(transform) {
                case MIRROR_X: state=state.mirror(Mirror.FRONT_BACK); break;
                case MIRROR_Y: state=TemplateTransforms.mirrorVerticalState(state); break;
                case MIRROR_Z: state=state.mirror(Mirror.LEFT_RIGHT); break;
                case ROTATE_90: state=state.rotate(Rotation.CLOCKWISE_90); break;
                case ROTATE_180: state=state.rotate(Rotation.CLOCKWISE_180); break;
                case ROTATE_270: state=state.rotate(Rotation.COUNTERCLOCKWISE_90); break;
            }
        }
        return state;
    }
    public static BlockData data(BlockData data, BlockPos pos) {
        return new BlockData(state(data.getState(),pos),data.getTileData());
    }
    public static List<BlockPos> apply(AbstractMode mode, AbstractMode.UseContext context, PlayerEntity player, List<BlockPos> source) {
        ItemStack stack=com.direwolf20.buildinggadgets.common.items.AbstractGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return source;
        MultitoolMode tool=MultitoolState.getActiveMode(stack);
        if (!(stack.getItem() instanceof BuildersMultitool) || (tool!=MultitoolMode.BUILD && tool!=MultitoolMode.EXCHANGING)) return source;
        LiveModifiers settings=MultitoolState.getLiveModifiers(stack);
        boolean h=MultitoolState.isLiveMirrorHorizontal(stack), v=MultitoolState.isLiveMirrorVertical(stack);
        if (!h && !v && !settings.enabled()) { if(context.getWorld().isClientSide) com.nstut.buildinggadgetsextra.common.LivePlanFeedback.clear(tool.ordinal()); return source; }
        BlockPos anchor=mode.withOffset(context.getStartPos(),context.getHitSide(),context.isPlaceOnTop());
        List<PlanPos> positions=new ArrayList<PlanPos>();
        for(BlockPos pos:source) positions.add(new PlanPos(pos.getX()-anchor.getX(),pos.getY()-anchor.getY(),pos.getZ()-anchor.getZ()));
        final List<PlannedPosition> plan;
        try { plan=OperationPlanner.compose(positions,PlanPos.ZERO,player.getDirection().getAxis()==Direction.Axis.X?MirrorAxis.Z:MirrorAxis.X,h,v,settings,ExtraConstants.MAX_LIVE_PLAN_POSITIONS); }
        catch(IllegalArgumentException invalid) {
            if(context.getWorld().isClientSide) {
                String reason=invalid instanceof OperationPlanner.PlanLimitExceededException?"Too many copies. Reduce range/count.":"Copies exceed coordinate bounds.";
                if(com.nstut.buildinggadgetsextra.common.LivePlanFeedback.report(tool.ordinal(),reason)) player.displayClientMessage(new net.minecraft.util.text.StringTextComponent(reason),true);
            }
            return Collections.emptyList();
        }
        if(context.getWorld().isClientSide) com.nstut.buildinggadgetsextra.common.LivePlanFeedback.clear(tool.ordinal());
        List<BlockPos> result=new ArrayList<BlockPos>();
        for(PlannedPosition entry:plan) {
            long x=(long)anchor.getX()+entry.position().x(), y=(long)anchor.getY()+entry.position().y(), z=(long)anchor.getZ()+entry.position().z();
            if(x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || y<Integer.MIN_VALUE || y>Integer.MAX_VALUE || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE) continue;
            Position pos=new Position(new BlockPos((int)x,(int)y,(int)z),entry.stateTransforms());
            if(entry.generated()) {
                if(!context.getWorld().hasChunkAt(pos) || !context.getWorld().getWorldBorder().isWithinBounds(pos)) continue;
                AbstractMode.UseContext resolved=new AbstractMode.UseContext(context.getWorld(),state(context.getSetState(),pos),context.getStartPos(),stack,context.getHitSide(),context.isPlaceOnTop(),context.isConnected());
                if(mode.isExchanging()) {
                    if(!((LegacyModeAccessor)mode).bge$exchangingValidator(pos,context.getWorldState(anchor),resolved)) continue;
                } else if(!mode.validator(player,pos,resolved)) continue;
            }
            result.add(pos);
        }
        return result;
    }
}
