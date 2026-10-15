package com.nstut.buildinggadgetsextra.client.ui;

import com.nstut.buildinggadgetsextra.common.CutPasteStep;
import com.nstut.openui.api.*;

/** One visible next action, with the same controls as the other tool settings. */
public final class CutPasteControls {
    private CutPasteControls() {}

    public static UIComponent create(CutPasteStep step, Runnable cut) {
        VStack content = new VStack().gap(4);
        if (step == CutPasteStep.PASTE) {
            content.child(new TextWidget("2. Place cut blocks"));
            content.child(new TextWidget("Close menu, aim at destination.").wrap());
            content.child(new TextWidget("Right-click to paste once.").wrap());
            content.child(new TextWidget("Wait for cutting to finish first.").wrap());
        } else {
            content.child(new TextWidget(step == CutPasteStep.CUT ? "1. Ready to cut" : "1. Select an area"));
            content.child(new TextWidget("Right-click: first corner").wrap());
            content.child(new TextWidget("Shift + right-click: other corner").wrap());
            ButtonWidget button = ModifierPanel.button("Cut selection", cut);
            button.enabled(step == CutPasteStep.CUT);
            button.tooltip("Removes the selected blocks and prepares them for pasting.");
            content.child(button);
        }
        return content;
    }
}
