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
        double[] scratch = new double[9];
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
        return blendPixel(src, dst, mode, opacity, new double[9], 0);
    }

    /// Blends one pixel with caller-owned scratch space (nine doubles); `seed` picks the
    /// pixels a dissolve keeps and should differ per pixel position.
    static int blendPixel(int src, int dst, BlendMode mode, float opacity, double[] scratch, int seed) {
        double as = ((src >>> 24) / 255.0) * opacity;
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

    private static void nonSeparable(BlendMode mode, double[] c) {
        double[] s = {c[0], c[1], c[2]};
        double[] b = {c[3], c[4], c[5]};
        double[] out = new double[3];
        switch (mode) {
            case HUE:
                setSat(s, sat(b), out);
                setLum(out, lum(b), out);
                break;
            case SATURATION:
                setSat(b, sat(s), out);
                setLum(out, lum(b), out);
                break;
            case COLOR:
                setLum(s, lum(b), out);
                break;
            case DARKER_COLOR:
                System.arraycopy(lum(s) < lum(b) ? s : b, 0, out, 0, 3);
                break;
            case LIGHTER_COLOR:
                System.arraycopy(lum(s) > lum(b) ? s : b, 0, out, 0, 3);
                break;
            default:
                setLum(b, lum(s), out);
                break;
        }
        c[6] = out[0];
        c[7] = out[1];
        c[8] = out[2];
    }

    private static double lum(double[] c) {
        return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2];
    }

    private static double sat(double[] c) {
        return Math.max(c[0], Math.max(c[1], c[2])) - Math.min(c[0], Math.min(c[1], c[2]));
    }

    private static void setLum(double[] c, double l, double[] out) {
        double d = l - lum(c);
        double r = c[0] + d;
        double g = c[1] + d;
        double bl = c[2] + d;
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
        out[0] = r;
        out[1] = g;
        out[2] = bl;
    }

    private static void setSat(double[] c, double s, double[] out) {
        int max = 0;
        int min = 0;
        for (int i = 1; i < 3; i++) {
            if (c[i] > c[max]) {
                max = i;
            }
            if (c[i] < c[min]) {
                min = i;
            }
        }
        if (max == min) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            return;
        }
        int mid = 3 - max - min;
        double range = c[max] - c[min];
        double midV = (c[mid] - c[min]) * s / range;
        out[mid] = midV;
        out[max] = s;
        out[min] = 0;
    }
}
