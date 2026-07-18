package com.nstut.buildinggadgetsextra.common.transform;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MirrorEngineTest {
    private static final Adapter ADAPTER = new Adapter();

    @Test
    void mirrorsEveryCoordinatePlaneAndMovesBlockEntities() {
        for (MirrorPlane plane : MirrorPlane.values()) {
            Pos original = new Pos(2, 3, 4);
            Entity entity = new Entity(original);
            List<Block> result = MirrorEngine.transform(
                    Collections.singletonList(new Block(original, "stairs")),
                    Collections.singletonList(entity), plane, ADAPTER);
            Pos expected = mirror(original, plane);
            assertEquals(Collections.singletonList(new Block(expected, "stairs:" + plane)), result);
            assertEquals(expected, entity.position);
        }
    }

    @Test
    void mirrorsUnmatchedBlockEntityPositionDirectly() {
        Entity entity = new Entity(new Pos(8, 9, 10));
        MirrorEngine.transform(Collections.singletonList(new Block(new Pos(1, 2, 3), "stone")),
                Collections.singletonList(entity), MirrorPlane.Y, ADAPTER);
        assertEquals(new Pos(8, -9, 10), entity.position);
    }

    @Test
    void transformsMapKeysAndValues() {
        Map<Pos, String> input = new HashMap<>();
        input.put(new Pos(1, 2, 3), "north");
        Map<Pos, String> result = MirrorEngine.transformMap(input, MirrorPlane.Z,
                new MirrorEngine.MapAdapter<String, Pos>() {
                    @Override public Pos mirrorPosition(Pos position, MirrorPlane plane) {
                        return mirror(position, plane);
                    }
                    @Override public String mirrorBlock(String block, MirrorPlane plane) {
                        return block + ":" + plane;
                    }
                });
        assertEquals(Collections.singletonMap(new Pos(1, 2, -3), "north:Z"), result);
    }

    private static Pos mirror(Pos position, MirrorPlane plane) {
        switch (plane) {
            case X: return new Pos(-position.x, position.y, position.z);
            case Y: return new Pos(position.x, -position.y, position.z);
            case Z: return new Pos(position.x, position.y, -position.z);
            default: throw new AssertionError(plane);
        }
    }

    private static final class Adapter implements MirrorEngine.Adapter<Block, Entity, Pos> {
        @Override public Pos blockPosition(Block block) { return block.position; }
        @Override public Pos blockEntityPosition(Entity entity) { return entity.position; }
        @Override public Pos mirrorPosition(Pos position, MirrorPlane plane) { return mirror(position, plane); }
        @Override public Block mirrorBlock(Block block, Pos newPosition, MirrorPlane plane) {
            return new Block(newPosition, block.state + ":" + plane);
        }
        @Override public void moveBlockEntity(Entity entity, Pos newPosition) { entity.position = newPosition; }
    }

    private static final class Pos {
        private final int x, y, z;
        private Pos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Pos)) return false;
            Pos pos = (Pos) other;
            return x == pos.x && y == pos.y && z == pos.z;
        }
        @Override public int hashCode() { return Objects.hash(x, y, z); }
        @Override public String toString() { return x + "," + y + "," + z; }
    }

    private static final class Block {
        private final Pos position;
        private final String state;
        private Block(Pos position, String state) { this.position = position; this.state = state; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Block)) return false;
            Block block = (Block) other;
            return position.equals(block.position) && state.equals(block.state);
        }
        @Override public int hashCode() { return Objects.hash(position, state); }
        @Override public String toString() { return state + "@" + position; }
    }

    private static final class Entity {
        private Pos position;
        private Entity(Pos position) { this.position = position; }
    }
}
