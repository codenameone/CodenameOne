/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.designer.css.raster;

import static com.codename1.designer.css.raster.RasterAssert.BLUE;
import static com.codename1.designer.css.raster.RasterAssert.GREEN;
import static com.codename1.designer.css.raster.RasterAssert.RED;
import static com.codename1.designer.css.raster.RasterAssert.assertNear;
import static com.codename1.designer.css.raster.RasterAssert.assertPixelNear;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.designer.css.raster.GradientSpec.Extent;
import com.codename1.designer.css.raster.GradientSpec.Shape;
import com.codename1.designer.css.raster.GradientSpec.Unit;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// Gradient geometry and colour, checked at points with a known answer.
class GradientPainterTest {
    private static final double EPS = 1e-9;
    /// A quarter of the way from red to blue.
    private static final int QUARTER = 0xffbf0040;
    private static final int HALF = 0xff800080;

    private static GradientSpec redBlue(GradientSpec g) {
        return g.addStop(RED).addStop(BLUE);
    }

    private static GradientPainter on(GradientSpec g, double w, double h) {
        return new GradientPainter(g, 0, 0, w, h);
    }

    @Test
    void ninetyDegreesRunsLeftToRight() {
        GradientPainter p = on(redBlue(GradientSpec.linear(90)), 100, 50);
        assertEquals(0, p.parameterAt(0, 10), EPS);
        assertEquals(0.25, p.parameterAt(25, 40), EPS);
        assertEquals(1, p.parameterAt(100, 0), EPS);
        assertEquals(RED, p.colorAtPoint(0, 25));
        assertNear(QUARTER, p.colorAtPoint(25, 25), 1, "25%");
        assertNear(HALF, p.colorAtPoint(50, 25), 1, "50%");
        assertEquals(BLUE, p.colorAtPoint(100, 25));
        // Beyond the ends the end colours continue.
        assertEquals(RED, p.colorAtPoint(-20, 25));
        assertEquals(BLUE, p.colorAtPoint(140, 25));
    }

    @Test
    void zeroDegreesRunsBottomToTop() {
        GradientPainter p = on(redBlue(GradientSpec.linear(0)), 100, 50);
        assertEquals(0, p.parameterAt(30, 50), EPS);
        assertEquals(1, p.parameterAt(30, 0), EPS);
        assertEquals(0.5, p.parameterAt(99, 25), EPS);
        assertEquals(RED, p.colorAtPoint(10, 50));
        assertEquals(BLUE, p.colorAtPoint(10, 0));
    }

    @Test
    void oneEightyDegreesRunsTopToBottomAndIsTheDefault() {
        GradientPainter p = on(redBlue(GradientSpec.linear(180)), 100, 50);
        assertEquals(0, p.parameterAt(30, 0), EPS);
        assertEquals(1, p.parameterAt(30, 50), EPS);
        assertNear(QUARTER, p.colorAtPoint(70, 12.5), 1, "25% down");
        GradientPainter d = on(redBlue(new GradientSpec()), 100, 50);
        assertEquals(0.25, d.parameterAt(5, 12.5), EPS);
    }

    @Test
    void twoSeventyDegreesRunsRightToLeft() {
        GradientPainter p = on(redBlue(GradientSpec.linear(270)), 100, 50);
        assertEquals(0, p.parameterAt(100, 7), EPS);
        assertEquals(1, p.parameterAt(0, 7), EPS);
    }

    @Test
    void fortyFiveDegreesReachesTheCornersOfASquare() {
        GradientPainter p = on(redBlue(GradientSpec.linear(45)), 100, 100);
        assertEquals(0, p.parameterAt(0, 100), EPS);
        assertEquals(1, p.parameterAt(100, 0), EPS);
        assertEquals(0.5, p.parameterAt(50, 50), EPS);
        assertEquals(0.5, p.parameterAt(0, 0), EPS);
        assertEquals(0.5, p.parameterAt(100, 100), EPS);
        assertEquals(RED, p.colorAtPoint(0, 100));
        assertEquals(BLUE, p.colorAtPoint(100, 0));
    }

    @Test
    void angledGradientLineIsLongEnoughToReachTheCorners() {
        // In a 200x100 box at 45 degrees the 0% and 100% lines still pass
        // through the bottom-left and top-right corners.
        GradientPainter p = on(redBlue(GradientSpec.linear(45)), 200, 100);
        assertEquals(0, p.parameterAt(0, 100), EPS);
        assertEquals(1, p.parameterAt(200, 0), EPS);
        // But the 50% line does not pass through the other two corners.
        assertEquals(1.0 / 3, p.parameterAt(0, 0), 1e-6);
    }

    @Test
    void toCornerUsesTheMagicAngle() {
        // "to top right" in a 200x100 box: 0% at the bottom left, 100% at the
        // top right, and the 50% line through the two other corners.
        GradientPainter p = on(redBlue(GradientSpec.linearTo(1, -1)), 200, 100);
        assertEquals(0, p.parameterAt(0, 100), EPS);
        assertEquals(1, p.parameterAt(200, 0), EPS);
        assertEquals(0.5, p.parameterAt(0, 0), EPS);
        assertEquals(0.5, p.parameterAt(200, 100), EPS);
        assertEquals(0.5, p.parameterAt(100, 50), EPS);

        GradientPainter bl = on(redBlue(GradientSpec.linearTo(-1, 1)), 200, 100);
        assertEquals(1, bl.parameterAt(0, 100), EPS);
        assertEquals(0, bl.parameterAt(200, 0), EPS);
        assertEquals(0.5, bl.parameterAt(0, 0), EPS);

        GradientPainter br = on(redBlue(GradientSpec.linearTo(1, 1)), 200, 100);
        assertEquals(1, br.parameterAt(200, 100), EPS);
        assertEquals(0.5, br.parameterAt(200, 0), EPS);

        GradientPainter tl = on(redBlue(GradientSpec.linearTo(-1, -1)), 200, 100);
        assertEquals(1, tl.parameterAt(0, 0), EPS);
        assertEquals(0.5, tl.parameterAt(0, 100), EPS);
    }

    @Test
    void toSideMatchesItsAngle() {
        assertEquals(0.25, on(redBlue(GradientSpec.linearTo(1, 0)), 100, 50).parameterAt(25, 9), EPS);
        assertEquals(0.75, on(redBlue(GradientSpec.linearTo(-1, 0)), 100, 50).parameterAt(25, 9), EPS);
        assertEquals(0.2, on(redBlue(GradientSpec.linearTo(0, 1)), 100, 50).parameterAt(25, 10), EPS);
        assertEquals(0.8, on(redBlue(GradientSpec.linearTo(0, -1)), 100, 50).parameterAt(25, 10), EPS);
        // Without a side the direction is the CSS default, to bottom.
        assertEquals(0.2, on(redBlue(GradientSpec.linearTo(0, 0)), 100, 50).parameterAt(25, 10), EPS);
    }

    @Test
    void gradientBoxOffsetIsHonoured() {
        GradientPainter p = new GradientPainter(redBlue(GradientSpec.linear(90)), 10, 20, 100, 50);
        assertEquals(0, p.parameterAt(10, 0), EPS);
        assertEquals(1, p.parameterAt(110, 0), EPS);
    }

    @Test
    void hardStopIsASharpEdge() {
        GradientSpec g = GradientSpec.linear(90)
                .addStop(RED, 0, Unit.PERCENT).addStop(RED, 50, Unit.PERCENT)
                .addStop(BLUE, 50, Unit.PERCENT).addStop(BLUE, 100, Unit.PERCENT);
        GradientPainter p = on(g, 100, 10);
        assertEquals(RED, p.colorAtPoint(49.999, 5));
        assertEquals(BLUE, p.colorAtPoint(50, 5), "exactly on the edge the later stop wins");
        assertEquals(BLUE, p.colorAtPoint(50.001, 5));
        int[] px = p.paint(100, 10);
        assertEquals(RED, px[49]);
        assertEquals(BLUE, px[50]);
    }

    @Test
    void stopsWithoutPositionsAreSpreadEvenly() {
        GradientSpec g = GradientSpec.linear(90).addStop(RED).addStop(GREEN).addStop(BLUE).addStop(RED);
        assertArrayEquals(new double[] {0, 1.0 / 3, 2.0 / 3, 1}, on(g, 90, 10).stopPositions(), EPS);
        assertEquals(GREEN, on(g, 90, 10).colorAtPoint(30, 5));
        assertEquals(BLUE, on(g, 90, 10).colorAtPoint(60, 5));
    }

    @Test
    void fixUpClampsAndFillsRuns() {
        // red, green 30%, blue, red, green 20%, blue
        GradientSpec g = GradientSpec.linear(90)
                .addStop(RED).addStop(GREEN, 30, Unit.PERCENT).addStop(BLUE).addStop(RED)
                .addStop(GREEN, 20, Unit.PERCENT).addStop(BLUE);
        // 20% is before 30% and is moved up to it; the run between two
        // stops at 30% collapses onto 30%.
        assertArrayEquals(new double[] {0, 0.3, 0.3, 0.3, 0.3, 1}, on(g, 100, 10).stopPositions(), EPS);

        GradientSpec h = GradientSpec.linear(90)
                .addStop(RED, 10, Unit.PERCENT).addStop(GREEN).addStop(BLUE).addStop(RED)
                .addStop(GREEN, 50, Unit.PERCENT);
        assertArrayEquals(new double[] {0.1, 0.2, 0.3, 0.4, 0.5}, on(h, 100, 10).stopPositions(), EPS);

        double[] raw = {Double.NaN, Double.NaN, 0.5, Double.NaN, 0.25, Double.NaN};
        GradientPainter.fixUp(raw);
        assertArrayEquals(new double[] {0, 0.25, 0.5, 0.5, 0.5, 1}, raw, EPS);
    }

    @Test
    void pixelStopsUseTheGradientLineLength() {
        GradientSpec g = GradientSpec.linear(90).addStop(RED, 10, Unit.PX).addStop(BLUE, 30, Unit.PX);
        GradientPainter p = on(g, 200, 10);
        assertArrayEquals(new double[] {0.05, 0.15}, p.stopPositions(), EPS);
        assertEquals(RED, p.colorAtPoint(5, 5));
        assertNear(HALF, p.colorAtPoint(20, 5), 1, "half way between 10px and 30px");
        assertEquals(BLUE, p.colorAtPoint(40, 5));
        // A px stop mixed with a percentage one.
        GradientSpec m = GradientSpec.linear(90).addStop(RED, 50, Unit.PX).addStop(BLUE, 75, Unit.PERCENT);
        assertArrayEquals(new double[] {0.25, 0.75}, on(m, 200, 10).stopPositions(), EPS);
    }

    @Test
    void repeatingGradientWraps() {
        GradientSpec g = GradientSpec.linear(90).repeating(true)
                .addStop(RED, 0, Unit.PX).addStop(BLUE, 20, Unit.PX);
        GradientPainter p = on(g, 100, 10);
        int first = p.colorAtPoint(5, 5);
        assertNear(QUARTER, first, 1, "5px into a 20px period");
        assertEquals(first, p.colorAtPoint(25, 5));
        assertEquals(first, p.colorAtPoint(45, 5));
        assertEquals(first, p.colorAtPoint(85, 5));
        assertNear(BLUE, p.colorAtPoint(19.99, 5), 1, "end of a period");
        assertNear(RED, p.colorAtPoint(20.01, 5), 1, "start of the next period");
        // It also repeats before the first stop.
        GradientSpec late = GradientSpec.linear(90).repeating(true)
                .addStop(RED, 40, Unit.PX).addStop(BLUE, 60, Unit.PX);
        assertNear(QUARTER, on(late, 100, 10).colorAtPoint(5, 5), 1, "wrapped backwards");
        // The non-repeating form of the same gradient is flat there.
        GradientSpec flat = GradientSpec.linear(90).addStop(RED, 40, Unit.PX).addStop(BLUE, 60, Unit.PX);
        assertEquals(RED, on(flat, 100, 10).colorAtPoint(5, 5));
    }

    @Test
    void fadeToTransparentIsNotGreyed() {
        // "transparent" is transparent BLACK. Interpolating the channels
        // unpremultiplied would give 0x80800000 half way.
        GradientSpec g = GradientSpec.linear(90).addStop(0x00000000).addStop(RED);
        GradientPainter p = on(g, 100, 10);
        assertNear(0x80ff0000, p.colorAtPoint(50, 5), 1, "half way to transparent");
        assertNear(0x40ff0000, p.colorAtPoint(25, 5), 1, "a quarter of the way");
        assertEquals(0, p.colorAtPoint(0, 5));
        // Between two colours of different alpha the more opaque one weighs more.
        GradientSpec h = GradientSpec.linear(90).addStop(0x40ff0000).addStop(0xff0000ff);
        // a = 0.6255; red = 0.5 * 0.251 / 0.6255 = 0.2006; blue = 0.5 / 0.6255 = 0.7994
        assertNear(0xa03300cc, on(h, 100, 10).colorAtPoint(50, 5), 2, "premultiplied mix");
    }

    @Test
    void radialCircleExtents() {
        // Centre at (50,25) in a 200x100 box: 50 left, 150 right, 25 top, 75 bottom.
        double[][] cases = {
            {Extent.CLOSEST_SIDE.ordinal(), 25},
            {Extent.FARTHEST_SIDE.ordinal(), 150},
            {Extent.CLOSEST_CORNER.ordinal(), Math.sqrt(50 * 50 + 25 * 25)},
            {Extent.FARTHEST_CORNER.ordinal(), Math.sqrt(150 * 150 + 75 * 75)},
        };
        for (double[] c : cases) {
            Extent e = Extent.values()[(int) c[0]];
            double r = c[1];
            GradientPainter p = on(redBlue(GradientSpec.radial(Shape.CIRCLE, e).center(50, false, 25, false)), 200, 100);
            assertEquals(0, p.parameterAt(50, 25), EPS, e + " centre");
            assertEquals(1, p.parameterAt(50 + r, 25), 1e-9, e + " right");
            assertEquals(1, p.parameterAt(50, 25 + r), 1e-9, e + " below");
            assertEquals(0.5, p.parameterAt(50 - r / 2, 25), 1e-9, e + " half way left");
            assertEquals(RED, p.colorAtPoint(50, 25), e + " centre colour");
            assertEquals(BLUE, p.colorAtPoint(50 + r, 25), e + " edge colour");
            assertNear(HALF, p.colorAtPoint(50, 25 - r / 2), 1, e + " half way colour");
        }
    }

    @Test
    void radialEllipseExtents() {
        Shape s = Shape.ELLIPSE;
        GradientPainter cs = on(redBlue(GradientSpec.radial(s, Extent.CLOSEST_SIDE).center(50, false, 25, false)), 200, 100);
        assertEquals(1, cs.parameterAt(100, 25), EPS);
        assertEquals(1, cs.parameterAt(0, 25), EPS);
        assertEquals(1, cs.parameterAt(50, 50), EPS);
        assertEquals(1, cs.parameterAt(50, 0), EPS);

        GradientPainter fs = on(redBlue(GradientSpec.radial(s, Extent.FARTHEST_SIDE).center(50, false, 25, false)), 200, 100);
        assertEquals(1, fs.parameterAt(200, 25), EPS);
        assertEquals(1, fs.parameterAt(50, 100), EPS);

        // The -corner ellipses pass through the corner and keep the aspect
        // ratio of the matching -side ellipse (2:1 here).
        GradientPainter cc = on(redBlue(GradientSpec.radial(s, Extent.CLOSEST_CORNER).center(50, false, 25, false)), 200, 100);
        assertEquals(1, cc.parameterAt(0, 0), EPS);
        assertEquals(1, cc.parameterAt(50 + 50 * Math.sqrt(2), 25), EPS);
        assertEquals(1, cc.parameterAt(50, 25 + 25 * Math.sqrt(2)), EPS);

        GradientPainter fc = on(redBlue(GradientSpec.radial(s, Extent.FARTHEST_CORNER).center(50, false, 25, false)), 200, 100);
        assertEquals(1, fc.parameterAt(200, 100), EPS);
        assertEquals(1, fc.parameterAt(50 + 150 * Math.sqrt(2), 25), EPS);

        // The CSS default: ellipse farthest-corner at the centre.
        GradientPainter d = on(redBlue(new GradientSpec().type(GradientSpec.Type.RADIAL)), 200, 100);
        assertEquals(0, d.parameterAt(100, 50), EPS);
        assertEquals(1, d.parameterAt(0, 0), EPS);
        assertEquals(1, d.parameterAt(200, 100), EPS);
        assertEquals(RED, d.colorAtPoint(100, 50));
        assertEquals(BLUE, d.colorAtPoint(200, 0));
    }

    @Test
    void radialExplicitRadiiAndPercentCentre() {
        GradientPainter e = on(redBlue(GradientSpec.radial(Shape.ELLIPSE, Extent.EXPLICIT)
                .radiusX(40).radiusY(20).center(50, false, 25, false)), 200, 100);
        assertEquals(1, e.parameterAt(90, 25), EPS);
        assertEquals(1, e.parameterAt(50, 45), EPS);
        assertEquals(0.5, e.parameterAt(30, 25), EPS);

        GradientPainter c = on(redBlue(GradientSpec.radial(Shape.CIRCLE, Extent.EXPLICIT)
                .radiusX(30).radiusY(999).center(50, false, 25, false)), 200, 100);
        assertEquals(1, c.parameterAt(50, 55), EPS, "a circle ignores radiusY");

        // 25% 50% of a 200x100 box at (10,20) is (60,70); closest side is 50 away.
        GradientPainter pc = new GradientPainter(redBlue(GradientSpec.radial(Shape.CIRCLE, Extent.CLOSEST_SIDE)
                .center(25, true, 50, true)), 10, 20, 200, 100);
        assertEquals(0, pc.parameterAt(60, 70), EPS);
        assertEquals(1, pc.parameterAt(110, 70), EPS);
        assertEquals(1, pc.parameterAt(60, 20), EPS);

        // Stops in px are measured along the horizontal radius.
        GradientPainter px = on(GradientSpec.radial(Shape.CIRCLE, Extent.EXPLICIT).radiusX(50)
                .addStop(RED, 10, Unit.PX).addStop(BLUE, 30, Unit.PX), 200, 100);
        assertEquals(RED, px.colorAtPoint(105, 50));
        assertNear(HALF, px.colorAtPoint(120, 50), 1, "20px from the centre");
        assertEquals(BLUE, px.colorAtPoint(100, 85));
    }

    @Test
    void degenerateRadialIsTheLastColour() {
        GradientPainter p = on(redBlue(GradientSpec.radial(Shape.CIRCLE, Extent.CLOSEST_SIDE)
                .center(0, true, 50, true)), 200, 100);
        assertEquals(BLUE, p.colorAtPoint(100, 50));
        assertEquals(BLUE, p.colorAtPoint(0, 50));
    }

    @Test
    void conicStartsAtTheTopAndTurnsClockwise() {
        GradientPainter p = on(redBlue(GradientSpec.conic(0)), 100, 100);
        assertEquals(0.25, p.parameterAt(90, 50), EPS);
        assertEquals(0.5, p.parameterAt(50, 90), EPS);
        assertEquals(0.75, p.parameterAt(10, 50), EPS);
        assertEquals(0.125, p.parameterAt(80, 20), EPS);
        assertNear(RED, p.colorAtPoint(50.01, 10), 1, "just past 0 degrees");
        assertNear(QUARTER, p.colorAtPoint(90, 50), 1, "90 degrees");
        assertNear(HALF, p.colorAtPoint(50, 90), 1, "180 degrees");
        assertNear(0xff4000bf, p.colorAtPoint(10, 50), 1, "270 degrees");
        assertNear(BLUE, p.colorAtPoint(49.99, 10), 1, "just before 360 degrees");
    }

    @Test
    void conicFromAngleRotatesTheStart() {
        GradientPainter p = on(redBlue(GradientSpec.conic(90)), 100, 100);
        assertNear(RED, p.colorAtPoint(90, 50.01), 1, "0 from the from angle");
        assertEquals(0.25, p.parameterAt(50, 90), EPS);
        assertEquals(0.5, p.parameterAt(10, 50), EPS);
        assertEquals(0.75, p.parameterAt(50, 10), EPS);
        assertNear(QUARTER, p.colorAtPoint(50, 90), 1, "90 past the from angle");
        assertNear(HALF, p.colorAtPoint(10, 50), 1, "180 past the from angle");
        assertNear(0xff4000bf, p.colorAtPoint(50, 10), 1, "270 past the from angle");
        GradientPainter neg = on(redBlue(GradientSpec.conic(-90)), 100, 100);
        assertEquals(0.25, neg.parameterAt(50, 10), EPS);
    }

    @Test
    void conicCentreAndDegreeStops() {
        GradientPainter p = on(GradientSpec.conic(0).center(20, false, 30, false)
                .addStop(RED, 0, Unit.DEGREES).addStop(BLUE, 180, Unit.DEGREES), 100, 100);
        assertArrayEquals(new double[] {0, 0.5}, p.stopPositions(), EPS);
        assertEquals(0.25, p.parameterAt(60, 30), EPS);
        assertNear(HALF, p.colorAtPoint(60, 30), 1, "90 of 180 degrees");
        assertEquals(BLUE, p.colorAtPoint(20, 70));
        assertEquals(BLUE, p.colorAtPoint(5, 30), "past the last stop");
        // A pie chart: hard stops.
        GradientPainter pie = on(GradientSpec.conic(0)
                .addStop(RED, 0, Unit.PERCENT).addStop(RED, 25, Unit.PERCENT)
                .addStop(BLUE, 25, Unit.PERCENT).addStop(BLUE, 100, Unit.PERCENT), 100, 100);
        assertEquals(RED, pie.colorAtPoint(80, 20));
        assertEquals(BLUE, pie.colorAtPoint(80, 80));
        assertEquals(BLUE, pie.colorAtPoint(20, 20));
        // Repeating: four identical quarter turns.
        GradientPainter rep = on(GradientSpec.conic(0).repeating(true)
                .addStop(RED, 0, Unit.DEGREES).addStop(BLUE, 90, Unit.DEGREES), 100, 100);
        int c = rep.colorAtPoint(70, 20);
        // 33.69 of 90 degrees.
        assertNear(0xffa0005f, c, 1, "first quarter turn");
        assertNear(c, rep.colorAtPoint(80, 70), 1, "second quarter turn");
        assertNear(c, rep.colorAtPoint(30, 80), 1, "third quarter turn");
        assertNear(c, rep.colorAtPoint(20, 30), 1, "fourth quarter turn");
    }

    @Test
    void invalidGradientsNameTheField() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(GradientSpec.linear(0).addStop(RED), 10, 10)).getMessage().startsWith("gradient.stops"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(GradientSpec.linear(0).stops(null), 10, 10)).getMessage().startsWith("gradient.stops"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(redBlue(GradientSpec.linear(0)).type(null), 10, 10)).getMessage().startsWith("gradient.type"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(GradientSpec.linear(0).addStop(RED, 10, Unit.DEGREES).addStop(BLUE), 10, 10))
                .getMessage().startsWith("gradient.stops[0].unit"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(GradientSpec.conic(0).addStop(RED).addStop(BLUE, 10, Unit.PX), 10, 10))
                .getMessage().startsWith("gradient.stops[1].unit"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(redBlue(GradientSpec.linear(Double.NaN)), 10, 10))
                .getMessage().startsWith("gradient.angleDeg"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(redBlue(GradientSpec.radial(Shape.CIRCLE, Extent.EXPLICIT).radiusX(-1)), 10, 10))
                .getMessage().startsWith("gradient.radiusX"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> on(redBlue(GradientSpec.radial(null, Extent.EXPLICIT)), 10, 10))
                .getMessage().startsWith("gradient.shape"));
    }

    @Test
    void rasterizerPaintsTheGradientOverTheBorderBox() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 50).pad(10).radii(10)
                .backgroundColor(GREEN).gradient(redBlue(GradientSpec.linear(90))));
        // Pixel (35,30) is centred 25.5px into the box: 25.5% of the way.
        assertPixelNear(0xffbe0041, img, 35, 30, 1);
        assertPixelNear(0xff7e0081, img, 60, 30, 1);
        // The same column has one colour from top to bottom.
        assertEquals(img.getRGB(35, 12), img.getRGB(35, 57));
        // Nothing outside the box, nor outside its rounded corner.
        assertTransparent(img, 5, 30);
        assertTransparent(img, 112, 30);
        assertTransparent(img, 10, 10);
        assertTransparent(img, 11, 11);
    }

    @Test
    void translucentGradientLetsTheBackgroundColourThrough() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 20).backgroundColor(0xff0000ff)
                .gradient(GradientSpec.linear(90).addStop(0x00ff0000).addStop(0xffff0000)));
        assertPixelNear(0xff0000ff, img, 0, 10, 2);
        assertPixelNear(0xff7f0080, img, 49, 10, 2);
        assertPixelNear(0xffff0000, img, 99, 10, 3);
    }

    @Test
    void gradientUnderABorderStartsAtTheBorderEdge() {
        // The gradient box is the border box, so a transparent border shows
        // the very start of the gradient.
        BufferedImage img = paint(BoxStyle.builder().size(100, 20)
                .border(new BorderSide(10, BorderStyle.SOLID, 0))
                .gradient(redBlue(GradientSpec.linear(90))));
        assertPixelNear(0xfffe0001, img, 0, 10, 1);
        assertPixelNear(0xff0100fe, img, 99, 10, 1);
    }
}
