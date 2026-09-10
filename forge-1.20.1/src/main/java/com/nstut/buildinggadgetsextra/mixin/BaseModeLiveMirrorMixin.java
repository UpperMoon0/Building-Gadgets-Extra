package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
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

        // BG2 mode collections are local offsets from `start`; BuildingUtils applies `start`
        // when rendering/placing. Mirror around the local operation origin, never a world coordinate.
        MirrorAxis horizontalAxis = player.getDirection().getAxis() == Direction.Axis.X
                ? MirrorAxis.Z : MirrorAxis.X;
        ArrayList<PlanPos> positions = new ArrayList<>(source.size());
        for (StatePos entry : source) {
            positions.add(new PlanPos(entry.pos.getX(), entry.pos.getY(), entry.pos.getZ()));
        }

        final List<PlannedPosition> planned;
        try {
            planned = OperationPlanner.mirrorCopies(
                    positions, PlanPos.ZERO, horizontalAxis, horizontal, vertical,
                    ExtraConstants.MAX_LIVE_PLAN_POSITIONS);
        } catch (IllegalArgumentException rejectedPlan) {
            return;
        }

        BaseMode mode = (BaseMode) (Object) this;
        ArrayList<StatePos> resolved = new ArrayList<>(planned.size());
        for (PlannedPosition plannedPosition : planned) {
            StatePos original = source.get(plannedPosition.sourceIndex());
            BlockState resolvedState = original.state;
            for (MirrorAxis axis : plannedPosition.stateMirrors()) {
                resolvedState = MirrorTransforms.mirrorState(resolvedState, plane(axis));
            }

            PlanPos position = plannedPosition.position();
            BlockPos localPos = new BlockPos(position.x(), position.y(), position.z());
            if (!plannedPosition.stateMirrors().isEmpty()) {
                BlockPos worldPos = start.offset(localPos);
                if (!mode.isPosValid(player.level(), player, worldPos, resolvedState)) continue;
            }
            resolved.add(new StatePos(resolvedState, localPos));
        }
        cir.setReturnValue(resolved);
    }

    private static MirrorPlane plane(MirrorAxis axis) {
        switch (axis) {
            case X: return MirrorPlane.X;
            case Y: return MirrorPlane.Y;
            case Z: return MirrorPlane.Z;
            default: throw new IllegalArgumentException("Unknown mirror axis " + axis);
        }
    }
}
