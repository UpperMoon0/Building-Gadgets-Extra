package com.nstut.buildinggadgetsextra.clienttest;

import com.direwolf20.buildinggadgets2.common.worlddata.BG2Data;
import com.direwolf20.buildinggadgets2.util.datatypes.StatePos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.UUID;

/** Network-only fixture: never assign these UUIDs to the held gadget or render its contents. */
public final class ModernCopyTransportFixture {
    public static final UUID GADGET_UUID = UUID.fromString("b6e20000-0000-4000-8000-000000000020");
    public static final UUID COPY_UUID = UUID.fromString("b6e20000-0000-4000-8000-000000100000");
    public static final int POSITION_COUNT = 1_000_000;

    private ModernCopyTransportFixture() {}

    public static CompoundTag millionBlockTemplate() {
        // Let BG2 supply its version-specific palette and position encoding without scanning a world
        // or allocating the serializer's million-entry position map.
        ArrayList<StatePos> seed = new ArrayList<>();
        seed.add(new StatePos(Blocks.AIR.defaultBlockState(), BlockPos.ZERO));
        seed.add(new StatePos(Blocks.STONE.defaultBlockState(), new BlockPos(1, 0, 0)));
        CompoundTag tag = BG2Data.statePosListToNBTMapArray(seed);
        ArrayList<StatePos> corner = new ArrayList<>();
        corner.add(new StatePos(Blocks.STONE.defaultBlockState(), new BlockPos(99, 99, 99)));
        tag.put("endpos", BG2Data.statePosListToNBTMapArray(corner).get("endpos"));
        int[] indices = new int[POSITION_COUNT];
        for (int index = 0; index < indices.length; index++) indices[index] = index & 1;
        tag.putIntArray("statelist", indices);
        return tag;
    }
}
