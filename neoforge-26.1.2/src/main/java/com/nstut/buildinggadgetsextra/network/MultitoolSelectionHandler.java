package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MultitoolSelectionHandler {
    private MultitoolSelectionHandler() {
    }

    public static void handle(MultitoolSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof BuildersMultitool multitool)) return;
            MultitoolMode[] modes = MultitoolMode.values();
            if (payload.toolOrdinal() < 0 || payload.toolOrdinal() >= modes.length) return;

            MultitoolMode selected = modes[payload.toolOrdinal()];
            multitool.selectTool(stack, selected);
            try {
                if (!payload.gadgetMode().isEmpty()) multitool.selectGadgetMode(stack, Identifier.parse(payload.gadgetMode()));
            } catch (IllegalArgumentException ignored) {
            }
        });
    }
}

