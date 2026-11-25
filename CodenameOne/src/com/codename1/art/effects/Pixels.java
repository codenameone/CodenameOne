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

import com.codename1.art.Raster;

/// Pixel helpers shared by the effects: alpha channels, blurs, morphology.
final class Pixels {
    private Pixels() {
    }

    /// A raster grown by `pad` pixels on every side.
    static Raster expand(Raster r, int pad) {
        if (pad <= 0) {
            return r.copy();
        }
        return r.resize(r.getX() - pad, r.getY() - pad, r.getWidth() + pad * 2, r.getHeight() + pad * 2);
    }

    /// The alpha channel, 0 to 1.
    static float[] alpha(Raster r) {
        int[] p = r.getPixels();
        int n = r.getWidth() * r.getHeight();
        float[] a = new float[n];
        for (int i = 0; i < n; i++) {
            a[i] = (p[i] >>> 24) / 255f;
        }
        return a;
    }

    /// Premultiplied channels: `[a, r, g, b]`, each 0 to 1.
    static float[][] premul(Raster r) {
        int[] p = r.getPixels();
        int n = r.getWidth() * r.getHeight();
        float[][] c = new float[4][n];
        for (int i = 0; i < n; i++) {
            int v = p[i];
            float a = (v >>> 24) / 255f;
            c[0][i] = a;
            c[1][i] = ((v >> 16) & 0xff) / 255f * a;
            c[2][i] = ((v >> 8) & 0xff) / 255f * a;
            c[3][i] = (v & 0xff) / 255f * a;
        }
        return c;
    }

    /// Writes premultiplied channels back as non-premultiplied pixels.
    static void unpremul(float[][] c, Raster r) {
        int[] p = r.getPixels();
        int n = r.getWidth() * r.getHeight();
        for (int i = 0; i < n; i++) {
            float a = clamp01(c[0][i]);
            if (a <= 0) {
                p[i] = 0;
                continue;
            }
            int ia = (int) (a * 255 + 0.5f);
            int rr = (int) (clamp01(c[1][i] / a) * 255 + 0.5f);
            int gg = (int) (clamp01(c[2][i] / a) * 255 + 0.5f);
            int bb = (int) (clamp01(c[3][i] / a) * 255 + 0.5f);
            p[i] = (ia << 24) | (rr << 16) | (gg << 8) | bb;
        }
    }

    static float clamp01(float v) {
        if (v < 0) {
            return 0;
        }
        return v > 1 ? 1 : v;
    }

    /// A raster of one color with `a` as alpha.
    static Raster fromAlpha(float[] a, int color, int x, int y, int w, int h, float opacity) {
        Raster r = new Raster(x, y, w, h);
        int[] p = r.getPixels();
        int ca = color >>> 24;
        int rgb = color & 0xffffff;
        for (int i = 0; i < a.length; i++) {
            float v = a[i] * opacity * ca;
            if (v > 0) {
                int ia = (int) (v + 0.5f);
                p[i] = ((ia > 255 ? 255 : ia) << 24) | rgb;
            }
        }
        return r;
    }

    /// The box sizes for three passes approximating a Gaussian (SVG 1.1, feGaussianBlur).
    static int boxSize(double sigma) {
        if (sigma <= 0) {
            return 0;
        }
        return (int) Math.floor(sigma * 3 * Math.sqrt(2 * Math.PI) / 4 + 0.5);
    }

    /// Blurs one channel in place.
    static void blur(float[] ch, int w, int h, double sx, double sy) {
        int bx = boxSize(sx);
        int by = boxSize(sy);
        float[] tmp = new float[Math.max(w, h)];
        if (bx > 0) {
            for (int pass = 0; pass < 3; pass++) {
                for (int y = 0; y < h; y++) {
                    boxLine(ch, y * w, 1, w, bx, pass, tmp);
                }
            }
        }
        if (by > 0) {
            for (int pass = 0; pass < 3; pass++) {
                for (int x = 0; x < w; x++) {
                    boxLine(ch, x, w, h, by, pass, tmp);
                }
            }
        }
    }

    private static void boxLine(float[] ch, int start, int stride, int n, int d, int pass, float[] tmp) {
        int left;
        int right;
        if ((d & 1) != 0) {
            left = d / 2;
            right = d / 2;
        } else if (pass == 0) {
            left = d / 2;
            right = d / 2 - 1;
        } else if (pass == 1) {
            left = d / 2 - 1;
            right = d / 2;
        } else {
            left = d / 2;
            right = d / 2;
            d = d + 1;
        }
        for (int i = 0; i < n; i++) {
            tmp[i] = ch[start + i * stride];
        }
        float sum = 0;
        for (int i = -left; i <= right; i++) {
            if (i >= 0 && i < n) {
                sum += tmp[i];
            }
        }
        for (int i = 0; i < n; i++) {
            ch[start + i * stride] = sum / d;
            int out = i - left;
            int in = i + right + 1;
            if (out >= 0 && out < n) {
                sum -= tmp[out];
            }
            if (in >= 0 && in < n) {
                sum += tmp[in];
            }
        }
    }

    /// A blurred copy, grown to hold the blur.
    static Raster blurRaster(Raster r, double sx, double sy) {
        int pad = (int) Math.ceil(Math.max(sx, sy) * 3);
        Raster e = expand(r, pad);
        float[][] c = premul(e);
        for (int k = 0; k < 4; k++) {
            blur(c[k], e.getWidth(), e.getHeight(), sx, sy);
        }
        unpremul(c, e);
        return e;
    }

    /// Dilates (`grow` true) or erodes a channel with a rectangle of radius `rx, ry`. Outside
    /// the raster counts as transparent. Each pass is van Herk/Gil-Werman, so the cost does not
    /// grow with the radius, which a hostile document can set as large as it likes.
    static float[] morph(float[] a, int w, int h, int rx, int ry, boolean grow) {
        float[] t = new float[a.length];
        int r = Math.min(rx, w);
        float[] line = new float[w + 2 * r];
        float[] res = new float[w];
        for (int y = 0; y < h; y++) {
            java.util.Arrays.fill(line, 0);
            System.arraycopy(a, y * w, line, r, w);
            window(line, w, r, grow, res);
            System.arraycopy(res, 0, t, y * w, w);
        }
        float[] o = new float[a.length];
        r = Math.min(ry, h);
        line = new float[h + 2 * r];
        res = new float[h];
        for (int x = 0; x < w; x++) {
            java.util.Arrays.fill(line, 0);
            for (int y = 0; y < h; y++) {
                line[r + y] = t[y * w + x];
            }
            window(line, h, r, grow, res);
            for (int y = 0; y < h; y++) {
                o[y * w + x] = res[y];
            }
        }
        return o;
    }

    /// The max (or min) of every window of `2r+1` values of a padded line, answered for the
    /// `n` windows centred on the unpadded values.
    private static void window(float[] line, int n, int r, boolean grow, float[] out) {
        int k = 2 * r + 1;
        int len = line.length;
        float[] g = new float[len];
        float[] hh = new float[len];
        for (int i = 0; i < len; i++) {
            g[i] = i % k == 0 ? line[i] : pick(g[i - 1], line[i], grow);
        }
        for (int i = len - 1; i >= 0; i--) {
            hh[i] = i == len - 1 || (i + 1) % k == 0 ? line[i] : pick(hh[i + 1], line[i], grow);
        }
        for (int i = 0; i < n; i++) {
            out[i] = pick(hh[i], g[i + k - 1], grow);
        }
    }

    private static float pick(float a, float b, boolean grow) {
        return grow ? Math.max(a, b) : Math.min(a, b);
    }

    /// The channel shifted by whole pixels; what moves in is `fill`.
    static float[] shift(float[] a, int w, int h, int dx, int dy, float fill) {
        float[] o = new float[a.length];
        for (int y = 0; y < h; y++) {
            int sy = y - dy;
            for (int x = 0; x < w; x++) {
                int sx = x - dx;
                o[y * w + x] = sx < 0 || sy < 0 || sx >= w || sy >= h ? fill : a[sy * w + sx];
            }
        }
        return o;
    }

    /// Luminance of a non-premultiplied pixel, 0 to 1.
    static float luminance(int p) {
        return (0.2126f * ((p >> 16) & 0xff) + 0.7152f * ((p >> 8) & 0xff) + 0.0722f * (p & 0xff)) / 255f;
    }

    static int pack(int a, float r, float g, float b) {
        return (a << 24) | (to8(r) << 16) | (to8(g) << 8) | to8(b);
    }

    static int to8(float v) {
        int i = (int) (v * 255 + 0.5f);
        if (i < 0) {
            return 0;
        }
        return i > 255 ? 255 : i;
    }
}
