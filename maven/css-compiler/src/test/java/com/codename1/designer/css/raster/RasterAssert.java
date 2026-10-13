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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;

/// Pixel assertions shared by the rasterizer tests.
final class RasterAssert {
    static final int RED = 0xffff0000;
    static final int GREEN = 0xff00ff00;
    static final int BLUE = 0xff0000ff;
    static final int YELLOW = 0xffffff00;
    static final int CYAN = 0xff00ffff;
    static final int MAGENTA = 0xffff00ff;
    static final int WHITE = 0xffffffff;
    static final int BLACK = 0xff000000;
    static final int GREY = 0xff808080;

    private RasterAssert() {
    }

    static BufferedImage paint(BoxStyle.Builder b) {
        return new CssBoxRasterizer().rasterize(b.build());
    }

    static int alpha(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24;
    }

    static String hex(int argb) {
        return "0x" + Integer.toHexString(argb);
    }

    static void assertPixel(int expected, BufferedImage img, int x, int y) {
        int actual = img.getRGB(x, y);
        if (expected != actual) {
            fail("pixel (" + x + "," + y + ") expected " + hex(expected) + " but was " + hex(actual));
        }
    }

    static void assertTransparent(BufferedImage img, int x, int y) {
        assertEquals(0, alpha(img, x, y), "pixel (" + x + "," + y + ") should be fully transparent, was "
                + hex(img.getRGB(x, y)));
    }

    static void assertOpaque(BufferedImage img, int x, int y) {
        assertEquals(255, alpha(img, x, y), "pixel (" + x + "," + y + ") should be fully opaque, was "
                + hex(img.getRGB(x, y)));
    }

    /// Asserts every channel, alpha included, is within `tol` of `expected`.
    static void assertNear(int expected, int actual, int tol, String what) {
        for (int shift = 24; shift >= 0; shift -= 8) {
            int e = (expected >> shift) & 0xff;
            int a = (actual >> shift) & 0xff;
            assertTrue(Math.abs(e - a) <= tol,
                    what + ": expected " + hex(expected) + " within " + tol + " but was " + hex(actual));
        }
    }

    static void assertPixelNear(int expected, BufferedImage img, int x, int y, int tol) {
        assertNear(expected, img.getRGB(x, y), tol, "pixel (" + x + "," + y + ")");
    }

    /// The number of runs of pixels with alpha above 128 along a row.
    static int runsInRow(BufferedImage img, int y) {
        int runs = 0;
        boolean in = false;
        for (int x = 0; x < img.getWidth(); x++) {
            boolean on = alpha(img, x, y) > 128;
            if (on && !in) {
                runs++;
            }
            in = on;
        }
        return runs;
    }

    /// A 10x10 image of four 5x5 quadrants: red top left, green top right,
    /// blue bottom left, yellow bottom right.
    static BufferedImage quadrants() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                img.setRGB(x, y, y < 5 ? (x < 5 ? RED : GREEN) : (x < 5 ? BLUE : YELLOW));
            }
        }
        return img;
    }
}
