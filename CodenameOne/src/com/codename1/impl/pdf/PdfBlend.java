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
package com.codename1.impl.pdf;

/// PDF blend modes and the compositing formula, on ARGB pixels:
/// `Cr = (1 - as) Cb + as ((1 - ab) Cs + ab B(Cb, Cs))`, with `as` the
/// source alpha after the soft mask and `ab` the backdrop alpha.
final class PdfBlend {
    static final int NORMAL = 0;
    static final int MULTIPLY = 1;
    static final int SCREEN = 2;
    static final int OVERLAY = 3;
    static final int DARKEN = 4;
    static final int LIGHTEN = 5;
    static final int COLOR_DODGE = 6;
    static final int COLOR_BURN = 7;
    static final int HARD_LIGHT = 8;
    static final int SOFT_LIGHT = 9;
    static final int DIFFERENCE = 10;
    static final int EXCLUSION = 11;
    static final int HUE = 12;
    static final int SATURATION = 13;
    static final int COLOR = 14;
    static final int LUMINOSITY = 15;

    private static final String[] NAMES = {"Normal", "Multiply", "Screen", "Overlay", "Darken", "Lighten",
        "ColorDodge", "ColorBurn", "HardLight", "SoftLight", "Difference", "Exclusion", "Hue", "Saturation",
        "Color", "Luminosity"};

    private PdfBlend() {
    }

    /// The mode a `BM` value names; an array names a list to pick the
    /// first known mode from. Unknown modes are Normal.
    static int parse(Object bm) {
        if (bm instanceof PdfName) {
            String n = ((PdfName) bm).value;
            for (int i = 0; i < NAMES.length; i++) {
                if (NAMES[i].equals(n)) {
                    return i;
                }
            }
            return "Compatible".equals(n) ? NORMAL : -1;
        }
        if (bm instanceof java.util.List) {
            java.util.List<?> l = (java.util.List<?>) bm;
            for (int i = 0; i < l.size(); i++) {
                int m = parse(l.get(i));
                if (m >= 0) {
                    return m;
                }
            }
        }
        return NORMAL;
    }

    /// Composites `src` over `back` (both `w * h` ARGB; `back` may be
    /// null for a white opaque page) with blend `mode`; `mask` scales the
    /// source alpha per pixel (0-255) when not null, `alpha` overall.
    /// Returns pixels to draw with ordinary source-over: their color
    /// already holds the blended result.
    static int[] composite(int[] back, int[] src, int[] mask, int mode, double alpha, int w, int h) {
        int n = w * h;
        int[] out = new int[n];
        int ga = (int) (Math.max(0, Math.min(1, alpha)) * 255 + 0.5);
        float[] b = new float[3];
        float[] s = new float[3];
        float[] r = new float[3];
        for (int i = 0; i < n; i++) {
            int sp = src[i];
            int as = sp >>> 24;
            if (mask != null) {
                as = as * mask[i] / 255;
            }
            as = as * ga / 255;
            if (as == 0) {
                continue;
            }
            if (mode == NORMAL) {
                out[i] = (as << 24) | (sp & 0xffffff);
                continue;
            }
            int bp = back == null ? 0xffffffff : back[i];
            float ab = (bp >>> 24) / 255f;
            b[0] = ((bp >> 16) & 0xff) / 255f;
            b[1] = ((bp >> 8) & 0xff) / 255f;
            b[2] = (bp & 0xff) / 255f;
            s[0] = ((sp >> 16) & 0xff) / 255f;
            s[1] = ((sp >> 8) & 0xff) / 255f;
            s[2] = (sp & 0xff) / 255f;
            blend(mode, b, s, r);
            int rgb = 0;
            for (int k = 0; k < 3; k++) {
                float c = (1 - ab) * s[k] + ab * r[k];
                int v = (int) (c * 255 + 0.5f);
                rgb = (rgb << 8) | (v < 0 ? 0 : v > 255 ? 255 : v);
            }
            out[i] = (as << 24) | rgb;
        }
        return out;
    }

    static void blend(int mode, float[] b, float[] s, float[] r) {
        if (mode >= HUE) {
            nonSeparable(mode, b, s, r);
            return;
        }
        for (int k = 0; k < 3; k++) {
            r[k] = separable(mode, b[k], s[k]);
        }
    }

    private static float separable(int mode, float cb, float cs) {
        switch (mode) {
            case MULTIPLY:
                return cb * cs;
            case SCREEN:
                return cb + cs - cb * cs;
            case OVERLAY:
                return hardLight(cs, cb);
            case DARKEN:
                return Math.min(cb, cs);
            case LIGHTEN:
                return Math.max(cb, cs);
            case COLOR_DODGE:
                if (cb <= 0) {
                    return 0;
                }
                return cs >= 1 ? 1 : Math.min(1, cb / (1 - cs));
            case COLOR_BURN:
                if (cb >= 1) {
                    return 1;
                }
                return cs <= 0 ? 0 : 1 - Math.min(1, (1 - cb) / cs);
            case HARD_LIGHT:
                return hardLight(cb, cs);
            case SOFT_LIGHT:
                if (cs <= 0.5f) {
                    return cb - (1 - 2 * cs) * cb * (1 - cb);
                } else {
                    float d = cb <= 0.25f ? ((16 * cb - 12) * cb + 4) * cb : (float) Math.sqrt(cb);
                    return cb + (2 * cs - 1) * (d - cb);
                }
            case DIFFERENCE:
                return Math.abs(cb - cs);
            case EXCLUSION:
                return cb + cs - 2 * cb * cs;
            default:
                return cs;
        }
    }

    private static float hardLight(float cb, float cs) {
        if (cs <= 0.5f) {
            return cb * 2 * cs;
        }
        float t = 2 * cs - 1;
        return cb + t - cb * t;
    }

    private static float lum(float[] c) {
        return 0.3f * c[0] + 0.59f * c[1] + 0.11f * c[2];
    }

    private static void setLum(float[] c, float l, float[] out) {
        float d = l - lum(c);
        for (int k = 0; k < 3; k++) {
            out[k] = c[k] + d;
        }
        float lv = lum(out);
        float n = Math.min(out[0], Math.min(out[1], out[2]));
        float x = Math.max(out[0], Math.max(out[1], out[2]));
        for (int k = 0; k < 3; k++) {
            if (n < 0 && lv - n != 0) {
                out[k] = lv + (out[k] - lv) * lv / (lv - n);
            }
            if (x > 1 && x - lv != 0) {
                out[k] = lv + (out[k] - lv) * (1 - lv) / (x - lv);
            }
        }
    }

    private static float sat(float[] c) {
        return Math.max(c[0], Math.max(c[1], c[2])) - Math.min(c[0], Math.min(c[1], c[2]));
    }

    private static void setSat(float[] c, float sv, float[] out) {
        int max = 0;
        int min = 0;
        for (int k = 1; k < 3; k++) {
            if (c[k] > c[max]) {
                max = k;
            }
            if (c[k] < c[min]) {
                min = k;
            }
        }
        if (max == min) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            return;
        }
        int mid = 3 - max - min;
        float range = c[max] - c[min];
        out[mid] = (c[mid] - c[min]) * sv / range;
        out[max] = sv;
        out[min] = 0;
    }

    private static void nonSeparable(int mode, float[] b, float[] s, float[] r) {
        float[] t = new float[3];
        switch (mode) {
            case HUE:
                setSat(s, sat(b), t);
                setLum(t, lum(b), r);
                break;
            case SATURATION:
                setSat(b, sat(s), t);
                setLum(t, lum(b), r);
                break;
            case COLOR:
                setLum(s, lum(b), r);
                break;
            default:
                setLum(b, lum(s), r);
                break;
        }
    }
}
