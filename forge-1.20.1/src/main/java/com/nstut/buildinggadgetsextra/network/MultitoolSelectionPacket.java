package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MultitoolSelectionPacket {
    private final int toolOrdinal;
    private final String gadgetMode;
    public MultitoolSelectionPacket(int toolOrdinal, String gadgetMode) { this.toolOrdinal = toolOrdinal; this.gadgetMode = gadgetMode; }
    public static void encode(MultitoolSelectionPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.toolOrdinal); buffer.writeUtf(packet.gadgetMode); }
    public static MultitoolSelectionPacket decode(FriendlyByteBuf buffer) { return new MultitoolSelectionPacket(buffer.readVarInt(), buffer.readUtf()); }
    public static void handle(MultitoolSelectionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack stack = BaseGadget.getGadget(player);
            if (!(stack.getItem() instanceof BuildersMultitool multitool)) return;
            MultitoolMode[] modes = MultitoolMode.values();
            if (packet.toolOrdinal < 0 || packet.toolOrdinal >= modes.length) return;
            MultitoolMode selected = modes[packet.toolOrdinal];
            if (selected != com.nstut.buildinggadgetsextra.item.MultitoolState.getActiveMode(stack)) multitool.selectTool(stack, selected);
            ResourceLocation requested = ResourceLocation.tryParse(packet.gadgetMode);
            if (requested != null) multitool.selectGadgetMode(stack, requested);
        });
        context.setPacketHandled(true);
    }
}
