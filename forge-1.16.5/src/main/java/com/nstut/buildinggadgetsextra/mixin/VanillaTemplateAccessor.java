package com.nstut.buildinggadgetsextra.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.Template;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;

@Mixin(Template.class)
public abstract class VanillaTemplateAccessor {
    @Shadow(aliases = "field_204769_a") @Final private List<Template.Palette> palettes;
    @Shadow(aliases = "field_186271_b") @Final private List<Template.EntityInfo> entityInfoList;
    @Shadow(aliases = "field_186272_c") private BlockPos size;

    public List<Template.Palette> buildingGadgetsExtra$getPalettes(){return palettes;}
    public List<Template.EntityInfo> buildingGadgetsExtra$getEntities(){return entityInfoList;}
    public void buildingGadgetsExtra$setSize(BlockPos value){size=value;}
}
