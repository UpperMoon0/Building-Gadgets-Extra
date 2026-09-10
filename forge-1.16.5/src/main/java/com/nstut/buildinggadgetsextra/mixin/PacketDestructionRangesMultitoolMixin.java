package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.packets.PacketDestructionGUI;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;

@Mixin(value = PacketDestructionGUI.Handler.class, remap = false)
public abstract class PacketDestructionRangesMultitoolMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$destruction(PacketDestructionGUI packet, Supplier<NetworkEvent.Context> supplier, CallbackInfo ci) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayerEntity player = context.getSender();
        if (player == null || !(AbstractGadget.getGadget(player).getItem() instanceof BuildersMultitool)) return;
        PacketDestructionRangesAccessor values = (PacketDestructionRangesAccessor) packet;
        context.enqueueWork(() -> {
            ItemStack stack = AbstractGadget.getGadget(player);
            if (!(stack.getItem() instanceof BuildersMultitool)
                    || MultitoolState.getActiveMode(stack) != MultitoolMode.DESTRUCTION) return;
            int left = values.buildingGadgetsExtra$getLeft(), right = values.buildingGadgetsExtra$getRight();
            int up = values.buildingGadgetsExtra$getUp(), down = values.buildingGadgetsExtra$getDown();
            int depth = values.buildingGadgetsExtra$getDepth();
            if (!MultitoolRangePolicy.validDestruction(left, right, up, down, depth,
                    ExtraConfig.multitoolDestructionSide(), ExtraConfig.multitoolDestructionSpan())) return;
            GadgetDestruction.setToolValue(stack, left, "left");
            GadgetDestruction.setToolValue(stack, right, "right");
            GadgetDestruction.setToolValue(stack, up, "up");
            GadgetDestruction.setToolValue(stack, down, "down");
            GadgetDestruction.setToolValue(stack, depth, "depth");
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
        ci.cancel();
    }
}

