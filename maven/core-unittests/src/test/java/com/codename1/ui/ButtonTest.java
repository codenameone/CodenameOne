package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ButtonTest extends UITestBase {

    @Test
    void testSetCommandBindsCommandAndFiresAction() throws Exception {
        when(implementation.isBuiltinSoundsEnabled()).thenReturn(false);
        AtomicInteger actionCount = new AtomicInteger();
        Command cmd = new Command("Go") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                actionCount.incrementAndGet();
            }
        };

        Button button = new Button();
        button.setCommand(cmd);

        assertEquals("Go", button.getText());
        assertSame(cmd, button.getCommand());
        assertTrue(button.getListeners().contains(cmd));

        Method fire = Button.class.getDeclaredMethod("fireActionEvent", int.class, int.class);
        fire.setAccessible(true);
        fire.invoke(button, 10, 20);

        assertEquals(1, actionCount.get());
    }

    @Test
    void testStateChangeListenerReceivesPressedAndReleasedEvents() {
        when(implementation.isBuiltinSoundsEnabled()).thenReturn(false);
        Button button = new Button();
        AtomicInteger stateChanges = new AtomicInteger();
        ActionListener listener = evt -> stateChanges.incrementAndGet();
        button.addStateChangeListener(listener);

        button.pressed();
        assertEquals(Button.STATE_PRESSED, button.getState());
        button.released();
        assertEquals(Button.STATE_ROLLOVER, button.getState());
        button.setReleased();
        assertEquals(Button.STATE_DEFAULT, button.getState());

        assertEquals(3, stateChanges.get());

        button.removeStateChangeListener(listener);
        button.pressed();
        assertEquals(3, stateChanges.get());
    }

    @Test
    void testBindStateMirrorsSourceStateUntilUnbound() {
        when(implementation.isBuiltinSoundsEnabled()).thenReturn(false);
        Button source = new Button();
        Button follower = new Button();
        follower.bindStateTo(source);

        source.pressed();
        assertEquals(Button.STATE_PRESSED, follower.getState());

        source.setReleased();
        assertEquals(Button.STATE_DEFAULT, follower.getState());

        follower.unbindStateFrom(source);
        source.pressed();
        assertEquals(Button.STATE_DEFAULT, follower.getState());
    }

    @Test
    void testRippleDefaultAppliesToNewButtons() {
        boolean original = Button.isButtonRippleEffectDefault();
        try {
            Button.setButtonRippleEffectDefault(true);
            Button ripple = new Button();
            assertTrue(ripple.isRippleEffect());

            Button.setButtonRippleEffectDefault(false);
            Button noRipple = new Button();
            assertFalse(noRipple.isRippleEffect());
        } finally {
            Button.setButtonRippleEffectDefault(original);
        }
    }
}
