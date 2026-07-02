/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

/**
 * Deterministic checks of the iOS-26 tab selection morph model at fixed progress values.
 * This is the model the fidelity animation-frame probe asserts against, so the motion
 * (position path, lens size/params, bar-grow timing) is validated numerically rather than
 * only by start/end screenshots.
 */
class TabSelectionMorphTest {

    // A cell that travels from x=0..w=100 to x=200..w=100, bar inner-x 10, pill y=50 h=40.
    private static final int FROM_X = 0, FROM_W = 100, TO_X = 200, TO_W = 100;
    private static final int INNER_X = 10, CAP_Y = 50, CAP_H = 40;
    private static final int BAR_LEFT = 10, BAR_RIGHT = 310;

    private static TabSelectionMorph.Tokens tokens() {
        TabSelectionMorph.Tokens tk = new TabSelectionMorph.Tokens();
        tk.stretch = 0.32f;
        tk.squashW = 0.18f;
        tk.grow = 0.18f;
        tk.squashH = 0.24f;
        tk.liftPx = 5;
        tk.bubbleWidthPct = 80;
        tk.overflowPct = 20;
        tk.downBiasPx = 0;
        tk.restMag = 1.08f;
        tk.peakMag = 1.18f;
        tk.peakAb = 0.02f;
        tk.tintStrength = 1.0f;
        tk.barGrowPct = 3;
        return tk;
    }

    private static TabSelectionMorph at(float t) {
        return TabSelectionMorph.compute(t, FROM_X, FROM_W, TO_X, TO_W,
                INNER_X, CAP_Y, CAP_H, BAR_LEFT, BAR_RIGHT, tokens());
    }

    @Test
    void settledStart_isCalmPillAtSourceCell() {
        TabSelectionMorph m = at(0f);
        // bubbleW = 100 * 80% = 80; centred: capX = innerX + 0 + (100-80)/2
        assertEquals(20, m.capX, "start pill x");
        assertEquals(80, m.capW, "start pill w");
        assertEquals(CAP_Y, m.capY);
        assertEquals(CAP_H, m.capH);
        assertEquals(0f, m.flight, 1e-4, "no flight at the ends");
        assertEquals(1.08f, m.magnify, 1e-4, "settled magnify == restMag");
        assertEquals(0f, m.aberration, 1e-4);
        assertEquals(48, m.lensH, "lensH == base (capH + overflow) at rest"); // 40 + 40*20%
        assertFalse(m.barGrow, "no bar-grow at t=0");
    }

    @Test
    void settledEnd_isCalmPillAtTargetCell() {
        TabSelectionMorph m = at(1f);
        // springEase(1)=1 -> x=200; centred bubble: 10 + 200 + (100-80)/2
        assertEquals(220, m.capX, "end pill x");
        assertEquals(80, m.capW);
        assertEquals(0f, m.flight, 1e-4);
        assertEquals(1.08f, m.magnify, 1e-4);
        assertEquals(48, m.lensH);
        assertFalse(m.barGrow, "no bar-grow at t=1");
    }

    @Test
    void midTravel_liftsMagnifyAndGrowsTaller() {
        TabSelectionMorph m = at(0.5f);
        assertEquals(1f, m.flight, 1e-3, "flight peaks across the middle of travel");
        assertEquals(1.18f, m.magnify, 1e-4, "magnify reaches peak mid-flight");
        assertTrue(m.lensH > 48, "drop is taller than its resting height mid-flight");
        assertTrue(m.capX > 20 && m.capX < 220, "pill is between source and target");
    }

    @Test
    void positionProgressesMonotonically() {
        int prev = at(0f).capX;
        for (float t = 0.1f; t <= 1.0f; t += 0.1f) {
            int x = at(t).capX;
            assertTrue(x >= prev - 1, "capX should not travel backwards (t=" + t + ")");
            prev = x;
        }
    }

    @Test
    void barGrowFiresEarlyThenStops() {
        assertTrue(at(0.15f).barGrow, "whole-bar grow is active during the early pulse");
        assertEquals(1.03f, at(0.15f).barGrowMag, 1e-3, "growMag = 1 + grow*barGrowPct/100");
        assertFalse(at(0.6f).barGrow, "bar-grow is gone by mid-travel");
    }

    /**
     * The full deterministic frame table at the review's fixed progress points
     * (0, 10, 25, 50, 75, 90, 100): pill rect, lens rect, magnify, aberration,
     * flight envelope and the bar-grow flag are pinned EXACTLY per frame. This is
     * the numeric ground truth the fidelity animation-frame captures validate
     * against -- any change to the motion path, overshoot (see t=0.90: the pill
     * passes the 220px target to 230px before settling), lens size or tint
     * timing shows up here as a frame-value diff, not as a vague "feels wrong".
     */
    @Test
    void frameTableAtFixedProgressPoints() {
        // {t, capX, capW, lensX, lensY, lensW, lensH, magnify, aberration, flight, barGrow}
        Object[][] frames = {
            {0.00f, 20, 80, 20, 46, 80, 48, 1.0800f, 0.0000f, 0.0000f, false},
            {0.10f, 32, 105, 32, 38, 105, 56, 1.1726f, 0.0185f, 0.9259f, true},
            {0.25f, 71, 105, 71, 37, 105, 56, 1.1800f, 0.0200f, 1.0000f, true},
            {0.50f, 164, 105, 164, 37, 105, 56, 1.1800f, 0.0200f, 1.0000f, false},
            {0.75f, 220, 90, 220, 38, 90, 61, 1.1300f, 0.0100f, 0.5000f, false},
            {0.90f, 230, 98, 230, 45, 98, 50, 1.0800f, 0.0000f, 0.0000f, false},
            {1.00f, 220, 80, 220, 46, 80, 48, 1.0800f, 0.0000f, 0.0000f, false},
        };
        for (Object[] f : frames) {
            float t = (Float) f[0];
            TabSelectionMorph m = at(t);
            String at = "t=" + t + " ";
            assertEquals(((Integer) f[1]).intValue(), m.capX, at + "capX");
            assertEquals(((Integer) f[2]).intValue(), m.capW, at + "capW");
            assertEquals(((Integer) f[3]).intValue(), m.lensX, at + "lensX");
            assertEquals(((Integer) f[4]).intValue(), m.lensY, at + "lensY");
            assertEquals(((Integer) f[5]).intValue(), m.lensW, at + "lensW");
            assertEquals(((Integer) f[6]).intValue(), m.lensH, at + "lensH");
            assertEquals(((Float) f[7]).floatValue(), m.magnify, 5e-5, at + "magnify");
            assertEquals(((Float) f[8]).floatValue(), m.aberration, 5e-5, at + "aberration");
            assertEquals(((Float) f[9]).floatValue(), m.flight, 5e-5, at + "flight");
            assertEquals(((Boolean) f[10]).booleanValue(), m.barGrow, at + "barGrow");
        }
    }

    @Test
    void isDeterministic() {
        TabSelectionMorph a = at(0.37f);
        TabSelectionMorph b = at(0.37f);
        assertEquals(a.capX, b.capX);
        assertEquals(a.lensW, b.lensW);
        assertEquals(a.lensH, b.lensH);
        assertEquals(a.magnify, b.magnify, 0f);
        assertEquals(a.aberration, b.aberration, 0f);
    }
}
