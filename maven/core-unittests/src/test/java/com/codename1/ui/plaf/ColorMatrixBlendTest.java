/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui.plaf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reference math of Graphics.colorMatrixRegion, which the JavaSE port runs and
 * the Metal shader cn1_fs_colormatrix mirrors.
 */
class ColorMatrixBlendTest {

    private static final float[] IDENTITY = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};

    private static int[] fill(int w, int h, int argb) {
        int[] p = new int[w * h];
        java.util.Arrays.fill(p, argb);
        return p;
    }

    @Test
    void identityMatrixLeavesPixelsAlone() {
        int[] p = fill(8, 4, 0xff336699);
        ColorMatrixBlend.apply(p, 8, 4, IDENTITY, null, 0, 0, 0, 1f);
        for (int v : p) {
            assertEquals(0xff336699, v);
        }
    }

    @Test
    void platterMatrixReproducesTheMeasuredGrey() {
        // The native dark platter turns the bar's 134 grey into 98.8.
        float[] dark = {1.1298f, -0.2361f, -0.0237f, -0.07f, -0.0701f, 0.964f, -0.0238f, -0.07f,
                -0.0702f, -0.236f, 1.1762f, -0.07f};
        int[] p = fill(4, 4, 0xff868686);
        ColorMatrixBlend.apply(p, 4, 4, dark, null, 0, 0, 0, 1f);
        assertEquals(99, (p[0] >> 16) & 0xff, 1);
        assertEquals(99, (p[0] >> 8) & 0xff, 1);
        assertEquals(99, p[0] & 0xff, 1);
    }

    @Test
    void amountMixesAndDestinationAlphaIsKept() {
        float[] black = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] p = fill(2, 2, 0x80c8c8c8);
        ColorMatrixBlend.apply(p, 2, 2, black, null, 0, 0, 0, 0.5f);
        assertEquals(0x80, (p[0] >>> 24) & 0xff);
        assertEquals(100, (p[0] >> 16) & 0xff);
    }

    @Test
    void capsuleLeavesTheCornersAndMaskZeroLeavesPixels() {
        float[] white = {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1};
        int w = 40;
        int h = 10;
        int[] p = fill(w, h, 0xff000000);
        ColorMatrixBlend.apply(p, w, h, white, null, 0, 0, -1, 1f);
        assertEquals(0xff000000, p[0], "outside the capsule's round end");
        assertEquals(0xffffffff, p[5 * w + 20], "inside the capsule");

        int[] q = fill(w, h, 0xff000000);
        int[] mask = new int[4];
        mask[1] = 0xff000000;   // only the top right quarter of a 2x2 mask
        ColorMatrixBlend.apply(q, w, h, white, mask, 2, 2, 0, 1f);
        assertEquals(0xff000000, q[0], "mask alpha 0 leaves the pixel");
        assertEquals(0xffffffff, q[w - 1], "mask alpha 255 applies fully");
        assertEquals(0xff000000, q[(h - 1) * w + w - 1]);
    }
}
