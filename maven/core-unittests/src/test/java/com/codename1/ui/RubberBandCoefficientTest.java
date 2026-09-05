/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

/**
 * What {@code rubberBandCoefficientInt} actually buys, in pixels.
 *
 * <p>Codename One compresses an over-edge drag with {@code c*x*D/(c*x + D)} for a finger
 * distance {@code x} and viewport {@code D}, where {@code c} is this constant. The default
 * 0.55 is UIScrollView's.</p>
 *
 * <p>A toolkit that instead damps each drag delta by {@code k*(1 - overscroll/D)^2}
 * integrates to the SAME closed form with {@code c = k}, so matching another platform's
 * overscroll is a matter of setting this one constant rather than replacing the physics.</p>
 *
 * <p>These pin the framework side of that: the closed form really is what
 * {@code rubberBandCompress} computes, and 0.52 versus 0.55 is a difference you can see.</p>
 */
class RubberBandCoefficientTest extends UITestBase {

    private static final int D = 800;

    private static void coefficient(int hundredths) {
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        // The @ is what makes it a CONSTANT rather than a style property.
        h.put("@rubberBandCoefficientInt", String.valueOf(hundredths));
        UIManager.getInstance().addThemeProps(h);
    }

    private static double closedForm(double finger, double c) {
        return c * finger * D / (c * finger + D);
    }

    @EdtTest
    void compressMatchesTheClosedFormAtACustomCoefficient() {
        coefficient(52);
        for (int finger : new int[] {10, 25, 50, 100, 200, 400, 800, 1600}) {
            assertEquals(closedForm(finger, 0.52), Component.rubberBandCompress(finger, D), 1.0,
                    "finger=" + finger);
        }
    }

    @EdtTest
    void compressMatchesTheClosedFormAtTheDefault() {
        coefficient(55);
        for (int finger : new int[] {10, 100, 400, 1600}) {
            assertEquals(closedForm(finger, 0.55), Component.rubberBandCompress(finger, D), 1.0,
                    "finger=" + finger);
        }
    }

    @EdtTest
    void theCoefficientMakesAVisibleDifference() {
        coefficient(55);
        int atDefault = Component.rubberBandCompress(400, D);
        coefficient(52);
        int atCustom = Component.rubberBandCompress(400, D);
        assertTrue(atDefault > atCustom,
                "0.55 must stretch further than 0.52: " + atDefault + " vs " + atCustom);
        // If the constant moved the band by a pixel it would not be worth setting.
        assertTrue(atDefault - atCustom >= 3,
                "expected a visible difference, got " + (atDefault - atCustom) + "px");
    }

    @EdtTest
    void compressAndDecompressAreInverses() {
        // The drag path reconstructs the finger distance from the displayed offset every
        // frame; if the pair drifted, a held overscroll would creep.
        coefficient(52);
        for (int finger : new int[] {20, 80, 200, 500}) {
            int compressed = Component.rubberBandCompress(finger, D);
            assertEquals(finger, Component.rubberBandDecompress(compressed, D), 2.0,
                    "finger=" + finger + " compressed=" + compressed);
        }
    }

    @EdtTest
    void theBandNeverExceedsTheViewport() {
        coefficient(52);
        // The curve asymptotes at D, so even an absurd drag cannot pull the content
        // further than one viewport past its edge.
        assertTrue(Component.rubberBandCompress(100000, D) < D, "must stay under the viewport");
        assertTrue(Component.rubberBandCompress(100000, D) > D * 0.9, "...but approach it");
    }
}
