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
package com.codename1.ui.chart.series;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The monotone spline never overshoots: tangents are flat at extrema and follow the data's
/// direction everywhere else.
public class MonotoneSplineTest {

    @Test
    public void tangentsFollowTheDataAndFlattenAtExtrema() {
        float[] x = {0, 10, 20, 30, 40, 50};
        float[] y = {10, 20, 80, 60, 60, 90};
        float[] t = LineSeries.monotoneTangents(x, y, 0, x.length - 1);
        assertEquals(x.length, t.length);
        // rising into the peak at index 2, falling out of it
        assertTrue(t[1] > 0);
        assertEquals(0, t[2], 1e-6);
        // a flat segment has flat tangents at both ends
        assertEquals(0, t[3], 1e-6);
        assertEquals(0, t[4], 1e-6);
        assertTrue(t[5] > 0);
    }

    @Test
    public void monotoneDataKeepsEveryTangentNonNegative() {
        float[] x = {0, 1, 2, 3, 4, 5, 6};
        float[] y = {0, 0.1f, 5, 5.1f, 5.2f, 40, 41};
        float[] t = LineSeries.monotoneTangents(x, y, 0, x.length - 1);
        for (int i = 0; i < t.length - 1; i++) {
            assertTrue(t[i] >= 0, "tangent " + i);
            // Fritsch-Carlson: alpha^2 + beta^2 <= 9 keeps the segment monotone
            float d = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
            if (d > 0) {
                float a = t[i] / d;
                float b = t[i + 1] / d;
                assertTrue(a * a + b * b <= 9.0001f, "segment " + i);
            }
        }
    }
}
