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

import static com.codename1.designer.css.raster.RasterAssert.BLACK;
import static com.codename1.designer.css.raster.RasterAssert.BLUE;
import static com.codename1.designer.css.raster.RasterAssert.GREEN;
import static com.codename1.designer.css.raster.RasterAssert.GREY;
import static com.codename1.designer.css.raster.RasterAssert.RED;
import static com.codename1.designer.css.raster.RasterAssert.WHITE;
import static com.codename1.designer.css.raster.RasterAssert.YELLOW;
import static com.codename1.designer.css.raster.RasterAssert.alpha;
import static com.codename1.designer.css.raster.RasterAssert.assertOpaque;
import static com.codename1.designer.css.raster.RasterAssert.assertPixel;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;
import static com.codename1.designer.css.raster.RasterAssert.runsInRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// Border geometry, mitres and the border styles.
class BorderPainterTest {
    private static BorderSide solid(double w, int color) {
        return new BorderSide(w, BorderStyle.SOLID, color);
    }

    private static BoxStyle.Builder fourColours(double t, double r, double b, double l) {
        return BoxStyle.builder().size(100, 80).backgroundColor(WHITE)
                .top(solid(t, RED)).right(solid(r, GREEN)).bottom(solid(b, BLUE)).left(solid(l, YELLOW));
    }

    @Test
    void parseIsCaseInsensitiveAndDefaultsToNone() {
        assertEquals(BorderStyle.SOLID, BorderStyle.parse("solid"));
        assertEquals(BorderStyle.DASHED, BorderStyle.parse("DASHED"));
        assertEquals(BorderStyle.DOTTED, BorderStyle.parse(" Dotted "));
        assertEquals(BorderStyle.DOUBLE, BorderStyle.parse("double"));
        assertEquals(BorderStyle.GROOVE, BorderStyle.parse("groove"));
        assertEquals(BorderStyle.RIDGE, BorderStyle.parse("ridge"));
        assertEquals(BorderStyle.INSET, BorderStyle.parse("inset"));
        assertEquals(BorderStyle.OUTSET, BorderStyle.parse("outset"));
        assertEquals(BorderStyle.HIDDEN, BorderStyle.parse("hidden"));
        assertEquals(BorderStyle.NONE, BorderStyle.parse("none"));
        assertEquals(BorderStyle.NONE, BorderStyle.parse(null));
        assertEquals(BorderStyle.NONE, BorderStyle.parse("wavy"));
        assertEquals(BorderStyle.NONE, BorderStyle.parse(""));
    }

    @Test
    void eachSideHasItsOwnColour() {
        BufferedImage img = paint(fourColours(10, 10, 10, 10));
        assertPixel(RED, img, 50, 5);
        assertPixel(GREEN, img, 95, 40);
        assertPixel(BLUE, img, 50, 75);
        assertPixel(YELLOW, img, 5, 40);
        assertPixel(WHITE, img, 50, 40);
        // The padding edge is exactly one border width in.
        assertPixel(YELLOW, img, 9, 40);
        assertPixel(WHITE, img, 10, 40);
        assertPixel(RED, img, 50, 9);
        assertPixel(WHITE, img, 50, 10);
        assertPixel(GREEN, img, 90, 40);
        assertPixel(WHITE, img, 89, 40);
        assertPixel(BLUE, img, 50, 70);
        assertPixel(WHITE, img, 50, 69);
    }

    @Test
    void cornersAreMitredAlongTheDiagonal() {
        BufferedImage img = paint(fourColours(10, 10, 10, 10));
        // Top-left: above the diagonal is the top side, below it the left.
        assertPixel(RED, img, 7, 3);
        assertPixel(YELLOW, img, 3, 7);
        assertNotEquals(img.getRGB(7, 3), img.getRGB(3, 7));
        // Top-right, bottom-right, bottom-left.
        assertPixel(RED, img, 92, 3);
        assertPixel(GREEN, img, 96, 7);
        assertPixel(GREEN, img, 96, 72);
        assertPixel(BLUE, img, 92, 76);
        assertPixel(BLUE, img, 7, 76);
        assertPixel(YELLOW, img, 3, 72);
        // A pixel the diagonal cuts in half is an even mix of both sides and
        // stays fully opaque: the two antialiased halves add up.
        int mix = img.getRGB(5, 5);
        assertEquals(255, mix >>> 24, "no translucent seam on the mitre");
        assertEquals(255, (mix >> 16) & 0xff, "red and yellow share full red");
        int green = (mix >> 8) & 0xff;
        assertTrue(green > 96 && green < 160, "half yellow, half red: green was " + green);
        assertEquals(0, mix & 0xff);
    }

    @Test
    void mitreFollowsUnequalWidths() {
        // Top 20, left 5: the mitre runs from (0,0) to (5,20).
        BufferedImage img = paint(fourColours(20, 5, 5, 5));
        assertPixel(YELLOW, img, 2, 15);
        assertPixel(RED, img, 4, 10);
        assertPixel(RED, img, 2, 3);
        assertPixel(YELLOW, img, 0, 6);
        assertPixel(WHITE, img, 5, 20);
        assertPixel(RED, img, 5, 19);
        assertPixel(YELLOW, img, 4, 20);
    }

    @Test
    void noSeamOnATransparentBackground() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100).radii(30)
                .top(solid(10, RED)).right(solid(10, RED)).bottom(solid(10, RED)).left(solid(10, YELLOW)));
        // Along the top-left mitre, inside the ring (radius 20..30 around (30,30)).
        assertOpaque(img, 12, 12);
        assertOpaque(img, 14, 14);
        assertTransparent(img, 19, 19);
        assertTransparent(img, 5, 5);
        assertPixel(RED, img, 20, 5);
        assertPixel(YELLOW, img, 5, 20);
    }

    @Test
    void uniformRoundedBorderIsARing() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100).radii(30).border(solid(10, BLACK)));
        assertPixel(BLACK, img, 50, 5);
        assertPixel(BLACK, img, 5, 50);
        assertPixel(BLACK, img, 94, 50);
        assertPixel(BLACK, img, 50, 94);
        assertTransparent(img, 50, 15);
        assertTransparent(img, 50, 50);
        // On the 45 degree line through the corner centre (30,30): the outer
        // radius is 30 and the inner one 30 - 10 = 20.
        assertPixel(BLACK, img, 12, 12);
        assertTransparent(img, 19, 19);
        assertTransparent(img, 5, 5);
        assertTransparent(img, 0, 0);
    }

    @Test
    void innerRadiusStopsAtZeroWhenTheBorderIsWiderThanTheRadius() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100).radii(5).backgroundColor(WHITE)
                .border(solid(10, BLACK)));
        // The padding edge has a square corner at (10,10).
        assertPixel(WHITE, img, 10, 10);
        assertPixel(BLACK, img, 9, 10);
        assertPixel(BLACK, img, 10, 9);
        assertTransparent(img, 0, 0);
    }

    @Test
    void noneAndHiddenTakeNoWidthAndPaintNothing() {
        BufferedImage none = paint(BoxStyle.builder().size(40, 40).backgroundColor(WHITE)
                .border(new BorderSide(10, BorderStyle.NONE, RED)));
        assertPixel(WHITE, none, 5, 5);
        assertPixel(WHITE, none, 20, 2);
        BufferedImage hidden = paint(BoxStyle.builder().size(40, 40).backgroundColor(WHITE)
                .border(new BorderSide(10, BorderStyle.HIDDEN, RED)));
        assertPixel(WHITE, hidden, 5, 5);
        // A zero width side next to a wide one gives the wide one the corner.
        BufferedImage one = paint(BoxStyle.builder().size(40, 40).backgroundColor(WHITE).top(solid(10, RED)));
        assertPixel(RED, one, 0, 0);
        assertPixel(RED, one, 0, 9);
        assertPixel(RED, one, 39, 9);
        assertPixel(WHITE, one, 0, 10);
    }

    @Test
    void transparentBorderShowsTheBackgroundUnderIt() {
        BufferedImage img = paint(BoxStyle.builder().size(40, 40).backgroundColor(WHITE)
                .border(solid(10, 0x00ff0000)));
        assertPixel(WHITE, img, 5, 5);
        assertPixel(WHITE, img, 20, 20);
    }

    @Test
    void translucentBorderBlendsOverTheBackground() {
        BufferedImage img = paint(BoxStyle.builder().size(40, 40).backgroundColor(WHITE)
                .border(solid(10, 0x80000000)));
        RasterAssert.assertPixelNear(0xff7f7f7f, img, 20, 5, 1);
        assertPixel(WHITE, img, 20, 20);
    }

    @Test
    void doubleIsTwoLinesAThirdOfTheWidthEach() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100)
                .border(new BorderSide(9, BorderStyle.DOUBLE, BLACK)));
        for (int y = 0; y < 3; y++) {
            assertPixel(BLACK, img, 50, y);
        }
        for (int y = 3; y < 6; y++) {
            assertTransparent(img, 50, y);
        }
        for (int y = 6; y < 9; y++) {
            assertPixel(BLACK, img, 50, y);
        }
        assertTransparent(img, 50, 9);
        // The same on the left side, and the lines turn the corner.
        assertPixel(BLACK, img, 1, 50);
        assertTransparent(img, 4, 50);
        assertPixel(BLACK, img, 7, 50);
        assertPixel(BLACK, img, 1, 1);
        assertTransparent(img, 4, 4);
        assertPixel(BLACK, img, 7, 7);
        // The gap is a frame of its own between the two lines.
        assertTransparent(img, 7, 4);
        assertTransparent(img, 4, 7);
        assertPixel(BLACK, img, 7, 1);
        assertPixel(BLACK, img, 1, 7);
        assertPixel(BLACK, img, 8, 6);
        assertPixel(BLACK, img, 6, 8);
    }

    @Test
    void thinDoubleFallsBackToSolid() {
        BufferedImage img = paint(BoxStyle.builder().size(20, 20)
                .border(new BorderSide(2, BorderStyle.DOUBLE, BLACK)));
        assertPixel(BLACK, img, 10, 0);
        assertPixel(BLACK, img, 10, 1);
        assertTransparent(img, 10, 2);
    }

    @Test
    void dashedAlternatesDashAndGap() {
        // Width 4 along 100px: dashes of 8 and nine of them fit with a gap
        // of (100 - 72) / 8 = 3.5, the count nearest the nominal gap of 4.
        BufferedImage img = paint(BoxStyle.builder().size(100, 20)
                .top(new BorderSide(4, BorderStyle.DASHED, BLACK)));
        assertEquals(9, runsInRow(img, 1));
        assertEquals(9, runsInRow(img, 3));
        assertEquals(0, runsInRow(img, 4), "nothing below the border");
        assertPixel(BLACK, img, 0, 1);
        assertPixel(BLACK, img, 7, 1);
        assertTransparent(img, 8, 1);
        assertTransparent(img, 10, 1);
        assertPixel(BLACK, img, 12, 1);
        assertPixel(BLACK, img, 18, 1);
        assertTransparent(img, 20, 1);
        assertPixel(BLACK, img, 99, 1);
        assertPixel(BLACK, img, 92, 3);
    }

    @Test
    void thinDashedUsesLongerDashes() {
        // Width 1: a dash of 3 and a nominal gap of 2.
        BufferedImage img = paint(BoxStyle.builder().size(48, 10)
                .top(new BorderSide(1, BorderStyle.DASHED, BLACK)));
        // (48 + 2) / (3 + 2) = 10 dashes with a gap of exactly 2.
        assertEquals(10, runsInRow(img, 0));
        assertPixel(BLACK, img, 0, 0);
        assertPixel(BLACK, img, 2, 0);
        assertTransparent(img, 3, 0);
        assertTransparent(img, 4, 0);
        assertPixel(BLACK, img, 5, 0);
    }

    @Test
    void dottedIsARowOfRoundDots() {
        // Width 6 along 120px: dots every 12px, centred on x = 0, 12 .. 120.
        BufferedImage img = paint(BoxStyle.builder().size(120, 30)
                .top(new BorderSide(6, BorderStyle.DOTTED, BLACK)));
        assertEquals(11, runsInRow(img, 3));
        assertPixel(BLACK, img, 12, 3);
        assertPixel(BLACK, img, 11, 2);
        assertTransparent(img, 6, 3);
        assertTransparent(img, 18, 2);
        // Round, not square: the corner of a dot's bounding box is nearly
        // empty while the top of the dot is nearly full.
        assertTrue(alpha(img, 9, 0) < 100, "dot corner should be mostly empty, alpha " + alpha(img, 9, 0));
        assertTrue(alpha(img, 12, 0) > 200, "dot top should be mostly full, alpha " + alpha(img, 12, 0));
        assertEquals(0, runsInRow(img, 6), "nothing below the border");
        // Half a dot sits on each end of the side.
        assertPixel(BLACK, img, 0, 3);
        assertPixel(BLACK, img, 119, 3);
    }

    @Test
    void dashedBorderFollowsARoundedCorner() {
        BufferedImage img = paint(BoxStyle.builder().size(120, 120).radii(40)
                .border(new BorderSide(6, BorderStyle.DASHED, BLACK)));
        // Nothing may leave the ring: outside the outer arc, inside the inner one.
        assertTransparent(img, 3, 3);
        assertTransparent(img, 60, 60);
        assertTransparent(img, 60, 7);
        assertTransparent(img, 18, 18);
        int painted = 0;
        int empty = 0;
        // Walk the middle of the ring around the top-left corner (radius 37).
        for (int deg = 185; deg < 270; deg += 2) {
            double rad = Math.toRadians(deg);
            int x = (int) (40 + 37 * Math.cos(rad));
            int y = (int) (40 + 37 * Math.sin(rad));
            if (alpha(img, x, y) > 200) {
                painted++;
            } else if (alpha(img, x, y) < 50) {
                empty++;
            }
        }
        assertTrue(painted > 10, "dashes on the corner arc, found " + painted);
        assertTrue(empty > 4, "gaps on the corner arc, found " + empty);
    }

    @Test
    void darkenMatchesChrome() {
        assertEquals(0xffababab, BorderPainter.darken(WHITE));
        assertEquals(BLACK, BorderPainter.darken(BLACK));
        // v = 128/255; (v - 0.33) / v = 0.3426; 128 * 0.3426 = 43.9
        assertEquals(0xff2c2c2c, BorderPainter.darken(GREY));
        assertEquals(0x80ab0000, BorderPainter.darken(0x80ff0000), "alpha is kept");
    }

    @Test
    void insetDarkensTopAndLeftOutsetBottomAndRight() {
        int dark = BorderPainter.darken(GREY);
        BufferedImage inset = paint(BoxStyle.builder().size(100, 80)
                .border(new BorderSide(10, BorderStyle.INSET, GREY)));
        assertPixel(dark, inset, 50, 5);
        assertPixel(dark, inset, 5, 40);
        assertPixel(GREY, inset, 50, 75);
        assertPixel(GREY, inset, 95, 40);
        BufferedImage outset = paint(BoxStyle.builder().size(100, 80)
                .border(new BorderSide(10, BorderStyle.OUTSET, GREY)));
        assertPixel(GREY, outset, 50, 5);
        assertPixel(GREY, outset, 5, 40);
        assertPixel(dark, outset, 50, 75);
        assertPixel(dark, outset, 95, 40);
    }

    @Test
    void grooveAndRidgeSplitTheWidthInTwo() {
        int dark = BorderPainter.darken(GREY);
        BufferedImage groove = paint(BoxStyle.builder().size(100, 80)
                .border(new BorderSide(10, BorderStyle.GROOVE, GREY)));
        assertPixel(dark, groove, 50, 2);
        assertPixel(GREY, groove, 50, 7);
        assertPixel(dark, groove, 2, 40);
        assertPixel(GREY, groove, 7, 40);
        assertPixel(GREY, groove, 50, 77);
        assertPixel(dark, groove, 50, 72);
        assertPixel(GREY, groove, 97, 40);
        assertPixel(dark, groove, 92, 40);
        BufferedImage ridge = paint(BoxStyle.builder().size(100, 80)
                .border(new BorderSide(10, BorderStyle.RIDGE, GREY)));
        assertPixel(GREY, ridge, 50, 2);
        assertPixel(dark, ridge, 50, 7);
        assertPixel(dark, ridge, 50, 77);
        assertPixel(GREY, ridge, 50, 72);
        // The split is exactly half way.
        assertPixel(GREY, ridge, 50, 4);
        assertPixel(dark, ridge, 50, 5);
    }

    @Test
    void effectiveWidthsIgnoreInvisibleStylesAndFitTheBox() {
        BorderSide[] sides = {
            solid(4, RED), new BorderSide(6, BorderStyle.NONE, RED),
            new BorderSide(8, BorderStyle.HIDDEN, RED), solid(2, RED)
        };
        double[] w = BorderPainter.effectiveWidths(sides, 100, 100);
        assertEquals(4, w[0]);
        assertEquals(0, w[1]);
        assertEquals(0, w[2]);
        assertEquals(2, w[3]);
        BorderSide[] wide = {solid(30, RED), solid(30, RED), solid(30, RED), solid(30, RED)};
        double[] scaled = BorderPainter.effectiveWidths(wide, 40, 100);
        assertEquals(20, scaled[1], 1e-9);
        assertEquals(20, scaled[3], 1e-9);
        // A box that is all border paints, and paints nothing but border.
        BufferedImage img = paint(BoxStyle.builder().size(40, 100).backgroundColor(WHITE).border(solid(30, RED)));
        assertPixel(RED, img, 20, 50);
        assertPixel(RED, img, 1, 1);
    }

    @Test
    void fractionalBoxKeepsFullCoverageOnTheBorderEdge() {
        // The box starts at x = 0 and is 30.5 wide: the last column is half
        // covered. A wedge edge coinciding with the ring edge would square
        // that to a quarter.
        BufferedImage img = paint(BoxStyle.builder().size(30.5, 30.5).pad(0)
                .top(solid(5, RED)).right(solid(5, GREEN)).bottom(solid(5, BLUE)).left(solid(5, YELLOW)));
        assertEquals(30, img.getWidth());
        BufferedImage padded = paint(BoxStyle.builder().size(30.5, 30.5).padRight(1).padBottom(1)
                .top(solid(5, RED)).right(solid(5, GREEN)).bottom(solid(5, BLUE)).left(solid(5, YELLOW)));
        int a = alpha(padded, 30, 15);
        assertTrue(a >= 120 && a <= 136, "half covered edge column, alpha was " + a);
        assertEquals(GREEN & 0xffffff, padded.getRGB(30, 15) & 0xffffff);
        int b = alpha(padded, 15, 30);
        assertTrue(b >= 120 && b <= 136, "half covered edge row, alpha was " + b);
        assertEquals(BLUE & 0xffffff, padded.getRGB(15, 30) & 0xffffff);
    }

    @Test
    void dashesTooShortToCountAreStillPaintedPromptly() {
        final BorderSide hair = new BorderSide(1e-7, BorderStyle.DASHED, 0xffff0000);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(20),
                () -> RasterAssert.paint(BoxStyle.builder().size(40, 40)
                        .top(hair).right(hair).bottom(hair).left(hair)));
    }

    @Test
    void dotsTooSmallToCountAreStillPaintedPromptly() {
        final BorderSide hair = new BorderSide(1e-7, BorderStyle.DOTTED, 0xffff0000);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(20),
                () -> RasterAssert.paint(BoxStyle.builder().size(40, 40)
                        .top(hair).right(hair).bottom(hair).left(hair)));
    }
}
