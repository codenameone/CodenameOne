package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class ClearableTextFieldTest extends UITestBase {

    @FormTest
    void testWrapCreatesContainer() {
        TextField tf = new TextField("Initial");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        assertNotNull(ctf);
        assertTrue(ctf.contains(tf));
    }

    @FormTest
    void testWrapWithIconSizeCreatesContainer() {
        TextField tf = new TextField("Test");
        ClearableTextField ctf = ClearableTextField.wrap(tf, 3.5f);

        assertNotNull(ctf);
        assertTrue(ctf.contains(tf));
    }

    @FormTest
    void testClearButtonExists() {
        TextField tf = new TextField("Test");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        // Find the clear button
        Button clearButton = null;
        for (Component c : ctf) {
            if (c instanceof Button) {
                clearButton = (Button) c;
                break;
            }
        }

        assertNotNull(clearButton, "Clear button should exist");
    }

    @FormTest
    void testClearButtonClearsTextField() {
        TextField tf = new TextField("Initial Text");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        // Find and click the clear button
        Button clearButton = null;
        for (Component c : ctf) {
            if (c instanceof Button) {
                clearButton = (Button) c;
                break;
            }
        }

        assertNotNull(clearButton);

        // Simulate button click with coordinates
        clearButton.fireActionEvent(0, 0);

        assertEquals("", tf.getText(), "Text should be cleared");
    }

    @FormTest
    void testUIIDInheritedFromTextField() {
        TextField tf = new TextField("Test");
        tf.setUIID("CustomTextField");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        assertEquals("CustomTextField", ctf.getUIID());
    }

    @FormTest
    void testLayoutIsBorderLayout() {
        TextField tf = new TextField("Test");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        assertTrue(ctf.getLayout() instanceof BorderLayout);
    }

    @FormTest
    void testTextFieldInCenterPosition() {
        TextField tf = new TextField("Test");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        BorderLayout layout = (BorderLayout) ctf.getLayout();
        Object constraint = layout.getComponentConstraint(tf);

        assertEquals(BorderLayout.CENTER, constraint);
    }

    @FormTest
    void testClearButtonInEastPosition() {
        TextField tf = new TextField("Test");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        Button clearButton = null;
        for (Component c : ctf) {
            if (c instanceof Button) {
                clearButton = (Button) c;
                break;
            }
        }

        assertNotNull(clearButton);
        BorderLayout layout = (BorderLayout) ctf.getLayout();
        Object constraint = layout.getComponentConstraint(clearButton);

        assertEquals(BorderLayout.EAST, constraint);
    }

    @FormTest
    void testMultipleClearOperations() {
        TextField tf = new TextField("Text 1");
        ClearableTextField ctf = ClearableTextField.wrap(tf);

        Button clearButton = null;
        for (Component c : ctf) {
            if (c instanceof Button) {
                clearButton = (Button) c;
                break;
            }
        }

        clearButton.fireActionEvent(0, 0);
        assertEquals("", tf.getText());

        tf.setText("Text 2");
        clearButton.fireActionEvent(0, 0);
        assertEquals("", tf.getText());

        tf.setText("Text 3");
        clearButton.fireActionEvent(0, 0);
        assertEquals("", tf.getText());
    }
}
