package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.network.packets.PacketDestructionGUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PacketDestructionGUI.class, remap = false)
public interface PacketDestructionRangesAccessor {
    @Accessor("left") int buildingGadgetsExtra$getLeft();
    @Accessor("right") int buildingGadgetsExtra$getRight();
    @Accessor("up") int buildingGadgetsExtra$getUp();
    @Accessor("down") int buildingGadgetsExtra$getDown();
    @Accessor("depth") int buildingGadgetsExtra$getDepth();
}

