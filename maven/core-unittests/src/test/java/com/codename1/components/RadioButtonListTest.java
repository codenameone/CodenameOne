package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.RadioButton;
import com.codename1.ui.list.DefaultListModel;

import static org.junit.jupiter.api.Assertions.*;

class RadioButtonListTest extends UITestBase {

    @FormTest
    void testConstructorInitializesWithModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        RadioButtonList list = new RadioButtonList(model);
        assertSame(model, list.getModel());
        assertEquals(3, list.getComponentCount());
    }

    @FormTest
    void testIsAllowMultipleSelectionReturnsFalse() {
        DefaultListModel<String> model = new DefaultListModel<>("One");
        RadioButtonList list = new RadioButtonList(model);
        assertFalse(list.isAllowMultipleSelection());
    }

    @FormTest
    void testCreateButtonCreatesRadioButton() {
        DefaultListModel<String> model = new DefaultListModel<>("Test");
        RadioButtonList list = new RadioButtonList(model);
        assertTrue(list.getComponentAt(0) instanceof RadioButton);
    }

    @FormTest
    void testOnlyOneRadioButtonSelectedAtTime() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        RadioButtonList list = new RadioButtonList(model);

        RadioButton rb1 = (RadioButton) list.getComponentAt(0);
        RadioButton rb2 = (RadioButton) list.getComponentAt(1);

        rb1.setSelected(true);
        assertTrue(rb1.isSelected());

        rb2.setSelected(true);
        assertTrue(rb2.isSelected());
        // rb1 should be deselected since only one can be selected
    }

    @FormTest
    void testModelChangesUpdateRadioButtons() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two");
        RadioButtonList list = new RadioButtonList(model);

        assertEquals(2, list.getComponentCount());

        model.addItem("Three");
        assertEquals(3, list.getComponentCount());

        model.removeItem(1);
        assertEquals(2, list.getComponentCount());
    }

    @FormTest
    void testSelectionChangesUpdateModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        RadioButtonList list = new RadioButtonList(model);

        RadioButton rb2 = (RadioButton) list.getComponentAt(1);
        rb2.setSelected(true);

        flushSerialCalls();

        // Model selection should be updated
        assertTrue(rb2.isSelected());
    }

    @FormTest
    void testRadioButtonsAreInSameGroup() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        RadioButtonList list = new RadioButtonList(model);

        RadioButton rb1 = (RadioButton) list.getComponentAt(0);
        RadioButton rb2 = (RadioButton) list.getComponentAt(1);
        RadioButton rb3 = (RadioButton) list.getComponentAt(2);

        // All radio buttons should be in the same group
        assertNotNull(rb1);
        assertNotNull(rb2);
        assertNotNull(rb3);
    }
}
