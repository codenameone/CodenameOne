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
package com.codename1.ui.plaf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The margin/padding parse has two implementations: an allocation-free fast
/// path and the original, kept as a fallback. They must agree on every input
/// the fast path accepts, or themes would render differently depending on a
/// detail of how a number was written.
public class ToFloatArrayFastTest {

    private void agrees(String s) {
        float[] fast = UIManager.toFloatArrayFast(s);
        assertNotNull(fast, "fast path declined a value it should parse: " + s);
        assertArrayEquals(UIManager.toFloatArrayFallback(s), fast, 0.0f, "mismatch for " + s);
    }

    private void declines(String s) {
        assertNull(UIManager.toFloatArrayFast(s), "fast path should have declined: " + s);
    }

    @Test
    public void agreesOnTheShapesThemesActuallyUse() {
        agrees("0,0,0,0");
        agrees("1,2,3,4");
        agrees("4,4,4,4");
        agrees("0,0,15,15");
        agrees("1.5,2,1.5,2");
        agrees("0.5,0.25,0.125,0.0625");
        agrees("10,20,30,40");
        agrees("100,200,300,400");
        agrees("-1,-2,-3,-4");
        agrees("+1,2,3,4");
        agrees("0.0,0.0,0.0,0.0");
        agrees("99999999,1,1,1");
    }

    /// Values written with a trailing or leading dot, an exponent, whitespace
    /// or anything else unusual must fall through rather than be guessed at.
    @Test
    public void declinesWhatItDoesNotUnderstand() {
        declines(null);
        declines("");
        declines("1,2,3");            // too few fields
        declines("1,2,3,4,5");        // too many
        declines("1e2,2,3,4");        // exponent
        declines(" 1,2,3,4");         // leading space
        declines("1 ,2,3,4");         // trailing space
        declines("1,,3,4");           // empty field
        declines("a,2,3,4");
        declines("1.2.3,2,3,4");
        declines("-,2,3,4");
        declines("999999999999,2,3,4");   // too large for the fast path
        declines("1,2,3,4,");             // trailing comma
    }

    /// Whatever the fast path declines must still parse the old way, so
    /// declining is never a behaviour change.
    @Test
    public void fallbackStillHandlesDeclinedButValidInput() {
        assertArrayEquals(new float[]{100f, 2f, 3f, 4f},
                UIManager.toFloatArrayFallback("1e2,2,3,4"), 0.0f);
        assertArrayEquals(new float[]{1f, 2f, 3f, 4f},
                UIManager.toFloatArrayFallback(" 1,2,3,4"), 0.0f);
    }

    /// The fast path must not be vacuous: if it declined everything the test
    /// above would still pass while the optimisation did nothing.
    @Test
    public void fastPathActuallyFires() {
        int accepted = 0;
        String[] realThemeValues = {
            "0,0,0,0", "1,1,1,1", "2,2,2,2", "4,4,4,4", "0,0,15,15",
            "3,3,3,3", "5,5,5,5", "1.5,1.5,1.5,1.5", "0,0,0,2", "8,8,8,8"
        };
        for (String v : realThemeValues) {
            if (UIManager.toFloatArrayFast(v) != null) {
                accepted++;
            }
        }
        assertEquals(realThemeValues.length, accepted,
                "fast path must accept every ordinary theme metric");
    }
}
