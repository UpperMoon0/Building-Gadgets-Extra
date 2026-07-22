package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class MultitoolCutPacket {
    public static void encode(MultitoolCutPacket packet, FriendlyByteBuf buffer) {}
    public static MultitoolCutPacket decode(FriendlyByteBuf buffer) { return new MultitoolCutPacket(); }
    public static void handle(MultitoolCutPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ItemStack stack = BaseGadget.getGadget(player);
            if (!(stack.getItem() instanceof BuildersMultitool) || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE) return;
            ((GadgetCutPaste) Registration.CutPaste_Gadget.get()).cutAndStore(player, stack);
        });
        context.setPacketHandled(true);
    }
}
