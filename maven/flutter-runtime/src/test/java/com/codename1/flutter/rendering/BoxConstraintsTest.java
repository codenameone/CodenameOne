package com.codename1.flutter.rendering;

import com.codename1.flutter.EdgeInsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxConstraintsTest {

    @Test
    void tightConstraintsForceExactSize() {
        BoxConstraints c = BoxConstraints.tight(100, 50);
        assertTrue(c.isTight());
        assertEquals(100, c.minWidth());
        assertEquals(100, c.maxWidth());
        assertEquals(50, c.minHeight());
        assertEquals(50, c.maxHeight());
        Size s = c.constrain(new Size(3, 900));
        assertEquals(new Size(100, 50), s);
    }

    @Test
    void looseConstraintsAllowAnySizeUpToMax() {
        BoxConstraints c = BoxConstraints.loose(200, 300);
        assertEquals(0, c.minWidth());
        assertEquals(0, c.minHeight());
        assertFalse(c.isTight());
        assertEquals(new Size(150, 300), c.constrain(new Size(150, 999)));
        assertEquals(new Size(0, 0), c.constrain(new Size(-5, 0)));
    }

    @Test
    void constrainClampsBothDirections() {
        BoxConstraints c = new BoxConstraints(10, 100, 20, 50);
        assertEquals(new Size(10, 20), c.constrain(Size.ZERO));
        assertEquals(new Size(100, 50), c.constrain(new Size(500, 500)));
        assertEquals(new Size(55, 33), c.constrain(new Size(55, 33)));
    }

    @Test
    void deflateRemovesInsetsAndNeverGoesNegative() {
        BoxConstraints c = new BoxConstraints(10, 100, 10, 100);
        BoxConstraints d = c.deflate(EdgeInsets.symmetric(8, 3));
        // horizontal insets = 16, vertical = 6
        assertEquals(0, d.minWidth());
        assertEquals(84, d.maxWidth());
        assertEquals(4, d.minHeight());
        assertEquals(94, d.maxHeight());

        BoxConstraints tiny = BoxConstraints.tight(10, 10);
        BoxConstraints dt = tiny.deflate(EdgeInsets.all(20));
        assertEquals(0, dt.minWidth());
        assertEquals(0, dt.maxWidth());
        assertEquals(0, dt.minHeight());
        assertEquals(0, dt.maxHeight());
    }

    @Test
    void deflateKeepsUnboundedMaxUnbounded() {
        BoxConstraints c = new BoxConstraints(0, Double.POSITIVE_INFINITY, 0, 500);
        BoxConstraints d = c.deflate(EdgeInsets.all(10));
        assertFalse(d.hasBoundedWidth());
        assertEquals(480, d.maxHeight());
    }

    @Test
    void loosenDropsMinimums() {
        BoxConstraints c = BoxConstraints.tight(100, 50);
        BoxConstraints l = c.loosen();
        assertEquals(0, l.minWidth());
        assertEquals(0, l.minHeight());
        assertEquals(100, l.maxWidth());
        assertEquals(50, l.maxHeight());
    }

    @Test
    void tightenClampsWithinExistingBounds() {
        BoxConstraints c = new BoxConstraints(10, 100, 10, 100);
        BoxConstraints t = c.tighten(50.0, 200.0);
        assertEquals(50, t.minWidth());
        assertEquals(50, t.maxWidth());
        // requested 200 clamps to the existing max of 100
        assertEquals(100, t.minHeight());
        assertEquals(100, t.maxHeight());
        // null leaves the axis untouched
        BoxConstraints w = c.tighten(null, 40.0);
        assertEquals(10, w.minWidth());
        assertEquals(100, w.maxWidth());
        assertEquals(40, w.minHeight());
        assertEquals(40, w.maxHeight());
    }
}
