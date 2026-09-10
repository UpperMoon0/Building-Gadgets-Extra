package com.nstut.buildinggadgetsextra.common.planner;

import java.util.Objects;

/** Immutable Minecraft-independent integer block position used by the shared operation planner. */
public final class PlanPos {
    public static final PlanPos ZERO = new PlanPos(0, 0, 0);

    private final int x;
    private final int y;
    private final int z;

    public PlanPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public int coordinate(MirrorAxis axis) {
        switch (axis) {
            case X: return x;
            case Y: return y;
            case Z: return z;
            default: throw new IllegalArgumentException("Unsupported axis: " + axis);
        }
    }

    public PlanPos withCoordinate(MirrorAxis axis, int value) {
        switch (axis) {
            case X: return new PlanPos(value, y, z);
            case Y: return new PlanPos(x, value, z);
            case Z: return new PlanPos(x, y, value);
            default: throw new IllegalArgumentException("Unsupported axis: " + axis);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PlanPos)) return false;
        PlanPos that = (PlanPos) other;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "PlanPos{" + x + "," + y + "," + z + "}";
    }
}
