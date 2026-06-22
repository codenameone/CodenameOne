package com.codename1.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSSGradientParserTest {

    @Test
    void linearAngleAndStops() {
        Gradient g = Gradient.parseCss("linear-gradient(45deg, #ff0000, #00ff00 50%, #0000ff)");
        assertNotNull(g);
        assertEquals(Gradient.KIND_LINEAR, g.getKind());
        assertEquals(Gradient.CYCLE_NONE, g.getCycleMethod());
        LinearGradient lg = (LinearGradient) g;
        assertEquals(45f, lg.getAngleDegrees(), 0.001f);
        assertArrayEquals(new int[]{0xffff0000, 0xff00ff00, 0xff0000ff}, lg.getColors());
        float[] pos = lg.getPositions();
        assertEquals(3, pos.length);
        assertEquals(0f, pos[0], 1e-4f);
        assertEquals(0.5f, pos[1], 1e-4f);
        assertEquals(1f, pos[2], 1e-4f);
    }

    @Test
    void linearDefaultAngleIs180() {
        Gradient g = Gradient.parseCss("linear-gradient(red, blue)");
        assertEquals(180f, ((LinearGradient) g).getAngleDegrees(), 0.001f);
    }

    @Test
    void linearToSideKeywords() {
        assertEquals(0f, ((LinearGradient) Gradient.parseCss("linear-gradient(to top, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(90f, ((LinearGradient) Gradient.parseCss("linear-gradient(to right, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(180f, ((LinearGradient) Gradient.parseCss("linear-gradient(to bottom, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(270f, ((LinearGradient) Gradient.parseCss("linear-gradient(to left, red, blue)")).getAngleDegrees(), 0.001f);
    }

    @Test
    void linearToCornerKeywords() {
        assertEquals(45f, ((LinearGradient) Gradient.parseCss("linear-gradient(to top right, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(135f, ((LinearGradient) Gradient.parseCss("linear-gradient(to bottom right, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(225f, ((LinearGradient) Gradient.parseCss("linear-gradient(to bottom left, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(315f, ((LinearGradient) Gradient.parseCss("linear-gradient(to top left, red, blue)")).getAngleDegrees(), 0.001f);
    }

    @Test
    void linearAngleUnits() {
        assertEquals(90f, ((LinearGradient) Gradient.parseCss("linear-gradient(0.25turn, red, blue)")).getAngleDegrees(), 0.001f);
        assertEquals(90f, ((LinearGradient) Gradient.parseCss("linear-gradient(100grad, red, blue)")).getAngleDegrees(), 0.1f);
        assertEquals(180f, ((LinearGradient) Gradient.parseCss("linear-gradient(3.14159rad, red, blue)")).getAngleDegrees(), 0.1f);
    }

    @Test
    void linearAutoDistributesMissingStops() {
        Gradient g = Gradient.parseCss("linear-gradient(45deg, red, green, blue, black)");
        float[] pos = g.getPositions();
        assertEquals(4, pos.length);
        assertEquals(0f, pos[0], 1e-4f);
        assertEquals(1f / 3f, pos[1], 1e-4f);
        assertEquals(2f / 3f, pos[2], 1e-4f);
        assertEquals(1f, pos[3], 1e-4f);
    }

    @Test
    void linearMismatchedAlphas() {
        Gradient g = Gradient.parseCss("linear-gradient(90deg, rgba(255, 0, 0, 0.6), blue)");
        assertEquals((int) (0.6f * 255f) << 24 | 0x00ff0000, g.getColors()[0]);
        assertEquals(0xff0000ff, g.getColors()[1]);
    }

    @Test
    void hexColorVariants() {
        assertEquals(0xffaabbcc, CSSColor.parse("#abc"));
        assertEquals(0xff112233, CSSColor.parse("#112233"));
        assertEquals(0x80112233, CSSColor.parse("#11223380"));
        // #abcd is CSS RGBA shorthand: r=#aa g=#bb b=#cc a=#dd -> ARGB 0xDDAABBCC.
        assertEquals(0xddaabbcc, CSSColor.parse("#abcd"));
    }

    @Test
    void rgbAndRgbaSyntax() {
        assertEquals(0xff112233, CSSColor.parse("rgb(17, 34, 51)"));
        assertEquals(0x80ffffff, CSSColor.parse("rgba(255, 255, 255, 0.502)"));
    }

    @Test
    void namedColors() {
        assertEquals(0xff0000ff, CSSColor.parse("blue"));
        assertEquals(0x00000000, CSSColor.parse("transparent"));
        assertEquals(0xff808080, CSSColor.parse("grey"));
    }

    @Test
    void radialShapeExtentAndCenter() {
        Gradient g = Gradient.parseCss("radial-gradient(circle farthest-corner at 30% 70%, #fff, #001)");
        assertEquals(Gradient.KIND_RADIAL, g.getKind());
        RadialGradient rg = (RadialGradient) g;
        assertEquals(RadialGradient.SHAPE_CIRCLE, rg.getShape());
        assertEquals(RadialGradient.EXTENT_FARTHEST_CORNER, rg.getExtent());
        assertEquals(0.30f, rg.getRelativeCenterX(), 1e-4f);
        assertEquals(0.70f, rg.getRelativeCenterY(), 1e-4f);
    }

    @Test
    void radialEllipseClosestSide() {
        Gradient g = Gradient.parseCss("radial-gradient(ellipse closest-side at 50% 50%, #ffeeff, #002233)");
        RadialGradient rg = (RadialGradient) g;
        assertEquals(RadialGradient.SHAPE_ELLIPSE, rg.getShape());
        assertEquals(RadialGradient.EXTENT_CLOSEST_SIDE, rg.getExtent());
    }

    @Test
    void radialNoHeader() {
        // No shape/extent header - just stops. Should default to ellipse / farthest-corner / center.
        Gradient g = Gradient.parseCss("radial-gradient(red, blue)");
        RadialGradient rg = (RadialGradient) g;
        assertEquals(RadialGradient.SHAPE_ELLIPSE, rg.getShape());
        assertEquals(RadialGradient.EXTENT_FARTHEST_CORNER, rg.getExtent());
        assertEquals(0.5f, rg.getRelativeCenterX(), 1e-4f);
        assertEquals(0.5f, rg.getRelativeCenterY(), 1e-4f);
    }

    @Test
    void conicFromAndAt() {
        Gradient g = Gradient.parseCss("conic-gradient(from 90deg at 50% 50%, red, yellow, green, blue, red)");
        assertEquals(Gradient.KIND_CONIC, g.getKind());
        ConicGradient cg = (ConicGradient) g;
        assertEquals(90f, cg.getFromAngleDegrees(), 0.001f);
        assertEquals(0.5f, cg.getRelativeCenterX(), 1e-4f);
        assertEquals(0.5f, cg.getRelativeCenterY(), 1e-4f);
        assertEquals(5, cg.getColors().length);
    }

    @Test
    void repeatingLinearSetsCycleMethod() {
        Gradient g = Gradient.parseCss("repeating-linear-gradient(45deg, #eeeeee 0%, #cc3344 10%)");
        assertEquals(Gradient.KIND_LINEAR, g.getKind());
        assertEquals(Gradient.CYCLE_REPEAT, g.getCycleMethod());
    }

    @Test
    void repeatingRadialSetsCycleMethod() {
        Gradient g = Gradient.parseCss("repeating-radial-gradient(circle at center, #ffffff 0%, #cc3344 16%)");
        assertEquals(Gradient.KIND_RADIAL, g.getKind());
        assertEquals(Gradient.CYCLE_REPEAT, g.getCycleMethod());
    }

    @Test
    void returnsNullForNonGradient() {
        assertNull(Gradient.parseCss(null));
        assertNull(Gradient.parseCss(""));
        assertNull(Gradient.parseCss("   "));
        assertNull(Gradient.parseCss("url(foo.png)"));
        assertNull(Gradient.parseCss("not-a-function"));
    }

    @Test
    void singleStopRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Gradient.parseCss("linear-gradient(red)"));
    }

    @Test
    void unknownDirectionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Gradient.parseCss("linear-gradient(to nowhere, red, blue)"));
    }

    @Test
    void unknownColorRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Gradient.parseCss("linear-gradient(45deg, periwinkle, blue)"));
    }

    @Test
    void stopsClampAtEndpoints() {
        Gradient g = Gradient.parseCss("linear-gradient(90deg, #ff0000 0%, #00ff00 50%, #0000ff 100%)");
        // sampleArgb at px=0 maps to a fraction just past 0%, so we should be
        // pulled almost entirely from the red stop. Allow a small slop for
        // pixel-center bias.
        int sampled = g.sampleArgb(0, 50, 100, 100);
        assertEquals(0xff, (sampled >> 24) & 0xff);
        assertTrue(((sampled >> 16) & 0xff) > 240, "Red channel near max, got " + Integer.toHexString(sampled));
        assertTrue((sampled & 0xff) < 16, "Blue channel near zero, got " + Integer.toHexString(sampled));
    }
}
