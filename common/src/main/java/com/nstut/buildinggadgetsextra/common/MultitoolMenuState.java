package com.nstut.buildinggadgetsextra.common;

import java.util.Objects;

/** Loader-independent two-level navigation state for the multitool radial menu. */
public final class MultitoolMenuState {
    private Page page = Page.GENERAL;
    private MultitoolMode selectedTool;

    public MultitoolMenuState(MultitoolMode selectedTool) {
        this.selectedTool = Objects.requireNonNull(selectedTool, "selectedTool");
    }

    public Page page() {
        return page;
    }

    public MultitoolMode selectedTool() {
        return selectedTool;
    }

    public void openTool(MultitoolMode tool) {
        selectedTool = Objects.requireNonNull(tool, "tool");
        page = Page.SUBMENU;
    }

    public void back() {
        page = Page.GENERAL;
    }

    public enum Page {
        GENERAL,
        SUBMENU
    }
}
