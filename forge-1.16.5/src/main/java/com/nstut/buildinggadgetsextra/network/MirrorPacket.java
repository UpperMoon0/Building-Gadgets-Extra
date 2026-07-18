package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets.common.capability.CapabilityTemplate;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.nstut.buildinggadgetsextra.common.ExtraConstants;
import com.nstut.buildinggadgetsextra.transform.TemplateTransforms;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public final class MirrorPacket {
    private final boolean vertical;

    public MirrorPacket(boolean vertical) {
        this.vertical = vertical;
    }

    public static void encode(MirrorPacket packet, PacketBuffer buffer) {
        buffer.writeBoolean(packet.vertical);
    }

    public static MirrorPacket decode(PacketBuffer buffer) {
        return new MirrorPacket(buffer.readBoolean());
    }

    public static void handle(MirrorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> apply(packet.vertical, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void apply(boolean vertical, ServerPlayerEntity player) {
        if (player == null) return;

        ItemStack gadget = AbstractGadget.getGadget(player);
        if (!(gadget.getItem() instanceof GadgetCopyPaste)) return;

        player.level.getCapability(CapabilityTemplate.TEMPLATE_PROVIDER_CAPABILITY).ifPresent(provider ->
                gadget.getCapability(CapabilityTemplate.TEMPLATE_KEY_CAPABILITY).ifPresent(key -> {
                    provider.setTemplate(key, vertical
                            ? TemplateTransforms.vertical(provider.getTemplateForKey(key))
                            : provider.getTemplateForKey(key).mirror(player.getDirection().getAxis()));
                    provider.requestRemoteUpdate(key, PacketDistributor.PLAYER.with(() -> player));
                    player.displayClientMessage(new TranslationTextComponent(vertical
                            ? ExtraConstants.MIRRORED_VERTICAL : ExtraConstants.MIRRORED_HORIZONTAL), true);
                }));
    }
}
