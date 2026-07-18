package com.nstut.buildinggadgetsextra.transform;

import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import com.direwolf20.buildinggadgets2.util.datatypes.TagPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MirrorTransforms {
    private static final Map<String, String> VERTICAL_VALUE_SWAPS = Map.of(
            "up", "down",
            "down", "up",
            "top", "bottom",
            "bottom", "top",
            "upper", "lower",
            "lower", "upper",
            "floor", "ceiling",
            "ceiling", "floor");

    private MirrorTransforms() {
    }

    public static ArrayList<StatePos> horizontal(
            ArrayList<StatePos> blocks, ArrayList<TagPos> blockEntities, Direction playerFacing) {
        boolean reflectZ = playerFacing.getAxis() == Direction.Axis.X;
        Mirror stateMirror = reflectZ ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        return transform(blocks, blockEntities,
                pos -> reflectZ
                        ? new BlockPos(pos.getX(), pos.getY(), -pos.getZ())
                        : new BlockPos(-pos.getX(), pos.getY(), pos.getZ()),
                state -> state.mirror(stateMirror));
    }

    public static ArrayList<StatePos> vertical(
            ArrayList<StatePos> blocks, ArrayList<TagPos> blockEntities) {
        return transform(blocks, blockEntities,
                pos -> new BlockPos(pos.getX(), -pos.getY(), pos.getZ()),
                MirrorTransforms::mirrorVertically);
    }

    private static ArrayList<StatePos> transform(
            ArrayList<StatePos> blocks,
            ArrayList<TagPos> blockEntities,
            PositionTransform positionTransform,
            StateTransform stateTransform) {
        ArrayList<StatePos> result = new ArrayList<>(blocks.size());
        Map<BlockPos, BlockPos> movedPositions = new HashMap<>();

        for (StatePos block : blocks) {
            BlockPos newPos = positionTransform.apply(block.pos);
            movedPositions.put(block.pos, newPos);
            result.add(new StatePos(stateTransform.apply(block.state), newPos));
        }

        if (blockEntities != null) {
            for (TagPos blockEntity : blockEntities) {
                blockEntity.pos = movedPositions.getOrDefault(
                        blockEntity.pos, positionTransform.apply(blockEntity.pos));
            }
        }
        return result;
    }

    private static BlockState mirrorVertically(BlockState state) {
        BlockState result = swapNamedProperties(state, "up", "down");
        for (Property<?> property : state.getProperties()) {
            Comparable<?> value = result.getValue(property);
            if (value instanceof Direction direction && direction.getAxis() == Direction.Axis.Y) {
                result = setValue(result, property, direction.getOpposite());
                continue;
            }

            String valueName = getValueName(property, value).toLowerCase(Locale.ROOT);
            String replacementName = VERTICAL_VALUE_SWAPS.get(valueName);
            if (replacementName != null) {
                result = setValueByName(result, property, replacementName);
            }
        }
        return result;
    }

    private static BlockState swapNamedProperties(BlockState state, String firstName, String secondName) {
        Property<?> first = state.getBlock().getStateDefinition().getProperty(firstName);
        Property<?> second = state.getBlock().getStateDefinition().getProperty(secondName);
        if (first == null || second == null || !first.getValueClass().equals(second.getValueClass())) {
            return state;
        }

        Comparable<?> firstValue = state.getValue(first);
        Comparable<?> secondValue = state.getValue(second);
        return setValue(setValue(state, first, secondValue), second, firstValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setValue(BlockState state, Property property, Comparable value) {
        return property.getPossibleValues().contains(value) ? state.setValue(property, value) : state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getValueName(Property property, Comparable value) {
        return property.getName(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setValueByName(BlockState state, Property property, String valueName) {
        return (BlockState) property.getValue(valueName)
                .map(value -> state.setValue(property, (Comparable) value))
                .orElse(state);
    }

    @FunctionalInterface
    private interface PositionTransform {
        BlockPos apply(BlockPos pos);
    }

    @FunctionalInterface
    private interface StateTransform {
        BlockState apply(BlockState state);
    }
}
