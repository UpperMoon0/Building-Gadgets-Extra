package com.nstut.buildinggadgetsextra.common.transform;

/** Version-neutral vertical block-state transformation backed by a Minecraft-version adapter. */
public final class VerticalStateMirror {
    private VerticalStateMirror() {
    }

    public static <S, P> S mirror(S state, Adapter<S, P> adapter) {
        S result = swapNamedProperties(state, "up", "down", adapter);
        for (P property : adapter.properties(state)) {
            Object value = adapter.value(result, property);
            if (adapter.isVerticalDirection(value)) {
                result = adapter.set(result, property, adapter.oppositeDirection(value));
                continue;
            }

            String replacement = VerticalValueMirror.replacement(adapter.valueName(property, value));
            if (replacement != null) {
                Object replacementValue = adapter.valueByName(property, replacement);
                if (replacementValue != null) result = adapter.set(result, property, replacementValue);
            }
        }
        return result;
    }

    private static <S, P> S swapNamedProperties(
            S state, String firstName, String secondName, Adapter<S, P> adapter) {
        P first = adapter.property(state, firstName);
        P second = adapter.property(state, secondName);
        if (first == null || second == null || !adapter.sameValueType(first, second)) return state;

        Object firstValue = adapter.value(state, first);
        Object secondValue = adapter.value(state, second);
        return adapter.set(adapter.set(state, first, secondValue), second, firstValue);
    }

    public interface Adapter<S, P> {
        Iterable<P> properties(S state);

        P property(S state, String name);

        boolean sameValueType(P first, P second);

        Object value(S state, P property);

        String valueName(P property, Object value);

        Object valueByName(P property, String name);

        S set(S state, P property, Object value);

        boolean isVerticalDirection(Object value);

        Object oppositeDirection(Object value);
    }
}
