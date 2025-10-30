package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.list.DefaultListModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

class SwitchListTest extends UITestBase {
    @BeforeEach
    void configureDisplay() {
        implementation.setBuiltinSoundsEnabled(false);
    }

    @Test
    void testAllowMultipleSelection() {
        SwitchList list = new SwitchList(new DefaultListModel<>("One", "Two"));
        assertTrue(list.isAllowMultipleSelection());
    }

    @FormTest
    void testCreateButtonContainsSwitchAndLabel() {
        DefaultListModel<String> model = new DefaultListModel<>("Alpha", "Beta");
        SwitchList list = new SwitchList(model);
        list.refresh();

        Component cell = list.getComponentAt(0);
        Switch sw = $(".switch", cell).asComponent(Switch.class);
        assertNotNull(sw, "Switch should be present in decorated component");
        Label label = findLabel(cell);
        assertNotNull(label);
        assertEquals("Alpha", label.getText());
    }

    @FormTest
    void testSetSelectedUpdatesSwitchState() {
        DefaultListModel<String> model = new DefaultListModel<>("Red", "Green");
        SwitchList list = new SwitchList(model);
        list.refresh();

        Component cell = list.getComponentAt(0);
        Switch sw = $(".switch", cell).asComponent(Switch.class);
        assertFalse(sw.isOn());

        list.setSelected(cell, true);
        assertTrue(sw.isOn());

        list.setSelected(cell, false);
        assertFalse(sw.isOn());
    }

    @FormTest
    void testChangeListenerSynchronizesModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        SwitchList list = new SwitchList(model);
        list.refresh();

        Component firstCell = list.getComponentAt(0);
        Switch sw = $(".switch", firstCell).asComponent(Switch.class);
        sw.setOn();
        assertTrue(Arrays.stream(model.getSelectedIndices()).anyMatch(i -> i == 0));

        list.undecorateComponent(firstCell);
        sw.setOff();
        assertTrue(Arrays.stream(model.getSelectedIndices()).anyMatch(i -> i == 0),
                "Removing listeners should stop model synchronization");
    }

    @FormTest
    void testUndecorateRemovesActionListener() {
        DefaultListModel<String> model = new DefaultListModel<>("A", "B");
        SwitchList list = new SwitchList(model);
        list.refresh();

        Component cell = list.getComponentAt(0);
        Switch sw = $(".switch", cell).asComponent(Switch.class);
        assertTrue(sw.getListeners().contains(list));

        list.undecorateComponent(cell);
        assertFalse(sw.getListeners().contains(list));
    }

    private Label findLabel(Component cell) {
        if (cell instanceof Label) {
            return (Label) cell;
        }
        if (cell instanceof Container) {
            Container container = (Container) cell;
            int count = container.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component child = container.getComponentAt(i);
                Label label = findLabel(child);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }
}
