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
import static com.codename1.designer.css.raster.RasterAssert.WHITE;
import static com.codename1.designer.css.raster.RasterAssert.alpha;
import static com.codename1.designer.css.raster.RasterAssert.assertPixel;
import static com.codename1.designer.css.raster.RasterAssert.assertPixelNear;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// Outer and inset box shadows. The outer tests use a 60x40 box padded by
/// 20, so the border box is x 20..80, y 20..60 in a 100x80 image.
class BoxShadowPainterTest {
    private static BoxStyle.Builder box() {
        return BoxStyle.builder().size(60, 40).pad(20);
    }

    private static Shadow outer(double dx, double dy, double blur, double spread, int color) {
        return new Shadow(dx, dy, blur, spread, color, false);
    }

    private static Shadow inset(double dx, double dy, double blur, double spread, int color) {
        return new Shadow(dx, dy, blur, spread, color, true);
    }

    private static long totalAlpha(BufferedImage img) {
        long s = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                s += alpha(img, x, y);
            }
        }
        return s;
    }

    @Test
    void outerShadowIsNotDrawnUnderTheBox() {
        // No background at all: whatever is under the box shows.
        BufferedImage img = paint(box().shadow(outer(0, 0, 10, 0, BLACK)));
        for (int y = 20; y < 60; y++) {
            for (int x = 20; x < 80; x++) {
                assertTransparent(img, x, y);
            }
        }
        // And it is there right outside, on all four sides.
        assertTrue(alpha(img, 19, 40) > 60, "left of the box: " + alpha(img, 19, 40));
        assertTrue(alpha(img, 80, 40) > 60, "right of the box: " + alpha(img, 80, 40));
        assertTrue(alpha(img, 50, 19) > 60, "above the box: " + alpha(img, 50, 19));
        assertTrue(alpha(img, 50, 60) > 60, "below the box: " + alpha(img, 50, 60));
    }

    @Test
    void outerShadowDoesNotShowThroughATranslucentBackground() {
        BufferedImage img = paint(box().backgroundColor(0x40ffffff).shadow(outer(0, 0, 10, 0, BLACK)));
        assertPixel(0x40ffffff, img, 50, 40);
        assertPixel(0x40ffffff, img, 21, 21);
        BufferedImage opaque = paint(box().backgroundColor(WHITE).shadow(outer(4, 4, 10, 2, BLACK)));
        assertPixel(WHITE, opaque, 50, 40);
        assertPixel(WHITE, opaque, 20, 20);
        assertPixel(WHITE, opaque, 79, 59);
    }

    @Test
    void blurFadesWithDistanceAndIsSymmetric() {
        BufferedImage img = paint(box().shadow(outer(0, 0, 10, 0, BLACK)));
        int a1 = alpha(img, 19, 40);
        int a5 = alpha(img, 15, 40);
        int a9 = alpha(img, 11, 40);
        int a19 = alpha(img, 1, 40);
        assertTrue(a1 > a5 && a5 > a9 && a9 >= a19, "fades: " + a1 + " " + a5 + " " + a9 + " " + a19);
        // Sigma is blur / 2 = 5. Half a pixel outside a long straight edge
        // the gaussian is at 1 - Phi(0.1) = 0.46.
        assertEquals(0.46 * 255, a1, 6);
        // 4.5px out: 1 - Phi(0.9) = 0.184.
        assertEquals(0.184 * 255, a5, 6);
        // 18.5px out, 3.7 sigma: nothing left.
        assertTrue(a19 <= 1, "beyond three sigma: " + a19);
        assertEquals(a1, alpha(img, 80, 40), 1);
        assertEquals(a5, alpha(img, 84, 40), 1);
        assertEquals(alpha(img, 50, 15), alpha(img, 50, 64), 1);
        assertEquals(alpha(img, 15, 15), alpha(img, 84, 64), 1);
        assertEquals(alpha(img, 15, 15), alpha(img, 84, 15), 1);
        // The colour is the shadow colour, untouched by the blur.
        assertEquals(0, img.getRGB(15, 40) & 0xffffff);
    }

    @Test
    void zeroBlurZeroSpreadZeroOffsetIsInvisible() {
        BufferedImage img = paint(box().shadow(outer(0, 0, 0, 0, BLACK)));
        assertEquals(0, totalAlpha(img));
    }

    @Test
    void spreadGrowsAHardEdgedShadow() {
        BufferedImage img = paint(box().shadow(outer(0, 0, 0, 5, BLACK)));
        // The shadow shape is x 15..85, y 15..65.
        assertPixel(BLACK, img, 15, 40);
        assertPixel(BLACK, img, 19, 40);
        assertTransparent(img, 14, 40);
        assertPixel(BLACK, img, 84, 40);
        assertTransparent(img, 85, 40);
        assertPixel(BLACK, img, 50, 15);
        assertTransparent(img, 50, 14);
        assertPixel(BLACK, img, 50, 64);
        assertTransparent(img, 50, 65);
        assertTransparent(img, 50, 40);
        // A square box keeps square shadow corners.
        assertPixel(BLACK, img, 15, 15);
        BufferedImage wider = paint(box().shadow(outer(0, 0, 0, 8, BLACK)));
        assertPixel(BLACK, wider, 12, 40);
        assertTransparent(wider, 11, 40);
        assertTrue(totalAlpha(wider) > totalAlpha(img));
    }

    @Test
    void negativeSpreadShrinksTheShadow() {
        // Shrunk by 5 and moved down 10: x 25..75, y 35..65. Only the strip
        // below the box (y 60..65) shows.
        BufferedImage img = paint(box().shadow(outer(0, 10, 0, -5, BLACK)));
        assertPixel(BLACK, img, 50, 62);
        assertPixel(BLACK, img, 25, 64);
        assertTransparent(img, 24, 62);
        assertTransparent(img, 75, 62);
        assertTransparent(img, 50, 65);
        assertTransparent(img, 50, 50);
    }

    @Test
    void offsetMovesTheShadow() {
        BufferedImage img = paint(box().shadow(outer(10, 0, 0, 0, BLACK)));
        // Moved right by 10: x 30..90. Only x 80..90 is outside the box.
        assertPixel(BLACK, img, 80, 40);
        assertPixel(BLACK, img, 89, 40);
        assertTransparent(img, 90, 40);
        assertTransparent(img, 19, 40);
        assertTransparent(img, 50, 19);
        assertTransparent(img, 50, 60);
        assertTransparent(img, 85, 19);
        assertPixel(BLACK, img, 85, 20);
        BufferedImage down = paint(box().shadow(outer(0, 7, 0, 0, BLACK)));
        assertPixel(BLACK, down, 50, 66);
        assertTransparent(down, 50, 67);
        assertTransparent(down, 50, 19);
        BufferedImage up = paint(box().shadow(outer(-3, -4, 0, 0, BLACK)));
        assertPixel(BLACK, up, 17, 16);
        assertTransparent(up, 16, 16);
        assertTransparent(up, 17, 15);
        assertTransparent(up, 80, 61);
    }

    @Test
    void shadowIsTintedWithItsColour() {
        BufferedImage img = paint(box().shadow(outer(0, 0, 0, 5, 0x80ff0000)));
        assertPixel(0x80ff0000, img, 17, 40);
        BufferedImage blurred = paint(box().shadow(outer(0, 0, 8, 5, 0x80336699)));
        int p = blurred.getRGB(12, 40);
        assertEquals(0x336699, p & 0xffffff, "the blur changes alpha only");
        assertTrue((p >>> 24) > 0 && (p >>> 24) < 128, "at most the shadow's own alpha: " + (p >>> 24));
        assertEquals(0, totalAlpha(paint(box().shadow(outer(3, 3, 5, 5, 0x00ff0000)))), "a transparent shadow");
    }

    @Test
    void shadowFollowsTheBorderRadius() {
        // Radius 10 grown by a spread of 5 is 15, around a centre at (30,30).
        BufferedImage round = paint(box().radii(10).shadow(outer(0, 0, 0, 5, BLACK)));
        assertTransparent(round, 16, 16);
        assertTransparent(round, 18, 18);
        assertPixel(BLACK, round, 15, 40);
        assertPixel(BLACK, round, 40, 15);
        // Between the shadow arc (radius 15) and the box arc (radius 10).
        assertPixel(BLACK, round, 21, 21);
        // Under the rounded box itself: nothing.
        assertTransparent(round, 24, 24);
        // A square corner stays square however far it spreads.
        BufferedImage square = paint(box().shadow(outer(0, 0, 0, 5, BLACK)));
        assertPixel(BLACK, square, 16, 16);
    }

    @Test
    void smallRadiusGrowsByLessThanTheSpread() {
        RoundedBox b = new RoundedBox(0, 0, 100, 100, new double[] {2, 2, 20, 20, 0, 0, 0, 0});
        RoundedBox g = b.grow(10);
        // r = 2, spread 10: 2 + 10 * (1 + (0.2 - 1)^3) = 6.88
        assertEquals(6.88, g.radiusX(RoundedBox.TOP_LEFT), 1e-9);
        assertEquals(30, g.radiusX(RoundedBox.TOP_RIGHT), 1e-9);
        assertEquals(0, g.radiusX(RoundedBox.BOTTOM_RIGHT), 0);
        assertEquals(-10, g.getX(), 0);
        assertEquals(120, g.getWidth(), 0);
    }

    @Test
    void haloClippedByASmallPadIsNotAnError() {
        // A 30px blur reaches about 45px; there are only 2px of room.
        BufferedImage img = paint(BoxStyle.builder().size(60, 40).pad(2).backgroundColor(WHITE)
                .shadow(outer(0, 0, 30, 0, BLACK)));
        assertEquals(64, img.getWidth());
        assertEquals(44, img.getHeight());
        assertPixel(WHITE, img, 32, 22);
        assertTrue(alpha(img, 0, 22) > 40, "the clipped halo is still there: " + alpha(img, 0, 22));
        assertTrue(alpha(img, 1, 22) > alpha(img, 0, 22), "and still fading outwards");
        assertTrue(alpha(img, 63, 22) > 40);
        // No padding at all: the shadow is entirely cut off, the box is intact.
        BufferedImage none = paint(BoxStyle.builder().size(60, 40).backgroundColor(WHITE)
                .shadow(outer(5, 5, 30, 10, BLACK)));
        assertEquals(60, none.getWidth());
        assertPixel(WHITE, none, 0, 0);
        assertPixel(WHITE, none, 59, 39);
    }

    @Test
    void hugeBlurUsesTheBoxPathAndStaysSmooth() {
        // Sigma 20 is above the exact kernel limit.
        BufferedImage img = paint(BoxStyle.builder().size(60, 40).pad(50).shadow(outer(0, 0, 40, 0, BLACK)));
        int prev = 256;
        for (int x = 49; x >= 0; x -= 4) {
            int a = alpha(img, x, 70);
            assertTrue(a <= prev, "monotonic fade at x = " + x + ": " + a + " after " + prev);
            prev = a;
        }
        assertTrue(alpha(img, 49, 70) > 40, "next to the box: " + alpha(img, 49, 70));
        assertEquals(alpha(img, 45, 70), alpha(img, 114, 70), 1, "symmetric");
        assertTransparent(img, 80, 70);
    }

    // The inset tests use a 60x40 box padded by 5 with a 5px border: the
    // border box is x 5..65, y 5..45 and the padding box x 10..60, y 10..40.

    private static BoxStyle.Builder bordered() {
        return BoxStyle.builder().size(60, 40).pad(5).backgroundColor(WHITE)
                .border(new BorderSide(5, BorderStyle.SOLID, 0));
    }

    @Test
    void insetShadowIsConfinedToThePaddingBox() {
        BufferedImage img = paint(bordered().shadow(inset(0, 0, 0, 4, BLACK)));
        // The hole is the padding box shrunk by 4: x 14..56, y 14..36.
        assertPixel(BLACK, img, 10, 25);
        assertPixel(BLACK, img, 13, 25);
        assertPixel(WHITE, img, 14, 25);
        assertPixel(WHITE, img, 35, 25);
        assertPixel(WHITE, img, 55, 25);
        assertPixel(BLACK, img, 56, 25);
        assertPixel(BLACK, img, 59, 25);
        assertPixel(BLACK, img, 35, 10);
        assertPixel(WHITE, img, 35, 14);
        assertPixel(BLACK, img, 35, 39);
        // Not over the (transparent) border, where the background shows...
        assertPixel(WHITE, img, 9, 25);
        assertPixel(WHITE, img, 60, 25);
        assertPixel(WHITE, img, 35, 9);
        assertPixel(WHITE, img, 35, 40);
        assertPixel(WHITE, img, 7, 7);
        // ...and not outside the box.
        assertTransparent(img, 4, 25);
        assertTransparent(img, 2, 2);
        assertTransparent(img, 65, 25);
    }

    @Test
    void insetShadowWithoutSpreadOrBlurIsInvisible() {
        BufferedImage img = paint(bordered().shadow(inset(0, 0, 0, 0, BLACK)));
        for (int y = 5; y < 45; y++) {
            for (int x = 5; x < 65; x++) {
                assertPixel(WHITE, img, x, y);
            }
        }
    }

    @Test
    void insetBlurFadesInwards() {
        BufferedImage img = paint(bordered().shadow(inset(0, 0, 8, 0, BLACK)));
        int edge = img.getRGB(10, 25) & 0xff;
        int near = img.getRGB(13, 25) & 0xff;
        int far = img.getRGB(18, 25) & 0xff;
        assertTrue(edge < near && near < far, "lighter away from the edge: " + edge + " " + near + " " + far);
        // Sigma 4; half a pixel inside the edge the shadow is 1 - Phi(0.125) = 0.45.
        assertEquals(255 * (1 - 0.45), edge, 6);
        assertPixel(WHITE, img, 35, 25);
        assertPixelNear(img.getRGB(10, 25), img, 59, 25, 1);
        assertPixelNear(img.getRGB(13, 25), img, 56, 25, 1);
        assertEquals(255, alpha(img, 10, 25));
        assertPixel(WHITE, img, 9, 25);
        assertTransparent(img, 4, 25);
    }

    @Test
    void insetOffsetMovesTheHole() {
        BufferedImage img = paint(bordered().shadow(inset(6, 0, 0, 0, BLACK)));
        // The hole moves right by 6: the left 6 columns of the padding box
        // are in shadow, the right edge is not.
        assertPixel(BLACK, img, 10, 25);
        assertPixel(BLACK, img, 15, 25);
        assertPixel(WHITE, img, 16, 25);
        assertPixel(WHITE, img, 59, 25);
        assertPixel(WHITE, img, 35, 10);
        BufferedImage up = paint(bordered().shadow(inset(0, -3, 0, 0, BLACK)));
        assertPixel(BLACK, up, 35, 39);
        assertPixel(BLACK, up, 35, 37);
        assertPixel(WHITE, up, 35, 36);
        assertPixel(WHITE, up, 35, 10);
    }

    @Test
    void insetShadowIsClippedToTheRoundedPaddingBox() {
        BufferedImage img = paint(BoxStyle.builder().size(60, 60).pad(5).radii(20)
                .shadow(inset(0, 0, 0, 100, 0x80ff0000)));
        // A spread larger than the box leaves no hole: the whole padding box
        // (here also the border box) is shadow, and nothing else is.
        assertPixel(0x80ff0000, img, 35, 35);
        assertPixel(0x80ff0000, img, 35, 5);
        assertTransparent(img, 6, 6);
        assertTransparent(img, 8, 8);
        assertTransparent(img, 63, 63);
        assertTransparent(img, 2, 35);
        assertPixelNear(0x80ff0000, img, 12, 12, 0);
    }

    @Test
    void insetShadowIsPaintedOverTheBackgroundAndUnderTheBorder() {
        BufferedImage img = paint(BoxStyle.builder().size(60, 40).pad(5).backgroundColor(WHITE)
                .border(new BorderSide(5, BorderStyle.SOLID, 0xff0000ff))
                .shadow(inset(0, 0, 0, 3, 0x80000000)));
        assertPixel(0xff0000ff, img, 7, 25);
        assertPixelNear(0xff7f7f7f, img, 11, 25, 1);
        assertPixel(WHITE, img, 35, 25);
    }
}
