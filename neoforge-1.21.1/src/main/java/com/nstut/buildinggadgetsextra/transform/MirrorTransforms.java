package com.nstut.buildinggadgetsextra.transform;

import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import com.nstut.buildinggadgetsextra.common.transform.MirrorEngine;
import com.nstut.buildinggadgetsextra.common.transform.MirrorPlane;
import com.nstut.buildinggadgetsextra.common.transform.VerticalStateMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;

public final class MirrorTransforms {
    private static final StatePosAdapter ADAPTER = new StatePosAdapter();

    private MirrorTransforms() {
    }

    public static ArrayList<StatePos> horizontal(
            ArrayList<StatePos> blocks, ArrayList<TagPos> blockEntities, Direction playerFacing) {
        MirrorPlane plane = playerFacing.getAxis() == Direction.Axis.X ? MirrorPlane.Z : MirrorPlane.X;
        return MirrorEngine.transform(blocks, blockEntities, plane, ADAPTER);
    }

    public static ArrayList<StatePos> vertical(
            ArrayList<StatePos> blocks, ArrayList<TagPos> blockEntities) {
        return MirrorEngine.transform(blocks, blockEntities, MirrorPlane.Y, ADAPTER);
    }

    private static final class StatePosAdapter implements MirrorEngine.Adapter<StatePos, TagPos, BlockPos>,
            VerticalStateMirror.Adapter<BlockState, Property<?>> {
        @Override
        public BlockPos blockPosition(StatePos block) {
            return block.pos;
        }

        @Override
        public BlockPos blockEntityPosition(TagPos blockEntity) {
            return blockEntity.pos;
        }

        @Override
        public BlockPos mirrorPosition(BlockPos position, MirrorPlane plane) {
            return switch (plane) {
                case X -> new BlockPos(-position.getX(), position.getY(), position.getZ());
                case Y -> new BlockPos(position.getX(), -position.getY(), position.getZ());
                case Z -> new BlockPos(position.getX(), position.getY(), -position.getZ());
            };
        }

        @Override
        public StatePos mirrorBlock(StatePos block, BlockPos newPosition, MirrorPlane plane) {
            BlockState state = switch (plane) {
                case X -> block.state.mirror(Mirror.FRONT_BACK);
                case Y -> VerticalStateMirror.mirror(block.state, this);
                case Z -> block.state.mirror(Mirror.LEFT_RIGHT);
            };
            return new StatePos(state, newPosition);
        }

        @Override
        public void moveBlockEntity(TagPos blockEntity, BlockPos newPosition) {
            blockEntity.pos = newPosition;
        }

        @Override public Iterable<Property<?>> properties(BlockState state) { return state.getProperties(); }
        @Override public Property<?> property(BlockState state, String name) {
            return state.getBlock().getStateDefinition().getProperty(name);
        }
        @Override public boolean sameValueType(Property<?> first, Property<?> second) {
            return first.getValueClass().equals(second.getValueClass());
        }
        @Override public Object value(BlockState state, Property<?> property) { return get(state, property); }
        @Override public String valueName(Property<?> property, Object value) { return name(property, value); }
        @Override public Object valueByName(Property<?> property, String name) { return property.getValue(name).orElse(null); }
        @Override public BlockState set(BlockState state, Property<?> property, Object value) {
            return setUnchecked(state, property, value);
        }
        @Override public boolean isVerticalDirection(Object value) {
            return value instanceof Direction && ((Direction) value).getAxis() == Direction.Axis.Y;
        }
        @Override public Object oppositeDirection(Object value) { return ((Direction) value).getOpposite(); }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Object get(BlockState state, Property property) { return state.getValue(property); }
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static String name(Property property, Object value) { return property.getName((Comparable) value); }
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static BlockState setUnchecked(BlockState state, Property property, Object value) {
            return property.getPossibleValues().contains(value) ? state.setValue(property, (Comparable) value) : state;
        }
    }
}
