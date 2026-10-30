package com.nstut.buildinggadgetsextra.mixin;

import com.direwolf20.buildinggadgets2.client.screen.CopyGUI;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiIncrementer;
import com.direwolf20.buildinggadgets2.client.screen.widgets.GuiTextFieldBase;
import com.nstut.buildinggadgetsextra.item.BuildersMultitool;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * BG2 1.0.8 sends copy coordinates on every incrementer change. In relative mode that immediately
 * folds Start X/Y/Z into the stored origin and resets the visible Start fields to zero, which makes
 * the controls look non-editable. The original Building Gadgets GUI deferred these edits until
 * Confirm. Keep upstream behavior for upstream gadgets and restore that interaction for the multitool.
 *
 * BG2's incrementer also leaves its integer backing value unchanged when the text box is typed into;
 * synchronize every valid draft before BG2 reads it for confirmation or converts coordinate modes.
 */
@Mixin(value = CopyGUI.class, remap = false)
public abstract class CopyGUIMultitoolMixin {
    @Shadow private ItemStack copyPasteTool;
    @Shadow private List<GuiIncrementer> fields;
    @Shadow private boolean absoluteCoords;
    @Shadow private BlockPos startPos;

    @Unique private BlockPos buildingGadgetsExtra$pendingWorldStart;
    @Unique private BlockPos buildingGadgetsExtra$pendingWorldEnd;

    @Inject(method = "onChange", at = @At("HEAD"), cancellable = true)
    private void buildingGadgetsExtra$deferMultitoolCopyChanges(int value, CallbackInfo ci) {
        if (copyPasteTool.getItem() instanceof BuildersMultitool) {
            ci.cancel();
        }
    }

    // Both methods are owned by BG2, so the remap=false Mixin stays valid in production.
    // The mode switch calls updateTextFields immediately afterward and would otherwise
    // replace typed drafts with the incrementers' old cached integers.
    @Inject(method = "coordsModeSwitch", at = @At("HEAD"))
    private void buildingGadgetsExtra$syncBeforeModeSwitch(CallbackInfo ci) {
        buildingGadgetsExtra$syncAllValidCopyValues();
        if (!(copyPasteTool.getItem() instanceof BuildersMultitool) || fields.size() != 6) return;
        BlockPos shownStart = new BlockPos(fields.get(0).getValue(), fields.get(1).getValue(), fields.get(2).getValue());
        BlockPos shownEnd = new BlockPos(fields.get(3).getValue(), fields.get(4).getValue(), fields.get(5).getValue());
        // BG2 computes relative End from the NEW Start when Confirm is pressed.
        // Its mode toggle instead converts End using the OLD Start; preserve world geometry.
        buildingGadgetsExtra$pendingWorldStart = absoluteCoords ? shownStart : startPos.offset(shownStart);
        buildingGadgetsExtra$pendingWorldEnd = absoluteCoords ? shownEnd : buildingGadgetsExtra$pendingWorldStart.offset(shownEnd);
    }

    @Inject(method = "updateTextFields", at = @At("RETURN"))
    private void buildingGadgetsExtra$restoreModeSwitchGeometry(CallbackInfo ci) {
        if (buildingGadgetsExtra$pendingWorldStart == null || !(copyPasteTool.getItem() instanceof BuildersMultitool)) return;
        BlockPos worldStart = buildingGadgetsExtra$pendingWorldStart;
        BlockPos worldEnd = buildingGadgetsExtra$pendingWorldEnd;
        buildingGadgetsExtra$pendingWorldStart = null;
        buildingGadgetsExtra$pendingWorldEnd = null;
        BlockPos shownStart = absoluteCoords ? worldStart : worldStart.subtract(startPos);
        BlockPos shownEnd = absoluteCoords ? worldEnd : worldEnd.subtract(worldStart);
        int[] values = {shownStart.getX(), shownStart.getY(), shownStart.getZ(),
                shownEnd.getX(), shownEnd.getY(), shownEnd.getZ()};
        for (int index = 0; index < values.length; index++) fields.get(index).setValue(values[index], false);
    }

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void buildingGadgetsExtra$syncBeforeConfirm(CallbackInfo ci) {
        buildingGadgetsExtra$syncAllValidCopyValues();
    }

    private void buildingGadgetsExtra$syncAllValidCopyValues() {
        if (!(copyPasteTool.getItem() instanceof BuildersMultitool)) return;
        for (GuiIncrementer incrementer : fields) {
            GuiTextFieldBase text = ((GuiIncrementerAccessor) (Object) incrementer).buildingGadgetsExtra$getField();
            String visible = text.getValue();
            if (visible == null || visible.isEmpty() || "-".equals(visible)) continue;
            // Numeric input is validated by BG2, but parse explicitly so incomplete drafts
            // cannot silently turn into zero. Keep previous values for invalid drafts.
            try {
                int value = Integer.parseInt(visible);
                if (value != incrementer.getValue()) incrementer.setValue(value, false);
            } catch (NumberFormatException ignored) {
                // Preserve the last committed value rather than corrupting the selection.
            }
        }
    }
}
