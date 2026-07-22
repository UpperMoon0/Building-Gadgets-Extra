package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.items.*;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class LegacyMultitoolPacket {
    public static final int SELECT_TOOL = 0, SELECT_ACTION = 1, RANGE = 2, ROTATE = 3, MIRROR = 4,
            UNDO = 5, ANCHOR = 6, FUZZY = 7, CONNECTED = 8, RAYTRACE = 9,
            PLACE_ATOP = 10, DESTROY_OVERLAY = 11, FLUID_ONLY = 12;
    private final int operation;
    private final int value;
    public LegacyMultitoolPacket(int operation, int value) { this.operation = operation; this.value = value; }
    public static void encode(LegacyMultitoolPacket packet, PacketBuffer buffer) { buffer.writeVarInt(packet.operation); buffer.writeVarInt(packet.value); }
    public static LegacyMultitoolPacket decode(PacketBuffer buffer) { return new LegacyMultitoolPacket(buffer.readVarInt(), buffer.readVarInt()); }
    public static void handle(LegacyMultitoolPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) return;
            ItemStack stack = AbstractGadget.getGadget(player);
            if (!(stack.getItem() instanceof BuildersMultitool)) return;
            BuildersMultitool multitool = (BuildersMultitool) stack.getItem();
            MultitoolMode active = MultitoolState.getActiveMode(stack);
            if (packet.operation == SELECT_TOOL) {
                MultitoolMode[] modes = MultitoolMode.values();
                if (packet.value >= 0 && packet.value < modes.length) MultitoolState.selectTool(stack, modes[packet.value]);
                return;
            }
            if (packet.operation == SELECT_ACTION) { MultitoolState.applyProfile(stack, active, packet.value); return; }
            AbstractGadget delegate = delegate(active);
            switch (packet.operation) {
                case RANGE: GadgetUtils.setToolRange(stack, MathHelper.clamp(GadgetUtils.getToolRange(stack) + packet.value, 1, Config.GADGETS.maxRange.get())); break;
                case ROTATE: delegate.onRotate(stack, player); break;
                case MIRROR: delegate.onMirror(stack, player); break;
                case UNDO: multitool.undo(player.level, player, stack); break;
                case ANCHOR: delegate.onAnchor(stack, player); break;
                case FUZZY: AbstractGadget.toggleFuzzy(player, stack); break;
                case CONNECTED: AbstractGadget.toggleConnectedArea(player, stack); break;
                case RAYTRACE: AbstractGadget.toggleRayTraceFluid(player, stack); break;
                case PLACE_ATOP: GadgetBuilding.togglePlaceAtop(player, stack); break;
                case DESTROY_OVERLAY: GadgetDestruction.switchOverlay(player, stack); break;
                case FLUID_ONLY: GadgetDestruction.toggleFluidMode(stack); break;
                default: break;
            }
        });
        context.setPacketHandled(true);
    }
    private static AbstractGadget delegate(MultitoolMode mode) {
        switch (mode) {
            case BUILD: return (AbstractGadget) OurItems.BUILDING_GADGET_ITEM.get();
            case EXCHANGING: return (AbstractGadget) OurItems.EXCHANGING_GADGET_ITEM.get();
            case DESTRUCTION: return (AbstractGadget) OurItems.DESTRUCTION_GADGET_ITEM.get();
            default: return (AbstractGadget) OurItems.COPY_PASTE_GADGET_ITEM.get();
        }
    }
}
