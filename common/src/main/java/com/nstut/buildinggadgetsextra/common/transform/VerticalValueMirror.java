package com.nstut.buildinggadgetsextra.common.transform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Names used by vanilla and modded block-state properties that reverse under a vertical mirror. */
public final class VerticalValueMirror {
    private static final Map<String, String> SWAPS;

    static {
        Map<String, String> swaps = new HashMap<String, String>();
        pair(swaps, "up", "down");
        pair(swaps, "top", "bottom");
        pair(swaps, "upper", "lower");
        pair(swaps, "floor", "ceiling");
        SWAPS = Collections.unmodifiableMap(swaps);
    }

    private VerticalValueMirror() {
    }

    public static String replacement(String valueName) {
        return valueName == null ? null : SWAPS.get(valueName.toLowerCase(Locale.ROOT));
    }

    private static void pair(Map<String, String> swaps, String first, String second) {
        swaps.put(first, second);
        swaps.put(second, first);
    }
}
