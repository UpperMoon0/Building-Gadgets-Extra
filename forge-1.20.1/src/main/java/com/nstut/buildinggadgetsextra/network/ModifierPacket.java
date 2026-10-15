package com.nstut.buildinggadgetsextra.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
import java.util.function.Supplier;
public final class ModifierPacket {
    private final int profile;
    private final int[] values;
    public ModifierPacket(int profile, LiveModifiers settings) {
        this.profile = profile; this.values = new int[LiveModifiers.FIELDS];
        for (int i = 0; i < values.length; i++) values[i] = settings.value(i);
    }
    private ModifierPacket(int profile, int[] values) { this.profile = profile; this.values = values; }
    public static void encode(ModifierPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.profile);
        for (int value : packet.values) buffer.writeInt(value);
    }
    public static ModifierPacket decode(FriendlyByteBuf buffer) {
        int profile = buffer.readInt(); int[] values = new int[LiveModifiers.FIELDS];
        for (int i = 0; i < values.length; i++) values[i] = buffer.readInt();
        return new ModifierPacket(profile, values);
    }
    public static void handle(ModifierPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ModifierHandler.apply(context.getSender(), packet.profile, packet.values));
        context.setPacketHandled(true);
    }
}
