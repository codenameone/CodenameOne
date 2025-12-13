package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Label;

import static org.junit.jupiter.api.Assertions.*;

class ComponentGroupTest extends UITestBase {

    @FormTest
    void testForceGroupAssignsUiids() {
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        Button first = new Button("First");
        Button middle = new Button("Middle");
        Button last = new Button("Last");
        group.add(first);
        group.add(middle);
        group.add(last);

        group.refreshTheme(true);
        assertEquals("ButtonGroupFirst", first.getUIID());
        assertEquals("ButtonGroup", middle.getUIID());
        assertEquals("ButtonGroupLast", last.getUIID());

        group.removeComponent(middle);
        group.refreshTheme(true);
        assertEquals("ButtonGroupFirst", first.getUIID());
        assertEquals("ButtonGroupLast", last.getUIID());
        assertEquals("Button", middle.getUIID());
    }

    @FormTest
    void testHorizontalModeUpdatesUiids() {
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        group.setHorizontal(true);
        Button first = new Button("One");
        Button second = new Button("Two");
        group.add(first);
        group.add(second);

        group.refreshTheme(true);
        assertTrue(group.isHorizontal());
        assertEquals("ToggleButtonFirst", first.getUIID());
        assertEquals("ToggleButtonLast", second.getUIID());

        group.setHorizontal(false);
        group.refreshTheme(true);
        assertFalse(group.isHorizontal());
    }

    @FormTest
    void testStaticEncloseHelpers() {
        Label l1 = new Label("A");
        Label l2 = new Label("B");
        ComponentGroup vertical = ComponentGroup.enclose(l1, l2);
        assertFalse(vertical.isHorizontal());

        ComponentGroup horizontal = ComponentGroup.encloseHorizontal(new Label("X"), new Label("Y"));
        assertTrue(horizontal.isHorizontal());
    }
}
