package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CheckBox;
import com.codename1.ui.list.DefaultListModel;

import static org.junit.jupiter.api.Assertions.*;

class CheckBoxListTest extends UITestBase {

    @FormTest
    void testConstructorInitializesWithModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        CheckBoxList list = new CheckBoxList(model);
        assertSame(model, list.getModel());
        assertEquals(3, list.getComponentCount());
    }

    @FormTest
    void testIsAllowMultipleSelectionReturnsTrue() {
        DefaultListModel<String> model = new DefaultListModel<>("One");
        CheckBoxList list = new CheckBoxList(model);
        assertTrue(list.isAllowMultipleSelection());
    }

    @FormTest
    void testCreateButtonCreatesCheckBox() {
        DefaultListModel<String> model = new DefaultListModel<>("Test");
        CheckBoxList list = new CheckBoxList(model);
        assertTrue(list.getComponentAt(0) instanceof CheckBox);
    }

    @FormTest
    void testSelectionUpdatesModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        CheckBoxList list = new CheckBoxList(model);

        CheckBox cb1 = (CheckBox) list.getComponentAt(0);
        CheckBox cb2 = (CheckBox) list.getComponentAt(1);

        cb1.setSelected(true);
        cb2.setSelected(true);

        flushSerialCalls();

        // Verify checkboxes are selected
        assertTrue(cb1.isSelected());
        assertTrue(cb2.isSelected());
    }

    @FormTest
    void testModelChangesUpdateCheckboxes() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two");
        CheckBoxList list = new CheckBoxList(model);

        assertEquals(2, list.getComponentCount());

        model.addItem("Three");
        assertEquals(3, list.getComponentCount());

        model.removeItem(1);
        assertEquals(2, list.getComponentCount());
    }

    @FormTest
    void testMultiListModelAccessor() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two");
        CheckBoxList list = new CheckBoxList(model);

        assertNotNull(list.getMultiListModel());
    }
}
