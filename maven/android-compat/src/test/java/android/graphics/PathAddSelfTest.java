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

/// `addPath(this)` appends one copy of the path as it was before the call,
/// instead of looping forever over operations it is itself appending.
public class PathAddSelfTest {

    private static final float EPS = 1e-3f;

    @Test(timeout = 10000)
    public void addingAPathToItselfAppendsOneCopy() {
        Path p = new Path();
        p.moveTo(0, 0);
        p.lineTo(10, 0);
        p.addPath(p, 0, 20);
        RectF b = new RectF();
        p.computeBounds(b, true);
        assertEquals(0f, b.left, EPS);
        assertEquals(10f, b.right, EPS);
        assertEquals(0f, b.top, EPS);
        assertEquals(20f, b.bottom, EPS);
        p.addPath(p);
        p.computeBounds(b, true);
        assertEquals(20f, b.bottom, EPS);
    }
}
