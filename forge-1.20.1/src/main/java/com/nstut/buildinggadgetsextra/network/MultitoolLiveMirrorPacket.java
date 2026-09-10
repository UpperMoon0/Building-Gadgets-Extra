package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MultitoolLiveMirrorPacket {
    private final boolean vertical;

    public MultitoolLiveMirrorPacket(boolean vertical) {
        this.vertical = vertical;
    }

    public static void encode(MultitoolLiveMirrorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.vertical);
    }

    public static MultitoolLiveMirrorPacket decode(FriendlyByteBuf buffer) {
        return new MultitoolLiveMirrorPacket(buffer.readBoolean());
    }

    public static void handle(MultitoolLiveMirrorPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack stack = BaseGadget.getGadget(player);
            if (!(stack.getItem() instanceof BuildersMultitool)) return;
            MultitoolMode mode = MultitoolState.getActiveMode(stack);
            if (mode != MultitoolMode.BUILD && mode != MultitoolMode.EXCHANGING) return;
            MultitoolState.toggleLiveMirror(stack, packet.vertical);
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
