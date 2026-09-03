package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.network.packets.PacketRangeChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PacketRangeChange.class, remap = false)
public interface PacketRangeChangeAccessor {
    @Accessor("range")
    int buildingGadgetsExtra$getRange();
}
