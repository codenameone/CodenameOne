package com.codename1.gaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link VirtualButton}: hit testing, label/color, pressed state and
/// safe-area anchoring.
class VirtualButtonTest {

    @Test
    void constructorAndGetters() {
        VirtualButton b = new VirtualButton(77, 200, 300, 50);
        assertEquals(77, b.getKeyCode());
        assertEquals(200, b.getCenterX(), 0.001);
        assertEquals(300, b.getCenterY(), 0.001);
        assertEquals(50, b.getRadius(), 0.001);
        assertNull(b.getLabel());
        assertFalse(b.isPressed());
        assertEquals(0xc0ffffff, b.getColor());   // default translucent white
    }

    @Test
    void containsRespectsRadius() {
        VirtualButton b = new VirtualButton(1, 100, 100, 40);
        assertTrue(b.contains(100, 100));
        assertTrue(b.contains(130, 100));
        assertFalse(b.contains(150, 100));
    }

    @Test
    void labelAndColorAreChainable() {
        VirtualButton b = new VirtualButton(1, 0, 0, 30);
        assertSame(b, b.setLabel("Jump"));
        assertSame(b, b.setColor(0xff00ff00));
        assertEquals("Jump", b.getLabel());
        assertEquals(0xff00ff00, b.getColor());
    }

    @Test
    void pressedState() {
        VirtualButton b = new VirtualButton(1, 0, 0, 30);
        b.setPressed(true);
        assertTrue(b.isPressed());
        b.setPressed(false);
        assertFalse(b.isPressed());
    }

    @Test
    void anchorBottomRight() {
        VirtualButton b = new VirtualButton(1, 0, 0, 25);
        b.setAnchor(TouchControls.RIGHT, TouchControls.BOTTOM, 15, 15);
        b.applyAnchor(0, 0, 800, 600);
        assertEquals(800 - 15 - 25, b.getCenterX(), 0.001);
        assertEquals(600 - 15 - 25, b.getCenterY(), 0.001);
    }

    @Test
    void anchorNoOpWhenAbsolute() {
        VirtualButton b = new VirtualButton(1, 70, 80, 25);
        b.applyAnchor(0, 0, 800, 600);
        assertEquals(70, b.getCenterX(), 0.001);
        assertEquals(80, b.getCenterY(), 0.001);
    }
}
