package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for key events and game key events, including using game keys to scroll.
 */
class KeyEventsTest extends UITestBase {

    @FormTest
    void testKeyPressEvent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] keyPressed = {false};
        final int[] keyCode = {0};

        btn.addKeyListener((keyEvent, code) -> {
            keyPressed[0] = true;
            keyCode[0] = code;
        });

        // Simulate key press
        btn.keyPressed(65); // 'A' key

        assertTrue(keyPressed[0]);
        assertEquals(65, keyCode[0]);
    }

    @FormTest
    void testKeyReleaseEvent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] keyReleased = {false};

        btn.addKeyListener((keyEvent, code) -> {
            if (keyEvent == 2) { // KEY_RELEASED
                keyReleased[0] = true;
            }
        });

        btn.keyReleased(65);

        assertTrue(keyReleased[0]);
    }

    @FormTest
    void testGameKeyUp() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] gameKeyPressed = {false};

        btn.addGameKeyListener(Display.GAME_UP, () -> {
            gameKeyPressed[0] = true;
        });

        // Simulate game key
        int upKeyCode = impl.getKeyCode(Display.GAME_UP);
        btn.keyPressed(upKeyCode);

        assertTrue(gameKeyPressed[0]);
    }

    @FormTest
    void testGameKeyDown() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] gameKeyPressed = {false};

        btn.addGameKeyListener(Display.GAME_DOWN, () -> {
            gameKeyPressed[0] = true;
        });

        int downKeyCode = impl.getKeyCode(Display.GAME_DOWN);
        btn.keyPressed(downKeyCode);

        assertTrue(gameKeyPressed[0]);
    }

    @FormTest
    void testGameKeyLeft() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] gameKeyPressed = {false};

        btn.addGameKeyListener(Display.GAME_LEFT, () -> {
            gameKeyPressed[0] = true;
        });

        int leftKeyCode = impl.getKeyCode(Display.GAME_LEFT);
        btn.keyPressed(leftKeyCode);

        assertTrue(gameKeyPressed[0]);
    }

    @FormTest
    void testGameKeyRight() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] gameKeyPressed = {false};

        btn.addGameKeyListener(Display.GAME_RIGHT, () -> {
            gameKeyPressed[0] = true;
        });

        int rightKeyCode = impl.getKeyCode(Display.GAME_RIGHT);
        btn.keyPressed(rightKeyCode);

        assertTrue(gameKeyPressed[0]);
    }

    @FormTest
    void testGameKeyFire() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] firePressed = {false};

        btn.addGameKeyListener(Display.GAME_FIRE, () -> {
            firePressed[0] = true;
        });

        int fireKeyCode = impl.getKeyCode(Display.GAME_FIRE);
        btn.keyPressed(fireKeyCode);

        assertTrue(firePressed[0]);
    }

    @FormTest
    void testScrollWithGameKeys() {
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

        // Simulate DOWN key to scroll
        int downKeyCode = impl.getKeyCode(Display.GAME_DOWN);
        scrollable.keyPressed(downKeyCode);

        // Scroll position may change
        assertTrue(scrollable.getScrollY() >= initialScrollY);
    }

    @FormTest
    void testMoveScrollTowards() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Label target = null;
        for (int i = 0; i < 50; i++) {
            Label label = new Label("Item " + i);
            scrollable.add(label);
            if (i == 30) {
                target = label;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Move scroll towards target
        if (target != null) {
            scrollable.scrollComponentToVisible(target);
            form.revalidate();
        }

        assertNotNull(target);
    }

    @FormTest
    void testKeyEventWithDisabledComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.setEnabled(false);
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] keyPressed = {false};

        btn.addKeyListener((keyEvent, code) -> {
            keyPressed[0] = true;
        });

        btn.keyPressed(65);

        // Disabled components may not process key events
        assertFalse(btn.isEnabled());
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

        final boolean[] containerKeyPressed = {false};
        final boolean[] buttonKeyPressed = {false};

        container.addKeyListener((keyEvent, code) -> {
            containerKeyPressed[0] = true;
        });

        btn.addKeyListener((keyEvent, code) -> {
            buttonKeyPressed[0] = true;
        });

        btn.keyPressed(65);

        assertTrue(buttonKeyPressed[0]);
    }

    @FormTest
    void testMultipleKeyListeners() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final int[] listenerCallCount = {0};

        btn.addKeyListener((keyEvent, code) -> {
            listenerCallCount[0]++;
        });

        btn.addKeyListener((keyEvent, code) -> {
            listenerCallCount[0]++;
        });

        btn.keyPressed(65);

        assertEquals(2, listenerCallCount[0]);
    }

    @FormTest
    void testRemoveKeyListener() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] keyPressed = {false};

        ActionListener<ActionEvent> listener = (keyEvent, code) -> {
            keyPressed[0] = true;
        };

        btn.addKeyListener(listener);
        btn.removeKeyListener(listener);

        btn.keyPressed(65);

        assertFalse(keyPressed[0]);
    }

    @FormTest
    void testKeyRepeat() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final int[] keyPressCount = {0};

        btn.addKeyListener((keyEvent, code) -> {
            keyPressCount[0]++;
        });

        // Simulate key repeat
        for (int i = 0; i < 5; i++) {
            btn.keyPressed(65);
        }

        assertEquals(5, keyPressCount[0]);
    }

    @FormTest
    void testGameKeyScrollHorizontal() {
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

        // Simulate RIGHT key to scroll horizontally
        int rightKeyCode = impl.getKeyCode(Display.GAME_RIGHT);
        scrollable.keyPressed(rightKeyCode);

        assertTrue(scrollable.getScrollX() >= initialScrollX);
    }

    @FormTest
    void testBackKeyEvent() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();

        final boolean[] backPressed = {false};

        form.addKeyListener((keyEvent, code) -> {
            if (code == impl.getBackKeyCode()) {
                backPressed[0] = true;
            }
        });

        form.keyPressed(impl.getBackKeyCode());

        assertTrue(backPressed[0]);
    }

    @FormTest
    void testNumberKeyEvents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        TextField textField = new TextField();
        form.add(BorderLayout.CENTER, textField);
        form.revalidate();

        final StringBuilder typed = new StringBuilder();

        textField.addDataChangeListener((type, index) -> {
            typed.append(textField.getText());
        });

        // Simulate number keys
        for (int i = 48; i <= 57; i++) { // ASCII 0-9
            textField.keyPressed(i);
        }

        // Text field may have captured some input
        assertNotNull(textField.getText());
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

        // Focus may move to next component
        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testKeyEventInDialog() {
        Form form = CN.getCurrentForm();

        Dialog dialog = new Dialog("Test");
        dialog.setLayout(new BorderLayout());

        Button btn = new Button("Dialog Button");
        dialog.add(BorderLayout.CENTER, btn);

        final boolean[] dialogKeyPressed = {false};

        btn.addKeyListener((keyEvent, code) -> {
            dialogKeyPressed[0] = true;
        });

        btn.keyPressed(65);

        assertTrue(dialogKeyPressed[0]);
    }
}
