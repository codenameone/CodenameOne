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

    @Test
    void visibleFramePacingPreventsAOneTickJump() {
        assertEquals(8, TabSelectionMorph.paceVisibleFrame(0, 8),
                "normal 60fps progress remains unchanged");
        assertEquals(14, TabSelectionMorph.paceVisibleFrame(0, 100),
                "a completed wall-clock motion still presents an intermediate frame");
        int visible = 0;
        int frames = 0;
        while (visible < 100) {
            visible = TabSelectionMorph.paceVisibleFrame(visible, 100);
            frames++;
        }
        assertEquals(8, frames, "a coalesced transition still paints a short frame sequence");
        assertEquals(100, visible);
    }

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
        assertEquals(CAP_H, m.lensH, "no overflow at rest -- the bulge is a flight effect");
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
        assertEquals(CAP_H, m.lensH, "no overflow at rest -- the bulge is a flight effect");
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
     * against -- any change to the measured monotonic motion path, lens size or tint
     * timing shows up here as a frame-value diff, not as a vague "feels wrong".
     */
    @Test
    void frameTableAtFixedProgressPoints() {
        // {t, capX, capW, lensX, lensY, lensW, lensH, magnify, aberration, flight, barGrow}
        Object[][] frames = {
            {0.00f, 20, 80, 20, 50, 80, 40, 1.0800f, 0.0000f, 0.0000f, false},
            {0.10f, 42, 105, 42, 39, 105, 54, 1.1726f, 0.0185f, 0.9259f, true},
            {0.25f, 101, 105, 101, 37, 105, 56, 1.1800f, 0.0200f, 1.0000f, true},
            {0.50f, 178, 105, 178, 37, 105, 56, 1.1800f, 0.0200f, 1.0000f, false},
            {0.75f, 213, 90, 213, 41, 90, 55, 1.1300f, 0.0100f, 0.5000f, false},
            {0.90f, 221, 89, 221, 49, 89, 42, 1.0800f, 0.0000f, 0.0000f, false},
            {1.00f, 220, 80, 220, 50, 80, 40, 1.0800f, 0.0000f, 0.0000f, false},
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

    /**
     * The theme controls the morph through a named preset plus two high-level
     * scalars (lens intensity, springiness) -- the "ios26" preset IS the
     * shipped iOS Modern tuning, pinned here so a preset edit is a visible,
     * reviewed change rather than a drive-by constant tweak.
     */
    @Test
    void presetIos26MatchesShippedTuning() {
        TabSelectionMorph.Tokens tk = TabSelectionMorph.Tokens.preset("ios26");
        assertEquals(0f, tk.stretch, 1e-6);
        assertEquals(0.05f, tk.squashW, 1e-6);
        assertEquals(0.01f, tk.grow, 1e-6);
        assertEquals(0.04f, tk.squashH, 1e-6);
        assertEquals(0.05f, tk.liftMm, 1e-6);
        assertEquals(100, tk.bubbleWidthPct);
        assertEquals(0, tk.overflowPct);
        assertEquals(0f, tk.downBiasMm, 1e-6);
        assertEquals(1.02f, tk.restMag, 1e-6);
        assertEquals(1.08f, tk.peakMag, 1e-6);
        assertEquals(0.0005f, tk.peakAb, 1e-6);
        assertEquals(1f, tk.tintStrength, 1e-6);
        assertEquals(0, tk.barGrowPct);
        assertEquals(0.35f, tk.spring, 1e-6, "native settle deformation is restrained");
        // unknown names fall back to ios26, "subtle" is a calmer variant
        assertEquals(tk.stretch, TabSelectionMorph.Tokens.preset("nonsense").stretch, 1e-6);
        assertEquals(0f, TabSelectionMorph.Tokens.preset("subtle").stretch, 1e-6,
                "both native presets keep the travelling drop to one cell");
    }

    @Test
    void lensIntensityScalesOpticsNotGeometry() {
        TabSelectionMorph.Tokens tk = TabSelectionMorph.Tokens.preset("ios26");
        tk.scaleLensIntensity(0f);
        assertEquals(1f, tk.restMag, 1e-6, "intensity 0 = optically flat drop");
        assertEquals(1f, tk.peakMag, 1e-6);
        assertEquals(0f, tk.peakAb, 1e-6);
        assertEquals(0f, tk.tintStrength, 1e-6);
        assertEquals(0f, tk.stretch, 1e-6, "geometry untouched by lens intensity");

        TabSelectionMorph.Tokens twice = TabSelectionMorph.Tokens.preset("ios26");
        twice.scaleLensIntensity(2f);
        assertEquals(1.16f, twice.peakMag, 1e-5, "intensity 2 doubles the magnify delta");
        assertEquals(1f, twice.tintStrength, 1e-6, "tint strength clamps at 1");
    }

    @Test
    void ios26PresetStaysWithinRecordedNativeDeformationBounds() {
        TabSelectionMorph.Tokens tk = TabSelectionMorph.Tokens.preset("ios26");
        tk.liftPx = 1;
        tk.downBiasPx = 0;
        int maxWidth = 0;
        int maxHeight = 0;
        float maxAberration = 0f;
        for (int frame = 0; frame <= 100; frame++) {
            TabSelectionMorph m = TabSelectionMorph.compute(frame / 100f,
                    FROM_X, FROM_W, TO_X, TO_W, INNER_X, CAP_Y, CAP_H,
                    BAR_LEFT, BAR_RIGHT, tk);
            maxWidth = Math.max(maxWidth, m.lensW);
            maxHeight = Math.max(maxHeight, m.lensH);
            maxAberration = Math.max(maxAberration, m.aberration);
        }
        assertTrue(maxWidth <= 100,
                "native drop stays at one cell; it must not engulf two tab glyphs");
        assertTrue(maxHeight <= 44,
                "native drop only subtly bulges beyond the 40px bar");
        assertTrue(maxAberration <= 0.00051f,
                "native chromatic separation is a rim detail, not a visible double image");
    }

    @Test
    void ios26PresetFrameTableMatchesRecordedTuning() {
        TabSelectionMorph.Tokens tk = TabSelectionMorph.Tokens.preset("ios26");
        tk.liftPx = 1;
        tk.downBiasPx = 0;
        // {time, capX, capW, lensY, lensW, lensH, magnify, aberration, flight}
        Object[][] frames = {
            {0.00f, 10, 100, 50, 100, 40, 1.0200000f, 0.0000000f, 0.0000000f},
            {0.10f, 29, 100, 50, 100, 40, 1.0755556f, 0.0004630f, 0.9259260f},
            {0.25f, 88, 100, 49, 100, 40, 1.0800000f, 0.0005000f, 1.0000000f},
            {0.50f, 165, 100, 49, 100, 40, 1.0800000f, 0.0005000f, 1.0000000f},
            {0.75f, 202, 98, 50, 98, 40, 1.0500000f, 0.0002500f, 0.5000000f},
            {0.90f, 209, 99, 50, 99, 40, 1.0200000f, 0.0000000f, 0.0000000f},
            {1.00f, 210, 100, 50, 100, 40, 1.0200000f, 0.0000000f, 0.0000000f}
        };
        for (Object[] frame : frames) {
            float time = (Float) frame[0];
            TabSelectionMorph m = TabSelectionMorph.compute(time,
                    FROM_X, FROM_W, TO_X, TO_W, INNER_X, CAP_Y, CAP_H,
                    BAR_LEFT, BAR_RIGHT, tk);
            String at = "t=" + time + " ";
            assertEquals(((Integer) frame[1]).intValue(), m.capX, at + "capX");
            assertEquals(((Integer) frame[2]).intValue(), m.capW, at + "capW");
            assertEquals(((Integer) frame[3]).intValue(), m.lensY, at + "lensY");
            assertEquals(((Integer) frame[4]).intValue(), m.lensW, at + "lensW");
            assertEquals(((Integer) frame[5]).intValue(), m.lensH, at + "lensH");
            assertEquals((Float) frame[6], m.magnify, 5e-7f, at + "magnify");
            assertEquals((Float) frame[7], m.aberration, 5e-7f, at + "aberration");
            assertEquals((Float) frame[8], m.flight, 5e-7f, at + "flight");
        }
    }

    @Test
    void springinessControlsStopDeformationWithoutPositionOvershoot() {
        // Sample the stop envelope at three springiness values. The native
        // centre path remains monotonic; spring changes only the liquid
        // width-compression/height-swell at the stop.
        TabSelectionMorph.Tokens none = tokens();
        none.spring = 0f;
        TabSelectionMorph.Tokens strong = tokens();
        strong.spring = 2f;
        TabSelectionMorph flat = TabSelectionMorph.compute(0.8f, FROM_X, FROM_W, TO_X, TO_W,
                INNER_X, CAP_Y, CAP_H, BAR_LEFT, BAR_RIGHT, none);
        TabSelectionMorph reference = at(0.8f);
        TabSelectionMorph elastic = TabSelectionMorph.compute(0.8f, FROM_X, FROM_W, TO_X, TO_W,
                INNER_X, CAP_Y, CAP_H, BAR_LEFT, BAR_RIGHT, strong);
        assertTrue(flat.lensW > reference.lensW && reference.lensW > elastic.lensW,
                "stronger spring compresses the stop width further");
        assertTrue(flat.lensH < reference.lensH && reference.lensH < elastic.lensH,
                "stronger spring swells the stop height further");
        assertEquals(at(1f).capX, TabSelectionMorph.compute(1f, FROM_X, FROM_W, TO_X, TO_W,
                INNER_X, CAP_Y, CAP_H, BAR_LEFT, BAR_RIGHT, strong).capX,
                "every springiness settles on the same target without overshoot");
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

    /**
     * A drop wider than its cell (bubbleWidthPct > 100, the shipped ios26
     * geometry) travelling to an END cell must never
     * paint outside the bar: the native pill overlaps neighbour cells, not
     * the backdrop past the bar's rounded ends.
     */
    @Test
    void wideDropStaysInsideTheBar() {
        TabSelectionMorph.Tokens tk = tokens();
        tk.bubbleWidthPct = 120;
        // last cell: flush against the bar's right edge
        int lastX = BAR_RIGHT - INNER_X - 100;
        for (float t = 0f; t <= 1.0001f; t += 0.05f) {
            TabSelectionMorph m = TabSelectionMorph.compute(t, FROM_X, FROM_W,
                    lastX, 100, INNER_X, CAP_Y, CAP_H, BAR_LEFT, BAR_RIGHT, tk);
            assertTrue(m.capX >= BAR_LEFT, "capX " + m.capX + " left of bar at t=" + t);
            assertTrue(m.capX + m.capW <= BAR_RIGHT,
                    "cap right edge " + (m.capX + m.capW) + " past bar at t=" + t);
            assertTrue(m.lensX >= BAR_LEFT, "lensX " + m.lensX + " left of bar at t=" + t);
            assertTrue(m.lensX + m.lensW <= BAR_RIGHT,
                    "lens right edge " + (m.lensX + m.lensW) + " past bar at t=" + t);
        }
    }
}
