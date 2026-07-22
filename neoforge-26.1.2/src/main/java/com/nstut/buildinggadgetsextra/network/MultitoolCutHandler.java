package com.nstut.buildinggadgetsextra.network;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MultitoolCutHandler {
    private MultitoolCutHandler() {}
    public static void handle(MultitoolCutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack stack = BaseGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof BuildersMultitool)
                    || MultitoolState.getActiveMode(stack) != MultitoolMode.CUT_PASTE) return;
            Registration.CutPaste_Gadget.get().cutAndStore(context.player(), stack);
        });
    }
}
