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
package com.codename1.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic checks of the iOS-26 liquid-glass switch-thumb droplet model at
 * fixed progress values -- the numeric ground truth for the fidelity
 * animation-frame captures, mirroring TabSelectionMorphTest for the tab lens.
 */
class SwitchThumbDropletTest {

    private static final int W = 100;
    private static final int H = 100;

    private static SwitchThumbDroplet at(float p) {
        SwitchThumbDroplet.Tokens tk = new SwitchThumbDroplet.Tokens();
        tk.stretch = 0.38f;   // switchLiquidStretchPct: 38 (the shipped iOS Modern value)
        tk.squash = 0.50f;    // switchLiquidSquashPct: 50
        return SwitchThumbDroplet.compute(p, W, H, tk);
    }

    @Test
    void restingEndsAreRound() {
        for (float p : new float[]{0f, 1f}) {
            SwitchThumbDroplet d = at(p);
            assertEquals(W, d.drawW, "p=" + p + " width");
            assertEquals(H, d.drawH, "p=" + p + " height");
            assertEquals(0, d.offsetX, "p=" + p + " offsetX");
            assertEquals(0, d.offsetY, "p=" + p + " offsetY");
        }
    }

    /**
     * The pinned frame table at the fidelity probe's fixed progress points.
     * env = sin(p*pi): 0.7071 at p=0.25/0.75, 1 at p=0.5.
     */
    @Test
    void frameTableAtFixedProgressPoints() {
        // {p, drawW, drawH, offsetX, offsetY}
        int[][] frames = {
            {0, 100, 100, 0, 0},
            {25, 127, 87, -13, 6},    // st = .7071*.38 = .2687
            {50, 138, 81, -19, 9},    // st = .38 (peak)
            {75, 127, 87, -13, 6},
            {100, 100, 100, 0, 0},
        };
        for (int[] f : frames) {
            SwitchThumbDroplet d = at(f[0] / 100f);
            String at = "p=" + f[0] + "% ";
            assertEquals(f[1], d.drawW, at + "drawW");
            assertEquals(f[2], d.drawH, at + "drawH");
            assertEquals(f[3], d.offsetX, at + "offsetX");
            assertEquals(f[4], d.offsetY, at + "offsetY");
        }
    }

    @Test
    void deformationIsSymmetricAroundMidSlide() {
        SwitchThumbDroplet a = at(0.3f);
        SwitchThumbDroplet b = at(0.7f);
        assertEquals(a.drawW, b.drawW);
        assertEquals(a.drawH, b.drawH);
    }

    @Test
    void peakStretchIsMidSlideAndBounded() {
        int peak = at(0.5f).drawW;
        assertTrue(peak > at(0.25f).drawW, "mid-slide is the widest frame");
        assertEquals(Math.round(W * 1.38f), peak, "peak width = 1 + stretch");
        assertTrue(at(0.5f).drawH < H, "peak squash flattens the thumb");
    }

    @Test
    void progressIsClamped() {
        SwitchThumbDroplet d = at(1.7f);
        assertEquals(W, d.drawW);
        assertEquals(H, d.drawH);
    }
}
