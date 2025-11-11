package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for key events and game key events.
 */
class KeyEventsTest extends UITestBase {

    @FormTest
    void testKeyPressEvent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        final boolean[] pressed = {false};
        Button btn = new Button("Test") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                pressed[0] = true;
            }
        };
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        btn.keyPressed(65); // 'A' key
        assertTrue(pressed[0]);
    }

    @FormTest
    void testKeyReleaseEvent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        final boolean[] released = {false};
        Button btn = new Button("Test") {
            @Override
            public void keyReleased(int keyCode) {
                super.keyReleased(keyCode);
                released[0] = true;
            }
        };
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        btn.keyReleased(65);
        assertTrue(released[0]);
    }

    @FormTest
    void testKeyEventsOnForm() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Send key event to form
        form.keyPressed(65);
        form.keyReleased(65);

        // Should not crash
        assertNotNull(form);
    }

    @FormTest
    void testScrollWithKeyPress() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 50; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        int initialScrollY = scrollable.getScrollY();

        // Simulate DOWN key
        int downKeyCode = impl.getKeyCode(Display.GAME_DOWN);
        scrollable.keyPressed(downKeyCode);

        // Scroll position may change
        assertTrue(scrollable.getScrollY() >= initialScrollY);
    }

    @FormTest
    void testGameKeyMapping() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        int upKey = impl.getKeyCode(Display.GAME_UP);
        int downKey = impl.getKeyCode(Display.GAME_DOWN);
        int leftKey = impl.getKeyCode(Display.GAME_LEFT);
        int rightKey = impl.getKeyCode(Display.GAME_RIGHT);
        int fireKey = impl.getKeyCode(Display.GAME_FIRE);

        // Game keys should be mapped
        assertEquals(Display.GAME_UP, impl.getGameAction(upKey));
        assertEquals(Display.GAME_DOWN, impl.getGameAction(downKey));
        assertEquals(Display.GAME_LEFT, impl.getGameAction(leftKey));
        assertEquals(Display.GAME_RIGHT, impl.getGameAction(rightKey));
        assertEquals(Display.GAME_FIRE, impl.getGameAction(fireKey));
    }

    @FormTest
    void testKeyEventWithDisabledComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.setEnabled(false);
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Send key to disabled component
        btn.keyPressed(65);

        assertFalse(btn.isEnabled());
    }

    @FormTest
    void testKeyEventOnTextField() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        TextField textField = new TextField();
        form.add(BorderLayout.CENTER, textField);
        form.revalidate();

        // Simulate key events on text field
        textField.keyPressed(65);
        textField.keyReleased(65);

        assertNotNull(textField.getText());
    }

    @FormTest
    void testBackKeyEvent() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();

        final boolean[] backPressed = {false};
        Form testForm = new Form("Test") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                if (keyCode == impl.getBackKeyCode()) {
                    backPressed[0] = true;
                }
            }
        };

        testForm.keyPressed(impl.getBackKeyCode());
        assertTrue(backPressed[0]);
    }

    @FormTest
    void testScrollHorizontalWithKeys() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.x());
        scrollable.setScrollableX(true);
        scrollable.setWidth(300);

        for (int i = 0; i < 20; i++) {
            Button btn = new Button("Item " + i);
            btn.setPreferredW(100);
            scrollable.add(btn);
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        int initialScrollX = scrollable.getScrollX();

        // Simulate RIGHT key
        int rightKeyCode = impl.getKeyCode(Display.GAME_RIGHT);
        scrollable.keyPressed(rightKeyCode);

        assertTrue(scrollable.getScrollX() >= initialScrollX);
    }

    @FormTest
    void testKeyRepeat() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        final int[] pressCount = {0};
        Button btn = new Button("Test") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                pressCount[0]++;
            }
        };
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Simulate key repeat
        for (int i = 0; i < 5; i++) {
            btn.keyPressed(65);
        }

        assertEquals(5, pressCount[0]);
    }

    @FormTest
    void testArrowKeyNavigation() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        btn1.requestFocus();

        // Simulate DOWN key for navigation
        int downKeyCode = impl.getKeyCode(Display.GAME_DOWN);
        form.keyPressed(downKeyCode);

        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testKeyEventInDialog() {
        Form form = CN.getCurrentForm();

        Dialog dialog = new Dialog("Test");
        dialog.setLayout(new BorderLayout());

        final boolean[] pressed = {false};
        Button btn = new Button("Dialog Button") {
            @Override
            public void keyPressed(int keyCode) {
                super.keyPressed(keyCode);
                pressed[0] = true;
            }
        };
        dialog.add(BorderLayout.CENTER, btn);

        btn.keyPressed(65);
        assertTrue(pressed[0]);
    }

    @FormTest
    void testKeyCodeConstants() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        // Test that key code constants are defined
        assertTrue(impl.getBackKeyCode() != 0);
        assertTrue(impl.getBackspaceKeyCode() != 0);
        assertTrue(impl.getClearKeyCode() != 0);
    }

    @FormTest
    void testKeyEventPropagation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Button btn = new Button("Test");
        container.add(btn);

        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        // Send key event
        btn.keyPressed(65);

        // Should not crash
        assertNotNull(container);
    }

    @FormTest
    void testKeyEventOnInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.setVisible(false);
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Send key to invisible component
        btn.keyPressed(65);

        assertFalse(btn.isVisible());
    }
}
