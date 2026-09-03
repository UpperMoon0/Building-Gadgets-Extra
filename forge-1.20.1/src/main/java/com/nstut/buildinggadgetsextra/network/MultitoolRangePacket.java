package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.mojang.logging.LogUtils;
import com.nstut.buildinggadgetsextra.common.DebugInstrumentation;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.OptionalInt;
import java.util.function.Supplier;

/** Server-authoritative range update for the Builder's Multitool on BG2 1.20.1. */
public final class MultitoolRangePacket {
    private static final Logger LOGGER = LogUtils.getLogger();
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
            if (player == null) {
                trace("range-reject", () -> "source=extra-packet reason=no-sender requested=" + packet.range);
                return;
            }
            apply(player, packet.range, "extra-packet");
        });
        context.setPacketHandled(true);
    }

    /**
     * Applies the exact server-side mutation used by the network handler.
     * Kept public so the in-game GameTest can verify both the authoritative item state and
     * the inventory slot update emitted back toward the client.
     */
    public static boolean apply(ServerPlayer player, int requestedRange) {
        return apply(player, requestedRange, "direct");
    }

    public static boolean apply(ServerPlayer player, int requestedRange, String source) {
        ItemStack stack = BaseGadget.getGadget(player);
        if (!(stack.getItem() instanceof BuildersMultitool)) {
            trace("range-reject", () -> "source=" + source + " reason=not-multitool requested=" + requestedRange);
            return false;
        }

        MultitoolMode mode = MultitoolState.getActiveMode(stack);
        int currentRange = GadgetNBT.getToolRange(stack);
        int configuredMax = ExtraConfig.multitoolMaxRange();
        OptionalInt range = MultitoolRangePolicy.resolve(mode, requestedRange, configuredMax);
        if (!range.isPresent()) {
            trace("range-reject", () -> "source=" + source
                    + " reason=unsupported-mode mode=" + mode.serializedName()
                    + " current=" + currentRange
                    + " requested=" + requestedRange
                    + " configuredMax=" + configuredMax);
            return false;
        }

        int resolvedRange = range.getAsInt();
        trace("range-apply", () -> "source=" + source
                + " mode=" + mode.serializedName()
                + " current=" + currentRange
                + " requested=" + requestedRange
                + " resolved=" + resolvedRange
                + " configuredMax=" + configuredMax);

        GadgetNBT.setToolRange(stack, resolvedRange);

        // The radial screen is not a container menu, so explicitly publish the changed held stack.
        // This guarantees the client copy used when the screen is reopened observes server state.
        player.containerMenu.broadcastChanges();
        trace("range-sync", () -> "source=" + source
                + " mode=" + mode.serializedName()
                + " publishedRange=" + GadgetNBT.getToolRange(stack)
                + " mechanism=container-broadcast");
        if (player.connection != null && player.connection.connection.channel() != null
                && player.connection.connection.isConnected()) {
            player.displayClientMessage(Component.translatable(
                    "buildinggadgets2.messages.range_set", resolvedRange), true);
        }
        return true;
    }

    private static void trace(String category, Supplier<String> message) {
        DebugInstrumentation.log(ExtraConfig.debugInstrumentation(), category, message, LOGGER::info);
    }
}
