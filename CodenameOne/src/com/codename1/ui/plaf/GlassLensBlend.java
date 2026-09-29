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

import com.codename1.util.MathUtil;

/// Reference implementation of Graphics.glassLensRegion: the optics of a raised
/// Liquid Glass lens (the lifted iOS 27 tab selection) laid over what is already
/// painted. Ports without a native path run this; the Metal shader
/// cn1_fs_glass_lens mirrors it.
///
/// The lens is a rounded rectangle (a capsule for a negative corner radius) whose
/// rim is a circular bevel of height `bevel`: a pixel `s` inside the rim shows the
/// surface displaced OUTWARDS along the rim's normal by
/// `D(s) = (h - s) / sqrt(1 - ((h - s) / h)^2)` (h = bevel, zero beyond it) -- the
/// refraction of a glass edge, which pulls whatever lies just outside the lens
/// (the edge of the bar it floats over, the backdrop) into its rim. The three
/// channels are displaced by `D * (1 - dispersion)`, `D` and `D * (1 + dispersion)`,
/// which fringes the rim. On top of that the surface:
///
/// - brightens by `brightness` inside;
/// - darkens by up to `endShade` in a band `endShadeWidth` wide along the rim,
///   weighted by how horizontal the rim's normal is (the rounded ends);
/// - is multiplied by `1 - outline` on a ring `outlineWidth` wide just inside the
///   rim;
/// - gains a highlight of `rimLight + rimLightVertical * |ny|` right inside that
///   ring, decaying as `exp(-d / rimWidth)` with the depth `d` past it;
/// - casts a soft shadow of depth `shadow` around its lower half, a band that
///   peaks `shadowWidth` outside the rim and has the same width.
///
/// Lengths are in pixels, brightness levels in 0..1 units. Everything is mixed
/// with the untouched surface by `amount`.
public final class GlassLensBlend {
    public static final int BEVEL = 0;
    public static final int MAX_SHIFT = 1;
    public static final int DISPERSION = 2;
    public static final int OUTLINE = 3;
    public static final int OUTLINE_WIDTH = 4;
    public static final int RIM_LIGHT = 5;
    public static final int RIM_LIGHT_VERTICAL = 6;
    public static final int RIM_WIDTH = 7;
    public static final int END_SHADE = 8;
    public static final int END_SHADE_WIDTH = 9;
    public static final int BRIGHTNESS = 10;
    public static final int SHADOW = 11;
    public static final int SHADOW_WIDTH = 12;
    /// Number of optics parameters.
    public static final int COUNT = 13;

    private GlassLensBlend() {
    }

    /// How far outside the lens rectangle the effect reaches (the shadow and the
    /// outline ring), in pixels.
    public static int margin(float[] optics) {
        return (int) Math.ceil(optics[SHADOW_WIDTH] * 3) + 1;
    }

    /// How far outside the painted area the refraction may sample, in pixels.
    public static int reach(float[] optics) {
        return (int) Math.ceil(optics[MAX_SHIFT]) + 1;
    }

    /// Outward displacement of a point `s` pixels inside a rim of bevel height `h`.
    public static float displacement(float s, float h, float max) {
        if (s >= h || h <= 0) {
            return 0;
        }
        float t = (h - s) / h;
        float d = (h - s) / (float) Math.sqrt(Math.max(1e-6f, 1 - t * t));
        return d > max ? max : d;
    }

    /// Applies the lens.
    ///
    /// #### Parameters
    ///
    /// - `src`: the surface, `sw * sh` ARGB pixels, read only
    ///
    /// - `sw`: source width
    ///
    /// - `sh`: source height
    ///
    /// - `dst`: receives the result for the output area, `ow * oh` pixels
    ///
    /// - `ox`: the output area's left edge inside `src`
    ///
    /// - `oy`: the output area's top edge inside `src`
    ///
    /// - `ow`: output width
    ///
    /// - `oh`: output height
    ///
    /// - `lx`: the lens rectangle's left edge inside `src` (may be fractional)
    ///
    /// - `ly`: the lens rectangle's top edge inside `src`
    ///
    /// - `lw`: lens width
    ///
    /// - `lh`: lens height
    ///
    /// - `cornerRadius`: corner radius in pixels, negative for a capsule
    ///
    /// - `optics`: `COUNT` parameters, see the constants
    ///
    /// - `amount`: overall strength 0..1
    public static void apply(int[] src, int sw, int sh, int[] dst, int ox, int oy, int ow, int oh,
            float lx, float ly, float lw, float lh, float cornerRadius, float[] optics, float amount) {
        float hw = lw / 2f;
        float hh = lh / 2f;
        float cx = lx + hw;
        float cy = ly + hh;
        float r = cornerRadius < 0 ? Math.min(hw, hh) : Math.min(cornerRadius, Math.min(hw, hh));
        float[] rgb = new float[3];
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                int sxp = ox + x;
                int syp = oy + y;
                int base = pixel(src, sw, sh, sxp, syp);
                if (amount <= 0) {
                    dst[y * ow + x] = base;
                    continue;
                }
                float px = sxp + 0.5f - cx;
                float py = syp + 0.5f - cy;
                float dx = Math.abs(px) - (hw - r);
                float dy = Math.abs(py) - (hh - r);
                float ax = dx > 0 ? dx : 0;
                float ay = dy > 0 ? dy : 0;
                float outside = (float) Math.sqrt(ax * ax + ay * ay);
                float sdf = outside + Math.min(Math.max(dx, dy), 0) - r;
                // Outward normal of the rounded rectangle at this point.
                float nx;
                float ny;
                if (dx > 0 && dy > 0 && outside > 0) {
                    nx = ax / outside;
                    ny = ay / outside;
                } else if (dx > dy) {
                    nx = 1;
                    ny = 0;
                } else {
                    nx = 0;
                    ny = 1;
                }
                nx = px < 0 ? -nx : nx;
                ny = py < 0 ? -ny : ny;
                float s = -sdf;
                float outR = ((base >> 16) & 0xff) / 255f;
                float outG = ((base >> 8) & 0xff) / 255f;
                float outB = (base & 0xff) / 255f;
                if (s > 0) {
                    float d = displacement(s, optics[BEVEL], optics[MAX_SHIFT]);
                    float disp = optics[DISPERSION];
                    for (int c = 0; c < 3; c++) {
                        float k = d * (1 + (c - 1) * disp);
                        rgb[c] = channel(src, sw, sh, sxp + 0.5f + nx * k, syp + 0.5f + ny * k, c);
                    }
                    float lum = optics[BRIGHTNESS];
                    float shade = 1;
                    if (optics[END_SHADE_WIDTH] > 0) {
                        float w = optics[END_SHADE_WIDTH];
                        float band = s < w * 0.7f ? 1 : (s >= w ? 0 : smooth((w - s) / (w * 0.3f)));
                        shade -= optics[END_SHADE] * Math.abs(nx) * band;
                    }
                    float rim = 0;
                    float past = s - optics[OUTLINE_WIDTH];
                    if (optics[RIM_WIDTH] > 0 && past >= 0) {
                        rim = (optics[RIM_LIGHT] + optics[RIM_LIGHT_VERTICAL] * Math.abs(ny))
                                * (float) MathUtil.exp(-past / optics[RIM_WIDTH]);
                    }
                    float inR = rgb[0] * shade + lum + rim;
                    float inG = rgb[1] * shade + lum + rim;
                    float inB = rgb[2] * shade + lum + rim;
                    // Anti-aliased rim: blend the lens over the surface by coverage.
                    float cov = s >= 1 ? 1 : s;
                    outR = outR + (inR - outR) * cov;
                    outG = outG + (inG - outG) * cov;
                    outB = outB + (inB - outB) * cov;
                } else if (optics[SHADOW] > 0 && optics[SHADOW_WIDTH] > 0 && ny > 0) {
                    float t = (sdf - optics[SHADOW_WIDTH]) / optics[SHADOW_WIDTH];
                    float f = optics[SHADOW] * ny * (float) MathUtil.exp(-t * t);
                    outR *= 1 - f;
                    outG *= 1 - f;
                    outB *= 1 - f;
                }
                if (optics[OUTLINE_WIDTH] > 0) {
                    float half = optics[OUTLINE_WIDTH] / 2f;
                    float ring = 1 - Math.abs(s - half) / half;
                    if (ring > 0) {
                        float f = optics[OUTLINE] * (ring > 1 ? 1 : ring);
                        outR *= 1 - f;
                        outG *= 1 - f;
                        outB *= 1 - f;
                    }
                }
                float br = ((base >> 16) & 0xff) / 255f;
                float bg = ((base >> 8) & 0xff) / 255f;
                float bb = (base & 0xff) / 255f;
                outR = br + (clamp(outR) - br) * amount;
                outG = bg + (clamp(outG) - bg) * amount;
                outB = bb + (clamp(outB) - bb) * amount;
                dst[y * ow + x] = (base & 0xff000000) | (Math.round(outR * 255) << 16)
                        | (Math.round(outG * 255) << 8) | Math.round(outB * 255);
            }
        }
    }

    private static float smooth(float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static int pixel(int[] src, int sw, int sh, int x, int y) {
        x = x < 0 ? 0 : (x >= sw ? sw - 1 : x);
        y = y < 0 ? 0 : (y >= sh ? sh - 1 : y);
        return src[y * sw + x];
    }

    /// Bilinear sample of one channel (0 red, 1 green, 2 blue) at pixel-centre
    /// coordinates (x, y), edge clamped.
    private static float channel(int[] src, int sw, int sh, float x, float y, int c) {
        float fx = x - 0.5f;
        float fy = y - 0.5f;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float tx = fx - x0;
        float ty = fy - y0;
        int shift = 16 - 8 * c;
        float c00 = (pixel(src, sw, sh, x0, y0) >> shift) & 0xff;
        float c10 = (pixel(src, sw, sh, x0 + 1, y0) >> shift) & 0xff;
        float c01 = (pixel(src, sw, sh, x0, y0 + 1) >> shift) & 0xff;
        float c11 = (pixel(src, sw, sh, x0 + 1, y0 + 1) >> shift) & 0xff;
        float top = c00 + (c10 - c00) * tx;
        float bot = c01 + (c11 - c01) * tx;
        return (top + (bot - top) * ty) / 255f;
    }
}
