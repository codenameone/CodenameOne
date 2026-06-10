package com.codename1.gaming;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link TouchControls}: routing multi-touch into the joystick and
/// buttons, mapping the stick to digital game actions, and safe-area relayout.
class TouchControlsTest extends UITestBase {

    private static int[] arr(int v) {
        return new int[]{v};
    }

    @Test
    void emptyHasNoControls() {
        TouchControls c = new TouchControls(new GameInput());
        assertFalse(c.hasControls());
        assertNull(c.getJoystick());
        assertEquals(0, c.getButtonCount());
    }

    @Test
    void addJoystickAndButton() {
        TouchControls c = new TouchControls(new GameInput());
        VirtualJoystick j = c.addJoystick(100, 100, 50);
        assertNotNull(j);
        assertSame(j, c.getJoystick());
        VirtualButton b = c.addButton(999, 300, 300, 40);
        assertEquals(1, c.getButtonCount());
        assertSame(b, c.getButton(0));
        assertTrue(c.hasControls());
    }

    @Test
    void pressingJoystickDrivesAxesAndGameKeys() {
        GameInput in = new GameInput();
        TouchControls c = new TouchControls(in);
        c.addJoystick(100, 100, 50);
        // touch at the right rim -> full right deflection
        c.onTouches(arr(150), arr(100), true);
        assertTrue(c.getJoystick().isActive());
        assertEquals(1f, in.getAxisX(), 0.001);
        assertTrue(in.isGameKeyDown(Display.GAME_RIGHT));
        assertFalse(in.isGameKeyDown(Display.GAME_LEFT));

        // release -> recenters and clears the digital direction
        c.onTouches(arr(150), arr(100), false);
        assertFalse(c.getJoystick().isActive());
        assertEquals(0f, in.getAxisX(), 0.001);
        assertFalse(in.isGameKeyDown(Display.GAME_RIGHT));
    }

    @Test
    void pressingJoystickUpMapsToGameUp() {
        GameInput in = new GameInput();
        TouchControls c = new TouchControls(in);
        c.addJoystick(100, 100, 50);
        c.onTouches(arr(100), arr(60), true);   // 40px up (y grows down) -> up past dead zone
        assertTrue(in.isGameKeyDown(Display.GAME_UP));
        assertTrue(in.getAxisY() < 0);
    }

    @Test
    void pressingButtonHoldsItsKey() {
        GameInput in = new GameInput();
        TouchControls c = new TouchControls(in);
        c.addButton(999, 300, 300, 40);
        c.onTouches(arr(300), arr(300), true);
        assertTrue(c.getButton(0).isPressed());
        assertTrue(in.isKeyDown(999));
        c.onTouches(arr(300), arr(300), false);
        assertFalse(c.getButton(0).isPressed());
        assertFalse(in.isKeyDown(999));
    }

    @Test
    void touchOutsideControlsDoesNothing() {
        GameInput in = new GameInput();
        TouchControls c = new TouchControls(in);
        c.addJoystick(100, 100, 50);
        c.onTouches(arr(500), arr(500), true);   // nowhere near the stick
        assertFalse(c.getJoystick().isActive());
        assertEquals(0f, in.getAxisX(), 0.001);
    }

    @Test
    void visibilityToggleResets() {
        GameInput in = new GameInput();
        TouchControls c = new TouchControls(in);
        c.addJoystick(100, 100, 50);
        c.onTouches(arr(150), arr(100), true);
        assertTrue(c.isVisible());
        c.setVisible(false);
        assertFalse(c.isVisible());
        assertFalse(c.getJoystick().isActive());   // hiding releases the stick
    }

    @Test
    void relayoutRepositionsAnchoredControls() {
        TouchControls c = new TouchControls(new GameInput());
        VirtualJoystick j = c.addJoystick(80, TouchControls.LEFT, TouchControls.BOTTOM, 24);
        c.addButton(1, 55, TouchControls.RIGHT, TouchControls.BOTTOM, 30);
        c.relayout(0, 0, 1000, 800);
        assertEquals(24 + 80, j.getCenterX(), 0.001);
        assertEquals(800 - 24 - 80, j.getCenterY(), 0.001);
        VirtualButton b = c.getButton(0);
        assertEquals(1000 - 30 - 55, b.getCenterX(), 0.001);
        assertEquals(800 - 30 - 55, b.getCenterY(), 0.001);
    }
}
