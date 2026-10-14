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

package com.codename1.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextInputGeometryTest {
    @Test void snapshotDoesNotRetainMutableArrays() {
        int[] points = {0,10,20,2,12, 1,20,20,2,12, 2,30,20,2,12};
        int[] selection = {10,20,20,12};
        int[] fallback = {30,20,2,12};
        TextInputGeometry geometry = new TextInputGeometry(points, 0, 2, selection, fallback);
        points[1] = 900; selection[0] = 900; fallback[0] = 900;
        assertArrayEquals(new int[]{10,20,2,12}, geometry.rectForOffset(0));
        assertArrayEquals(new int[]{10,20,20,12}, geometry.selectionRects(2,0));
        assertArrayEquals(new int[]{30,20,2,12}, geometry.rectForOffset(99));
        int[] returned = geometry.rectForOffset(0); returned[0] = 400;
        assertEquals(10, geometry.rectForOffset(0)[0]);
    }

    @Test void hitTestingChoosesNearestLineThenCaretAndSupportsRtlPositions() {
        TextInputGeometry geometry = new TextInputGeometry(new int[]{
            0,10,20,2,12, 1,30,20,2,12, 2,20,20,2,12,
            3,10,40,2,12, 4,30,40,2,12}, 0,0,new int[0],new int[]{10,20,2,12});
        assertEquals(2, geometry.offsetAtPoint(21,24));
        assertEquals(4, geometry.offsetAtPoint(29,45));
        assertEquals(3, geometry.offsetAtPoint(-100,500));
        assertArrayEquals(new int[0], geometry.selectionRects(1,1));
        assertArrayEquals(new int[]{20,20,10,12}, geometry.selectionRects(1,2));
    }
    @Test void adjacentLinesDoNotShareTheirBoundaryPixel() {
        TextInputGeometry geometry = new TextInputGeometry(new int[]{
            0,10,20,1,12, 1,100,32,1,12}, 0,0,new int[0],new int[]{10,20,1,12});
        assertEquals(0, geometry.offsetAtPoint(10,31));
        assertEquals(1, geometry.offsetAtPoint(10,32));
        assertEquals(1, geometry.offsetAtPoint(10,43));
    }

}
