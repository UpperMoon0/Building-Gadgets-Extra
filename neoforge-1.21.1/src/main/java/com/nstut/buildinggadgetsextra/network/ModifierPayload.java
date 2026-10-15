package com.nstut.buildinggadgetsextra.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.nstut.buildinggadgetsextra.common.planner.LiveModifiers;
public record ModifierPayload(int profile, int[] values) implements CustomPacketPayload {
    public static final Type<ModifierPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("buildinggadgetsextra", "modifiers"));
    public ModifierPayload(int profile, LiveModifiers settings) { this(profile, fields(settings)); }
    private static int[] fields(LiveModifiers settings) {
        int[] values = new int[LiveModifiers.FIELDS]; for (int i=0;i<values.length;i++) values[i]=settings.value(i); return values;
    }
    public static final StreamCodec<ByteBuf, ModifierPayload> STREAM_CODEC = new StreamCodec<>() {
        public ModifierPayload decode(ByteBuf buffer) {
            int profile=buffer.readInt(); int[] values=new int[LiveModifiers.FIELDS];
            for(int i=0;i<values.length;i++) values[i]=buffer.readInt(); return new ModifierPayload(profile,values);
        }
        public void encode(ByteBuf buffer, ModifierPayload value) {
            buffer.writeInt(value.profile()); for(int field:value.values()) buffer.writeInt(field);
        }
    };
    public Type<ModifierPayload> type() { return TYPE; }
}
