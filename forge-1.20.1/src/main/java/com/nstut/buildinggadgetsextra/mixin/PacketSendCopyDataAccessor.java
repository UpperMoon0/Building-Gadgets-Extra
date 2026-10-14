package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.network.packets.PacketSendCopyData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.UUID;

@Mixin(value = PacketSendCopyData.class, remap = false)
public interface PacketSendCopyDataAccessor {
    @Accessor("gadgetUUID") UUID buildingGadgetsExtra$getGadgetId();
    @Accessor("copyUUID") UUID buildingGadgetsExtra$getCopyId();
    @Accessor("tag") CompoundTag buildingGadgetsExtra$getTag();
}
