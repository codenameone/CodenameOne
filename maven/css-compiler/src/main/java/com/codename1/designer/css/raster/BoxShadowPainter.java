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

/// Paints a CSS `box-shadow`, outer or inset.
///
/// Both kinds blur a coverage mask and tint it. Only the alpha of the shape
/// is blurred, which is the same as blurring a premultiplied image of one
/// colour and avoids its rounding. The gaussian's standard deviation is
/// half the CSS blur radius, as the specification defines it.
///
/// The mask is drawn into a buffer larger than the image by the reach of
/// the blur, so a shadow shape that runs off the image still contributes
/// correctly to the pixels inside it, and a pad smaller than the halo just
/// crops the halo.
public final class BoxShadowPainter {
    private BoxShadowPainter() {
    }

    /// Paints an outer shadow.
    ///
    /// The shadow shape is the border box grown by the spread and moved by
    /// the offset. CSS does not draw an outer shadow under the box that
    /// casts it, so the shadow is multiplied by the inverse of the border
    /// box coverage: a translucent or transparent background does not show
    /// the shadow through it.
    ///
    /// #### Parameters
    ///
    /// - `dst`: the image, `w * h` non-premultiplied ARGB values
    ///
    /// - `w`: image width
    ///
    /// - `h`: image height
    ///
    /// - `borderBox`: the border box in image coordinates
    ///
    /// - `borderCoverage`: the coverage mask of `borderBox`, `w * h` values
    ///
    /// - `shadow`: the shadow, not inset
    public static void paintOuter(int[] dst, int w, int h, RoundedBox borderBox, float[] borderCoverage,
            Shadow shadow) {
        if ((shadow.color >>> 24) == 0) {
            return;
        }
        RoundedBox shape = borderBox.grow(shadow.spread).translate(shadow.offsetX, shadow.offsetY);
        if (shape.isEmpty()) {
            return;
        }
        double sigma = Math.max(0, shadow.blur) / 2;
        int m = GaussianBlur.radius(sigma) + 1;
        int bw = w + 2 * m;
        int bh = h + 2 * m;
        float[] cov = Pixels.coverage(shape.toPath(), bw, bh, m, m);
        GaussianBlur.blur(cov, bw, bh, sigma);
        for (int y = 0; y < h; y++) {
            int src = (y + m) * bw + m;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                float a = cov[src + x] * (1f - borderCoverage[row + x]);
                if (a > 0f) {
                    Pixels.blend(dst, row + x, shadow.color, a);
                }
            }
        }
    }

    /// Paints an inset shadow.
    ///
    /// The shadow is everything outside a hole: the padding box shrunk by
    /// the spread and moved by the offset. Blurring "everything outside"
    /// needs no infinite buffer, because the blur of an inverted mask is the
    /// inverse of the blurred mask. The result is confined to the rounded
    /// padding box.
    ///
    /// #### Parameters
    ///
    /// - `dst`: the image, `w * h` non-premultiplied ARGB values
    ///
    /// - `w`: image width
    ///
    /// - `h`: image height
    ///
    /// - `paddingBox`: the padding box in image coordinates
    ///
    /// - `shadow`: the shadow, inset
    public static void paintInset(int[] dst, int w, int h, RoundedBox paddingBox, Shadow shadow) {
        if ((shadow.color >>> 24) == 0 || paddingBox.isEmpty()) {
            return;
        }
        float[] clip = Pixels.coverage(paddingBox.toPath(), w, h, 0, 0);
        RoundedBox hole = paddingBox.grow(-shadow.spread).translate(shadow.offsetX, shadow.offsetY);
        double sigma = Math.max(0, shadow.blur) / 2;
        int m = GaussianBlur.radius(sigma) + 1;
        int bw = w + 2 * m;
        int bh = h + 2 * m;
        float[] cov;
        if (hole.isEmpty()) {
            cov = new float[bw * bh];
        } else {
            cov = Pixels.coverage(hole.toPath(), bw, bh, m, m);
            GaussianBlur.blur(cov, bw, bh, sigma);
        }
        for (int y = 0; y < h; y++) {
            int src = (y + m) * bw + m;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                float c = clip[row + x];
                if (c > 0f) {
                    float a = (1f - cov[src + x]) * c;
                    if (a > 0f) {
                        Pixels.blend(dst, row + x, shadow.color, a);
                    }
                }
            }
        }
    }
}
