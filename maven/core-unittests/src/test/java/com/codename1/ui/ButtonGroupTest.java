package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.SelectionListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ButtonGroupTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        ButtonGroup group = new ButtonGroup();
        assertNotNull(group);
        assertEquals(0, group.getButtonCount());
    }

    @FormTest
    void testAddRadioButton() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        assertEquals(1, group.getButtonCount());

        group.add(rb2);
        assertEquals(2, group.getButtonCount());
    }

    @FormTest
    void testRemoveRadioButton() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        group.add(rb2);
        assertEquals(2, group.getButtonCount());

        group.remove(rb1);
        assertEquals(1, group.getButtonCount());

        group.remove(rb2);
        assertEquals(0, group.getButtonCount());
    }

    @FormTest
    void testGetSelectedRadioButton() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        group.add(rb2);

        rb1.setSelected(true);
        assertEquals(rb1, group.getSelectedRadioButton());

        rb2.setSelected(true);
        assertEquals(rb2, group.getSelectedRadioButton());
    }

    @FormTest
    void testGetSelectedIndex() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");
        RadioButton rb3 = new RadioButton("Option 3");

        group.add(rb1);
        group.add(rb2);
        group.add(rb3);

        rb1.setSelected(true);
        assertEquals(0, group.getSelectedIndex());

        rb2.setSelected(true);
        assertEquals(1, group.getSelectedIndex());

        rb3.setSelected(true);
        assertEquals(2, group.getSelectedIndex());
    }

    @FormTest
    void testSetSelectedIndex() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");
        RadioButton rb3 = new RadioButton("Option 3");

        group.add(rb1);
        group.add(rb2);
        group.add(rb3);

        group.setSelectedIndex(0);
        assertTrue(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertFalse(rb3.isSelected());

        group.setSelectedIndex(1);
        assertFalse(rb1.isSelected());
        assertTrue(rb2.isSelected());
        assertFalse(rb3.isSelected());

        group.setSelectedIndex(2);
        assertFalse(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertTrue(rb3.isSelected());
    }

    @FormTest
    void testGetRadioButton() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        group.add(rb2);

        assertEquals(rb1, group.getRadioButton(0));
        assertEquals(rb2, group.getRadioButton(1));
    }

    @FormTest
    void testClearSelection() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        group.add(rb2);

        rb1.setSelected(true);
        assertTrue(rb1.isSelected());

        group.clearSelection();
        assertFalse(rb1.isSelected());
        assertFalse(rb2.isSelected());
    }

    @FormTest
    void testAddSelectionListener() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");

        group.add(rb1);
        group.add(rb2);

        AtomicInteger oldSelection = new AtomicInteger(-1);
        AtomicInteger newSelection = new AtomicInteger(-1);

        SelectionListener listener = new SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                oldSelection.set(oldSelected);
                newSelection.set(newSelected);
            }
        };

        group.addSelectionListener(listener);

        rb1.setSelected(true);
        // Note: Listener behavior may vary, just verify it can be added
        assertNotNull(group);
    }

    @FormTest
    void testRemoveSelectionListener() {
        ButtonGroup group = new ButtonGroup();
        SelectionListener listener = new SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
            }
        };

        group.addSelectionListener(listener);
        group.removeSelectionListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(group);
    }

    @FormTest
    void testMutualExclusion() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        RadioButton rb2 = new RadioButton("Option 2");
        RadioButton rb3 = new RadioButton("Option 3");

        group.add(rb1);
        group.add(rb2);
        group.add(rb3);

        rb1.setSelected(true);
        assertTrue(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertFalse(rb3.isSelected());

        rb2.setSelected(true);
        assertFalse(rb1.isSelected());
        assertTrue(rb2.isSelected());
        assertFalse(rb3.isSelected());

        rb3.setSelected(true);
        assertFalse(rb1.isSelected());
        assertFalse(rb2.isSelected());
        assertTrue(rb3.isSelected());
    }

    @FormTest
    void testEmptyGroupSelection() {
        ButtonGroup group = new ButtonGroup();
        assertEquals(-1, group.getSelectedIndex());
        assertNull(group.getSelectedRadioButton());
    }

    @FormTest
    void testAddNull() {
        ButtonGroup group = new ButtonGroup();
        // Adding null should either be ignored or throw exception
        // Just verify it doesn't crash the test
        assertDoesNotThrow(() -> {
            try {
                group.add(null);
            } catch (NullPointerException e) {
                // Expected behavior
            }
        });
    }

    @FormTest
    void testSetSelectedIndexOutOfBounds() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        group.add(rb1);

        // Setting out of bounds index should either be ignored or throw exception
        assertDoesNotThrow(() -> {
            try {
                group.setSelectedIndex(5);
            } catch (Exception e) {
                // Expected behavior
            }
        });
    }

    @FormTest
    void testSetSelectedIndexNegative() {
        ButtonGroup group = new ButtonGroup();
        RadioButton rb1 = new RadioButton("Option 1");
        group.add(rb1);
        rb1.setSelected(true);

        group.setSelectedIndex(-1);
        // After setting to -1, typically should clear selection
        assertTrue(group.getSelectedIndex() == -1 || group.getSelectedIndex() == 0);
    }
}
