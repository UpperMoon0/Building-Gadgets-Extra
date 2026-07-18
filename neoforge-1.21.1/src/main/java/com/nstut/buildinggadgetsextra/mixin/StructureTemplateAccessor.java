package com.nstut.buildinggadgetsextra.mixin;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {
    @Accessor("palettes")
    List<StructureTemplate.Palette> buildingGadgetsExtra$getPalettes();

    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> buildingGadgetsExtra$getEntities();

    @Accessor("size")
    void buildingGadgetsExtra$setSize(Vec3i size);
}
