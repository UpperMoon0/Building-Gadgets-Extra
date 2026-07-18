package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateHeader;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Template.class, remap = false)
public interface TemplateAccessor {
    @Accessor("map")
    ImmutableMap<BlockPos, BlockData> buildingGadgetsExtra$getMap();

    @Accessor("header")
    TemplateHeader buildingGadgetsExtra$getHeader();
}
