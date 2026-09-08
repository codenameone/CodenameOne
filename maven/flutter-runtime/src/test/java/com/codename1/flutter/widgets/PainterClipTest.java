package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A CustomPainter keeps the ground an ancestor Transform shifts it onto.
 *
 * <p>Flutter does not clip a CustomPainter to the box it was handed -- an
 * ancestor ClipRect does that -- so a painter may draw well outside its own
 * size. Codename One clips every component to its bounds, and when an ancestor
 * Transform has moved the origin those bounds move with it, so the part of the
 * drawing the shift brings into view is cut off instead. The 2D-transformations
 * demo centres a board wider than the screen by translating it, and lost a strip
 * down the right-hand side exactly as wide as the shift.</p>
 */
class PainterClipTest {

    /** The demo's real numbers: a 1029-wide surface at x=48, shifted 192 left. */
    @Test
    void aShiftedSurfaceClipsToWhereItIsOnScreen() {
        int[] box = CustomPaintRenderElement.onScreenClip(
                -144, 468, 0, 0, 48, 468, 1029, 1651);
        // Translated space, so screen x 48..1077 is 192..1221 here.
        assertArrayEquals(new int[] {192, 0, 1029, 1651}, box);
    }

    @Test
    void anUnshiftedSurfaceIsLeftAlone() {
        // translate + parent-relative == absolute, so no transform is in play
        // and the clip already in force is the right one.
        assertNull(CustomPaintRenderElement.onScreenClip(
                48, 468, 0, 0, 48, 468, 1029, 1651));
    }

    @Test
    void aVerticalShiftIsUndoneToo() {
        int[] box = CustomPaintRenderElement.onScreenClip(
                48, 300, 0, 0, 48, 468, 100, 200);
        assertArrayEquals(new int[] {0, 168, 100, 200}, box);
    }
}
