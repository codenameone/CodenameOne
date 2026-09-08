package com.codename1.flutter.material;

import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A floating action button is a fixed Material size, not whatever its glyph
 * happens to need.
 *
 * <p>The size came from the component's preferred size, which is the glyph plus
 * whatever padding the theme in force carries. With no theme entry for the UIID
 * that is 83 device pixels against the reference's 168 -- less than half.</p>
 */
class FabSizeTest {

    @Test
    void aRegularFabIsAFixedSquare() {
        Size s = FabRenderElement.materialSize(false, 12);
        assertEquals(Dp.px(56), s.width(), 0.001);
        assertEquals(Dp.px(56), s.height(), 0.001);
    }

    @Test
    void anExtendedFabFixesItsHeightAndFollowsItsLabel() {
        // Material 3 puts the extended form at 56 too, not Material 2's 48.
        Size s = FabRenderElement.materialSize(true, Dp.px(300));
        assertEquals(Dp.px(300), s.width(), 0.001);
        assertEquals(Dp.px(56), s.height(), 0.001);
    }

    @Test
    void anExtendedFabIsNeverNarrowerThanTheMinimum() {
        Size s = FabRenderElement.materialSize(true, 4);
        assertEquals(Dp.px(80), s.width(), 0.001);
    }
}
