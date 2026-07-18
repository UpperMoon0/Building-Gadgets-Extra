package com.nstut.buildinggadgetsextra.common.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version-neutral mirror traversal. Minecraft and Building Gadgets types are supplied by a small
 * per-version adapter, keeping packet handlers and template stores free of duplicated iteration logic.
 */
public final class MirrorEngine {
    private MirrorEngine() {
    }

    public static <B, E, P> ArrayList<B> transform(
            List<B> blocks,
            List<E> blockEntities,
            MirrorPlane plane,
            Adapter<B, E, P> adapter) {
        ArrayList<B> result = new ArrayList<B>(blocks.size());
        Map<P, P> movedPositions = new HashMap<P, P>();

        for (B block : blocks) {
            P oldPosition = adapter.blockPosition(block);
            P newPosition = adapter.mirrorPosition(oldPosition, plane);
            movedPositions.put(oldPosition, newPosition);
            result.add(adapter.mirrorBlock(block, newPosition, plane));
        }

        if (blockEntities != null) {
            for (E blockEntity : blockEntities) {
                P oldPosition = adapter.blockEntityPosition(blockEntity);
                P newPosition = movedPositions.get(oldPosition);
                if (newPosition == null) {
                    newPosition = adapter.mirrorPosition(oldPosition, plane);
                }
                adapter.moveBlockEntity(blockEntity, newPosition);
            }
        }
        return result;
    }

    public static <B, P> Map<P, B> transformMap(
            Map<P, B> blocks,
            MirrorPlane plane,
            MapAdapter<B, P> adapter) {
        Map<P, B> result = new HashMap<P, B>(blocks.size());
        for (Map.Entry<P, B> entry : blocks.entrySet()) {
            P newPosition = adapter.mirrorPosition(entry.getKey(), plane);
            result.put(newPosition, adapter.mirrorBlock(entry.getValue(), plane));
        }
        return result;
    }

    public interface Adapter<B, E, P> {
        P blockPosition(B block);

        P blockEntityPosition(E blockEntity);

        P mirrorPosition(P position, MirrorPlane plane);

        B mirrorBlock(B block, P newPosition, MirrorPlane plane);

        void moveBlockEntity(E blockEntity, P newPosition);
    }

    public interface MapAdapter<B, P> {
        P mirrorPosition(P position, MirrorPlane plane);

        B mirrorBlock(B block, MirrorPlane plane);
    }
}
