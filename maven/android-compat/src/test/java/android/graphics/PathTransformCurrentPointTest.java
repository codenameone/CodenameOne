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

/// `transform()` moves the current point and contour start with the path,
/// so a relative command or a close appended afterwards continues from the
/// transformed position instead of jumping back to the old one.
public class PathTransformCurrentPointTest {

    private static final float EPS = 1e-3f;

    @Test
    public void relativeCommandContinuesFromTheTransformedPoint() {
        Path p = new Path();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        Matrix m = new Matrix();
        m.setTranslate(100, 50);
        p.transform(m);
        p.rLineTo(5, 0);
        RectF b = new RectF();
        p.computeBounds(b, true);
        assertEquals(100f, b.left, EPS);
        assertEquals(115f, b.right, EPS);
        assertEquals(50f, b.top, EPS);
        assertEquals(50f, b.bottom, EPS);
    }

    @Test
    public void closeThenContinueStartsAtTheTransformedContourStart() {
        Path p = new Path();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.lineTo(10, 10);
        Matrix m = new Matrix();
        m.setTranslate(100, 100);
        p.transform(m);
        p.close();
        p.rLineTo(0, -5);
        RectF b = new RectF();
        p.computeBounds(b, true);
        assertEquals(100f, b.left, EPS);
        assertEquals(95f, b.top, EPS);
    }
}
