package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.network.packets.PacketDestructionRanges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PacketDestructionRanges.class, remap = false)
public interface PacketDestructionRangesAccessor {
    @Accessor("left") int buildingGadgetsExtra$getLeft();
    @Accessor("right") int buildingGadgetsExtra$getRight();
    @Accessor("up") int buildingGadgetsExtra$getUp();
    @Accessor("down") int buildingGadgetsExtra$getDown();
    @Accessor("depth") int buildingGadgetsExtra$getDepth();
}
