/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * The corner cut applied to an image that fills a rounded Material.
 *
 * <p>Flutter expresses card rounding as a clip, and so does this runtime — but a clip is
 * only as good as the port: on Codename One's iOS Metal backend a polygon clip masks
 * geometry and does NOT mask a textured quad, so a card's artwork paints over its own
 * rounded corners. An image that IS the surface therefore rounds its own bitmap, once,
 * when it is scaled.</p>
 */
class RoundedImageCornersTest {

    /// Runs the private corner cut over a plain opaque ARGB block.
    private static int[] cut(int w, int h, int radius) throws Exception {
        int[] argb = new int[w * h];
        java.util.Arrays.fill(argb, 0xFF204060);
        Method m = ImageRenderElement.class.getDeclaredMethod(
                "roundCornersInPlace", int[].class, int.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(null, argb, w, h, radius);
        return argb;
    }

    @Test
    @DisplayName("the extreme corner pixels are cleared, the centre is untouched")
    void cornersAreCutAndTheBodyIsNot() throws Exception {
        int w = 40;
        int h = 30;
        int[] p = cut(w, h, 8);

        assertEquals(0, p[0], "top left corner must be transparent");
        assertEquals(0, p[w - 1], "top right corner must be transparent");
        assertEquals(0, p[(h - 1) * w], "bottom left corner must be transparent");
        assertEquals(0, p[(h - 1) * w + w - 1], "bottom right corner must be transparent");

        assertEquals(0xFF204060, p[(h / 2) * w + w / 2], "the centre must be untouched");
        // Mid-edge pixels are inside the shape on every side.
        assertEquals(0xFF204060, p[(h / 2) * w], "the left edge midpoint is inside the shape");
        assertEquals(0xFF204060, p[w / 2], "the top edge midpoint is inside the shape");
    }

    @Test
    @DisplayName("a pixel just inside the arc survives")
    void theArcIsACircleNotABox() throws Exception {
        int w = 40;
        int h = 30;
        int r = 10;
        int[] p = cut(w, h, r);
        // The diagonal of the corner box is outside the arc; the point next to the arc's
        // own centre line is inside it. A box-shaped cut would clear both.
        assertEquals(0, p[0], "the extreme diagonal is outside the arc");
        assertNotEquals(0, p[(r - 1) * w + (r - 1)],
                "just inside the arc must survive - the cut is a circle, not a square");
    }

    @Test
    @DisplayName("zero radius leaves every pixel alone")
    void zeroRadiusIsANoop() throws Exception {
        int[] p = cut(10, 10, 0);
        for (int i = 0; i < p.length; i++) {
            assertEquals(0xFF204060, p[i], "index " + i);
        }
    }
}
