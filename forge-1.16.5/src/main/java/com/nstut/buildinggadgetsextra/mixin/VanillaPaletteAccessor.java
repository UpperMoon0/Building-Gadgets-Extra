package com.nstut.buildinggadgetsextra.mixin;

import net.minecraft.world.gen.feature.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;

@Mixin(Template.Palette.class)
public interface VanillaPaletteAccessor {
    @Invoker("<init>") static Template.Palette buildingGadgetsExtra$create(List<Template.BlockInfo> blocks) { throw new AssertionError(); }
}
