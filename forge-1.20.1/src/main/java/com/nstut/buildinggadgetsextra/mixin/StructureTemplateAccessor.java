package com.nstut.buildinggadgetsextra.mixin;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateAccessor {
    @Shadow(aliases = "f_74482_") @Final private List<StructureTemplate.Palette> palettes;
    @Shadow(aliases = "f_74483_") @Final private List<StructureTemplate.StructureEntityInfo> entityInfoList;
    @Shadow(aliases = "f_74484_") private Vec3i size;

    public List<StructureTemplate.Palette> buildingGadgetsExtra$getPalettes() { return palettes; }
    public List<StructureTemplate.StructureEntityInfo> buildingGadgetsExtra$getEntities() { return entityInfoList; }
    public void buildingGadgetsExtra$setSize(Vec3i value) { size = value; }
}
