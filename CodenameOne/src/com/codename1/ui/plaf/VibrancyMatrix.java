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

/// The colour matrix iOS uses to draw VIBRANT content over glass -- tab bar
/// icons and labels, among others.
///
/// A vibrant glyph is not painted in its tint. Each of its pixels is a colour
/// transform of whatever lies BEHIND it: `out = clamp(M * backdrop + offset)`,
/// with the glyph's coverage deciding how much of `out` replaces the backdrop
/// (Graphics.colorMatrixRegion paints exactly that). The tint only chooses the
/// matrix. That is why the selected tab of an iOS tab bar is a different blue
/// over every backdrop, and far more vivid than the plain accent in dark mode.
///
/// MEASURED, NOT APPROXIMATED BY EYE. The matrices were read from UIKit itself: the
/// motion probe (scripts/fidelity-app/ios-native-ref/motion-probe) sets a tab
/// bar's tint to thousands of colours on the iOS 27 simulator and reads back the
/// `vibrantColorMatrix` filter UIKit installs on the item layers. They follow closed
/// forms exactly, with `t` the tint, `d = 1 - t` and `e = 0.5 * min(t) / max(t)`:
///
/// - dark: `M = e * I + (0.5 - e) * 1 * t^T / |t|^2`, `offset = t`
/// - light: `M = e * I + 0.3 * (0.5 - e) * (1 + t) * d^T / |d|^2`,
///   `offset = alpha * (1 + t) - 1 + e * t` with
///   `alpha = 1 - e - 0.3 * (0.5 - e) * sum(d) / |d|^2`, which is what makes a
///   white backdrop come out as exactly `t`
/// - grey tints `g` (both appearances): `M = 5/16 * I / q`,
///   `offset = (19/16 * g - 1/4) / q` with `q = 1 + 0.05 * g * (1 - g)`
///
/// Against every matrix the probe logged (7024 distinct light tints, 1354 dark, and
/// the grey levels of both) the worst entry is off by 5e-6, the precision of the
/// log itself.
/// `tabmotion.py vibrancy` re-checks them against new captures.
public final class VibrancyMatrix {

    private VibrancyMatrix() {
    }

    /// The vibrancy matrix for content tinted `rgb`, as 12 floats: three rows
    /// (red, green, blue) of `[r, g, b, offset]`, in 0..1 units, the layout
    /// Graphics.colorMatrixRegion takes.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the tint, 0xRRGGBB
    ///
    /// - `dark`: true for the dark appearance
    ///
    /// #### Returns
    ///
    /// a new 12-element matrix
    public static float[] forTint(int rgb, boolean dark) {
        double[] t = {((rgb >> 16) & 0xff) / 255.0, ((rgb >> 8) & 0xff) / 255.0, (rgb & 0xff) / 255.0};
        double mx = Math.max(t[0], Math.max(t[1], t[2]));
        double mn = Math.min(t[0], Math.min(t[1], t[2]));
        float[] m = new float[12];
        if (mx - mn < 1e-9) {
            double q = 1 + 0.05 * mx * (1 - mx);
            double gain = 5.0 / 16 / q;
            double off = (19.0 / 16 * mx - 0.25) / q;
            for (int r = 0; r < 3; r++) {
                m[r * 4 + r] = (float) gain;
                m[r * 4 + 3] = (float) off;
            }
            return m;
        }
        double e = 0.5 * mn / mx;
        if (dark) {
            double tt = t[0] * t[0] + t[1] * t[1] + t[2] * t[2];
            double a = 0.5 - e;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    m[r * 4 + c] = (float) ((r == c ? e : 0) + a * t[c] / tt);
                }
                m[r * 4 + 3] = (float) t[r];
            }
            return m;
        }
        double sum = 0;
        double sq = 0;
        for (int i = 0; i < 3; i++) {
            double d = 1 - t[i];
            sum += d;
            sq += d * d;
        }
        double c = 0.3 * (0.5 - e) / sq;
        double alpha = 1 - e - c * sum;
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 3; col++) {
                m[r * 4 + col] = (float) (c * (1 + t[r]) * (1 - t[col]) + (r == col ? e : 0));
            }
            m[r * 4 + 3] = (float) (alpha * (1 + t[r]) - 1 + e * t[r]);
        }
        return m;
    }

    /// Applies a 12-float colour matrix to one 0xRRGGBB colour (clamped), the
    /// per-pixel operation of Graphics.colorMatrixRegion at full coverage.
    ///
    /// #### Parameters
    ///
    /// - `m`: the matrix, as returned by `#forTint(int, boolean)`
    ///
    /// - `rgb`: the colour, 0xRRGGBB
    ///
    /// #### Returns
    ///
    /// the transformed colour, 0xRRGGBB
    public static int apply(float[] m, int rgb) {
        float r = ((rgb >> 16) & 0xff) / 255f;
        float g = ((rgb >> 8) & 0xff) / 255f;
        float b = (rgb & 0xff) / 255f;
        int out = 0;
        for (int row = 0; row < 3; row++) {
            float v = m[row * 4] * r + m[row * 4 + 1] * g + m[row * 4 + 2] * b + m[row * 4 + 3];
            int iv = Math.round((v < 0 ? 0 : (v > 1 ? 1 : v)) * 255);
            out = (out << 8) | iv;
        }
        return out;
    }
}
