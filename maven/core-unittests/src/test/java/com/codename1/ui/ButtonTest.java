package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ButtonTest extends UITestBase {

    @Test
    void toolbarCommandCanHideTextWhenItHasAnIcon() {
        Hashtable props = new Hashtable();
        props.put("@hideToolbarCommandTextWithIconBool", "true");
        UIManager.getInstance().addThemeProps(props);
        try {
            Command iconCommand = new Command("Settings", Image.createImage(1, 1));
            iconCommand.putClientProperty("TitleCommand", Boolean.TRUE);
            Button iconButton = new Button(iconCommand);
            assertEquals("", iconButton.getText());
            assertEquals("Settings", iconButton.getAccessibilityText());

            Command textCommand = new Command("Done");
            textCommand.putClientProperty("TitleCommand", Boolean.TRUE);
            assertEquals("Done", new Button(textCommand).getText(),
                    "text-only toolbar commands must retain their label");

            Command ordinaryCommand = new Command("Open", Image.createImage(1, 1));
            assertEquals("Open", new Button(ordinaryCommand).getText(),
                    "the toolbar setting must not hide side-menu or ordinary command text");
        } finally {
            Hashtable reset = new Hashtable();
            reset.put("@hideToolbarCommandTextWithIconBool", "false");
            UIManager.getInstance().addThemeProps(reset);
        }
    }

    @FormTest
    void testSetCommandBindsCommandAndFiresAction() throws Exception {
        implementation.setBuiltinSoundsEnabled(false);
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

    @FormTest
    void testStateChangeListenerReceivesPressedAndReleasedEvents() {
        implementation.setBuiltinSoundsEnabled(false);
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

    @FormTest
    void testBindStateMirrorsSourceStateUntilUnbound() {
        implementation.setBuiltinSoundsEnabled(false);
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

    @FormTest
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
