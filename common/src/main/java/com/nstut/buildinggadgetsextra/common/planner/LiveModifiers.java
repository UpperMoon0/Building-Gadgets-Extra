package com.nstut.buildinggadgetsextra.common.planner;

/** Bounded settings shared by every client, server and persisted profile. Counts include the original. */
public final class LiveModifiers {
    public static final int FIELDS = 8;
    public static final LiveModifiers NONE = new LiveModifiers(1, 0, 0, 0, 1, 0, 0, 0);
    public static final String[] ORDERS = {"Mirror → Array → Radial", "Mirror → Radial → Array",
            "Array → Mirror → Radial", "Array → Radial → Mirror",
            "Radial → Mirror → Array", "Radial → Array → Mirror"};
    private final int[] values;

    public LiveModifiers(int array, int x, int y, int z, int radial, int pivotX, int pivotZ, int order) {
        if (array < 1 || array > 16 || radial < 1 || radial > 16 || order < 0 || order >= 6
                || !offset(x) || !offset(y) || !offset(z) || !offset(pivotX) || !offset(pivotZ)) {
            throw new IllegalArgumentException("invalid modifier settings");
        }
        values = new int[]{array, x, y, z, radial, pivotX, pivotZ, order};
    }
    private static boolean offset(int value) { return value >= -64 && value <= 64; }
    public int value(int field) { return values[field]; }
    public boolean enabled() { return value(0) > 1 || value(4) > 1; }
    public static LiveModifiers from(int[] values) {
        if (values == null || values.length != FIELDS) throw new IllegalArgumentException("invalid modifier fields");
        return new LiveModifiers(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7]);
    }
}
