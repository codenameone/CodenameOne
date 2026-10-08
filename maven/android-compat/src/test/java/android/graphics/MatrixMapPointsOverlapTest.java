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

import static org.junit.Assert.assertArrayEquals;

/// `mapPoints` within one array reads every source point before an
/// overlapping destination overwrites it.
public class MatrixMapPointsOverlapTest {

    @Test
    public void shiftingPointsForwardInTheSameArrayMapsTheOriginals() {
        Matrix m = new Matrix();
        m.setTranslate(10, 20);
        float[] pts = {1, 2, 3, 4, 0, 0};
        m.mapPoints(pts, 2, pts, 0, 2);
        assertArrayEquals(new float[]{1, 2, 11, 22, 13, 24}, pts, 0f);
    }

    @Test
    public void shiftingPointsBackInTheSameArrayMapsTheOriginals() {
        Matrix m = new Matrix();
        m.setScale(2, 3);
        float[] pts = {0, 0, 1, 2, 3, 4};
        m.mapPoints(pts, 0, pts, 2, 2);
        assertArrayEquals(new float[]{2, 6, 6, 12, 3, 4}, pts, 0f);
    }
}
