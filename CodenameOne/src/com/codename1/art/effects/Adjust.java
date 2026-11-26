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
package com.codename1.art.effects;

import com.codename1.art.GradientPaint;
import com.codename1.util.MathUtil;

/// Per-pixel color adjustments. Alpha is kept.
final class Adjust {
    private Adjust() {
    }

    static void apply(Effect e, int[] px) {
        String k = e.getKind();
        if (Effect.CURVES.equals(k) || Effect.LEVELS.equals(k) || Effect.POSTERIZE.equals(k)
                || Effect.EXPOSURE.equals(k) || Effect.INVERT.equals(k)) {
            int[][] lut = luts(e);
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                if ((p >>> 24) == 0) {
                    continue;
                }
                px[i] = (p & 0xff000000) | (lut[0][(p >> 16) & 0xff] << 16)
                        | (lut[1][(p >> 8) & 0xff] << 8) | lut[2][p & 0xff];
            }
            return;
        }
        float[] m = null;
        if (Effect.COLOR_MATRIX.equals(k)) {
            m = e.getFloats("matrix", null);
            if (m == null || m.length < 20) {
                return;
            }
        }
        GradientPaint map = null;
        if (Effect.GRADIENT_MAP.equals(k)) {
            map = new GradientPaint(GradientPaint.LINEAR, e.getFloats("positions", new float[]{0, 1}),
                    e.getColors("colors", new int[]{0xff000000, 0xffffffff}));
        }
        float[] rgb = new float[3];
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            if (a == 0) {
                continue;
            }
            float r = ((p >> 16) & 0xff) / 255f;
            float g = ((p >> 8) & 0xff) / 255f;
            float b = (p & 0xff) / 255f;
            if (m != null) {
                float af = a / 255f;
                float nr = m[0] * r + m[1] * g + m[2] * b + m[3] * af + m[4];
                float ng = m[5] * r + m[6] * g + m[7] * b + m[8] * af + m[9];
                float nb = m[10] * r + m[11] * g + m[12] * b + m[13] * af + m[14];
                float na = m[15] * r + m[16] * g + m[17] * b + m[18] * af + m[19];
                px[i] = Pixels.pack(Pixels.to8(na), nr, ng, nb);
                continue;
            }
            if (map != null) {
                int c = map.colorFor(Pixels.luminance(p));
                px[i] = (a << 24) | (c & 0xffffff);
                continue;
            }
            rgb[0] = r;
            rgb[1] = g;
            rgb[2] = b;
            pixel(e, k, rgb);
            px[i] = Pixels.pack(a, rgb[0], rgb[1], rgb[2]);
        }
    }

    private static void pixel(Effect e, String k, float[] c) {
        if (Effect.HUE_SATURATION.equals(k)) {
            float[] hsl = toHsl(c[0], c[1], c[2]);
            if (e.getFloat("colorize", 0) != 0) {
                hsl[0] = e.getFloat("hue", 0) / 360f;
                hsl[1] = Pixels.clamp01((e.getFloat("saturation", 0) + 1) / 2);
            } else {
                hsl[0] = hsl[0] + e.getFloat("hue", 0) / 360f;
                hsl[0] = hsl[0] - (float) Math.floor(hsl[0]);
                float sat = e.getFloat("saturation", 0);
                hsl[1] = sat < 0 ? hsl[1] * (1 + sat) : hsl[1] + (1 - hsl[1]) * sat;
            }
            float li = e.getFloat("lightness", 0);
            hsl[2] = li < 0 ? hsl[2] * (1 + li) : hsl[2] + (1 - hsl[2]) * li;
            fromHsl(hsl[0], Pixels.clamp01(hsl[1]), Pixels.clamp01(hsl[2]), c);
        } else if (Effect.BRIGHTNESS_CONTRAST.equals(k)) {
            float br = e.getFloat("brightness", 0);
            float ct = e.getFloat("contrast", 0);
            float f = ct >= 0 ? 1 / Math.max(0.01f, 1 - ct) : 1 + ct;
            for (int i = 0; i < 3; i++) {
                c[i] = Pixels.clamp01((c[i] - 0.5f) * f + 0.5f + br);
            }
        } else if (Effect.THRESHOLD.equals(k)) {
            float v = (0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2]) >= e.getFloat("level", 0.5f) ? 1 : 0;
            c[0] = v;
            c[1] = v;
            c[2] = v;
        } else if (Effect.CHANNEL_MIXER.equals(k)) {
            float[] m = e.getFloats("matrix", new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0});
            if (m.length >= 12) {
                float r = c[0];
                float g = c[1];
                float b = c[2];
                c[0] = Pixels.clamp01(m[0] * r + m[1] * g + m[2] * b + m[3]);
                c[1] = Pixels.clamp01(m[4] * r + m[5] * g + m[6] * b + m[7]);
                c[2] = Pixels.clamp01(m[8] * r + m[9] * g + m[10] * b + m[11]);
                if (e.getFloat("monochrome", 0) != 0) {
                    c[1] = c[0];
                    c[2] = c[0];
                }
            }
        } else if (Effect.PHOTO_FILTER.equals(k)) {
            int col = e.getColor("color", 0xffec8a00);
            float d = e.getFloat("density", 0.25f);
            float lum = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
            float fr = ((col >> 16) & 0xff) / 255f;
            float fg = ((col >> 8) & 0xff) / 255f;
            float fb = (col & 0xff) / 255f;
            c[0] = c[0] + (c[0] * fr - c[0]) * d * 2;
            c[1] = c[1] + (c[1] * fg - c[1]) * d * 2;
            c[2] = c[2] + (c[2] * fb - c[2]) * d * 2;
            if (e.getFloat("preserveLuminosity", 1) != 0) {
                float nl = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
                if (nl > 0) {
                    float f = lum / nl;
                    for (int i = 0; i < 3; i++) {
                        c[i] *= f;
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                c[i] = Pixels.clamp01(c[i]);
            }
        } else if (Effect.VIBRANCE.equals(k)) {
            float amt = e.getFloat("amount", 0);
            float max = Math.max(c[0], Math.max(c[1], c[2]));
            float avg = (c[0] + c[1] + c[2]) / 3;
            float f = amt * (1 - (max - avg) * 3);
            for (int i = 0; i < 3; i++) {
                c[i] = Pixels.clamp01(c[i] + (c[i] - avg) * f);
            }
            // Photoshop's vibrance layer also has a plain saturation slider, -1 to 1.
            float sat = e.getFloat("saturation", 0);
            if (sat != 0) {
                float lum = 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
                for (int i = 0; i < 3; i++) {
                    c[i] = Pixels.clamp01(lum + (c[i] - lum) * (1 + sat));
                }
            }
        } else if (Effect.BLACK_AND_WHITE.equals(k)) {
            float[] wts = e.getFloats("weights", new float[]{0.4f, 0.6f, 0.4f, 0.6f, 0.2f, 0.8f});
            float[] hsl = toHsl(c[0], c[1], c[2]);
            float hue = hsl[0] * 6;
            int i0 = (int) Math.floor(hue) % 6;
            int i1 = (i0 + 1) % 6;
            float f = hue - (float) Math.floor(hue);
            float wt = wts.length >= 6 ? wts[i0] + (wts[i1] - wts[i0]) * f : 0.5f;
            float lum = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
            float v = Pixels.clamp01(lum + (wt - 0.5f) * hsl[1]);
            c[0] = v;
            c[1] = v;
            c[2] = v;
        } else if (Effect.COLOR_BALANCE.equals(k)) {
            colorBalance(e, c);
        } else if (Effect.SELECTIVE_COLOR.equals(k)) {
            selectiveColor(e, c);
        }
    }

    private static float smooth(float a, float b, float x) {
        float t = Pixels.clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    private static void colorBalance(Effect e, float[] c) {
        float[] sh = e.getFloats("shadows", new float[3]);
        float[] mid = e.getFloats("midtones", new float[3]);
        float[] hi = e.getFloats("highlights", new float[3]);
        float l = (Math.max(c[0], Math.max(c[1], c[2])) + Math.min(c[0], Math.min(c[1], c[2]))) / 2;
        float before = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
        float ws = 1 - smooth(0, 0.5f, l);
        float wh = smooth(0.5f, 1, l);
        float wm = 1 - ws - wh;
        for (int i = 0; i < 3; i++) {
            float d = (i < sh.length ? sh[i] : 0) * ws + (i < mid.length ? mid[i] : 0) * wm
                    + (i < hi.length ? hi[i] : 0) * wh;
            c[i] = Pixels.clamp01(c[i] + d * 0.5f);
        }
        if (e.getFloat("preserveLuminosity", 1) != 0) {
            float after = 0.2126f * c[0] + 0.7152f * c[1] + 0.0722f * c[2];
            float diff = before - after;
            for (int i = 0; i < 3; i++) {
                c[i] = Pixels.clamp01(c[i] + diff);
            }
        }
    }

    private static void selectiveColor(Effect e, float[] c) {
        float[] r = e.getFloats("ranges", null);
        if (r == null || r.length < 36) {
            return;
        }
        boolean abs = e.getFloat("absolute", 0) != 0;
        float max = Math.max(c[0], Math.max(c[1], c[2]));
        float min = Math.min(c[0], Math.min(c[1], c[2]));
        float mid = c[0] + c[1] + c[2] - max - min;
        float[] w = new float[9];
        // Reds, yellows, greens, cyans, blues and magentas: a primary weighs max - mid, a
        // secondary mid - min, when the channels are in that order.
        int[] prim = {0, -1, 1, -1, 2, -1};
        for (int k = 0; k < 6; k++) {
            if (prim[k] >= 0) {
                w[k] = c[prim[k]] == max ? max - mid : 0;
            } else {
                int a = k == 1 ? 0 : (k == 3 ? 1 : 2);
                int b = k == 1 ? 1 : (k == 3 ? 2 : 0);
                int other = 3 - a - b;
                w[k] = c[other] == min ? Math.min(c[a], c[b]) - min : 0;
            }
        }
        w[6] = min > 0.5f ? (min - 0.5f) * 2 : 0;
        w[7] = Math.max(0, 1 - (Math.abs(max - 0.5f) + Math.abs(min - 0.5f)));
        w[8] = max < 0.5f ? (0.5f - max) * 2 : 0;
        float[] ink = {1 - c[0], 1 - c[1], 1 - c[2]};
        float[] out = {ink[0], ink[1], ink[2]};
        for (int k = 0; k < 9; k++) {
            if (w[k] == 0) {
                continue;
            }
            float black = r[k * 4 + 3];
            for (int i = 0; i < 3; i++) {
                float adj = r[k * 4 + i] + black;
                float amount = abs ? adj : adj * ink[i];
                out[i] += amount * w[k];
            }
        }
        for (int i = 0; i < 3; i++) {
            c[i] = Pixels.clamp01(1 - out[i]);
        }
    }

    private static int[][] luts(Effect e) {
        String k = e.getKind();
        int[][] l = new int[3][256];
        for (int ch = 0; ch < 3; ch++) {
            for (int i = 0; i < 256; i++) {
                float v = i / 255f;
                if (Effect.INVERT.equals(k)) {
                    v = 1 - v;
                } else if (Effect.LEVELS.equals(k)) {
                    float ib = e.getFloat("inBlack", 0);
                    float iw = e.getFloat("inWhite", 1);
                    float ga = Math.max(0.01f, e.getFloat("gamma", 1));
                    float ob = e.getFloat("outBlack", 0);
                    float ow = e.getFloat("outWhite", 1);
                    float t = iw <= ib ? (v >= iw ? 1 : 0) : Pixels.clamp01((v - ib) / (iw - ib));
                    t = (float) MathUtil.pow(t, 1 / ga);
                    v = ob + (ow - ob) * t;
                } else if (Effect.POSTERIZE.equals(k)) {
                    int n = Math.max(2, (int) e.getFloat("levels", 4));
                    v = Math.round(v * (n - 1)) / (float) (n - 1);
                } else if (Effect.EXPOSURE.equals(k)) {
                    v = v * (float) MathUtil.pow(2, e.getFloat("exposure", 0)) + e.getFloat("offset", 0);
                    v = (float) MathUtil.pow(Pixels.clamp01(v), 1 / Math.max(0.01f, e.getFloat("gamma", 1)));
                } else if (Effect.CURVES.equals(k)) {
                    float[] master = e.getFloats("master", null);
                    String[] names = {"red", "green", "blue"};
                    float[] own = e.getFloats(names[ch], null);
                    if (own != null) {
                        v = curve(own, v);
                    }
                    if (master != null) {
                        v = curve(master, v);
                    }
                }
                l[ch][i] = Pixels.to8(v);
            }
        }
        return l;
    }

    /// Monotone cubic interpolation through `x, y` pairs (Fritsch-Carlson).
    static float curve(float[] pts, float x) {
        int n = pts.length / 2;
        if (n == 0) {
            return x;
        }
        if (n == 1 || x <= pts[0]) {
            return Pixels.clamp01(pts[1]);
        }
        if (x >= pts[(n - 1) * 2]) {
            return Pixels.clamp01(pts[(n - 1) * 2 + 1]);
        }
        float[] d = new float[n - 1];
        for (int i = 0; i < n - 1; i++) {
            float dx = pts[(i + 1) * 2] - pts[i * 2];
            d[i] = dx <= 0 ? 0 : (pts[(i + 1) * 2 + 1] - pts[i * 2 + 1]) / dx;
        }
        float[] m = new float[n];
        m[0] = d[0];
        m[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) {
            m[i] = d[i - 1] * d[i] <= 0 ? 0 : (d[i - 1] + d[i]) / 2;
        }
        for (int i = 0; i < n - 1; i++) {
            if (d[i] == 0) {
                m[i] = 0;
                m[i + 1] = 0;
            } else {
                float a = m[i] / d[i];
                float b = m[i + 1] / d[i];
                float s = a * a + b * b;
                if (s > 9) {
                    float t = 3 / (float) Math.sqrt(s);
                    m[i] = t * a * d[i];
                    m[i + 1] = t * b * d[i];
                }
            }
        }
        int i = 0;
        while (i < n - 2 && x > pts[(i + 1) * 2]) {
            i++;
        }
        float x0 = pts[i * 2];
        float x1 = pts[(i + 1) * 2];
        float y0 = pts[i * 2 + 1];
        float y1 = pts[(i + 1) * 2 + 1];
        float hh = x1 - x0;
        if (hh <= 0) {
            return Pixels.clamp01(y1);
        }
        float t = (x - x0) / hh;
        float t2 = t * t;
        float t3 = t2 * t;
        float v = (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * hh * m[i]
                + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * hh * m[i + 1];
        return Pixels.clamp01(v);
    }

    static float[] toHsl(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2;
        float h = 0;
        float s = 0;
        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }
        return new float[]{h, s, l};
    }

    static void fromHsl(float h, float s, float l, float[] out) {
        if (s == 0) {
            out[0] = l;
            out[1] = l;
            out[2] = l;
            return;
        }
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        out[0] = hue(p, q, h + 1f / 3);
        out[1] = hue(p, q, h);
        out[2] = hue(p, q, h - 1f / 3);
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1f / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 0.5f) {
            return q;
        }
        if (t < 2f / 3) {
            return p + (q - p) * (2f / 3 - t) * 6;
        }
        return p;
    }
}
