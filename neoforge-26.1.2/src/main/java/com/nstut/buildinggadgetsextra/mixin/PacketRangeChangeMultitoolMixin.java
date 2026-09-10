package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.common.network.data.RangeChangePayload;
import com.direwolf20.buildinggadgets2.common.network.handler.PacketRangeChange;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.mojang.logging.LogUtils;
import com.nstut.buildinggadgetsextra.common.DebugInstrumentation;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.common.MultitoolRangePolicy;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import com.nstut.buildinggadgetsextra.setup.ExtraConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(value = PacketRangeChange.class, remap = false)
public abstract class PacketRangeChangeMultitoolMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$handleMultitool(RangeChangePayload payload, IPayloadContext context,
                                                       CallbackInfo ci) {
        ItemStack held = BaseGadget.getGadget(context.player());
        if (!(held.getItem() instanceof BuildersMultitool)) return;
        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof BuildersMultitool)) {
                trace("range-reject", () -> "source=bg2-range-payload reason=held-item-changed requested=" + payload.range());
                return;
            }

            MultitoolMode mode = MultitoolState.getActiveMode(stack);
            int currentRange = GadgetNBT.getToolRange(stack);
            int configuredMax = ExtraConfig.multitoolMaxRange();
            OptionalInt range = MultitoolRangePolicy.resolve(mode, payload.range(), configuredMax);
            if (!range.isPresent()) {
                trace("range-reject", () -> "source=bg2-range-payload"
                        + " reason=unsupported-mode mode=" + mode.serializedName()
                        + " current=" + currentRange
                        + " requested=" + payload.range()
                        + " configuredMax=" + configuredMax);
                return;
            }

            int resolvedRange = range.getAsInt();
            trace("range-apply", () -> "source=bg2-range-payload"
                    + " mode=" + mode.serializedName()
                    + " current=" + currentRange
                    + " requested=" + payload.range()
                    + " resolved=" + resolvedRange
                    + " configuredMax=" + configuredMax);
            GadgetNBT.setToolRange(stack, resolvedRange);
            trace("range-state", () -> "source=bg2-range-payload"
                    + " mode=" + mode.serializedName()
                    + " serverRange=" + GadgetNBT.getToolRange(stack)
                    + " mutation=complete");
            if (context.player() instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            context.player().sendOverlayMessage(Component.translatable(
                    "buildinggadgets2.messages.range_set", resolvedRange));
        });
        ci.cancel();
    }

    private static void trace(String category, Supplier<String> message) {
        DebugInstrumentation.log(ExtraConfig.debugInstrumentation(), category, message, LOGGER::info);
    }
}
