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
package com.codename1.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.flutter.widgets.BouncingScrollPhysics;
import com.codename1.flutter.widgets.FixedScrollMetrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Codename One's overscroll and Flutter's are the same curve.
 *
 * <p>They do not look alike. Codename One compresses an over-edge drag in closed form,
 * {@code c*x*D/(c*x + D)} for a finger distance {@code x} and viewport {@code D};
 * {@link BouncingScrollPhysics} damps each individual drag delta by
 * {@code 0.52*(1 - overscroll/D)^2} and never mentions total distance. Integrating the
 * second gives the first with {@code c = 0.52}, so the only real difference is the
 * coefficient - Codename One ships UIScrollView's 0.55, Flutter uses 0.52 - and matching
 * the overscroll is one theme constant rather than a reimplementation.</p>
 *
 * <p>That is a derivation, and a derivation is exactly the kind of thing that is quietly
 * wrong, so these tests check it numerically: integrate the per-delta friction and see
 * whether the closed form falls out.</p>
 *
 * <p>The other half of the claim - that Codename One's own {@code rubberBandCompress}
 * really does compute that closed form once the coefficient is 52 - is checked by
 * {@code RubberBandCoefficientTest} in core-unittests, which is where the headless
 * Display those framework calls need already exists.</p>
 */
class RubberBandParityTest {

    private static final double D = 800;
    private static final double FLUTTER_C = 0.52;

    private static FixedScrollMetrics atOverscroll(double over) {
        FixedScrollMetrics m = new FixedScrollMetrics();
        m.minScrollExtent(0);
        m.maxScrollExtent(1000);
        m.viewportDimension(D);
        m.pixels(-over);          // past the leading edge
        return m;
    }

    /**
     * Walks a finger {@code totalFinger} pixels past the edge in small steps, applying the
     * physics to each, and returns how far the content actually moved. This is what a real
     * drag is: many small deltas, not one big one.
     */
    private static double integrate(double totalFinger, double step) {
        BouncingScrollPhysics physics = new BouncingScrollPhysics();
        double overscroll = 0;
        for (double moved = 0; moved < totalFinger; moved += step) {
            // Flutter applies the result as `pixels -= offset`, so a POSITIVE offset is
            // the one that pushes further past the leading edge.
            overscroll += physics.applyPhysicsToUserOffset(atOverscroll(overscroll), step);
        }
        return overscroll;
    }

    private static double closedForm(double finger, double c) {
        return c * finger * D / (c * finger + D);
    }

    @Test
    @DisplayName("integrating Flutter's per-delta friction gives Codename One's closed form")
    void theTwoModelsAreTheSameCurve() {
        for (double finger : new double[] {10, 50, 100, 200, 400, 800, 1600}) {
            double integrated = integrate(finger, 0.05);
            double closed = closedForm(finger, FLUTTER_C);
            // 1% of the closed form, which at these distances is well under a pixel; the
            // residue is the step size, not a disagreement between the models.
            assertEquals(closed, integrated, Math.max(0.5, closed * 0.01),
                    "finger=" + finger + " integrated=" + integrated + " closed=" + closed);
        }
    }

    @Test
    @DisplayName("a finer step converges on the closed form, confirming it is the integral")
    void refiningTheStepConverges() {
        double finger = 200;
        double closed = closedForm(finger, FLUTTER_C);
        double coarse = Math.abs(integrate(finger, 1.0) - closed);
        double fine = Math.abs(integrate(finger, 0.05) - closed);
        assertTrue(fine < coarse,
                "halving the step should move towards the closed form: coarse=" + coarse
                        + " fine=" + fine);
    }

    /** Both edges compress identically — an asymmetric rubber band reads as a broken list. */
    @Test
    @DisplayName("the leading and trailing edges resist the same")
    void bothEdgesAgree() {
        BouncingScrollPhysics physics = new BouncingScrollPhysics();
        for (double over : new double[] {15, 60, 150, 300}) {
            FixedScrollMetrics top = atOverscroll(over);
            FixedScrollMetrics bottom = new FixedScrollMetrics();
            bottom.minScrollExtent(0);
            bottom.maxScrollExtent(1000);
            bottom.viewportDimension(D);
            bottom.pixels(1000 + over);
            assertEquals(physics.applyPhysicsToUserOffset(top, 10.0),
                    -physics.applyPhysicsToUserOffset(bottom, -10.0), 1e-9,
                    "overscroll=" + over);
        }
    }

}
