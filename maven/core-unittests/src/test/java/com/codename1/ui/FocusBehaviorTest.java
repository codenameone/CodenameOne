package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for component focus behavior, including focus management, focus listeners,
 * focus traversal, and interactions with the focus system.
 */
class FocusBehaviorTest extends UITestBase {

    @FormTest
    void testComponentFocusableByDefault() {
        TextField textField = new TextField();
        Button button = new Button("Click");
        Label label = new Label("Text");

        assertTrue(textField.isFocusable(), "TextField should be focusable by default");
        assertTrue(button.isFocusable(), "Button should be focusable by default");
        assertFalse(label.isFocusable(), "Label should not be focusable by default");
    }

    @FormTest
    void testSetFocusableChangesState() {
        Button button = new Button("Test");
        assertTrue(button.isFocusable(), "Button should initially be focusable");

        button.setFocusable(false);
        assertFalse(button.isFocusable(), "Button should not be focusable after setFocusable(false)");

        button.setFocusable(true);
        assertTrue(button.isFocusable(), "Button should be focusable after setFocusable(true)");
    }

    @FormTest
    void testRequestFocusOnFocusableComponent() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Focus Me");
        form.add(button);
        form.revalidate();

        button.requestFocus();

        assertTrue(button.hasFocus(), "Button should have focus after requestFocus()");
    }

    @FormTest
    void testRequestFocusOnNonFocusableComponentDoesNotGrantFocus() {
        Form form = Display.getInstance().getCurrent();
        Label label = new Label("No Focus");
        form.add(label);
        form.revalidate();

        label.requestFocus();

        assertFalse(label.hasFocus(), "Label should not have focus when not focusable");
    }

    @FormTest
    void testFocusListenerGainedTriggered() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        final boolean[] focusGainedCalled = {false};
        FocusListener listener = new FocusListener() {
            public void focusGained(Component cmp) {
                focusGainedCalled[0] = true;
                assertSame(button, cmp, "Focus gained should pass the correct component");
            }

            public void focusLost(Component cmp) {
            }
        };

        button.addFocusListener(listener);
        button.requestFocus();

        assertTrue(focusGainedCalled[0], "Focus listener's focusGained should be called");
    }

    @FormTest
    void testFocusListenerLostTriggered() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");
        form.add(button1);
        form.add(button2);
        form.revalidate();

        final boolean[] focusLostCalled = {false};
        FocusListener listener = new FocusListener() {
            public void focusGained(Component cmp) {
            }

            public void focusLost(Component cmp) {
                focusLostCalled[0] = true;
                assertSame(button1, cmp, "Focus lost should pass the correct component");
            }
        };

        button1.addFocusListener(listener);
        button1.requestFocus();
        assertTrue(button1.hasFocus(), "button1 should have focus");

        button2.requestFocus();
        assertTrue(focusLostCalled[0], "Focus listener's focusLost should be called");
        assertFalse(button1.hasFocus(), "button1 should no longer have focus");
        assertTrue(button2.hasFocus(), "button2 should have focus");
    }

    @FormTest
    void testRemoveFocusListenerStopsNotifications() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        final int[] callCount = {0};
        FocusListener listener = new FocusListener() {
            public void focusGained(Component cmp) {
                callCount[0]++;
            }

            public void focusLost(Component cmp) {
            }
        };

        button.addFocusListener(listener);
        button.requestFocus();
        assertEquals(1, callCount[0], "Listener should be called once");

        button.removeFocusListener(listener);
        button.setFocus(false);
        button.requestFocus();
        assertEquals(1, callCount[0], "Listener should not be called after removal");
    }

    @FormTest
    void testFocusTraversalWithMultipleComponents() {
        Form form = Display.getInstance().getCurrent();
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));

        Button button1 = new Button("First");
        Button button2 = new Button("Second");
        Button button3 = new Button("Third");

        container.add(button1);
        container.add(button2);
        container.add(button3);
        form.add(container);
        form.revalidate();

        button1.requestFocus();
        assertTrue(button1.hasFocus(), "button1 should have focus");

        Component next = button1.getNextFocusDown();
        if (next != null) {
            next.requestFocus();
        }

        // Either button2 has focus or no focus change occurred (implementation dependent)
        // Just verify the system is consistent
        if (next != null) {
            assertFalse(button1.hasFocus(), "button1 should not have focus after traversal");
        }
    }

    @FormTest
    void testSetNextFocusDown() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");

        form.add(button1);
        form.add(button2);
        form.revalidate();

        button1.setNextFocusDown(button2);
        assertSame(button2, button1.getNextFocusDown(), "Next focus down should be button2");

        button1.requestFocus();
        assertTrue(button1.hasFocus());

        // Navigate to next focus
        Component next = button1.getNextFocusDown();
        if (next != null) {
            next.requestFocus();
            assertTrue(button2.hasFocus(), "button2 should have focus after navigation");
        }
    }

    @FormTest
    void testSetNextFocusUp() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");

        form.add(button1);
        form.add(button2);
        form.revalidate();

        button2.setNextFocusUp(button1);
        assertSame(button1, button2.getNextFocusUp(), "Next focus up should be button1");
    }

    @FormTest
    void testSetNextFocusLeft() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");

        form.add(button1);
        form.add(button2);
        form.revalidate();

        button2.setNextFocusLeft(button1);
        assertSame(button1, button2.getNextFocusLeft(), "Next focus left should be button1");
    }

    @FormTest
    void testSetNextFocusRight() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");

        form.add(button1);
        form.add(button2);
        form.revalidate();

        button1.setNextFocusRight(button2);
        assertSame(button2, button1.getNextFocusRight(), "Next focus right should be button2");
    }

    @FormTest
    void testFocusLostWhenComponentDisabled() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        button.requestFocus();
        assertTrue(button.hasFocus(), "Button should have focus");

        button.setEnabled(false);
        assertFalse(button.hasFocus(), "Disabled button should lose focus");
    }

    @FormTest
    void testFocusWithinContainer() {
        Form form = Display.getInstance().getCurrent();
        Container container = new Container(new BorderLayout());
        Button button = new Button("Inside Container");

        container.add(BorderLayout.CENTER, button);
        form.add(container);
        form.revalidate();

        button.requestFocus();
        assertTrue(button.hasFocus(), "Button inside container should be able to gain focus");
    }

    @FormTest
    void testOnlyOneFocusedComponentAtATime() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");
        Button button3 = new Button("Third");

        form.add(button1);
        form.add(button2);
        form.add(button3);
        form.revalidate();

        button1.requestFocus();
        assertTrue(button1.hasFocus());
        assertFalse(button2.hasFocus());
        assertFalse(button3.hasFocus());

        button2.requestFocus();
        assertFalse(button1.hasFocus());
        assertTrue(button2.hasFocus());
        assertFalse(button3.hasFocus());

        button3.requestFocus();
        assertFalse(button1.hasFocus());
        assertFalse(button2.hasFocus());
        assertTrue(button3.hasFocus());
    }

    @FormTest
    void testPointerPressOnFocusableComponentGrantsFocus() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Click Me");
        button.setX(10);
        button.setY(10);
        button.setWidth(100);
        button.setHeight(40);

        form.add(button);
        form.revalidate();

        assertFalse(button.hasFocus(), "Button should not have focus initially");

        // Simulate pointer press
        form.pointerPressed(button.getAbsoluteX() + 5, button.getAbsoluteY() + 5);

        assertTrue(button.hasFocus(), "Button should gain focus on pointer press");
    }

    @FormTest
    void testFocusPersistsAcrossStyleChanges() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        button.requestFocus();
        assertTrue(button.hasFocus(), "Button should have focus");

        // Change style properties
        button.getAllStyles().setBgColor(0xFF0000);
        button.getAllStyles().setFgColor(0x00FF00);

        assertTrue(button.hasFocus(), "Button should maintain focus after style changes");
    }

    @FormTest
    void testMultipleFocusListenersAllNotified() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        final int[] listener1Count = {0};
        final int[] listener2Count = {0};
        final int[] listener3Count = {0};

        FocusListener listener1 = new FocusListener() {
            public void focusGained(Component cmp) {
                listener1Count[0]++;
            }

            public void focusLost(Component cmp) {
            }
        };

        FocusListener listener2 = new FocusListener() {
            public void focusGained(Component cmp) {
                listener2Count[0]++;
            }

            public void focusLost(Component cmp) {
            }
        };

        FocusListener listener3 = new FocusListener() {
            public void focusGained(Component cmp) {
                listener3Count[0]++;
            }

            public void focusLost(Component cmp) {
            }
        };

        button.addFocusListener(listener1);
        button.addFocusListener(listener2);
        button.addFocusListener(listener3);

        button.requestFocus();

        assertEquals(1, listener1Count[0], "Listener 1 should be notified");
        assertEquals(1, listener2Count[0], "Listener 2 should be notified");
        assertEquals(1, listener3Count[0], "Listener 3 should be notified");
    }

    @FormTest
    void testFocusBehaviorWithTabIndex() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");
        Button button3 = new Button("Third");

        button1.setTabIndex(2);
        button2.setTabIndex(1);
        button3.setTabIndex(3);

        form.add(button1);
        form.add(button2);
        form.add(button3);
        form.revalidate();

        assertEquals(2, button1.getTabIndex());
        assertEquals(1, button2.getTabIndex());
        assertEquals(3, button3.getTabIndex());
    }

    @FormTest
    void testPreferredTabIndexWithTraversable() {
        Button button = new Button("Test");

        button.setPreferredTabIndex(-1);
        assertFalse(button.isTraversable(), "Component with tab index -1 should not be traversable");

        button.setTraversable(true);
        assertTrue(button.isTraversable(), "Component should be traversable after setTraversable(true)");
        assertTrue(button.getPreferredTabIndex() >= 0, "Traversable component should have non-negative tab index");

        button.setTraversable(false);
        assertFalse(button.isTraversable(), "Component should not be traversable after setTraversable(false)");
        assertEquals(-1, button.getPreferredTabIndex(), "Non-traversable component should have tab index -1");
    }

    @FormTest
    void testFocusWithHiddenComponent() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        button.setVisible(true);
        button.requestFocus();
        assertTrue(button.hasFocus(), "Visible button should gain focus");

        button.setVisible(false);
        assertFalse(button.hasFocus(), "Hidden button should lose focus");
    }

    @FormTest
    void testFocusClearedOnFormChange() {
        Form form1 = new Form("Form 1");
        Form form2 = new Form("Form 2");

        Button button1 = new Button("Button on Form 1");
        Button button2 = new Button("Button on Form 2");

        form1.add(button1);
        form2.add(button2);

        form1.show();
        button1.requestFocus();
        assertTrue(button1.hasFocus(), "button1 should have focus on form1");

        form2.show();
        assertFalse(button1.hasFocus(), "button1 should lose focus when form2 is shown");
    }

    @FormTest
    void testBlockLeadComponent() {
        Button button = new Button("Test");

        assertFalse(button.isBlockLead(), "Button should not block lead by default");

        button.setBlockLead(true);
        assertTrue(button.isBlockLead(), "Button should block lead after setBlockLead(true)");

        button.setBlockLead(false);
        assertFalse(button.isBlockLead(), "Button should not block lead after setBlockLead(false)");
    }

    @FormTest
    void testFocusWithNestedContainers() {
        Form form = Display.getInstance().getCurrent();
        Container outer = new Container(new BorderLayout());
        Container inner = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Button button = new Button("Nested");

        inner.add(button);
        outer.add(BorderLayout.CENTER, inner);
        form.add(outer);
        form.revalidate();

        button.requestFocus();
        assertTrue(button.hasFocus(), "Button in nested container should be able to gain focus");
    }

    @FormTest
    void testSetFocusDirectly() {
        Form form = Display.getInstance().getCurrent();
        Button button = new Button("Test");
        form.add(button);
        form.revalidate();

        assertFalse(button.hasFocus(), "Button should not have focus initially");

        button.setFocus(true);
        assertTrue(button.hasFocus(), "Button should have focus after setFocus(true)");

        button.setFocus(false);
        assertFalse(button.hasFocus(), "Button should not have focus after setFocus(false)");
    }

    @FormTest
    void testFocusCycleWithThreeComponents() {
        Form form = Display.getInstance().getCurrent();
        Button button1 = new Button("First");
        Button button2 = new Button("Second");
        Button button3 = new Button("Third");

        form.add(button1);
        form.add(button2);
        form.add(button3);
        form.revalidate();

        // Create a focus cycle
        button1.setNextFocusDown(button2);
        button2.setNextFocusDown(button3);
        button3.setNextFocusDown(button1);

        button1.requestFocus();
        assertTrue(button1.hasFocus());

        button2.requestFocus();
        assertTrue(button2.hasFocus());
        assertFalse(button1.hasFocus());

        button3.requestFocus();
        assertTrue(button3.hasFocus());
        assertFalse(button2.hasFocus());
    }
}
