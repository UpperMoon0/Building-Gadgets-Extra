package com.nstut.buildinggadgetsextra.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultitoolMenuStateTest {
    @Test
    void navigationIsExplicitAndRetainsTheSelectedTool() {
        MultitoolMenuState menu = new MultitoolMenuState(MultitoolMode.BUILD);
        assertEquals(MultitoolMenuState.Page.GENERAL, menu.page());

        menu.openTool(MultitoolMode.CUT_PASTE);
        assertEquals(MultitoolMenuState.Page.SUBMENU, menu.page());
        assertEquals(MultitoolMode.CUT_PASTE, menu.selectedTool());

        menu.back();
        assertEquals(MultitoolMenuState.Page.GENERAL, menu.page());
        assertEquals(MultitoolMode.CUT_PASTE, menu.selectedTool());
    }
}
