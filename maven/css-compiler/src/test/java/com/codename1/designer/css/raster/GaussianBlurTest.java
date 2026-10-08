/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.designer.css.raster;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/// The gaussian blur: conservation, symmetry and width, on both the exact
/// kernel and the box approximation used for large blurs.
class GaussianBlurTest {
    private static double sum(float[] d) {
        double s = 0;
        for (float v : d) {
            s += v;
        }
        return s;
    }

    private static float[] impulse(int size) {
        float[] d = new float[size * size];
        d[(size / 2) * size + size / 2] = 1f;
        return d;
    }

    /// The variance of the plane along x around the middle column.
    private static double varianceX(float[] d, int size) {
        double v = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - size / 2;
                v += d[y * size + x] * dx * dx;
            }
        }
        return v / sum(d);
    }

    @Test
    void kernelIsNormalisedSymmetricAndPeaksInTheMiddle() {
        float[] k = GaussianBlur.kernel(2.5);
        assertEquals(2 * 8 + 1, k.length, "truncated at three standard deviations");
        double s = 0;
        for (int i = 0; i < k.length; i++) {
            s += k[i];
            assertEquals(k[i], k[k.length - 1 - i], 0f);
            if (i > 0 && i <= k.length / 2) {
                assertTrue(k[i] > k[i - 1], "weights rise towards the middle");
            }
        }
        assertEquals(1, s, 1e-6);
    }

    @Test
    void zeroSigmaLeavesThePlaneAlone() {
        float[] d = impulse(9);
        float[] copy = d.clone();
        GaussianBlur.blur(d, 9, 9, 0);
        assertArrayEquals(copy, d);
        GaussianBlur.blur(d, 9, 9, -3);
        assertArrayEquals(copy, d);
        assertEquals(0, GaussianBlur.radius(0));
        assertEquals(0, GaussianBlur.radius(-1));
        assertEquals(6, GaussianBlur.radius(2));
        assertEquals(8, GaussianBlur.radius(2.5));
    }

    @Test
    void massIsConservedAwayFromTheEdges() {
        int size = 101;
        float[] d = new float[size * size];
        for (int y = 45; y < 55; y++) {
            for (int x = 45; x < 55; x++) {
                d[y * size + x] = 1f;
            }
        }
        GaussianBlur.blur(d, size, size, 3);
        assertEquals(100, sum(d), 0.01);
        for (float v : d) {
            assertTrue(v >= 0f && v <= 1.0001f, "a blur never leaves the input range: " + v);
        }
    }

    @Test
    void blurIsSymmetric() {
        int size = 101;
        float[] d = new float[size * size];
        // A block centred on 49.5, 49.5.
        for (int y = 45; y < 55; y++) {
            for (int x = 45; x < 55; x++) {
                d[y * size + x] = 1f;
            }
        }
        GaussianBlur.blur(d, size, size, 4);
        for (int k = 0; k < 30; k++) {
            float left = d[50 * size + (44 - k)];
            float right = d[50 * size + (55 + k)];
            float up = d[(44 - k) * size + 50];
            float down = d[(55 + k) * size + 50];
            assertEquals(left, right, 1e-6f, "left/right at distance " + k);
            assertEquals(up, down, 1e-6f, "up/down at distance " + k);
            assertEquals(left, up, 1e-6f, "the kernel is the same on both axes, distance " + k);
            if (k > 0 && left > 1e-4f) {
                assertTrue(left < d[50 * size + (44 - k + 1)], "values fall off with distance");
            }
        }
    }

    @Test
    void widerBlurHasALowerPeakAndTheRightVariance() {
        int size = 81;
        float[] narrow = impulse(size);
        float[] wide = impulse(size);
        GaussianBlur.blur(narrow, size, size, 2);
        GaussianBlur.blur(wide, size, size, 5);
        int mid = (size / 2) * size + size / 2;
        assertTrue(narrow[mid] > wide[mid] * 4, "sigma 2 peak " + narrow[mid] + " vs sigma 5 peak " + wide[mid]);
        // The peak of a 2D gaussian is 1 / (2 pi sigma^2).
        assertEquals(1 / (2 * Math.PI * 4), narrow[mid], 0.004);
        assertEquals(1 / (2 * Math.PI * 25), wide[mid], 0.0007);
        assertEquals(1, sum(narrow), 1e-4);
        assertEquals(1, sum(wide), 1e-4);
        // Truncating at 3 sigma loses a little of the tails.
        assertEquals(4, varianceX(narrow, size), 0.25);
        assertEquals(25, varianceX(wide, size), 1.5);
        // The value one sigma out is exp(-1/2) of the peak.
        assertEquals(Math.exp(-0.5), wide[mid + 5] / wide[mid], 0.01);
    }

    @Test
    void largeSigmaUsesBoxesThatMatchTheGaussian() {
        double sigma = 20;
        assertTrue(sigma > GaussianBlur.EXACT_SIGMA_LIMIT);
        int[] sizes = GaussianBlur.boxSizes(sigma);
        assertEquals(3, sizes.length);
        double variance = 0;
        int reach = 0;
        for (int s : sizes) {
            assertEquals(1, s % 2, "box widths are odd");
            variance += (s * (double) s - 1) / 12;
            reach += (s - 1) / 2;
        }
        // Odd widths cannot hit the variance exactly; 39, 39, 41 is 393.3.
        assertEquals(sigma * sigma, variance, 8, "three boxes add up to the gaussian's variance");
        assertEquals(reach, GaussianBlur.radius(sigma));

        int size = 2 * reach + 41;
        float[] d = impulse(size);
        GaussianBlur.blur(d, size, size, sigma);
        assertEquals(1, sum(d), 1e-3, "mass conserved");
        assertEquals(variance, varianceX(d, size), 1);
        int mid = (size / 2) * size + size / 2;
        assertEquals(1 / (2 * Math.PI * sigma * sigma), d[mid], 0.00006);
        for (int k = 1; k < reach; k += 3) {
            assertEquals(d[mid - k], d[mid + k], 1e-7f, "symmetric in x at " + k);
            assertEquals(d[mid - k * size], d[mid + k * size], 1e-7f, "symmetric in y at " + k);
            assertEquals(d[mid - k], d[mid - k * size], 1e-7f, "same on both axes at " + k);
        }
        // Nothing travels further than radius() says.
        // (The running sums leave rounding residue, so "nothing" is not
        // bit-exact zero.)
        assertTrue(Math.abs(d[mid + reach + 1]) < 1e-10f, "beyond the reach: " + d[mid + reach + 1]);
        assertTrue(d[mid + reach] > 1e-8f, "at the reach: " + d[mid + reach]);
    }

    @Test
    void outsideThePlaneCountsAsTransparent() {
        int size = 31;
        float[] d = new float[size * size];
        java.util.Arrays.fill(d, 1f);
        GaussianBlur.blur(d, size, size, 2);
        assertEquals(1, d[15 * size + 15], 1e-4, "the middle is too far from an edge to notice it");
        // An edge pixel sees its own weight plus one of the two tails.
        float[] k = GaussianBlur.kernel(2);
        double edge = 0.5 + k[k.length / 2] / 2;
        assertEquals(edge, d[15 * size], 1e-4, "an edge pixel sees half its kernel");
        assertEquals(edge * edge, d[0], 1e-4, "a corner pixel sees half of it on both axes");
        assertTrue(d[0] < 0.4, "well short of the full value");
    }

    @Test
    void imageBlurIsPremultiplied() {
        // Left half: fully transparent but with green in its RGB bits.
        // Right half: opaque red.
        BufferedImage src = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 40; x++) {
                src.setRGB(x, y, x < 20 ? 0x0000ff00 : 0xffff0000);
            }
        }
        BufferedImage out = GaussianBlur.blur(src, 3);
        assertEquals(BufferedImage.TYPE_INT_ARGB, out.getType());
        assertEquals(40, out.getWidth());
        assertEquals(20, out.getHeight());
        boolean sawPartial = false;
        for (int x = 0; x < 40; x++) {
            int p = out.getRGB(x, 10);
            int a = p >>> 24;
            if (a > 0) {
                assertEquals(0, (p >> 8) & 0xff, "no green may leak from transparent pixels, x = " + x);
                assertEquals(255, (p >> 16) & 0xff, "the colour stays pure red, x = " + x);
            }
            sawPartial |= a > 20 && a < 235;
        }
        assertTrue(sawPartial, "the edge is blurred");
        int a19 = out.getRGB(19, 10) >>> 24;
        int a20 = out.getRGB(20, 10) >>> 24;
        assertEquals(255, a19 + a20, 2, "the two pixels next to the edge mirror each other");
        assertEquals(0, src.getRGB(0, 0) >>> 24, "the source image is not modified");
    }
}
