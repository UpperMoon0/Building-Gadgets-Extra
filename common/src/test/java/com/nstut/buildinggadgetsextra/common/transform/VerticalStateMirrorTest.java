package com.nstut.buildinggadgetsextra.common.transform;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VerticalStateMirrorTest {
    @Test
    void recognizesCommonVerticalValuePairs() {
        assertEquals("down", VerticalValueMirror.replacement("UP"));
        assertEquals("top", VerticalValueMirror.replacement("bottom"));
        assertEquals("lower", VerticalValueMirror.replacement("upper"));
        assertEquals("ceiling", VerticalValueMirror.replacement("floor"));
        assertNull(VerticalValueMirror.replacement("north"));
        assertNull(VerticalValueMirror.replacement(null));
    }

    @Test
    void swapsUpDownPropertiesAndVerticalFacingValues() {
        Property up = new Property("up", Boolean.class, true, false);
        Property down = new Property("down", Boolean.class, true, false);
        Property half = new Property("half", String.class, "top", "bottom");
        Property face = new Property("face", String.class, "floor", "ceiling", "wall");
        Property facing = new Property("facing", Direction.class, Direction.UP, Direction.DOWN, Direction.NORTH);
        State state = new State()
                .with(up, true).with(down, false).with(half, "top")
                .with(face, "floor").with(facing, Direction.UP);

        State mirrored = VerticalStateMirror.mirror(state, new Adapter());
        assertEquals(false, mirrored.values.get(up));
        assertEquals(true, mirrored.values.get(down));
        assertEquals("bottom", mirrored.values.get(half));
        assertEquals("ceiling", mirrored.values.get(face));
        assertEquals(Direction.DOWN, mirrored.values.get(facing));
    }

    private enum Direction { UP, DOWN, NORTH }

    private static final class Property {
        private final String name;
        private final Class<?> type;
        private final Object[] values;
        private Property(String name, Class<?> type, Object... values) {
            this.name = name;
            this.type = type;
            this.values = values;
        }
    }

    private static final class State {
        private final LinkedHashMap<Property, Object> values;
        private State() { values = new LinkedHashMap<>(); }
        private State(LinkedHashMap<Property, Object> values) { this.values = values; }
        private State with(Property property, Object value) { values.put(property, value); return this; }
        private State set(Property property, Object value) {
            LinkedHashMap<Property, Object> copy = new LinkedHashMap<>(values);
            copy.put(property, value);
            return new State(copy);
        }
    }

    private static final class Adapter implements VerticalStateMirror.Adapter<State, Property> {
        @Override public Iterable<Property> properties(State state) { return state.values.keySet(); }
        @Override public Property property(State state, String name) {
            for (Property property : state.values.keySet()) if (property.name.equals(name)) return property;
            return null;
        }
        @Override public boolean sameValueType(Property first, Property second) { return first.type == second.type; }
        @Override public Object value(State state, Property property) { return state.values.get(property); }
        @Override public String valueName(Property property, Object value) {
            return value instanceof Enum ? ((Enum<?>) value).name().toLowerCase() : String.valueOf(value);
        }
        @Override public Object valueByName(Property property, String name) {
            for (Object value : property.values) if (valueName(property, value).equals(name)) return value;
            return null;
        }
        @Override public State set(State state, Property property, Object value) { return state.set(property, value); }
        @Override public boolean isVerticalDirection(Object value) {
            return value == Direction.UP || value == Direction.DOWN;
        }
        @Override public Object oppositeDirection(Object value) {
            return value == Direction.UP ? Direction.DOWN : Direction.UP;
        }
    }
}
