package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.modes.BaseMode;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.planner.MirrorAxis;
import com.nstut.buildinggadgetsextra.common.planner.OperationPlanner;
import com.nstut.buildinggadgetsextra.common.planner.PlanPos;
import com.nstut.buildinggadgetsextra.common.planner.PlannedPosition;
import com.nstut.buildinggadgetsextra.common.transform.MirrorPlane;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.transform.MirrorTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BaseMode.class, remap = false)
public abstract class BaseModeLiveMirrorMixin {
    @Inject(method = "collect", at = @At("RETURN"), cancellable = true)
    private void bge$applyLiveMirror(Direction hitSide, Player player, BlockPos start, BlockState state,
                                     CallbackInfoReturnable<ArrayList<StatePos>> cir) {
        ItemStack gadget = BaseGadget.getGadget(player);
        if (!(gadget.getItem() instanceof BuildersMultitool)) return;
        MultitoolMode tool = MultitoolState.getActiveMode(gadget);
        if (tool != MultitoolMode.BUILD && tool != MultitoolMode.EXCHANGING) return;
        boolean horizontal = MultitoolState.isLiveMirrorHorizontal(gadget);
        boolean vertical = MultitoolState.isLiveMirrorVertical(gadget);
        if (!horizontal && !vertical) return;

        ArrayList<StatePos> source = cir.getReturnValue();
        if (source == null || source.isEmpty()) return;
        BlockPos anchor = GadgetNBT.getAnchorPos(gadget);
        if (anchor == null || GadgetNBT.nullPos.equals(anchor)) anchor = start;
        MirrorAxis horizontalAxis = player.getDirection().getAxis() == Direction.Axis.X ? MirrorAxis.Z : MirrorAxis.X;

        ArrayList<PlanPos> positions = new ArrayList<>(source.size());
        for (StatePos entry : source) positions.add(new PlanPos(entry.pos.getX(), entry.pos.getY(), entry.pos.getZ()));

        final List<PlannedPosition> planned;
        try {
            planned = OperationPlanner.mirrorCopies(positions,
                    new PlanPos(anchor.getX(), anchor.getY(), anchor.getZ()), horizontalAxis,
                    horizontal, vertical, ExtraConstants.MAX_LIVE_PLAN_POSITIONS);
        } catch (IllegalArgumentException rejectedPlan) {
            return;
        }

        ArrayList<StatePos> resolved = new ArrayList<>(planned.size());
        for (PlannedPosition plannedPosition : planned) {
            StatePos original = source.get(plannedPosition.sourceIndex());
            BlockState resolvedState = original.state;
            for (MirrorAxis axis : plannedPosition.stateMirrors()) resolvedState = MirrorTransforms.mirrorState(resolvedState, plane(axis));
            PlanPos position = plannedPosition.position();
            resolved.add(new StatePos(resolvedState, new BlockPos(position.x(), position.y(), position.z())));
        }
        cir.setReturnValue(resolved);
    }

    private static MirrorPlane plane(MirrorAxis axis) {
        return switch (axis) {
            case X -> MirrorPlane.X;
            case Y -> MirrorPlane.Y;
            case Z -> MirrorPlane.Z;
        };
    }
}
