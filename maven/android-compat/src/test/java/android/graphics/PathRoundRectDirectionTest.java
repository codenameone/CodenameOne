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
package android.graphics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `addRoundRect` walks the outline in the requested direction, so an inner
/// counter-clockwise rounded rectangle winds against a clockwise outer
/// contour and cuts a hole under the non-zero rule.
public class PathRoundRectDirectionTest {

    /// Signed shoelace area of the flattened single-contour path; positive
    /// means clockwise in screen coordinates (y grows downwards).
    private static float signedArea(Path p) {
        float[] a = p.approximate(0.1f);
        int n = a.length / 3;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += a[i * 3 + 1] * a[j * 3 + 2] - a[j * 3 + 1] * a[i * 3 + 2];
        }
        return (float) (sum / 2);
    }

    @Test
    public void ccwRoundRectWindsOppositeToCw() {
        Path cw = new Path();
        cw.addRoundRect(new RectF(10, 20, 110, 80), 12, 8, Path.Direction.CW);
        Path ccw = new Path();
        ccw.addRoundRect(new RectF(10, 20, 110, 80), 12, 8, Path.Direction.CCW);
        float aCw = signedArea(cw);
        float aCcw = signedArea(ccw);
        assertTrue("CW area should be positive: " + aCw, aCw > 0);
        assertTrue("CCW area should be negative: " + aCcw, aCcw < 0);
        assertEquals(aCw, -aCcw, 0.5f);
    }

    @Test
    public void ccwRoundRectCoversTheSameOutline() {
        float[] radii = {4, 4, 10, 6, 0, 0, 20, 20};
        Path cw = new Path();
        cw.addRoundRect(new RectF(10, 20, 110, 80), radii, Path.Direction.CW);
        Path ccw = new Path();
        ccw.addRoundRect(new RectF(10, 20, 110, 80), radii, Path.Direction.CCW);
        RectF a = new RectF();
        RectF b = new RectF();
        cw.computeBounds(a, true);
        ccw.computeBounds(b, true);
        assertEquals(a.left, b.left, 1e-3f);
        assertEquals(a.top, b.top, 1e-3f);
        assertEquals(a.right, b.right, 1e-3f);
        assertEquals(a.bottom, b.bottom, 1e-3f);
        assertEquals(signedArea(cw), -signedArea(ccw), 0.5f);
    }
}
