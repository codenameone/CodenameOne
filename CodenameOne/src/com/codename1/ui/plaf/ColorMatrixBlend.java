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

/// Reference implementation of Graphics.colorMatrixRegion over an ARGB buffer.
///
/// Every pixel `p` (channels in 0..1) becomes `p + (clamp(M * p + offset) - p) * k`,
/// where `k = amount * shape * mask`:
///
/// - `shape` is the anti-aliased coverage of a rounded rectangle filling the
///   region, `clamp(0.5 - sdf, 0, 1)` at the pixel centre, with corner radius
///   `cornerRadius` (a negative radius makes a capsule; zero covers the whole
///   rectangle);
/// - `mask` is the alpha of the optional mask sampled at the same relative
///   position (nearest pixel), which is how vibrant content is drawn: the mask
///   holds the glyphs, and each glyph pixel takes its colour from the matrix
///   applied to what lies behind it.
///
/// The destination alpha is kept. Ports without a native path (JavaSE, the
/// JavaScript port, iOS off-screen images) call this; the Metal shader
/// cn1_fs_colormatrix mirrors it.
public final class ColorMatrixBlend {
    private ColorMatrixBlend() {
    }

    /// Applies the blend in place.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the destination region, `w * h` pixels, row major
    ///
    /// - `w`: region width in pixels
    ///
    /// - `h`: region height in pixels
    ///
    /// - `matrix`: 12 floats, rows r, g, b of `[r, g, b, offset]` in 0..1 units
    ///
    /// - `mask`: the mask pixels (ARGB, alpha used), or null for no mask
    ///
    /// - `maskW`: mask width in pixels
    ///
    /// - `maskH`: mask height in pixels
    ///
    /// - `cornerRadius`: corner radius in region pixels, negative for a capsule
    ///
    /// - `amount`: overall strength 0..1
    public static void apply(int[] argb, int w, int h, float[] matrix, int[] mask, int maskW, int maskH,
            float cornerRadius, float amount) {
        if (amount <= 0 || w <= 0 || h <= 0) {
            return;
        }
        float hw = w / 2f;
        float hh = h / 2f;
        float r = 0;
        if (cornerRadius != 0) {
            r = cornerRadius < 0 ? Math.min(hw, hh) : Math.min(cornerRadius, Math.min(hw, hh));
        }
        for (int y = 0; y < h; y++) {
            float py = y + 0.5f - hh;
            for (int x = 0; x < w; x++) {
                float k = amount;
                if (r > 0) {
                    float px = x + 0.5f - hw;
                    float dx = Math.abs(px) - (hw - r);
                    float dy = Math.abs(py) - (hh - r);
                    float ax = dx > 0 ? dx : 0;
                    float ay = dy > 0 ? dy : 0;
                    float sdf = (float) Math.sqrt(ax * ax + ay * ay) + Math.min(Math.max(dx, dy), 0) - r;
                    float cov = 0.5f - sdf;
                    k *= cov < 0 ? 0 : (cov > 1 ? 1 : cov);
                }
                if (mask != null) {
                    int mx = x * maskW / w;
                    int my = y * maskH / h;
                    k *= ((mask[my * maskW + mx] >>> 24) & 0xff) / 255f;
                }
                if (k <= 0) {
                    continue;
                }
                int i = y * w + x;
                int p = argb[i];
                float pr = ((p >> 16) & 0xff) / 255f;
                float pg = ((p >> 8) & 0xff) / 255f;
                float pb = (p & 0xff) / 255f;
                int out = p & 0xff000000;
                for (int row = 0; row < 3; row++) {
                    float v = matrix[row * 4] * pr + matrix[row * 4 + 1] * pg + matrix[row * 4 + 2] * pb
                            + matrix[row * 4 + 3];
                    v = v < 0 ? 0 : (v > 1 ? 1 : v);
                    float src = row == 0 ? pr : (row == 1 ? pg : pb);
                    float o = src + (v - src) * k;
                    out |= Math.round(o * 255) << (16 - 8 * row);
                }
                argb[i] = out;
            }
        }
    }
}
