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
package com.codename1.flutter.animation;

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.EdgeInsetsDirectional;
import com.codename1.flutter.Radius;
import com.codename1.flutter.RelativeRect;
import com.codename1.flutter.vectormath.Matrix4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tween has to produce values BETWEEN its ends.
 *
 * <p>Four of them used to fall through to the base class's fallback, which returns
 * {@code begin} below t=0.5 and {@code end} above it. Anything animated by one of them
 * therefore did not move: it sat at its start value for half the duration and then
 * appeared, already finished. The gallery's settings panel is animated by
 * {@link RelativeRectTween} -- it begins a full screen-height above the viewport and
 * slides down over 80ms -- so tapping the settings button produced a menu that popped
 * into place rather than one that slid.</p>
 *
 * <p>Each test samples a value strictly inside the range and asserts it is strictly
 * between the ends, which is exactly what the stepping fallback cannot do.</p>
 */
class TweenInterpolationTest {

    private static void between(double lo, double hi, double actual, String what) {
        assertTrue(actual > Math.min(lo, hi) && actual < Math.max(lo, hi),
                what + ": expected strictly between " + lo + " and " + hi + ", got " + actual);
    }

    @Test
    void aRelativeRectMovesEveryFrame() {
        RelativeRectTween t = new RelativeRectTween();
        t.begin(RelativeRect.fromLTRB(0, -800, 0, 0));
        t.end(RelativeRect.fill);

        RelativeRect quarter = t.transform(0.25);
        RelativeRect half = t.transform(0.5);
        RelativeRect threeQuarters = t.transform(0.75);

        assertEquals(-600, quarter.top(), 1e-9);
        assertEquals(-400, half.top(), 1e-9);
        assertEquals(-200, threeQuarters.top(), 1e-9);
        // and it still lands exactly on its ends
        assertEquals(-800, t.transform(0.0).top(), 1e-9);
        assertEquals(0, t.transform(1.0).top(), 1e-9);
    }

    @Test
    void aBorderRadiusOpensGradually() {
        BorderRadiusTween t = new BorderRadiusTween();
        t.begin(BorderRadius.circular(0));
        t.end(BorderRadius.circular(40));
        between(0, 40, t.transform(0.25).topLeft().x(), "topLeft at 0.25");
        assertEquals(20, t.transform(0.5).bottomRight().x(), 1e-9);
    }

    @Test
    void insetsEaseRatherThanJump() {
        EdgeInsetsGeometryTween t = new EdgeInsetsGeometryTween();
        t.begin(EdgeInsets.all(0));
        t.end(EdgeInsets.all(16));
        EdgeInsets mid = (EdgeInsets) t.transform(0.5);
        assertEquals(8, mid.left(), 1e-9);
        assertEquals(8, mid.bottom(), 1e-9);
    }

    /// Two directional insets must stay directional across the animation -- becoming
    /// left-to-right at the first frame would flip the padding on an RTL layout.
    @Test
    void twoDirectionalInsetsStayDirectional() {
        EdgeInsetsGeometryTween t = new EdgeInsetsGeometryTween();
        t.begin(EdgeInsetsDirectional.fromSTEB(0, 0, 0, 0));
        t.end(EdgeInsetsDirectional.fromSTEB(20, 4, 8, 12));
        Object mid = t.transform(0.5);
        assertInstanceOf(EdgeInsetsDirectional.class, mid);
        assertEquals(10, ((EdgeInsetsDirectional) mid).start(), 1e-9);
        assertEquals(4, ((EdgeInsetsDirectional) mid).end(), 1e-9);
    }

    @Test
    void aMatrixInterpolatesComponentwise() {
        Matrix4Tween t = new Matrix4Tween();
        t.begin(Matrix4.identity());
        t.end(Matrix4.translationValues(100, 40, 0));
        Matrix4 mid = t.transform(0.5);
        assertEquals(50.0, mid.storage().get(12), 1e-9);
        assertEquals(20.0, mid.storage().get(13), 1e-9);
        // the diagonal is 1 at both ends, so it must stay 1 throughout
        assertEquals(1.0, mid.storage().get(0), 1e-9);
    }

    /// The base class still steps for a type nothing knows how to interpolate. That is
    /// the deliberate fallback, not an oversight, and it must not start returning nulls.
    @Test
    void anUninterpolatableTypeStillStepsRatherThanFailing() {
        Tween<String> t = new Tween<String>();
        t.begin("a");
        t.end("b");
        assertEquals("a", t.transform(0.25));
        assertEquals("b", t.transform(0.75));
    }
}
