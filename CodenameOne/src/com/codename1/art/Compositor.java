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
package com.codename1.art;

/// Software compositing of ARGB pixel arrays.
///
/// Uses the formulas of the W3C Compositing and Blending specification: the blended color is
/// mixed with the layer's color by the backdrop's alpha, then composited source-over. Pixels
/// are non-premultiplied `0xAARRGGBB` ints, as `Image.getRGB` returns them.
public final class Compositor {
    private Compositor() {
    }

    /// Composites `src` over `dst` in place.
    ///
    /// #### Parameters
    ///
    /// - `src`: the layer's pixels
    /// - `dst`: the backdrop, overwritten with the result; same length as `src`
    /// - `mode`: how colors combine
    /// - `opacity`: the layer's opacity, 0 to 1, applied on top of each pixel's own alpha
    public static void composite(int[] src, int[] dst, BlendMode mode, float opacity) {
        if (src.length != dst.length) {
            throw new IllegalArgumentException("src and dst differ in length");
        }
        double[] scratch = new double[Compositor.SCRATCH];
        for (int i = 0; i < src.length; i++) {
            dst[i] = blendPixel(src[i], dst[i], mode, opacity, scratch, i);
        }
    }

    /// Composites one pixel and returns the result.
    ///
    /// #### Parameters
    ///
    /// - `src`: the layer's pixel
    /// - `dst`: the backdrop pixel
    /// - `mode`: how colors combine
    /// - `opacity`: the layer's opacity, 0 to 1
    public static int blend(int src, int dst, BlendMode mode, float opacity) {
        return blendPixel(src, dst, mode, opacity, new double[SCRATCH], 0);
    }

    /// The scratch space `blend(int, int, BlendMode, float, double[])` needs, in doubles.
    public static final int SCRATCH = 18;

    /// Blends one pixel without allocating, for loops over many pixels.
    ///
    /// #### Parameters
    ///
    /// - `src`: the layer's pixel
    /// - `dst`: the backdrop pixel
    /// - `mode`: how colors combine
    /// - `opacity`: the layer's opacity, 0 to 1
    /// - `scratch`: at least `SCRATCH` doubles, reused across calls
    ///
    /// #### Returns
    ///
    /// the composited pixel
    public static int blend(int src, int dst, BlendMode mode, float opacity, double[] scratch) {
        return blendPixel(src, dst, mode, opacity, scratch, 0);
    }

    /// Blends one pixel with caller-owned scratch space (`SCRATCH` doubles); `seed` picks the
    /// pixels a dissolve keeps and should differ per pixel position.
    static int blendPixel(int src, int dst, BlendMode mode, float opacity, double[] scratch, int seed) {
        // NaN and out-of-range opacities are clamped, so they never reach the arithmetic.
        if (!(opacity > 0)) { // NOPMD LogicInversion - true for NaN too, which opacity <= 0 is not
            return dst;
        }
        double as = ((src >>> 24) / 255.0) * Math.min(1, opacity);
        if (as <= 0) {
            return dst;
        }
        if (mode == BlendMode.DISSOLVE) {
            int h = seed * 0x9E3779B1;
            h ^= h >>> 15;
            h *= 0x85EBCA77;
            h ^= h >>> 13;
            double r = (h >>> 8) / (double) (1 << 24);
            return r < as ? (0xff000000 | (src & 0xffffff)) : dst;
        }
        if (mode == BlendMode.PLUS_LIGHTER || mode == BlendMode.PLUS_DARKER) {
            return plus(src, dst, as, mode == BlendMode.PLUS_LIGHTER);
        }
        double[] cs = scratch;
        double ab = (dst >>> 24) / 255.0;
        cs[0] = ((src >> 16) & 0xff) / 255.0;
        cs[1] = ((src >> 8) & 0xff) / 255.0;
        cs[2] = (src & 0xff) / 255.0;
        cs[3] = ((dst >> 16) & 0xff) / 255.0;
        cs[4] = ((dst >> 8) & 0xff) / 255.0;
        cs[5] = (dst & 0xff) / 255.0;
        if (mode.isNonSeparable()) {
            nonSeparable(mode, cs);
        } else {
            for (int c = 0; c < 3; c++) {
                cs[6 + c] = separable(mode, cs[c], cs[3 + c]);
            }
        }
        double ao = as + ab * (1 - as);
        int out = toByte(ao) << 24;
        for (int c = 0; c < 3; c++) {
            double mixed = (1 - ab) * cs[c] + ab * cs[6 + c];
            double co = ao <= 0 ? 0 : (as * mixed + ab * cs[3 + c] * (1 - as)) / ao;
            out |= toByte(co) << (16 - 8 * c);
        }
        return out;
    }

    private static int plus(int src, int dst, double as, boolean lighter) {
        double ab = (dst >>> 24) / 255.0;
        double ao = Math.min(1, as + ab);
        if (ao <= 0) {
            return 0;
        }
        int out = toByte(ao) << 24;
        for (int c = 0; c < 3; c++) {
            int sh = 16 - 8 * c;
            double s = ((src >> sh) & 0xff) / 255.0 * as;
            double b = ((dst >> sh) & 0xff) / 255.0 * ab;
            double v = lighter ? Math.min(ao, s + b) : Math.max(0, ao - ((as - s) + (ab - b)));
            out |= toByte(v / ao) << sh;
        }
        return out;
    }

    private static int toByte(double v) {
        int i = (int) (v * 255.0 + 0.5);
        if (i < 0) {
            return 0;
        }
        return i > 255 ? 255 : i;
    }

    static double separable(BlendMode mode, double s, double b) {
        switch (mode) {
            case MULTIPLY:
                return s * b;
            case SCREEN:
                return s + b - s * b;
            case OVERLAY:
                return hardLight(b, s);
            case DARKEN:
                return Math.min(s, b);
            case LIGHTEN:
                return Math.max(s, b);
            case COLOR_DODGE:
                if (b == 0) {
                    return 0;
                }
                return s >= 1 ? 1 : Math.min(1, b / (1 - s));
            case COLOR_BURN:
                if (b >= 1) {
                    return 1;
                }
                return s <= 0 ? 0 : 1 - Math.min(1, (1 - b) / s);
            case HARD_LIGHT:
                return hardLight(s, b);
            case SOFT_LIGHT:
                if (s <= 0.5) {
                    return b - (1 - 2 * s) * b * (1 - b);
                }
                double d = b <= 0.25 ? ((16 * b - 12) * b + 4) * b : Math.sqrt(b);
                return b + (2 * s - 1) * (d - b);
            case DIFFERENCE:
                return Math.abs(b - s);
            case EXCLUSION:
                return b + s - 2 * b * s;
            case LINEAR_BURN:
                return Math.max(0, s + b - 1);
            case LINEAR_DODGE:
                return Math.min(1, s + b);
            case VIVID_LIGHT:
                if (s <= 0.5) {
                    return s <= 0 ? (b >= 1 ? 1 : 0) : Math.max(0, 1 - (1 - b) / (2 * s));
                }
                return s >= 1 ? (b <= 0 ? 0 : 1) : Math.min(1, b / (2 * (1 - s)));
            case LINEAR_LIGHT:
                return Math.max(0, Math.min(1, b + 2 * s - 1));
            case PIN_LIGHT:
                return s <= 0.5 ? Math.min(b, 2 * s) : Math.max(b, 2 * s - 1);
            case HARD_MIX:
                return s + b >= 1 ? 1 : 0;
            case SUBTRACT:
                return Math.max(0, b - s);
            case DIVIDE:
                return s <= 0 ? (b <= 0 ? 0 : 1) : Math.min(1, b / s);
            default:
                return s;
        }
    }

    private static double hardLight(double s, double b) {
        if (s <= 0.5) {
            return b * 2 * s;
        }
        double s2 = 2 * s - 1;
        return b + s2 - b * s2;
    }

    // The source color is c[0..2], the backdrop c[3..5]; the result goes to c[6..8] and
    // c[9..11] is working space. Nothing is allocated: this runs once per pixel.
    private static void nonSeparable(BlendMode mode, double[] c) {
        switch (mode) {
            case HUE:
                setSat(c, 0, sat(c, 3), c, 9);
                setLum(c, 9, lum(c, 3), c, 6);
                break;
            case SATURATION:
                setSat(c, 3, sat(c, 0), c, 9);
                setLum(c, 9, lum(c, 3), c, 6);
                break;
            case COLOR:
                setLum(c, 0, lum(c, 3), c, 6);
                break;
            case DARKER_COLOR:
                System.arraycopy(c, lum(c, 0) < lum(c, 3) ? 0 : 3, c, 6, 3);
                break;
            case LIGHTER_COLOR:
                System.arraycopy(c, lum(c, 0) > lum(c, 3) ? 0 : 3, c, 6, 3);
                break;
            default:
                setLum(c, 3, lum(c, 0), c, 6);
                break;
        }
    }

    private static double lum(double[] c, int o) {
        return 0.3 * c[o] + 0.59 * c[o + 1] + 0.11 * c[o + 2];
    }

    private static double sat(double[] c, int o) {
        return Math.max(c[o], Math.max(c[o + 1], c[o + 2])) - Math.min(c[o], Math.min(c[o + 1], c[o + 2]));
    }

    // ClipColor as the W3C compositing specification writes it: n and x come from the color
    // before clipping, and the two clips apply one after the other.
    private static void setLum(double[] c, int o, double l, double[] out, int oo) {
        double d = l - lum(c, o);
        double r = c[o] + d;
        double g = c[o + 1] + d;
        double bl = c[o + 2] + d;
        double ll = 0.3 * r + 0.59 * g + 0.11 * bl;
        double n = Math.min(r, Math.min(g, bl));
        double x = Math.max(r, Math.max(g, bl));
        if (n < 0) {
            r = ll + (r - ll) * ll / (ll - n);
            g = ll + (g - ll) * ll / (ll - n);
            bl = ll + (bl - ll) * ll / (ll - n);
        }
        if (x > 1) {
            r = ll + (r - ll) * (1 - ll) / (x - ll);
            g = ll + (g - ll) * (1 - ll) / (x - ll);
            bl = ll + (bl - ll) * (1 - ll) / (x - ll);
        }
        out[oo] = r;
        out[oo + 1] = g;
        out[oo + 2] = bl;
    }

    private static void setSat(double[] c, int o, double s, double[] out, int oo) {
        int max = 0;
        int min = 0;
        for (int i = 1; i < 3; i++) {
            if (c[o + i] > c[o + max]) {
                max = i;
            }
            if (c[o + i] < c[o + min]) {
                min = i;
            }
        }
        if (max == min) {
            out[oo] = 0;
            out[oo + 1] = 0;
            out[oo + 2] = 0;
            return;
        }
        int mid = 3 - max - min;
        double range = c[o + max] - c[o + min];
        out[oo + mid] = (c[o + mid] - c[o + min]) * s / range;
        out[oo + max] = s;
        out[oo + min] = 0;
    }
}
