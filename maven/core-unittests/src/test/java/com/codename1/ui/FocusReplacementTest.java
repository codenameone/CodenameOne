package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for replacing focused components with focusable/non-focusable components.
 */
class FocusReplacementTest extends UITestBase {

    @FormTest
    void testReplaceFocusedComponentWithFocusable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Focus btn2
        btn2.requestFocus();
        assertTrue(btn2.hasFocus());

        // Replace btn2 with another focusable component
        Button replacement = new Button("Replacement");
        form.replace(btn2, replacement, null);
        form.revalidate();

        // Replacement should receive focus
        assertFalse(form.contains(btn2));
        assertTrue(form.contains(replacement));
    }

    @FormTest
    void testReplaceFocusedComponentWithNonFocusable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Focus btn2
        btn2.requestFocus();
        assertTrue(btn2.hasFocus());

        // Replace btn2 with non-focusable label
        Label label = new Label("Non-Focusable");
        label.setFocusable(false);
        form.replace(btn2, label, null);
        form.revalidate();

        // Focus should move to another focusable component
        assertFalse(form.contains(btn2));
        assertTrue(form.contains(label));
        assertFalse(label.hasFocus());
    }

    @FormTest
    void testReplaceNonFocusedComponentWithFocusable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Focus btn1
        btn1.requestFocus();
        assertTrue(btn1.hasFocus());

        // Replace btn2 (not focused) with another button
        Button replacement = new Button("Replacement");
        form.replace(btn2, replacement, null);
        form.revalidate();

        // Focus should remain on btn1
        assertTrue(btn1.hasFocus());
        assertTrue(form.contains(replacement));
    }

    @FormTest
    void testRemoveFocusedComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Focus btn2
        btn2.requestFocus();
        assertTrue(btn2.hasFocus());

        // Remove btn2
        form.removeComponent(btn2);
        form.revalidate();

        // Focus should move to another component
        assertFalse(form.contains(btn2));
    }

    @FormTest
    void testReplaceFocusedWithDisabledComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Focus btn2
        btn2.requestFocus();
        assertTrue(btn2.hasFocus());

        // Replace with disabled button
        Button disabled = new Button("Disabled");
        disabled.setEnabled(false);
        form.replace(btn2, disabled, null);
        form.revalidate();

        // Disabled component should not have focus
        assertFalse(disabled.hasFocus());
    }

    @FormTest
    void testReplaceMultipleFocusableComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        btn2.requestFocus();

        // Replace all buttons
        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.replace(btn1, label1, null);
        form.replace(btn2, label2, null);
        form.replace(btn3, label3, null);
        form.revalidate();

        // No label should have focus (they're not focusable by default)
        assertFalse(label1.hasFocus());
        assertFalse(label2.hasFocus());
        assertFalse(label3.hasFocus());
    }

    @FormTest
    void testReplaceFocusedComponentInContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        container.addAll(btn1, btn2);
        form.add(container);
        form.revalidate();

        btn2.requestFocus();
        assertTrue(btn2.hasFocus());

        // Replace in container
        Button replacement = new Button("Replacement");
        container.replace(btn2, replacement, null);
        form.revalidate();

        assertTrue(container.contains(replacement));
        assertFalse(container.contains(btn2));
    }

    @FormTest
    void testReplaceFocusableWithInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        btn2.requestFocus();

        // Replace with invisible component
        Button invisible = new Button("Invisible");
        invisible.setVisible(false);
        form.replace(btn2, invisible, null);
        form.revalidate();

        assertFalse(invisible.isVisible());
        assertFalse(invisible.hasFocus());
    }

    @FormTest
    void testFocusTransferOnReplace() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        btn2.requestFocus();

        // Replace and check focus transfer
        TextField textField = new TextField();
        form.replace(btn2, textField, null);
        form.revalidate();

        assertTrue(form.contains(textField));
    }

    @FormTest
    void testReplaceFocusedComponentWithContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        btn2.requestFocus();

        // Replace with container
        Container replacement = new Container(BoxLayout.y());
        replacement.add(new Button("New Button"));
        form.replace(btn2, replacement, null);
        form.revalidate();

        assertTrue(form.contains(replacement));
    }

    @FormTest
    void testReplaceLastFocusableComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Only Button");
        form.add(btn);
        form.revalidate();

        btn.requestFocus();
        assertTrue(btn.hasFocus());

        // Replace with non-focusable
        Label label = new Label("Label");
        form.replace(btn, label, null);
        form.revalidate();

        assertFalse(label.hasFocus());
    }

    @FormTest
    void testReplaceFocusedComponentWithSameFocusableType() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TextField tf1 = new TextField("Text 1");
        TextField tf2 = new TextField("Text 2");

        form.addAll(tf1, tf2);
        form.revalidate();

        tf1.requestFocus();

        // Replace with another text field
        TextField replacement = new TextField("Replacement");
        form.replace(tf1, replacement, null);
        form.revalidate();

        assertTrue(form.contains(replacement));
    }

    @FormTest
    void testReplaceWithTransitionAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        btn1.requestFocus();

        // Replace with transition
        Button replacement = new Button("Replacement");
        form.replaceAndWait(btn1, replacement, null);

        assertTrue(form.contains(replacement));
        assertFalse(form.contains(btn1));
    }

    @FormTest
    void testReplaceFocusedInNestedContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container outer = new Container(BoxLayout.y());
        Container inner = new Container(BoxLayout.y());

        Button btn = new Button("Button");
        inner.add(btn);
        outer.add(inner);
        form.add(outer);
        form.revalidate();

        btn.requestFocus();

        // Replace in nested structure
        Label label = new Label("Label");
        inner.replace(btn, label, null);
        form.revalidate();

        assertTrue(inner.contains(label));
    }

    @FormTest
    void testReplaceAllFocusableWithNonFocusable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        btn2.requestFocus();

        // Replace all with labels
        form.removeAll();
        form.addAll(new Label("L1"), new Label("L2"), new Label("L3"));
        form.revalidate();

        // Form should handle lack of focusable components
        assertEquals(3, form.getComponentCount());
    }
}
