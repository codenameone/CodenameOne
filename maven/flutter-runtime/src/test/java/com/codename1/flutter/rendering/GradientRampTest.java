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

/**
 * A gradient's colour part way along its ramp.
 *
 * <p>The fill used to read only the first and last entries of the ramp, so a gradient
 * with three or more stops lost every colour between them. It also only ever produced
 * those two colours, because the ramp was handed straight to the port's linear-gradient
 * primitive; the banded fill that replaced it asks for a colour per band, which is what
 * this answers.</p>
 */
class GradientRampTest {

    private static final int RED = 0xffff0000;
    private static final int BLUE = 0xff0000ff;
    private static final int GREEN = 0xff00ff00;

    private static String hex(int argb) {
        String s = Integer.toHexString(argb);
        while (s.length() < 8) {
            s = "0" + s;
        }
        return s;
    }

    @Test
    void theEndsAreThemselves() {
        int[] ramp = {RED, BLUE};
        assertEquals(hex(RED), hex(GraphicsCanvas.rampAt(ramp, 0)));
        assertEquals(hex(BLUE), hex(GraphicsCanvas.rampAt(ramp, 1)));
    }

    @Test
    void theMiddleOfTwoStopsIsHalfway() {
        assertEquals(hex(0xff800080), hex(GraphicsCanvas.rampAt(new int[] {RED, BLUE}, 0.5)));
    }

    /// The case the old code could not express at all: a middle stop is a colour the
    /// ramp must actually pass through.
    @Test
    void aThirdStopIsNotSkipped() {
        int[] ramp = {RED, GREEN, BLUE};
        assertEquals(hex(GREEN), hex(GraphicsCanvas.rampAt(ramp, 0.5)));
        assertEquals(hex(0xff808000), hex(GraphicsCanvas.rampAt(ramp, 0.25)));
        assertEquals(hex(0xff008080), hex(GraphicsCanvas.rampAt(ramp, 0.75)));
    }

    @Test
    void alphaInterpolatesToo() {
        int[] ramp = {0x00ff0000, 0xffff0000};
        assertEquals(hex(0x80ff0000), hex(GraphicsCanvas.rampAt(ramp, 0.5)));
    }

    @Test
    void aSingleStopIsThatColourEverywhere() {
        int[] ramp = {RED};
        assertEquals(hex(RED), hex(GraphicsCanvas.rampAt(ramp, 0)));
        assertEquals(hex(RED), hex(GraphicsCanvas.rampAt(ramp, 0.37)));
        assertEquals(hex(RED), hex(GraphicsCanvas.rampAt(ramp, 1)));
    }

    /// Out-of-range values are clamped rather than wrapping or throwing: the band loop
    /// samples at band midpoints, and rounding must never walk off the end of the array.
    @Test
    void outOfRangeClamps() {
        int[] ramp = {RED, BLUE};
        assertEquals(hex(RED), hex(GraphicsCanvas.rampAt(ramp, -0.5)));
        assertEquals(hex(BLUE), hex(GraphicsCanvas.rampAt(ramp, 1.5)));
    }
}
