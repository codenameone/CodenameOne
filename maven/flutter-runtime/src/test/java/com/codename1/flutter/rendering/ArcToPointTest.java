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
package com.codename1.flutter.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * arcToPoint has to produce an ARC, not the chord across it.
 *
 * <p>Both path converters used to answer a straight line here. A bottom app bar's notch
 * is two quadratics either side of one of these, so the outline curved down, cut straight
 * across, and curved back up -- a dimple with a bump in it.</p>
 */
class ArcToPointTest {

    /** Every returned point must sit on the circle the arc was asked for. */
    private static void onCircle(double[] pts, double cx, double cy, double r) {
        for (int i = 0; i < pts.length; i += 2) {
            double d = Math.hypot(pts[i] - cx, pts[i + 1] - cy);
            assertEquals(r, d, 1e-6,
                    "point " + i / 2 + " at (" + pts[i] + "," + pts[i + 1] + ") is off the circle");
        }
    }

    @Test
    void aSemicircleBowsAwayFromTheChord() {
        // (-10,0) to (10,0) with radius 10 is a half circle; centre must be the origin.
        double[] pts = GraphicsCanvas.arcToPoint(-10, 0, 10, 0, 10, false, true, 8);
        onCircle(pts, 0, 0, 10);
        // the mid point is off the chord by the full radius
        double midY = pts[3 * 2 + 1];
        assertEquals(10.0, Math.abs(midY), 1e-6);
    }

    @Test
    void theOppositeSweepBowsTheOtherWay() {
        double[] cw = GraphicsCanvas.arcToPoint(-10, 0, 10, 0, 10, false, true, 8);
        double[] ccw = GraphicsCanvas.arcToPoint(-10, 0, 10, 0, 10, false, false, 8);
        assertTrue(cw[7] * ccw[7] < 0, "clockwise and anticlockwise must bow to opposite sides");
    }

    @Test
    void itEndsWhereItWasToldTo() {
        double[] pts = GraphicsCanvas.arcToPoint(0, 0, 12, 5, 9, false, true, 12);
        assertEquals(12.0, pts[pts.length - 2], 1e-6);
        assertEquals(5.0, pts[pts.length - 1], 1e-6);
    }

    /// A radius too small for the two points is grown to the smallest that reaches, as
    /// SVG does, rather than producing NaN.
    @Test
    void aRadiusTooSmallIsGrown() {
        double[] pts = GraphicsCanvas.arcToPoint(-10, 0, 10, 0, 3, false, true, 6);
        onCircle(pts, 0, 0, 10);
    }

    /// Degenerate input asks the caller to draw the chord instead of guessing.
    @Test
    void aZeroLengthArcAnswersNothing() {
        assertNull(GraphicsCanvas.arcToPoint(5, 5, 5, 5, 10, false, true, 8));
        assertNull(GraphicsCanvas.arcToPoint(0, 0, 10, 0, 10, false, true, 1));
    }

    /// The curve is monotone across the notch: no point may double back past the end,
    /// which is what a bump looks like.
    @Test
    void theArcDoesNotDoubleBack() {
        double[] pts = GraphicsCanvas.arcToPoint(-10, 0, 10, 0, 10, false, true, 16);
        double prev = -10;
        for (int i = 0; i < pts.length; i += 2) {
            assertTrue(pts[i] >= prev - 1e-9, "x went backwards at " + i / 2);
            prev = pts[i];
        }
    }
}
