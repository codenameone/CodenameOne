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
import static com.codename1.designer.css.raster.RasterAssert.WHITE;
import static com.codename1.designer.css.raster.RasterAssert.YELLOW;
import static com.codename1.designer.css.raster.RasterAssert.assertPixel;
import static com.codename1.designer.css.raster.RasterAssert.assertPixelNear;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;
import static com.codename1.designer.css.raster.RasterAssert.quadrants;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.codename1.designer.css.raster.BackgroundImage.Repeat;
import com.codename1.designer.css.raster.BackgroundImage.Size;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// Background image repeat, position and size, with a 10x10 image of four
/// coloured 5x5 quadrants: red and green on top, blue and yellow below.
class BackgroundImagePainterTest {
    private static BufferedImage box30(BackgroundImage bg) {
        return paint(BoxStyle.builder().size(30, 30).backgroundImage(bg));
    }

    @Test
    void repeatTilesBothAxes() {
        BufferedImage img = box30(new BackgroundImage(quadrants()));
        assertPixel(RED, img, 2, 2);
        assertPixel(GREEN, img, 7, 2);
        assertPixel(BLUE, img, 2, 7);
        assertPixel(YELLOW, img, 7, 7);
        // Every tile is a copy, pixel for pixel.
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                assertPixel(img.getRGB(x % 10, y % 10), img, x, y);
            }
        }
        assertPixel(RED, img, 20, 20);
        assertPixel(YELLOW, img, 29, 29);
    }

    @Test
    void noRepeatPaintsOneTile() {
        BufferedImage img = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT));
        assertPixel(RED, img, 0, 0);
        assertPixel(YELLOW, img, 9, 9);
        assertTransparent(img, 10, 2);
        assertTransparent(img, 2, 10);
        assertTransparent(img, 20, 20);
        // Over a background colour the rest of the box keeps that colour.
        BufferedImage over = paint(BoxStyle.builder().size(30, 30).backgroundColor(WHITE)
                .backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)));
        assertPixel(RED, over, 2, 2);
        assertPixel(WHITE, over, 12, 2);
    }

    @Test
    void repeatXAndRepeatYTileOneAxis() {
        BufferedImage x = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.REPEAT_X));
        assertPixel(RED, x, 12, 2);
        assertPixel(YELLOW, x, 27, 7);
        assertTransparent(x, 2, 12);
        assertTransparent(x, 27, 10);
        BufferedImage y = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.REPEAT_Y));
        assertPixel(RED, y, 2, 12);
        assertPixel(YELLOW, y, 7, 27);
        assertTransparent(y, 12, 2);
        assertTransparent(y, 10, 27);
    }

    @Test
    void pixelPositionOffsetsTheTile() {
        BufferedImage img = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(10, false, 5, false));
        assertPixel(RED, img, 10, 5);
        assertPixel(GREEN, img, 15, 5);
        assertPixel(YELLOW, img, 19, 14);
        assertTransparent(img, 9, 7);
        assertTransparent(img, 12, 4);
        assertTransparent(img, 20, 7);
        assertTransparent(img, 12, 15);
        // A repeating image keeps its phase.
        BufferedImage rep = box30(new BackgroundImage(quadrants()).withPosition(3, false, 4, false));
        assertPixel(RED, rep, 3, 4);
        assertPixel(RED, rep, 13, 14);
        assertPixel(YELLOW, rep, 2, 3);
        assertPixel(GREEN, rep, 0, 5);
        // A negative position cuts the tile off at the box edge.
        BufferedImage neg = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(-5, false, -5, false));
        assertPixel(YELLOW, neg, 0, 0);
        assertPixel(YELLOW, neg, 4, 4);
        assertTransparent(neg, 5, 2);
    }

    @Test
    void percentPositionIsAFractionOfTheFreeSpace() {
        BufferedImage end = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(100, true, 100, true));
        // 100% of (30 - 10) = 20.
        assertPixel(RED, end, 20, 20);
        assertPixel(YELLOW, end, 29, 29);
        assertTransparent(end, 19, 22);
        assertTransparent(end, 22, 19);
        BufferedImage mid = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(50, true, 50, true));
        assertPixel(RED, mid, 10, 10);
        assertPixel(YELLOW, mid, 19, 19);
        assertTransparent(mid, 9, 12);
        assertTransparent(mid, 20, 12);
        BufferedImage mixed = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(25, true, 3, false));
        assertPixel(RED, mixed, 5, 3);
        assertTransparent(mixed, 4, 3);
        assertTransparent(mixed, 5, 2);
    }

    @Test
    void positionIsRelativeToThePaddingBoxButTheImagePaintsUnderTheBorder() {
        BoxStyle.Builder b = BoxStyle.builder().size(30, 30).border(new BorderSide(5, BorderStyle.SOLID, 0));
        BufferedImage one = paint(b.backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)));
        assertPixel(RED, one, 5, 5);
        assertPixel(YELLOW, one, 14, 14);
        assertTransparent(one, 4, 6);
        assertTransparent(one, 2, 2);
        // 100% is measured in the padding box too: 5 + (20 - 10) = 15.
        BufferedImage end = paint(b.backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                .withPosition(100, true, 100, true)));
        assertPixel(RED, end, 15, 15);
        assertPixel(YELLOW, end, 24, 24);
        assertTransparent(end, 25, 20);
        // Tiles continue under the border, out to the border edge.
        BufferedImage rep = paint(b.backgroundImage(new BackgroundImage(quadrants())));
        assertPixel(YELLOW, rep, 2, 2);
        assertPixel(RED, rep, 5, 5);
        assertPixel(YELLOW, rep, 0, 0);
        assertPixel(RED, rep, 29, 29);
    }

    @Test
    void explicitSizeScalesTheTile() {
        BufferedImage img = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT).withSize(20, 20));
        // Each quadrant is now 10x10. Check away from the interpolated seams.
        assertPixel(RED, img, 3, 3);
        assertPixel(GREEN, img, 15, 4);
        assertPixel(BLUE, img, 4, 15);
        assertPixel(YELLOW, img, 16, 16);
        assertTransparent(img, 22, 5);
        assertTransparent(img, 5, 22);
        // A non-uniform size stretches each axis on its own.
        BufferedImage wide = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT).withSize(30, 10));
        assertPixel(RED, wide, 5, 1);
        assertPixel(GREEN, wide, 24, 1);
        assertPixel(BLUE, wide, 5, 8);
        assertTransparent(wide, 5, 11);
        // The seam between two quadrants is interpolated, not a hard edge
        // moved off by a pixel: both sides of it are close to their colour.
        assertPixelNear(RED, img, 8, 3, 40);
        assertPixelNear(GREEN, img, 11, 3, 40);
    }

    @Test
    void autoAxisKeepsTheAspectRatio() {
        BufferedImage wide = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        BackgroundImage bg = new BackgroundImage(wide);
        assertArrayEquals(new double[] {20, 10}, BackgroundImagePainter.tileSize(bg, 100, 100));
        assertArrayEquals(new double[] {40, 20}, BackgroundImagePainter.tileSize(bg.withSize(40, -1), 100, 100));
        assertArrayEquals(new double[] {60, 30}, BackgroundImagePainter.tileSize(bg.withSize(-1, 30), 100, 100));
        assertArrayEquals(new double[] {20, 10}, BackgroundImagePainter.tileSize(bg.withSize(-1, -1), 100, 100));
        assertArrayEquals(new double[] {7, 9}, BackgroundImagePainter.tileSize(bg.withSize(7, 9), 100, 100));
        // cover fills the area, contain fits inside it.
        assertArrayEquals(new double[] {200, 100}, BackgroundImagePainter.tileSize(bg.withSize(Size.COVER), 100, 100));
        assertArrayEquals(new double[] {100, 50}, BackgroundImagePainter.tileSize(bg.withSize(Size.CONTAIN), 100, 100));
        assertArrayEquals(new double[] {60, 30}, BackgroundImagePainter.tileSize(bg.withSize(Size.COVER), 60, 20));
        assertArrayEquals(new double[] {40, 20}, BackgroundImagePainter.tileSize(bg.withSize(Size.CONTAIN), 60, 20));

        BufferedImage img = box30(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT).withSize(20, -1));
        assertPixel(YELLOW, img, 16, 16);
        assertTransparent(img, 5, 22);
    }

    @Test
    void coverAndContain() {
        // A 40x20 box: cover scales the 10x10 image by 4, contain by 2.
        BufferedImage cover = paint(BoxStyle.builder().size(40, 20)
                .backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT).withSize(Size.COVER)));
        assertPixel(RED, cover, 8, 8);
        assertPixel(GREEN, cover, 30, 8);
        assertPixel(RED, cover, 8, 17);
        // Centred, the lower half comes into view.
        BufferedImage centred = paint(BoxStyle.builder().size(40, 20)
                .backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                        .withSize(Size.COVER).withPosition(50, true, 50, true)));
        assertPixel(RED, centred, 8, 3);
        assertPixel(BLUE, centred, 8, 16);
        assertPixel(YELLOW, centred, 30, 16);

        BufferedImage contain = paint(BoxStyle.builder().size(40, 20)
                .backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT).withSize(Size.CONTAIN)));
        assertPixel(RED, contain, 4, 4);
        assertPixel(GREEN, contain, 15, 4);
        assertPixel(YELLOW, contain, 15, 15);
        assertTransparent(contain, 22, 10);
        BufferedImage containRepeat = paint(BoxStyle.builder().size(40, 20)
                .backgroundImage(new BackgroundImage(quadrants()).withSize(Size.CONTAIN)));
        assertPixel(RED, containRepeat, 24, 4);
        assertPixel(YELLOW, containRepeat, 35, 15);
    }

    @Test
    void largeReductionAveragesTheSource() {
        // A 64x64 checkerboard of single black and white pixels shrunk to
        // 4x4 is grey, not whichever pixels a single sample happens to hit.
        BufferedImage checker = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                checker.setRGB(x, y, ((x + y) & 1) == 0 ? 0xff000000 : 0xffffffff);
            }
        }
        BufferedImage img = paint(BoxStyle.builder().size(4, 4)
                .backgroundImage(new BackgroundImage(checker).withSize(Size.COVER)));
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                assertPixelNear(0xff808080, img, x, y, 6);
            }
        }
    }

    @Test
    void imageIsClippedToTheRoundedBorderBoxAndPaintedOverTheGradient() {
        BufferedImage img = paint(BoxStyle.builder().size(30, 30).radii(10)
                .gradient(GradientSpec.linear(90).addStop(WHITE).addStop(WHITE))
                .backgroundImage(new BackgroundImage(quadrants()).withRepeat(Repeat.NO_REPEAT)
                        .withPosition(10, false, 10, false)));
        assertTransparent(img, 0, 0);
        assertTransparent(img, 1, 1);
        assertPixel(WHITE, img, 5, 15);
        assertPixel(RED, img, 12, 12);
        assertPixel(YELLOW, img, 17, 17);
        BufferedImage corner = paint(BoxStyle.builder().size(30, 30).radii(10)
                .backgroundImage(new BackgroundImage(quadrants())));
        assertTransparent(corner, 1, 1);
        assertPixel(YELLOW, corner, 7, 7);
        assertPixel(GREEN, corner, 15, 0);
    }

    @Test
    void manyTilesFallBackToATexture() {
        // 200x200 tiles of 2x2 is 10000 tiles, over the tile-by-tile limit.
        BufferedImage dot = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        dot.setRGB(0, 0, RED);
        dot.setRGB(1, 0, GREEN);
        dot.setRGB(0, 1, BLUE);
        dot.setRGB(1, 1, YELLOW);
        BufferedImage img = paint(BoxStyle.builder().size(200, 200).backgroundImage(new BackgroundImage(dot)));
        assertPixel(RED, img, 0, 0);
        assertPixel(GREEN, img, 1, 0);
        assertPixel(BLUE, img, 0, 1);
        assertPixel(YELLOW, img, 1, 1);
        assertPixel(RED, img, 100, 100);
        assertPixel(YELLOW, img, 199, 199);
        assertPixel(GREEN, img, 151, 76);
    }

    @Test
    void tilesTooSmallToCountAreStillPaintedPromptly() {
        // Both tile counts are past what a long holds, and their product
        // wraps around to something small.
        final BufferedImage dot = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(20),
                () -> paint(BoxStyle.builder().size(30, 30)
                        .backgroundImage(new BackgroundImage(dot).withSize(1e-300, 1e-300))));
    }
}
