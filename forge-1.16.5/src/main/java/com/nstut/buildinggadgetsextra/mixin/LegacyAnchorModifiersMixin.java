package com.nstut.buildinggadgetsextra.mixin;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.common.planner.StateTransform;
import com.nstut.buildinggadgetsextra.transform.LegacyLivePlan;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=GadgetUtils.class,remap=false)
public abstract class LegacyAnchorModifiersMixin {
    @Inject(method="setAnchor(Lnet/minecraft/item/ItemStack;Ljava/util/List;)V",at=@At("HEAD"))
    private static void bge$save(ItemStack stack, List<BlockPos> coordinates, CallbackInfo ci) {
        if(!(stack.getItem() instanceof BuildersMultitool)) return;
        ListNBT entries=new ListNBT();
        if(coordinates.size()<=ExtraConstants.MAX_LIVE_PLAN_POSITIONS) for(BlockPos pos:coordinates) {
            CompoundNBT entry=new CompoundNBT(); entry.putLong("Pos",pos.asLong());
            List<StateTransform> ops=pos instanceof LegacyLivePlan.Position?((LegacyLivePlan.Position)pos).transforms:Collections.<StateTransform>emptyList();
            int[] ids=new int[ops.size()]; for(int i=0;i<ids.length;i++) ids[i]=ops.get(i).ordinal();
            entry.putIntArray("Ops",ids); entries.add(entry);
        }
        stack.getOrCreateTag().put("BGEAnchorTransforms",entries);
    }
    @Inject(method="getAnchor",at=@At("RETURN"),cancellable=true)
    private static void bge$restore(ItemStack stack, CallbackInfoReturnable<Optional<List<BlockPos>>> cir) {
        if(!(stack.getItem() instanceof BuildersMultitool) || !cir.getReturnValue().isPresent()) return;
        List<BlockPos> positions=cir.getReturnValue().get();
        ListNBT entries=stack.getOrCreateTag().getList("BGEAnchorTransforms",10);
        if(entries.isEmpty()) return;
        if(positions.size()>ExtraConstants.MAX_LIVE_PLAN_POSITIONS || entries.size()!=positions.size()) { cir.setReturnValue(Optional.empty()); return; }
        List<BlockPos> result=new ArrayList<BlockPos>();
        for(int i=0;i<positions.size();i++) {
            CompoundNBT entry=entries.getCompound(i); int[] ids=entry.getIntArray("Ops");
            if(entry.getLong("Pos")!=positions.get(i).asLong() || ids.length>3) { cir.setReturnValue(Optional.empty()); return; }
            List<StateTransform> ops=new ArrayList<StateTransform>();
            for(int id:ids) {
                if(id<0 || id>=StateTransform.values().length) { cir.setReturnValue(Optional.empty()); return; }
                ops.add(StateTransform.values()[id]);
            }
            result.add(new LegacyLivePlan.Position(positions.get(i),ops));
        }
        cir.setReturnValue(Optional.of(result));
    }
}
