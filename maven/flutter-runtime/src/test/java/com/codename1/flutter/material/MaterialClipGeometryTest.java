package com.codename1.flutter.material;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rounded clip a Material surface installs, intersected with the clip it inherits.
 *
 * <p>This used to go through {@code GeneralPath.intersection()}, a general polygon clipper
 * that allocated per card per frame and drove the collector from inside paint. It is now
 * computed directly, which is only correct if the intersection really does reduce to
 * "the intersected bounds, rounded at the corners the clip left alone" - so that is what
 * these assert.</p>
 */
class MaterialClipGeometryTest {

    /// Bounds and the four corner radii, clockwise from the top left.
    private static int[] geom(int x, int y, int w, int h, int r,
            int cx, int cy, int cw, int ch) {
        int[] out = new int[8];
        assertTrue(MaterialRenderElement.clipGeometry(out, x, y, w, h, r, cx, cy, cw, ch),
                "expected the surface to be at least partly visible");
        return out;
    }

    @Test
    @DisplayName("fully inside the clip: untouched bounds, all four corners rounded")
    void insideClipKeepsEveryCorner() {
        assertArrayEquals(new int[] {100, 100, 200, 150, 20, 20, 20, 20},
                geom(100, 100, 200, 150, 20, 0, 0, 1000, 1000));
    }

    @Test
    @DisplayName("a card half off the left of its viewport keeps only its right corners")
    void cutOnTheLeftSquaresTheLeftCorners() {
        // The viewport starts at x=200; the card runs 100..300, so its left half is gone.
        assertArrayEquals(new int[] {200, 100, 100, 150, 0, 20, 20, 0},
                geom(100, 100, 200, 150, 20, 200, 0, 800, 1000));
    }

    @Test
    @DisplayName("a card half off the right of its viewport keeps only its left corners")
    void cutOnTheRightSquaresTheRightCorners() {
        assertArrayEquals(new int[] {100, 100, 100, 150, 20, 0, 0, 20},
                geom(100, 100, 200, 150, 20, 0, 0, 200, 1000));
    }

    @Test
    @DisplayName("a card cut top and bottom keeps no corners but still clips to the band")
    void cutOnBothAxesSquaresEverything() {
        assertArrayEquals(new int[] {100, 120, 200, 100, 0, 0, 0, 0},
                geom(100, 100, 200, 150, 20, 0, 120, 1000, 100));
    }

    @Test
    @DisplayName("the surviving radius never exceeds half the visible box")
    void radiusIsClampedToTheVisibleSliver() {
        // Only 24px of the card's right edge is left, so a 20px corner would overlap itself.
        int[] q = geom(100, 100, 200, 150, 20, 276, 0, 800, 1000);
        assertArrayEquals(new int[] {276, 100, 24, 150}, new int[] {q[0], q[1], q[2], q[3]});
        assertArrayEquals(new int[] {0, 12, 12, 0}, new int[] {q[4], q[5], q[6], q[7]});
    }

    @Test
    @DisplayName("scrolled entirely out of the viewport: nothing to paint")
    void offscreenReportsNothingVisible() {
        assertFalse(MaterialRenderElement.clipGeometry(new int[8],
                100, 100, 200, 150, 20, 400, 0, 300, 1000));
        // Touching edges only - an empty intersection, not a one-pixel sliver.
        assertFalse(MaterialRenderElement.clipGeometry(new int[8],
                100, 100, 200, 150, 20, 300, 0, 300, 1000));
    }

    @Test
    @DisplayName("the clip is a genuine intersection, never wider than what it inherited")
    void neverPaintsOutsideTheInheritedClip() {
        // The bug this guards: setClip(Shape) REPLACES the clip, so a surface that ignored
        // the incoming one would paint its rounded rect over its neighbours.
        int[] q = geom(100, 100, 200, 150, 20, 150, 130, 100, 60);
        assertTrue(q[0] >= 150 && q[1] >= 130, "origin escaped the inherited clip");
        assertTrue(q[0] + q[2] <= 250 && q[1] + q[3] <= 190, "extent escaped the inherited clip");
    }
}
