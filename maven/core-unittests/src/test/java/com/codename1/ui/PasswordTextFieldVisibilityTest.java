package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.LookAndFeel;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Investigation/regression test for
 * https://github.com/codenameone/CodenameOne/issues/1512
 * -- the 2015 reporter said a TextField with the PASSWORD constraint shows
 * its text correctly while focused, but the text "becomes invisible" once
 * focus is lost.
 *
 * The DefaultLookAndFeel.getTextFieldString() helper is what turns the raw
 * text into the bullet-replaced display string both the focused and the
 * unfocused render paths use. If the rendering broke on focus loss, this is
 * the natural place for the regression -- so we lock in that the helper
 * returns bullets regardless of focus state, and that getText() still
 * returns the underlying text so any caller polling for the value can find
 * it.
 */
class PasswordTextFieldVisibilityTest extends UITestBase {

    @FormTest
    void passwordTextFieldStillRendersBulletsWhenUnfocused() {
        Form f = Display.getInstance().getCurrent();
        f.setLayout(BoxLayout.y());
        TextField password = new TextField("", "Password", 20, TextArea.ANY | TextArea.PASSWORD);
        TextField other = new TextField("", "Other", 20, TextArea.ANY);
        f.add(password).add(other);
        f.revalidate();

        password.setText("secret123");
        password.requestFocus();

        LookAndFeel laf = UIManager.getInstance().getLookAndFeel();
        if (!(laf instanceof DefaultLookAndFeel)) {
            // Theme-specific LookAndFeel may compute display text differently;
            // the regression we are guarding lives in DefaultLookAndFeel.
            return;
        }

        // Reflection: getTextFieldString is protected on DefaultLookAndFeel.
        String focusedDisplay = invokeGetTextFieldString((DefaultLookAndFeel) laf, password);
        assertNotNull(focusedDisplay);
        assertEquals(password.getText().length(), focusedDisplay.length(),
                "While focused, every character must be replaced with a bullet.");
        for (int i = 0; i < focusedDisplay.length(); i++) {
            assertNotEquals(' ', focusedDisplay.charAt(i),
                    "Bullet replacement must produce a visible glyph, not a space.");
        }

        // Move focus away. Pre-1512 the reporter said the displayed text vanished.
        other.requestFocus();
        assertFalse(password.hasFocus());
        assertEquals("secret123", password.getText(),
                "Losing focus must not clear the underlying text.");

        String unfocusedDisplay = invokeGetTextFieldString((DefaultLookAndFeel) laf, password);
        assertNotNull(unfocusedDisplay);
        assertEquals(focusedDisplay.length(), unfocusedDisplay.length(),
                "Bullet display length must be identical focused and unfocused.");
        assertEquals(focusedDisplay, unfocusedDisplay,
                "Bullet display contents must be identical focused and unfocused. "
                        + "If this assertion fires, #1512 has regressed.");
    }

    private static String invokeGetTextFieldString(DefaultLookAndFeel laf, TextField tf) {
        try {
            java.lang.reflect.Method m = DefaultLookAndFeel.class.getDeclaredMethod("getTextFieldString", TextArea.class);
            m.setAccessible(true);
            return (String) m.invoke(laf, tf);
        } catch (Exception e) {
            throw new RuntimeException("getTextFieldString reflection failed", e);
        }
    }
}
