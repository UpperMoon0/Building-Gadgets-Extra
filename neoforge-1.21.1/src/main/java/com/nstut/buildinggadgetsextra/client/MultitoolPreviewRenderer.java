package com.nstut.buildinggadgetsextra.client;

import com.direwolf20.buildinggadgets2.client.events.RenderLevelLast;
import com.direwolf20.buildinggadgets2.client.renderer.DestructionRenderer;
import com.direwolf20.buildinggadgets2.client.renderer.VBORenderer;
import com.direwolf20.buildinggadgets2.setup.Registration;
import com.direwolf20.buildinggadgets2.util.GadgetNBT;
import com.nstut.buildinggadgetsextra.common.MultitoolMode;
import com.nstut.buildinggadgetsextra.item.MultitoolState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class MultitoolPreviewRenderer {
    private MultitoolPreviewRenderer() {
    }

    public static void render(RenderLevelStageEvent event, Player player, ItemStack multitool) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        MultitoolMode mode = MultitoolState.getActiveMode(multitool);
        Item delegate = switch (mode) {
            case BUILD -> Registration.Building_Gadget.get();
            case EXCHANGING -> Registration.Exchanging_Gadget.get();
            case COPY_PASTE -> Registration.CopyPaste_Gadget.get();
            case CUT_PASTE -> Registration.CutPaste_Gadget.get();
            case DESTRUCTION -> Registration.Destruction_Gadget.get();
        };
        ItemStack view = new ItemStack(delegate);
        view.applyComponents(multitool.getComponentsPatch());

        if (mode == MultitoolMode.DESTRUCTION) {
            DestructionRenderer.render(event, player, view);
        } else {
            VBORenderer.buildRender(event, player, view);
            VBORenderer.drawRender(event, player, view);
        }

        BlockPos anchor = GadgetNBT.getAnchorPos(multitool);
        if (anchor != null && !anchor.equals(GadgetNBT.nullPos)) {
            RenderLevelLast.renderSelectedBlock(event, anchor);
        }
    }
}
