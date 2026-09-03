package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.OptionalInt;
import java.util.function.Supplier;

/** Server-authoritative range update for the Builder's Multitool on BG2 1.20.1. */
public final class MultitoolRangePacket {
    private final int range;

    public MultitoolRangePacket(int range) {
        this.range = range;
    }

    public static void encode(MultitoolRangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.range);
    }

    public static MultitoolRangePacket decode(FriendlyByteBuf buffer) {
        return new MultitoolRangePacket(buffer.readVarInt());
    }

    public static void handle(MultitoolRangePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) apply(player, packet.range);
        });
        context.setPacketHandled(true);
    }

    /**
     * Applies the exact server-side mutation used by the network handler.
     * Kept public so the in-game GameTest can verify both the authoritative item state and
     * the inventory slot update emitted back toward the client.
     */
    public static boolean apply(ServerPlayer player, int requestedRange) {
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)) return false;

        OptionalInt range = MultitoolRangePolicy.resolve(
                MultitoolState.getActiveMode(stack), requestedRange, ExtraConfig.multitoolMaxRange());
        if (!range.isPresent()) return false;

        GadgetNBT.setToolRange(stack, range.getAsInt());

        // The radial screen is not a container menu, so explicitly publish the changed held stack.
        // This guarantees the client copy used when the screen is reopened observes server state.
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "buildinggadgets2.messages.range_set", range.getAsInt()), true);
        return true;
    }
}
