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
package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for
 * https://github.com/codenameone/CodenameOne/issues/1524
 * -- the cubic-bezier motion now follows the standard CSS
 * cubic-bezier(x1, y1, x2, y2) curve, with P0 = (0,0) and P3 = (1,1)
 * implicit. The previous implementation was plugging the four floats into
 * a 1D Bernstein-basis polynomial directly, which produced a different
 * curve.
 *
 * Verified against reference values you can confirm with any CSS engine
 * or with cubic-bezier.com.
 */
class CubicBezierMotionTest extends UITestBase {

    /// Drives a Motion from sourceValue to destinationValue and reads its
    /// value at the given motion-relative time, simulating one frame of an
    /// animation. {@link Motion#setCurrentMotionTime(long)} accepts the
    /// motion-relative time directly.
    private static int sample(Motion m, long currentMillis) {
        m.setCurrentMotionTime(currentMillis);
        return m.getValue();
    }

    @Test
    void linearControlPointsApproximateLinearOutput() {
        // cubic-bezier(0, 0, 1, 1) is the identity curve, so output should
        // be approximately proportional to time.
        Motion m = Motion.createCubicBezierMotion(0, 1000, 1000, 0f, 0f, 1f, 1f);
        // Don't call m.start(): that sets startTime to AnimationTime.now()
        // (huge) and Motion.getValue() has a gate `startTime > currentMotionTime`
        // that would short-circuit to sourceValue. Leaving startTime at its
        // default 0 keeps the manual-time driver path clean.

        assertEquals(500, sample(m, 500), 25,
                "linear cubic-bezier should produce ~500 at t=0.5");
        assertEquals(250, sample(m, 250), 25,
                "linear cubic-bezier should produce ~250 at t=0.25");
    }

    @Test
    void easeOutCurveMatchesCSSReferenceAtMidpoint() {
        // Reporter's curve: cubic-bezier(0, 0, 0.75, 1). The CSS-correct
        // value at t=0.5 is approximately 0.619 (find u with Bx(u)=0.5,
        // then By(u)). Pre-fix the implementation returned ~0.406 -- so the
        // bound below distinguishes the two.
        Motion m = Motion.createCubicBezierMotion(0, 1000, 1000, 0f, 0f, 0.75f, 1f);
        // Don't call m.start(): that sets startTime to AnimationTime.now()
        // (huge) and Motion.getValue() has a gate `startTime > currentMotionTime`
        // that would short-circuit to sourceValue. Leaving startTime at its
        // default 0 keeps the manual-time driver path clean.

        int mid = sample(m, 500);
        // CSS-correct ~ 619; broad band tolerates Newton-Raphson rounding.
        assertTrue(mid >= 580 && mid <= 660,
                "cubic-bezier(0, 0, 0.75, 1) at t=0.5 should be ~619 (CSS), got " + mid
                        + ". The buggy pre-fix value was ~406. See #1524.");
    }

    @Test
    void easeInOutCurveStartsSlowEndsSlow() {
        // cubic-bezier(0.42, 0, 0.58, 1) is the canonical "ease-in-out".
        // At t=0.5 the value should be exactly 0.5 by symmetry.
        Motion m = Motion.createCubicBezierMotion(0, 1000, 1000, 0.42f, 0f, 0.58f, 1f);
        // Don't call m.start(): that sets startTime to AnimationTime.now()
        // (huge) and Motion.getValue() has a gate `startTime > currentMotionTime`
        // that would short-circuit to sourceValue. Leaving startTime at its
        // default 0 keeps the manual-time driver path clean.

        int mid = sample(m, 500);
        assertTrue(mid >= 480 && mid <= 520,
                "ease-in-out should hit ~500 at t=0.5, got " + mid);

        // Early sample should lag (S-curve), late sample should lead.
        int quarter = sample(m, 250);
        int threeQuarter = sample(m, 750);
        assertTrue(quarter < 250,
                "ease-in-out lags linear at t=0.25, got " + quarter);
        assertTrue(threeQuarter > 750,
                "ease-in-out leads linear at t=0.75, got " + threeQuarter);
    }

    @Test
    void endpointsAreExactlyHitForArbitraryControlPoints() {
        Motion m = Motion.createCubicBezierMotion(100, 400, 500, 0.2f, 0.8f, 0.6f, 0.1f);
        // Don't call m.start(): that sets startTime to AnimationTime.now()
        // (huge) and Motion.getValue() has a gate `startTime > currentMotionTime`
        // that would short-circuit to sourceValue. Leaving startTime at its
        // default 0 keeps the manual-time driver path clean.

        assertEquals(100, sample(m, 0),
                "value at t=0 must be the source exactly");
        // At t = duration the motion is finished and getCubicValue() bails
        // straight to destinationValue.
        assertEquals(400, sample(m, 500),
                "value at t=duration must be the destination exactly");
    }

    @Test
    void worksWithDecreasingRange() {
        // dest < source -- the value should still ease from 1000 down to 0
        // and never wander outside the [0, 1000] envelope.
        Motion m = Motion.createCubicBezierMotion(1000, 0, 1000, 0f, 0f, 0.75f, 1f);
        // Don't call m.start(): that sets startTime to AnimationTime.now()
        // (huge) and Motion.getValue() has a gate `startTime > currentMotionTime`
        // that would short-circuit to sourceValue. Leaving startTime at its
        // default 0 keeps the manual-time driver path clean.

        for (int t = 0; t <= 1000; t += 100) {
            int v = sample(m, t);
            assertTrue(v >= 0 && v <= 1000,
                    "value out of envelope at t=" + t + ": " + v);
        }
        assertEquals(1000, sample(m, 0));
        assertEquals(0, sample(m, 1000));
    }

    @Test
    void aThreePointMotionWithAFlatSegmentStaysContinuousAtTheJoin() {
        // Midpoint on the bottom edge: the first segment has no vertical extent. Its Y
        // cannot be normalized, and substituting linear progress there made the value run
        // up toward t -- about 499 of 1000 just before the join -- and then drop to 0 at
        // it. The segment's own cubic is flat at 0 all the way.
        Motion m = Motion.createThreePointCubicMotion(0, 1000, 1000,
                0.1f, 0f, 0.4f, 0f,
                0.5f, 0f,
                0.6f, 0.3f, 0.9f, 1f);
        assertEquals(0, sample(m, 499), 20, "the flat first segment stays at its level");
        assertEquals(0, sample(m, 250), 20);
        int atJoin = sample(m, 500);
        assertTrue(Math.abs(atJoin - sample(m, 499)) <= 20, "no jump at the join");
    }
}
