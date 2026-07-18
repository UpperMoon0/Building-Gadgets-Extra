package com.nstut.buildinggadgetsextra.transform;

import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.google.common.collect.ImmutableMap;
import com.nstut.buildinggadgetsextra.common.transform.MirrorEngine;
import com.nstut.buildinggadgetsextra.common.transform.MirrorPlane;
import com.nstut.buildinggadgetsextra.common.transform.VerticalStateMirror;
import com.nstut.buildinggadgetsextra.mixin.TemplateAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public final class TemplateTransforms {
    private static final LegacyAdapter ADAPTER = new LegacyAdapter();

    private TemplateTransforms() {
    }

    public static Template vertical(Template template) {
        TemplateAccessor accessor = (TemplateAccessor) (Object) template;
        if (accessor.buildingGadgetsExtra$getMap().isEmpty()) return template;
        Map<BlockPos, BlockData> mirrored = MirrorEngine.transformMap(
                accessor.buildingGadgetsExtra$getMap(), MirrorPlane.Y, ADAPTER);

        Region.Builder bounds = Region.enclosingBuilder();
        for (BlockPos position : mirrored.keySet()) bounds.enclose(position);
        TemplateHeader header = TemplateHeader.builderOf(
                accessor.buildingGadgetsExtra$getHeader(), bounds.build()).build();
        return new Template(ImmutableMap.copyOf(mirrored), header).normalize();
    }

    private static final class LegacyAdapter implements MirrorEngine.MapAdapter<BlockData, BlockPos>,
            VerticalStateMirror.Adapter<BlockState, Property<?>> {
        @Override
        public BlockPos mirrorPosition(BlockPos position, MirrorPlane plane) {
            return new BlockPos(position.getX(), -position.getY(), position.getZ());
        }

        @Override
        public BlockData mirrorBlock(BlockData block, MirrorPlane plane) {
            return new BlockData(VerticalStateMirror.mirror(block.getState(), this), block.getTileData());
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
