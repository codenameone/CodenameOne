package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the opt-in iOS-style button release dim-out (the pressed background
 * fades back to normal over a few frames instead of snapping off). The fade is
 * gated by the {@code buttonReleaseFadeDurationInt} theme constant so it must be
 * inert by default and only arm on release once a theme sets the constant.
 */
public class ButtonReleaseFadeTest extends UITestBase {

    private static Button laidOutButton(String text) {
        Button b = new Button(text);
        b.setX(0);
        b.setY(0);
        b.setWidth(120);
        b.setHeight(44);
        return b;
    }

    @Test
    public void noFadeWhenConstantUnset() {
        Button b = laidOutButton("off");
        b.pressed();
        b.released(10, 10);
        assertFalse(b.isReleaseFadeActive(),
                "release must not fade when buttonReleaseFadeDurationInt is unset (default 0)");
    }

    @Test
    public void releaseArmsFadeWhenConstantEnabled() {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("@buttonReleaseFadeDurationInt", "180");
        UIManager.getInstance().addThemeProps(props);
        try {
            // The constant is read in the Button constructor, so build after enabling it.
            Button b = laidOutButton("on");
            b.pressed();
            assertTrue(b.isPressedStyle(), "button should be in the pressed state before release");
            b.released(10, 10);
            assertTrue(b.isReleaseFadeActive(),
                    "release should arm the dim-out fade when the constant is set");
        } finally {
            // Reset so the enabled constant doesn't leak into sibling tests.
            Hashtable<String, String> reset = new Hashtable<String, String>();
            reset.put("@buttonReleaseFadeDurationInt", "0");
            UIManager.getInstance().addThemeProps(reset);
        }
    }
}
