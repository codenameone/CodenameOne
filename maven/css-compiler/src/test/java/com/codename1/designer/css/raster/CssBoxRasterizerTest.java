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

import static com.codename1.designer.css.raster.RasterAssert.RED;
import static com.codename1.designer.css.raster.RasterAssert.alpha;
import static com.codename1.designer.css.raster.RasterAssert.assertOpaque;
import static com.codename1.designer.css.raster.RasterAssert.assertPixel;
import static com.codename1.designer.css.raster.RasterAssert.assertTransparent;
import static com.codename1.designer.css.raster.RasterAssert.paint;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// Image size, box placement, fills, rounded corners and input validation.
class CssBoxRasterizerTest {
    private static final int FILL = 0xff336699;

    @Test
    void imageIsArgbAndTheSizeOfAnUnpaddedBox() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 40).backgroundColor(FILL));
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.getType());
        assertEquals(100, img.getWidth());
        assertEquals(40, img.getHeight());
        assertPixel(FILL, img, 0, 0);
        assertPixel(FILL, img, 99, 39);
        assertPixel(FILL, img, 50, 20);
    }

    @Test
    void paddingSurroundsTheBoxWithTransparentPixels() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 40)
                .padTop(3).padRight(5).padBottom(7).padLeft(9).backgroundColor(FILL));
        assertEquals(114, img.getWidth());
        assertEquals(50, img.getHeight());
        // The border box starts exactly at (padLeft, padTop)...
        assertPixel(FILL, img, 9, 3);
        assertTransparent(img, 8, 3);
        assertTransparent(img, 9, 2);
        // ...and ends exactly padRight / padBottom short of the far edge.
        assertPixel(FILL, img, 108, 42);
        assertTransparent(img, 109, 42);
        assertTransparent(img, 108, 43);
        assertTransparent(img, 0, 0);
        assertTransparent(img, 113, 49);
    }

    @Test
    void fractionalBoxTruncatesTheImageButNotTheBox() {
        BufferedImage img = paint(BoxStyle.builder().size(100.5, 40.5).pad(1).backgroundColor(FILL));
        assertEquals(102, img.getWidth(), "(int) (100.5 + 1 + 1)");
        assertEquals(42, img.getHeight(), "(int) (40.5 + 1 + 1)");
        assertPixel(FILL, img, 100, 20);
        // The box ends at x = 101.5, half way through the last column.
        int a = alpha(img, 101, 20);
        assertTrue(a >= 120 && a <= 136, "half covered column should be about 50% opaque, alpha was " + a);
        int b = alpha(img, 50, 41);
        assertTrue(b >= 120 && b <= 136, "half covered row should be about 50% opaque, alpha was " + b);
        assertEquals(FILL & 0xffffff, img.getRGB(101, 20) & 0xffffff, "an edge pixel keeps the fill colour");
    }

    @Test
    void imageIsNeverSmallerThanOnePixel() {
        BufferedImage img = paint(BoxStyle.builder().size(0.4, 0.4).backgroundColor(FILL));
        assertEquals(1, img.getWidth());
        assertEquals(1, img.getHeight());
        int a = alpha(img, 0, 0);
        assertTrue(a > 20 && a < 60, "a 0.4 x 0.4 box covers 16% of the pixel, alpha was " + a);
    }

    @Test
    void translucentFillKeepsItsExactAlphaAndColour() {
        BufferedImage img = paint(BoxStyle.builder().size(20, 20).pad(2).backgroundColor(0x80336699));
        assertPixel(0x80336699, img, 10, 10);
        assertTransparent(img, 1, 10);
    }

    @Test
    void emptyStylePaintsNothing() {
        BufferedImage img = paint(BoxStyle.builder().size(10, 10));
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertTransparent(img, x, y);
            }
        }
    }

    @Test
    void roundedCornerIsTransparentOutsideAndOpaqueInside() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100).radii(20).backgroundColor(FILL));
        assertTransparent(img, 0, 0);
        assertTransparent(img, 2, 2);
        assertTransparent(img, 99, 0);
        assertTransparent(img, 0, 99);
        assertTransparent(img, 97, 97);
        // (6,6) lies wholly inside the arc of radius 20 around (20,20).
        assertPixel(FILL, img, 6, 6);
        assertPixel(FILL, img, 50, 50);
        assertPixel(FILL, img, 50, 0);
        assertPixel(FILL, img, 0, 50);
        // The arc crosses pixel (5,5): antialiased, neither empty nor full.
        int a = alpha(img, 5, 5);
        assertTrue(a > 0 && a < 255, "the pixel on the arc should be partially covered, alpha was " + a);
    }

    @Test
    void perCornerRadiiOnlyRoundTheirOwnCorner() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 100)
                .radii(30, 30, 0, 0, 0, 0, 0, 0).backgroundColor(FILL));
        assertTransparent(img, 2, 2);
        assertPixel(FILL, img, 99, 0);
        assertPixel(FILL, img, 99, 99);
        assertPixel(FILL, img, 0, 99);
    }

    @Test
    void ellipticalCornerFollowsBothRadii() {
        // Top-left radius 40 wide by 10 high: the ellipse is centred on (40,10).
        BufferedImage img = paint(BoxStyle.builder().size(100, 60)
                .radii(40, 10, 0, 0, 0, 0, 0, 0).backgroundColor(FILL));
        // ((10.5-40)/40)^2 + ((1.5-10)/10)^2 = 1.27: outside the ellipse,
        // although a circle of radius 10 would have covered it.
        assertTransparent(img, 10, 1);
        // ((20.5-40)/40)^2 + ((5.5-10)/10)^2 = 0.44: inside.
        assertPixel(FILL, img, 20, 5);
        // The ellipse meets the top edge at x = 40 and the left edge at y = 10.
        assertPixel(FILL, img, 41, 0);
        assertPixel(FILL, img, 0, 11);
        assertTransparent(img, 0, 0);
        assertTrue(alpha(img, 30, 0) < 255, "x = 30 on the top row is still on the curve");
    }

    @Test
    void oversizedRadiusIsScaledDownToAPill() {
        BufferedImage img = paint(BoxStyle.builder().size(100, 40).radii(1000).backgroundColor(FILL));
        // The overlap rule scales 1000 to 20: half the height.
        assertTransparent(img, 0, 0);
        assertTransparent(img, 3, 3);
        assertTransparent(img, 96, 36);
        assertPixel(FILL, img, 7, 7);
        assertPixel(FILL, img, 50, 0);
        assertPixel(FILL, img, 50, 39);
        // The straight part of the top edge is exactly x = 20..80.
        assertPixel(FILL, img, 21, 0);
        assertPixel(FILL, img, 78, 0);
        assertTrue(alpha(img, 15, 0) < 255, "x = 15 on the top row is on the curve");
        // The ends are semicircles touching the left and right edge mid height.
        assertTrue(alpha(img, 0, 20) > 200, "the pill touches the left edge at mid height");
        assertTrue(alpha(img, 99, 19) > 200, "the pill touches the right edge at mid height");
        assertTrue(alpha(img, 0, 10) < 30, "the left edge is clear of the pill away from mid height");
    }

    @Test
    void aSliverTooLongToAllocateIsRefused() {
        // A tenth of a pixel high still takes a whole row of the image.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(100000000, 0.1)));
        assertTrue(e.getMessage().contains("more than this rasterizer paints"), e.getMessage());
    }

    @Test
    void zeroSizeNamesTheField() {
        IllegalArgumentException w = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(0, 10)));
        assertTrue(w.getMessage().startsWith("borderBoxWidth"), w.getMessage());
        IllegalArgumentException h = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, -1)));
        assertTrue(h.getMessage().startsWith("borderBoxHeight"), h.getMessage());
        IllegalArgumentException nan = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(Double.NaN, 10)));
        assertTrue(nan.getMessage().startsWith("borderBoxWidth"), nan.getMessage());
    }

    @Test
    void nullSideNamesTheSide() {
        IllegalArgumentException top = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).top(null)));
        assertTrue(top.getMessage().startsWith("top "), top.getMessage());
        IllegalArgumentException left = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).left(null)));
        assertTrue(left.getMessage().startsWith("left "), left.getMessage());
        IllegalArgumentException style = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).right(new BorderSide(1, null, RED))));
        assertTrue(style.getMessage().startsWith("right.style"), style.getMessage());
        IllegalArgumentException width = assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).bottom(new BorderSide(-1, BorderStyle.SOLID, RED))));
        assertTrue(width.getMessage().startsWith("bottom.width"), width.getMessage());
    }

    @Test
    void otherInvalidInputNamesItsField() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new CssBoxRasterizer().rasterize(null)).getMessage().startsWith("style"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).padLeft(-1))).getMessage().startsWith("padLeft"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).radii(1, 2, 3))).getMessage().startsWith("radii"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).radii((double[]) null))).getMessage().startsWith("radii"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).radii(1, 1, 1, 1, 1, -4, 1, 1)))
                .getMessage().startsWith("radii[5]"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).shadow(new Shadow(0, 0, -1, 0, RED, false))))
                .getMessage().startsWith("shadow.blur"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).backgroundImage(new BackgroundImage(null))))
                .getMessage().startsWith("backgroundImage.image"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).borderImage(
                        new BorderImage(null, 1, 1, 1, 1, false, BorderImage.Mode.STRETCH))))
                .getMessage().startsWith("borderImage.image"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(10, 10).gradient(GradientSpec.linear(0).addStop(RED))))
                .getMessage().startsWith("gradient.stops"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> paint(BoxStyle.builder().size(1e6, 1e6))).getMessage().startsWith("borderBoxWidth"));
    }

    @Test
    void builderRoundTripsEveryField() {
        BorderSide t = new BorderSide(1, BorderStyle.SOLID, 1);
        BorderSide r = new BorderSide(2, BorderStyle.DASHED, 2);
        BorderSide b = new BorderSide(3, BorderStyle.DOTTED, 3);
        BorderSide l = new BorderSide(4, BorderStyle.DOUBLE, 4);
        Shadow shadow = new Shadow(1, 2, 3, 4, 5, true);
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BackgroundImage bg = new BackgroundImage(image);
        BorderImage bi = new BorderImage(image, 1, 1, 1, 1, true, BorderImage.Mode.ROUND);
        GradientSpec gradient = GradientSpec.linear(33).addStop(1).addStop(2);
        BoxStyle s = BoxStyle.builder().borderBoxWidth(11.5).borderBoxHeight(12.5)
                .padTop(1).padRight(2).padBottom(3).padLeft(4).backgroundColor(0x12345678)
                .gradient(gradient).backgroundImage(bg).top(t).right(r).bottom(b).left(l)
                .radii(1, 2, 3, 4, 5, 6, 7, 8).shadow(shadow).borderImage(bi).build();
        assertEquals(11.5, s.getBorderBoxWidth());
        assertEquals(12.5, s.getBorderBoxHeight());
        assertEquals(1, s.getPadTop());
        assertEquals(2, s.getPadRight());
        assertEquals(3, s.getPadBottom());
        assertEquals(4, s.getPadLeft());
        assertEquals(0x12345678, s.getBackgroundColor());
        assertSame(bg, s.getBackgroundImage());
        assertSame(t, s.getTop());
        assertSame(r, s.getRight());
        assertSame(b, s.getBottom());
        assertSame(l, s.getLeft());
        assertSame(shadow, s.getShadow());
        assertSame(bi, s.getBorderImage());
        assertArrayEquals(new double[] {1, 2, 3, 4, 5, 6, 7, 8}, s.getRadii());
        assertEquals(33, s.getGradient().getAngleDeg());
        // The style is immutable: neither the caller's spec nor a returned
        // array reaches back into it.
        gradient.angleDeg(90);
        s.getRadii()[0] = 99;
        assertEquals(33, s.getGradient().getAngleDeg());
        assertEquals(1, s.getRadii()[0]);
        assertNotNull(new CssBoxRasterizer().rasterize(s));
    }

    @Test
    void radiiShorthandsExpand() {
        assertArrayEquals(new double[] {5, 5, 5, 5, 5, 5, 5, 5},
                BoxStyle.builder().radii(5).build().getRadii());
        assertArrayEquals(new double[] {1, 1, 2, 2, 3, 3, 4, 4},
                BoxStyle.builder().radii(1, 2, 3, 4).build().getRadii());
    }

    @Test
    void defaultSidesAreNeverNull() {
        BoxStyle s = BoxStyle.builder().size(1, 1).build();
        assertSame(BorderSide.NONE, s.getTop());
        assertSame(BorderSide.NONE, s.getRight());
        assertSame(BorderSide.NONE, s.getBottom());
        assertSame(BorderSide.NONE, s.getLeft());
        assertOpaque(paint(BoxStyle.builder().size(1, 1).backgroundColor(RED)), 0, 0);
    }

    @Test
    void aGradientIsMeasuredInThePaddingBoxNotTheBorderBox() {
        // 100 wide with a 20px border left and right: the padding box runs
        // from 20 to 80. A hard stop at 50% therefore falls at x = 50 either
        // way, but one at 25% falls at 35, not at 25.
        BorderSide thick = new BorderSide(20, BorderStyle.SOLID, 0x00000000);
        GradientSpec gradient = GradientSpec.linear(90)
                .addStop(0xffff0000, 25, GradientSpec.Unit.PERCENT)
                .addStop(0xff0000ff, 25, GradientSpec.Unit.PERCENT);
        BufferedImage img = new CssBoxRasterizer().rasterize(
                BoxStyle.builder().size(100, 10).left(thick).right(thick).gradient(gradient).build());

        assertEquals(0xffff0000, img.getRGB(30, 5), "red up to a quarter of the padding box");
        assertEquals(0xff0000ff, img.getRGB(40, 5), "blue after it");
    }
}
