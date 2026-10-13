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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/// Pixel helpers shared by the painters: antialiased coverage masks and
/// source-over blending on non-premultiplied ARGB.
///
/// Every layer in this package is composited as "colour times coverage
/// mask", rather than through a `Graphics2D` clip, because a Java2D clip is
/// not antialiased and would leave a jagged edge on every rounded corner.
final class Pixels {
    private Pixels() {
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /// The antialiased coverage, 0 to 1 per pixel, of a shape drawn into a
    /// `w` by `h` buffer after translating it by `(dx, dy)`.
    static float[] coverage(Shape s, int w, int h, double dx, double dy) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            hints(g);
            g.translate(dx, dy);
            g.setColor(Color.WHITE);
            g.fill(s);
        } finally {
            g.dispose();
        }
        int[] d = img.getRGB(0, 0, w, h, null, 0, w);
        float[] out = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            int a = d[i] >>> 24;
            if (a != 0) {
                out[i] = a == 255 ? 1f : a / 255f;
            }
        }
        return out;
    }

    /// Blends `argb`, scaled by `cov`, over pixel `i` of `dst`.
    static void blend(int[] dst, int i, int argb, float cov) {
        float sa = (argb >>> 24) / 255f * cov;
        if (sa <= 0f) {
            return;
        }
        int d = dst[i];
        int da8 = d >>> 24;
        if (da8 == 0) {
            dst[i] = (clamp8(sa * 255f) << 24) | (argb & 0xffffff);
            return;
        }
        if (sa >= 1f) {
            dst[i] = argb | 0xff000000;
            return;
        }
        float da = da8 / 255f * (1f - sa);
        float oa = sa + da;
        float r = (((argb >> 16) & 0xff) * sa + ((d >> 16) & 0xff) * da) / oa;
        float g = (((argb >> 8) & 0xff) * sa + ((d >> 8) & 0xff) * da) / oa;
        float b = ((argb & 0xff) * sa + (d & 0xff) * da) / oa;
        dst[i] = (clamp8(oa * 255f) << 24) | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    /// Blends one colour over `dst` through a coverage mask.
    static void fill(int[] dst, int argb, float[] mask) {
        if ((argb >>> 24) == 0) {
            return;
        }
        for (int i = 0; i < dst.length; i++) {
            float m = mask[i];
            if (m > 0f) {
                blend(dst, i, argb, m);
            }
        }
    }

    /// Blends a layer of non-premultiplied ARGB over `dst` through a
    /// coverage mask.
    static void layer(int[] dst, int[] src, float[] mask) {
        for (int i = 0; i < dst.length; i++) {
            float m = mask[i];
            if (m > 0f && (src[i] >>> 24) != 0) {
                blend(dst, i, src[i], m);
            }
        }
    }

    static int clamp8(float v) {
        int i = Math.round(v);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }
}
