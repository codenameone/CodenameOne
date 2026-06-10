package com.codename1.gaming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Unit tests for {@link VirtualJoystick}: analog axis math, dead zone, clamping,
/// press/drag/release and safe-area anchoring. Pure logic, no GPU or Display needed.
class VirtualJoystickTest {

    @Test
    void constructorDefaults() {
        VirtualJoystick j = new VirtualJoystick(100, 200, 80);
        assertEquals(100, j.getCenterX(), 0.001);
        assertEquals(200, j.getCenterY(), 0.001);
        assertEquals(80, j.getRadius(), 0.001);
        assertFalse(j.isActive());
        assertEquals(0, j.getAxisX(), 0.001);
        assertEquals(0, j.getAxisY(), 0.001);
        // knob rests at the center
        assertEquals(100, j.getKnobX(), 0.001);
        assertEquals(200, j.getKnobY(), 0.001);
        // default knob radius is 45% of the base radius
        assertEquals(80 * 0.45f, j.getKnobRadius(), 0.001);
    }

    @Test
    void fullRightDeflectionGivesAxisXOne() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(150, 100);   // exactly radius to the right
        assertTrue(j.isActive());
        assertEquals(1f, j.getAxisX(), 0.001);
        assertEquals(0f, j.getAxisY(), 0.001);
        assertEquals(150, j.getKnobX(), 0.001);
        assertEquals(100, j.getKnobY(), 0.001);
    }

    @Test
    void fullDownDeflectionGivesAxisYOne() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(100, 150);   // radius downward (y grows down)
        assertEquals(0f, j.getAxisX(), 0.001);
        assertEquals(1f, j.getAxisY(), 0.001);
    }

    @Test
    void insideDeadZoneReadsAsZero() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(105, 100);   // 0.1 of the radius -> below the 0.2 dead zone
        assertEquals(0f, j.getAxisX(), 0.001);
        assertEquals(0f, j.getAxisY(), 0.001);
    }

    @Test
    void beyondDeadZonePassesThrough() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(130, 100);   // 0.6 of the radius -> above the dead zone
        assertEquals(0.6f, j.getAxisX(), 0.001);
    }

    @Test
    void deflectionBeyondRadiusIsClampedToRadius() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(400, 100);   // way past the radius
        assertEquals(1f, j.getAxisX(), 0.001);     // axis saturates at 1
        assertEquals(150, j.getKnobX(), 0.001);    // knob clamped to the rim
    }

    @Test
    void releaseRecentersAndZeroesAxes() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.press(150, 100);
        assertTrue(j.isActive());
        j.release();
        assertFalse(j.isActive());
        assertEquals(0f, j.getAxisX(), 0.001);
        assertEquals(0f, j.getAxisY(), 0.001);
        assertEquals(100, j.getKnobX(), 0.001);
        assertEquals(100, j.getKnobY(), 0.001);
    }

    @Test
    void dragOnlyTracksWhileActive() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        j.drag(150, 100);                 // not pressed -> ignored
        assertEquals(0f, j.getAxisX(), 0.001);
        j.press(100, 100);
        j.drag(150, 100);                 // now it tracks
        assertEquals(1f, j.getAxisX(), 0.001);
    }

    @Test
    void containsRespectsRadius() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        assertTrue(j.contains(100, 100));
        assertTrue(j.contains(140, 100));   // within radius
        assertFalse(j.contains(200, 100));  // outside radius
    }

    @Test
    void customDeadZone() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        assertSame(j, j.setDeadZone(0.5f));
        assertEquals(0.5f, j.getDeadZone(), 0.001);
        j.press(120, 100);                  // 0.4 of radius -> inside 0.5 dead zone
        assertEquals(0f, j.getAxisX(), 0.001);
        j.press(130, 100);                  // 0.6 of radius -> outside
        assertEquals(0.6f, j.getAxisX(), 0.001);
    }

    @Test
    void setKnobRadiusIsChainable() {
        VirtualJoystick j = new VirtualJoystick(100, 100, 50);
        assertSame(j, j.setKnobRadius(12));
        assertEquals(12, j.getKnobRadius(), 0.001);
    }

    @Test
    void anchorBottomLeftWithinSafeArea() {
        VirtualJoystick j = new VirtualJoystick(0, 0, 60);
        j.setAnchor(TouchControls.LEFT, TouchControls.BOTTOM, 20, 20);
        j.applyAnchor(0, 0, 1000, 800);
        assertEquals(20 + 60, j.getCenterX(), 0.001);          // margin + radius from left
        assertEquals(800 - 20 - 60, j.getCenterY(), 0.001);    // margin + radius up from bottom
    }

    @Test
    void anchorTopRightAndCenter() {
        VirtualJoystick j = new VirtualJoystick(0, 0, 40);
        j.setAnchor(TouchControls.RIGHT, TouchControls.TOP, 10, 10);
        j.applyAnchor(100, 50, 600, 400);
        assertEquals(100 + 600 - 10 - 40, j.getCenterX(), 0.001);
        assertEquals(50 + 10 + 40, j.getCenterY(), 0.001);

        VirtualJoystick c = new VirtualJoystick(0, 0, 40);
        c.setAnchor(TouchControls.CENTER, TouchControls.CENTER, 0, 0);
        c.applyAnchor(0, 0, 1000, 800);
        assertEquals(500, c.getCenterX(), 0.001);
        assertEquals(400, c.getCenterY(), 0.001);
    }

    @Test
    void applyAnchorIsNoOpWhenNotAnchored() {
        VirtualJoystick j = new VirtualJoystick(123, 456, 40);
        j.applyAnchor(0, 0, 1000, 800);   // never anchored -> unchanged
        assertEquals(123, j.getCenterX(), 0.001);
        assertEquals(456, j.getCenterY(), 0.001);
    }
}
