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
package com.codename1.flutter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;
import java.util.Map;

/**
 * The scroll-physics constants are installed as CONSTANTS.
 *
 * <p>This looks like a test of a spelling, and it is — but it is the spelling that decides
 * whether the values do anything at all. {@code UIManager.buildTheme} sorts an incoming
 * props table by the leading {@code @}: keys that have it become theme constants, keys
 * that do not become ordinary style properties. Both are accepted silently, so a constant
 * written without the prefix is stored, never read, and {@code getThemeConstant} goes on
 * returning its default.</p>
 *
 * <p>That is not hypothetical. The fling-distance constant was installed without the
 * prefix and had no effect for the whole time it was believed to be fixing the scroll —
 * on a device, where the only symptom was that the feel never quite matched and no error
 * was ever raised.</p>
 */
class ScrollPhysicsPropsTest {

    @Test
    @DisplayName("every physics key is prefixed, or the value is silently ignored")
    void everyKeyIsAThemeConstant() {
        Hashtable<String, Object> props = FlutterUI.scrollPhysicsProps();
        assertTrue(!props.isEmpty(), "expected some physics constants");
        for (Map.Entry<String, Object> e : props.entrySet()) {
            assertTrue(e.getKey().startsWith("@"),
                    "'" + e.getKey() + "' is missing the @ that makes it a theme constant, "
                            + "so UIManager will file it as a style property and "
                            + "getThemeConstant will keep returning the default");
        }
    }

    @Test
    @DisplayName("the fling distance matches Flutter's friction simulation")
    void flingDistanceIsFlutterst() {
        // CN1 coasts velocity * this/1000; Flutter's iOS FrictionSimulation (drag 0.135)
        // travels -v/ln(0.135) = 0.4994 * v. 950 - the framework default - is 1.9x too far.
        assertEquals("500", FlutterUI.scrollPhysicsProps().get("@DecayMotionScaleFactorInt"));
    }

    @Test
    @DisplayName("the rubber band matches BouncingScrollPhysics")
    void rubberBandIsFluttersCoefficient() {
        // Hundredths. Integrating Flutter's per-delta friction gives CN1's closed form with
        // c = 0.52; the framework default is UIScrollView's 0.55. See RubberBandParityTest.
        assertEquals("52", FlutterUI.scrollPhysicsProps().get("@rubberBandCoefficientInt"));
    }
}
