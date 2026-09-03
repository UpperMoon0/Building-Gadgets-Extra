package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.packets.PacketDestructionRanges;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * BG2 1.0.8 only accepts PacketDestructionRanges when the held item is a concrete GadgetDestruction.
 * The Builder's Multitool extends GadgetCopyPaste and therefore every destruction-range edit was
 * silently discarded. Handle the packet for the multitool while retaining BG2's GUI bounds and
 * requiring the destruction profile to be active.
 */
@Mixin(value = PacketDestructionRanges.class, remap = false)
public abstract class PacketDestructionRangesMultitoolMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void buildingGadgetsExtra$handleMultitoolDestructionRanges(
            PacketDestructionRanges packet, Supplier<NetworkEvent.Context> supplier, CallbackInfo ci) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender == null) return;

        ItemStack held = BaseGadget.getGadget(sender);
        if (!(held.getItem() instanceof BuildersMultitool)) return;

        PacketDestructionRangesAccessor values = (PacketDestructionRangesAccessor) (Object) packet;
        int left = values.buildingGadgetsExtra$getLeft();
        int right = values.buildingGadgetsExtra$getRight();
        int up = values.buildingGadgetsExtra$getUp();
        int down = values.buildingGadgetsExtra$getDown();
        int depth = values.buildingGadgetsExtra$getDepth();

        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(sender);
            if (!(stack.getItem() instanceof BuildersMultitool)) return;
            if (MultitoolState.getActiveMode(stack) != MultitoolMode.DESTRUCTION) return;
            if (!buildingGadgetsExtra$isValidRange(left, right, up, down, depth)) return;

            GadgetNBT.setToolValue(stack, left, "left");
            GadgetNBT.setToolValue(stack, right, "right");
            GadgetNBT.setToolValue(stack, up, "up");
            GadgetNBT.setToolValue(stack, down, "down");
            GadgetNBT.setToolValue(stack, depth, "depth");
            sender.containerMenu.broadcastChanges();
        });

        context.setPacketHandled(true);
        ci.cancel();
    }

    private static boolean buildingGadgetsExtra$isValidRange(int left, int right, int up, int down, int depth) {
        if (left < 0 || right < 0 || up < 0 || down < 0 || depth < 0) return false;
        if (left > 16 || right > 16 || up > 16 || down > 16 || depth > 16) return false;
        return left + right <= 16 && up + down <= 16;
    }
}
