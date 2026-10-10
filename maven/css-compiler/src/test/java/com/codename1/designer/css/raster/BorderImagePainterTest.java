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
import static com.codename1.designer.css.raster.RasterAssert.CYAN;
import static com.codename1.designer.css.raster.RasterAssert.GREEN;
import static com.codename1.designer.css.raster.RasterAssert.GREY;
import static com.codename1.designer.css.raster.RasterAssert.MAGENTA;
import static com.codename1.designer.css.raster.RasterAssert.RED;
import static com.codename1.designer.css.raster.RasterAssert.WHITE;
import static com.codename1.designer.css.raster.RasterAssert.YELLOW;
import static com.codename1.designer.css.raster.RasterAssert.assertPixel;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;

import com.codename1.designer.css.raster.BorderImage.Mode;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// The nine slices of a border image and where they land.
class BorderImagePainterTest {
    private static final int BORDER = 0xff123456;

    /// A 30x30 image cut 10px from each edge: red, green, blue and yellow
    /// corners clockwise from the top left; cyan, magenta, white and black
    /// edges clockwise from the top; a grey middle.
    private static BufferedImage nine() {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        int[][] colours = {
            {RED, CYAN, GREEN},
            {BLACK, GREY, MAGENTA},
            {YELLOW, WHITE, BLUE},
        };
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 30; x++) {
                img.setRGB(x, y, colours[y / 10][x / 10]);
            }
        }
        return img;
    }

    /// A 100x60 box with borders of 5 (top), 10 (right), 15 (bottom) and
    /// 20 (left), so every cell of the grid has its own size.
    private static BoxStyle.Builder box() {
        return BoxStyle.builder().size(100, 60)
                .top(new BorderSide(5, BorderStyle.SOLID, BORDER))
                .right(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .bottom(new BorderSide(15, BorderStyle.SOLID, BORDER))
                .left(new BorderSide(20, BorderStyle.SOLID, BORDER));
    }

    @Test
    void stretchPutsEachSliceInItsCell() {
        BufferedImage img = paint(box().borderImage(new BorderImage(nine(), 10, 10, 10, 10, false, Mode.STRETCH)));
        // Corners: 20x5, 10x5, 10x15 and 20x15.
        assertPixel(RED, img, 0, 0);
        assertPixel(RED, img, 19, 4);
        assertPixel(GREEN, img, 90, 0);
        assertPixel(GREEN, img, 99, 4);
        assertPixel(BLUE, img, 90, 45);
        assertPixel(BLUE, img, 99, 59);
        assertPixel(YELLOW, img, 0, 45);
        assertPixel(YELLOW, img, 19, 59);
        // Edges, checked at both ends so a misplaced cell shows.
        assertPixel(CYAN, img, 20, 0);
        assertPixel(CYAN, img, 89, 4);
        assertPixel(MAGENTA, img, 90, 5);
        assertPixel(MAGENTA, img, 99, 44);
        assertPixel(WHITE, img, 20, 45);
        assertPixel(WHITE, img, 89, 59);
        assertPixel(BLACK, img, 0, 5);
        assertPixel(BLACK, img, 19, 44);
        // The middle is left alone without fill.
        assertTransparent(img, 20, 5);
        assertTransparent(img, 55, 25);
        assertTransparent(img, 89, 44);
    }

    @Test
    void fillPaintsTheMiddleSlice() {
        BufferedImage img = paint(box().borderImage(new BorderImage(nine(), 10, 10, 10, 10, true, Mode.STRETCH)));
        assertPixel(GREY, img, 20, 5);
        assertPixel(GREY, img, 55, 25);
        assertPixel(GREY, img, 89, 44);
        assertPixel(CYAN, img, 55, 4);
        assertPixel(BLACK, img, 19, 25);
    }

    @Test
    void borderImageReplacesTheBorderStyles() {
        BufferedImage img = paint(box().backgroundColor(0xff010203)
                .borderImage(new BorderImage(nine(), 10, 10, 10, 10, false, Mode.STRETCH)));
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 100; x++) {
                if (img.getRGB(x, y) == BORDER) {
                    throw new AssertionError("the solid border was painted at (" + x + "," + y + ")");
                }
            }
        }
        // The background is still there, under the unfilled middle.
        assertPixel(0xff010203, img, 55, 25);
        assertPixel(CYAN, img, 55, 2);
    }

    @Test
    void borderImageIsPlacedOnThePaddedBox() {
        BufferedImage img = paint(box().pad(7).borderImage(new BorderImage(nine(), 10, 10, 10, 10, false, Mode.STRETCH)));
        assertTransparent(img, 6, 7);
        assertTransparent(img, 7, 6);
        assertPixel(RED, img, 7, 7);
        assertPixel(BLUE, img, 106, 66);
        assertTransparent(img, 107, 66);
    }

    @Test
    void sideWithoutAWidthGetsNoSlice() {
        BufferedImage img = paint(BoxStyle.builder().size(60, 40)
                .top(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(nine(), 10, 10, 10, 10, false, Mode.STRETCH)));
        // Only the top edge has a thickness; the corners have no width.
        assertPixel(CYAN, img, 0, 0);
        assertPixel(CYAN, img, 59, 9);
        assertTransparent(img, 30, 10);
        assertTransparent(img, 0, 39);
    }

    @Test
    void unevenSlicesCutTheImageWhereTheySay() {
        // Slice 20 from the top: the top row of cells now takes the red,
        // cyan and green of the first band AND the black, grey, magenta of
        // the second, squeezed into the 5px border.
        BufferedImage img = paint(box().borderImage(new BorderImage(nine(), 20, 10, 10, 10, false, Mode.STRETCH)));
        assertPixel(RED, img, 5, 0);
        assertPixel(BLACK, img, 5, 4);
        assertPixel(CYAN, img, 50, 0);
        assertPixel(GREY, img, 50, 4);
        // The left edge has nothing left between the top and bottom slices.
        assertTransparent(img, 5, 25);
    }

    /// The same layout with a top edge slice that is cyan on its left half
    /// and magenta on its right half, to tell tiles apart.
    private static BufferedImage striped() {
        BufferedImage img = nine();
        for (int y = 0; y < 10; y++) {
            for (int x = 15; x < 20; x++) {
                img.setRGB(x, y, MAGENTA);
            }
        }
        return img;
    }

    @Test
    void repeatCentresWholeTilesAndCutsTheEnds() {
        // A 10px border: the 10x10 edge slice tiles at its own size. The
        // top cell is x 10..90, 80 wide; a centred tile starts at 45, so
        // tiles start at 5, 15, 25 ... and the first is cut in half.
        BufferedImage img = paint(BoxStyle.builder().size(100, 60).border(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(striped(), 10, 10, 10, 10, false, Mode.REPEAT)));
        assertPixel(MAGENTA, img, 10, 5);
        assertPixel(MAGENTA, img, 14, 5);
        assertPixel(CYAN, img, 15, 5);
        assertPixel(CYAN, img, 19, 5);
        assertPixel(MAGENTA, img, 20, 5);
        assertPixel(CYAN, img, 85, 5);
        assertPixel(CYAN, img, 89, 5);
        // The corners are untouched by the tiling.
        assertPixel(RED, img, 9, 5);
        assertPixel(GREEN, img, 90, 5);
    }

    @Test
    void roundFitsAWholeNumberOfTiles() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 60).border(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(striped(), 10, 10, 10, 10, false, Mode.ROUND)));
        // 80 / 10 is exactly 8 tiles, starting on the cell edge.
        assertPixel(CYAN, img, 10, 5);
        assertPixel(CYAN, img, 14, 5);
        assertPixel(MAGENTA, img, 15, 5);
        assertPixel(MAGENTA, img, 19, 5);
        assertPixel(CYAN, img, 20, 5);
        assertPixel(MAGENTA, img, 89, 5);
        // 100 wide cell (box 120): still whole tiles, 10 of them.
        BufferedImage wide = paint(BoxStyle.builder().size(120, 60).border(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(striped(), 10, 10, 10, 10, false, Mode.ROUND)));
        assertPixel(CYAN, wide, 10, 5);
        assertPixel(MAGENTA, wide, 109, 5);
        // 84 wide cell: 8 tiles of 10.5. Stretching one tile over the cell
        // would put magenta only in the right half.
        BufferedImage odd = paint(BoxStyle.builder().size(104, 60).border(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(striped(), 10, 10, 10, 10, false, Mode.ROUND)));
        assertPixel(CYAN, odd, 11, 5);
        assertPixel(MAGENTA, odd, 18, 5);
        assertPixel(CYAN, odd, 22, 5);
        assertPixel(MAGENTA, odd, 92, 5);
    }

    @Test
    void stretchScalesOneTileOverTheCell() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 60).border(new BorderSide(10, BorderStyle.SOLID, BORDER))
                .borderImage(new BorderImage(striped(), 10, 10, 10, 10, false, Mode.STRETCH)));
        assertPixel(CYAN, img, 12, 5);
        assertPixel(CYAN, img, 40, 5);
        assertPixel(MAGENTA, img, 60, 5);
        assertPixel(MAGENTA, img, 88, 5);
    }
}
