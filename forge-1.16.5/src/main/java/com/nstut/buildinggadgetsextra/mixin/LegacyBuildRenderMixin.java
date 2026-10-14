package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.client.renders.BuildRender;
import com.nstut.buildinggadgetsextra.transform.LegacyLivePlan;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.util.Iterator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(value=BuildRender.class,remap=false)
public abstract class LegacyBuildRenderMixin {
    @Unique private BlockPos bge$current;
    @Redirect(method="render",at=@At(value="INVOKE",target="Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<BlockPos> bge$track(List<BlockPos> positions) {
        return LegacyLivePlan.trackingIterator(positions, pos -> bge$current = pos);
    }
    @ModifyArg(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/BlockRendererDispatcher;renderBlock(Lnet/minecraft/block/BlockState;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IILnet/minecraftforge/client/model/data/IModelData;)V",remap=false),index=0)
    private BlockState bge$state(BlockState state) { return LegacyLivePlan.state(state,bge$current); }
}
