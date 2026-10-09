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
import static org.junit.Assert.fail;

/// Perspective is refused by `setValues` rather than stored and ignored. The
/// matrix used to keep a non-affine third row that mapping, concatenation,
/// inversion and drawing all disregarded, so `(10, 0)` under `MPERSP_0 = 0.1`
/// mapped to `(10, 0)` instead of Android's `(5, 0)`.
public class MatrixPerspectiveTest {

    private static float[] affine() {
        return new float[] {2, 0, 3, 0, 2, 4, 0, 0, 1};
    }

    @Test
    public void nonAffineValuesAreRefused() {
        for (int i = Matrix.MPERSP_0; i <= Matrix.MPERSP_2; i++) {
            float[] v = affine();
            v[i] += 0.1f;
            Matrix m = new Matrix();
            try {
                m.setValues(v);
                fail("perspective entry " + i + " accepted");
            } catch (IllegalArgumentException expected) {
                // not silently ignored
            }
            assertEquals("a refused setValues leaves the matrix alone", new Matrix(), m);
        }
    }

    @Test
    public void affineValuesRoundTrip() {
        Matrix m = new Matrix();
        m.setValues(affine());
        float[] pts = {10, 0};
        m.mapPoints(pts);
        assertEquals(23, pts[0], 0);
        assertEquals(4, pts[1], 0);
        float[] out = new float[9];
        m.getValues(out);
        Matrix copy = new Matrix();
        copy.setValues(out);
        assertEquals(m, copy);
    }
}
