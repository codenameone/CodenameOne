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
package com.codename1.image;

/// Resampling of interleaved 8-bit band data, the two-pass fixed-point
/// convolution of Pillow's `Resample.c`.
///
/// The results match Pillow byte for byte, which is why the Python runtime's
/// `PIL.Image.resize` delegates here. Pixels are `bands` consecutive bytes
/// (`L`, `LA`, `RGB`, `RGBA`, ...), rows packed with no padding. Images with
/// alpha should be premultiplied first ([#premultiply(byte[], int)]) and
/// unpremultiplied afterwards, as Pillow does, so transparent pixels do not
/// bleed their color into their neighbours.
///
/// [RasterImage#scale(int, int, int)] wraps this for ARGB images.
@com.codename1.impl.SharedWithBackend
public final class Resampler {
    /// Nearest neighbour: each output pixel takes the input pixel under its center.
    public static final int NEAREST = 0;
    /// Lanczos, a windowed sinc with a support of three pixels.
    public static final int LANCZOS = 1;
    /// Linear interpolation (a triangle filter).
    public static final int BILINEAR = 2;
    /// Cubic interpolation, Pillow's `a = -0.5` kernel.
    public static final int BICUBIC = 3;
    /// Box filter: every input pixel under an output pixel weighs the same.
    public static final int BOX = 4;
    /// Hamming-windowed sinc with a support of one pixel.
    public static final int HAMMING = 5;

    private static final int PRECISION_BITS = 22;

    private Resampler() {
    }

    private static double support(int filter) {
        switch (filter) {
            case BOX:
                return 0.5;
            case BICUBIC:
                return 2.0;
            case LANCZOS:
                return 3.0;
            default:
                return 1.0;
        }
    }

    private static double sinc(double x) {
        if (x == 0.0) {
            return 1.0;
        }
        double px = x * Math.PI;
        return Math.sin(px) / px;
    }

    private static double kernel(int filter, double x) {
        double t = x < 0 ? -x : x;
        switch (filter) {
            case BOX:
                return x > -0.5 && x <= 0.5 ? 1.0 : 0.0;
            case HAMMING:
                if (t == 0.0) {
                    return 1.0;
                }
                if (t >= 1.0) {
                    return 0.0;
                }
                t = t * Math.PI;
                return Math.sin(t) / t * (0.54 + 0.46 * Math.cos(t));
            case BICUBIC:
                if (t < 1.0) {
                    return (1.5 * t - 2.5) * t * t + 1;
                }
                if (t < 2.0) {
                    return (((t - 5) * t + 8) * t - 4) * -0.5;
                }
                return 0.0;
            case LANCZOS:
                return x > -3.0 && x < 3.0 ? sinc(x) * sinc(x / 3.0) : 0.0;
            default:
                return t < 1.0 ? 1.0 - t : 0.0;
        }
    }

    private static int coeffs(int inSize, double in0, double in1, int outSize, int[] bounds, int[][] out,
                              int filter) {
        double scale = (in1 - in0) / outSize;
        double filterscale = scale < 1.0 ? 1.0 : scale;
        double support = support(filter) * filterscale;
        int ksize = (int) Math.ceil(support) * 2 + 1;
        int[] kk = new int[outSize * ksize];
        double[] k = new double[ksize];
        for (int xx = 0; xx < outSize; xx++) {
            double center = in0 + (xx + 0.5) * scale;
            double ww = 0.0;
            double ss = 1.0 / filterscale;
            int xmin = (int) (center - support + 0.5);
            if (xmin < 0) {
                xmin = 0;
            }
            int xmax = (int) (center + support + 0.5);
            if (xmax > inSize) {
                xmax = inSize;
            }
            xmax -= xmin;
            for (int x = 0; x < xmax; x++) {
                double w = kernel(filter, (x + xmin - center + 0.5) * ss);
                k[x] = w;
                ww += w;
            }
            for (int x = 0; x < xmax; x++) {
                double v = ww != 0.0 ? k[x] / ww : k[x];
                kk[xx * ksize + x] = v < 0 ? (int) (-0.5 + v * (1 << PRECISION_BITS))
                        : (int) (0.5 + v * (1 << PRECISION_BITS));
            }
            bounds[xx * 2] = xmin;
            bounds[xx * 2 + 1] = xmax;
        }
        out[0] = kk;
        return ksize;
    }

    private static byte clip8(int v) {
        int s = v >> PRECISION_BITS;
        return (byte) (s < 0 ? 0 : s > 255 ? 255 : s);
    }

    /// Resamples the region `box` (`x0, y0, x1, y1`, in source pixels, may be
    /// fractional) of a `w` x `h` image to `nw` x `nh`.
    ///
    /// #### Parameters
    ///
    /// - `src`: the source pixels, `w * h * bands` bytes
    /// - `w`: source width
    /// - `h`: source height
    /// - `bands`: bytes per pixel
    /// - `nw`: output width
    /// - `nh`: output height
    /// - `box`: the source region, four values
    /// - `filter`: one of the filter constants of this class
    ///
    /// #### Returns
    ///
    /// a new array of `nw * nh * bands` bytes
    public static byte[] resample(byte[] src, int w, int h, int bands, int nw, int nh, double[] box, int filter) {
        if (filter == NEAREST) {
            return nearest(src, w, h, bands, nw, nh, box);
        }
        byte[] cur = src;
        int cw = w;
        boolean horizontal = nw != w || box[0] != 0 || box[2] != nw;
        boolean vertical = nh != h || box[1] != 0 || box[3] != nh;
        if (horizontal) {
            int[] bounds = new int[nw * 2];
            int[][] kh = new int[1][];
            int ks = coeffs(cw, box[0], box[2], nw, bounds, kh, filter);
            byte[] r = new byte[nw * h * bands];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < nw; x++) {
                    int xmin = bounds[x * 2];
                    int n = bounds[x * 2 + 1];
                    for (int b = 0; b < bands; b++) {
                        int ss = 1 << (PRECISION_BITS - 1);
                        for (int j = 0; j < n; j++) {
                            ss += (cur[(y * cw + xmin + j) * bands + b] & 0xff) * kh[0][x * ks + j];
                        }
                        r[(y * nw + x) * bands + b] = clip8(ss);
                    }
                }
            }
            cur = r;
            cw = nw;
        }
        if (vertical) {
            int[] bounds = new int[nh * 2];
            int[][] kv = new int[1][];
            int ks = coeffs(h, box[1], box[3], nh, bounds, kv, filter);
            byte[] r = new byte[cw * nh * bands];
            for (int y = 0; y < nh; y++) {
                int ymin = bounds[y * 2];
                int n = bounds[y * 2 + 1];
                for (int x = 0; x < cw; x++) {
                    for (int b = 0; b < bands; b++) {
                        int ss = 1 << (PRECISION_BITS - 1);
                        for (int j = 0; j < n; j++) {
                            ss += (cur[((ymin + j) * cw + x) * bands + b] & 0xff) * kv[0][y * ks + j];
                        }
                        r[(y * cw + x) * bands + b] = clip8(ss);
                    }
                }
            }
            cur = r;
        }
        if (cur == src) { //NOPMD CompareObjectsWithEquals - identity is meant
            cur = new byte[src.length];
            System.arraycopy(src, 0, cur, 0, src.length);
        }
        return cur;
    }

    private static byte[] nearest(byte[] src, int w, int h, int bands, int nw, int nh, double[] box) {
        byte[] r = new byte[nw * nh * bands];
        double sx = (box[2] - box[0]) / nw;
        double sy = (box[3] - box[1]) / nh;
        for (int y = 0; y < nh; y++) {
            int iy = (int) Math.floor(box[1] + (y + 0.5) * sy);
            iy = iy < 0 ? 0 : iy >= h ? h - 1 : iy;
            for (int x = 0; x < nw; x++) {
                int ix = (int) Math.floor(box[0] + (x + 0.5) * sx);
                ix = ix < 0 ? 0 : ix >= w ? w - 1 : ix;
                System.arraycopy(src, (iy * w + ix) * bands, r, (y * nw + x) * bands, bands);
            }
        }
        return r;
    }

    /// Premultiplies the color bands by the last (alpha) band, Pillow's
    /// rounding (`RGBA` to `RGBa`).
    ///
    /// #### Parameters
    ///
    /// - `d`: interleaved pixels whose last band is alpha
    /// - `bands`: bytes per pixel
    ///
    /// #### Returns
    ///
    /// a new premultiplied array
    public static byte[] premultiply(byte[] d, int bands) {
        byte[] r = new byte[d.length];
        System.arraycopy(d, 0, r, 0, d.length);
        for (int q = 0; q + bands <= d.length; q += bands) {
            int alpha = d[q + bands - 1] & 0xff;
            for (int b = 0; b < bands - 1; b++) {
                int t = (d[q + b] & 0xff) * alpha + 128;
                r[q + b] = (byte) (((t >> 8) + t) >> 8);
            }
        }
        return r;
    }

    /// Reverses [#premultiply(byte[], int)] in place, as Pillow's `rgbA2rgba`.
    ///
    /// #### Parameters
    ///
    /// - `d`: interleaved premultiplied pixels whose last band is alpha
    /// - `bands`: bytes per pixel
    ///
    /// #### Returns
    ///
    /// `d`
    public static byte[] unpremultiply(byte[] d, int bands) {
        for (int q = 0; q + bands <= d.length; q += bands) {
            int alpha = d[q + bands - 1] & 0xff;
            if (alpha == 0 || alpha == 255) {
                continue;
            }
            for (int b = 0; b < bands - 1; b++) {
                int v = (255 * (d[q + b] & 0xff)) / alpha;
                d[q + b] = (byte) (v > 255 ? 255 : v);
            }
        }
        return d;
    }
}
