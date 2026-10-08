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

import java.awt.image.BufferedImage;

/// A separable gaussian blur over float planes.
///
/// `java.awt.image.ConvolveOp` is deliberately not used: it either leaves
/// the edge pixels untouched or zero-fills them, and it convolves the
/// non-premultiplied channels of an ARGB image, which bleeds the colour of
/// fully transparent pixels into the result.
///
/// Everything outside the plane counts as zero (transparent). A caller that
/// needs content beyond its visible area, as a shadow does, adds a margin of
/// [#radius(double)] around it and crops afterwards.
///
/// Up to a standard deviation of [#EXACT_SIGMA_LIMIT] the kernel is the
/// sampled gaussian truncated at three standard deviations and normalised.
/// Above it the blur is three successive box blurs whose widths are chosen
/// to match the variance, which is visually indistinguishable at that size
/// and costs the same for any radius.
public final class GaussianBlur {
    /// The largest standard deviation blurred with the exact kernel.
    public static final double EXACT_SIGMA_LIMIT = 16;

    private GaussianBlur() {
    }

    /// How far, in pixels, the blur for `sigma` carries a value: the number
    /// of pixels on each side of a plane that can influence, or be
    /// influenced by, a given pixel.
    public static int radius(double sigma) {
        if (!(sigma > 0)) {
            return 0;
        }
        if (sigma <= EXACT_SIGMA_LIMIT) {
            return (int) Math.ceil(sigma * 3);
        }
        int r = 0;
        for (int size : boxSizes(sigma)) {
            r += (size - 1) / 2;
        }
        return r;
    }

    /// The normalised gaussian weights for `sigma`, `2 * radius + 1` of
    /// them, symmetric around the middle.
    public static float[] kernel(double sigma) {
        int r = (int) Math.ceil(sigma * 3);
        double[] k = new double[2 * r + 1];
        double sum = 0;
        for (int i = -r; i <= r; i++) {
            double v = Math.exp(-(i * (double) i) / (2 * sigma * sigma));
            k[i + r] = v;
            sum += v;
        }
        float[] out = new float[k.length];
        for (int i = 0; i < k.length; i++) {
            out[i] = (float) (k[i] / sum);
        }
        return out;
    }

    /// The widths of the three box filters that approximate a gaussian of
    /// the given standard deviation. All are odd.
    static int[] boxSizes(double sigma) {
        int n = 3;
        double ideal = Math.sqrt(12 * sigma * sigma / n + 1);
        int lower = (int) Math.floor(ideal);
        if (lower % 2 == 0) {
            lower--;
        }
        int upper = lower + 2;
        double m = (12 * sigma * sigma - n * lower * (double) lower - 4.0 * n * lower - 3.0 * n)
                / (-4.0 * lower - 4);
        int count = (int) Math.round(m);
        int[] sizes = new int[n];
        for (int i = 0; i < n; i++) {
            sizes[i] = i < count ? lower : upper;
        }
        return sizes;
    }

    /// Blurs one plane in place.
    ///
    /// #### Parameters
    ///
    /// - `data`: `w * h` values, row by row
    ///
    /// - `w`: plane width
    ///
    /// - `h`: plane height
    ///
    /// - `sigma`: the standard deviation in pixels; zero or less is a no-op
    public static void blur(float[] data, int w, int h, double sigma) {
        if (!(sigma > 0) || w <= 0 || h <= 0) {
            return;
        }
        float[] line = new float[Math.max(w, h)];
        if (sigma <= EXACT_SIGMA_LIMIT) {
            float[] k = kernel(sigma);
            for (int y = 0; y < h; y++) {
                exactLine(data, line, y * w, 1, w, k);
            }
            for (int x = 0; x < w; x++) {
                exactLine(data, line, x, w, h, k);
            }
        } else {
            int[] sizes = boxSizes(sigma);
            for (int size : sizes) {
                int r = (size - 1) / 2;
                for (int y = 0; y < h; y++) {
                    boxLine(data, line, y * w, 1, w, r);
                }
            }
            for (int size : sizes) {
                int r = (size - 1) / 2;
                for (int x = 0; x < w; x++) {
                    boxLine(data, line, x, w, h, r);
                }
            }
        }
    }

    private static void exactLine(float[] data, float[] line, int off, int stride, int len, float[] k) {
        int r = (k.length - 1) / 2;
        int first = -1;
        int last = -1;
        for (int i = 0; i < len; i++) {
            float v = data[off + i * stride];
            line[i] = v;
            if (v != 0f) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        if (first < 0) {
            return;
        }
        int from = Math.max(0, first - r);
        int to = Math.min(len - 1, last + r);
        for (int i = from; i <= to; i++) {
            int lo = Math.max(first, i - r);
            int hi = Math.min(last, i + r);
            float sum = 0f;
            for (int j = lo; j <= hi; j++) {
                sum += line[j] * k[j - i + r];
            }
            data[off + i * stride] = sum;
        }
    }

    private static void boxLine(float[] data, float[] line, int off, int stride, int len, int r) {
        boolean any = false;
        for (int i = 0; i < len; i++) {
            float v = data[off + i * stride];
            line[i] = v;
            any |= v != 0f;
        }
        if (!any || r <= 0) {
            return;
        }
        float inv = 1f / (2 * r + 1);
        double sum = 0;
        for (int j = 0; j <= Math.min(r, len - 1); j++) {
            sum += line[j];
        }
        for (int i = 0; i < len; i++) {
            data[off + i * stride] = (float) (sum * inv);
            int add = i + r + 1;
            int drop = i - r;
            if (add < len) {
                sum += line[add];
            }
            if (drop >= 0) {
                sum -= line[drop];
            }
        }
    }

    /// Blurs an image and returns the result as a new `TYPE_INT_ARGB` image
    /// of the same size. The channels are blurred premultiplied by alpha, so
    /// a transparent pixel contributes no colour whatever its RGB bits are.
    public static BufferedImage blur(BufferedImage src, double sigma) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        float[] a = new float[px.length];
        float[] r = new float[px.length];
        float[] g = new float[px.length];
        float[] b = new float[px.length];
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            float al = (p >>> 24) / 255f;
            a[i] = al;
            r[i] = ((p >> 16) & 0xff) * al;
            g[i] = ((p >> 8) & 0xff) * al;
            b[i] = (p & 0xff) * al;
        }
        blur(a, w, h, sigma);
        blur(r, w, h, sigma);
        blur(g, w, h, sigma);
        blur(b, w, h, sigma);
        for (int i = 0; i < px.length; i++) {
            float al = a[i];
            if (al <= 0f) {
                px[i] = 0;
            } else {
                px[i] = (Pixels.clamp8(al * 255f) << 24)
                        | (Pixels.clamp8(r[i] / al) << 16)
                        | (Pixels.clamp8(g[i] / al) << 8)
                        | Pixels.clamp8(b[i] / al);
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }
}
