/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Evaluates a {@link Gradient} per pixel into an ARGB buffer, for the effects that need
 * the shader's colour at every pixel ({@code ShaderMask}) rather than a filled shape.
 * Linear and radial gradients honour their stops; the tile mode is clamp.
 */
public final class GradientRaster {
    private GradientRaster() {
    }

    public static int[] rasterize(Gradient g, int w, int h) {
        int[] out = new int[Math.max(0, w * h)];
        int[] ramp = g.colorRamp();
        if (ramp.length == 0 || out.length == 0) {
            return out;
        }
        double[] stops = stops(g, ramp.length);
        if (g instanceof RadialGradient) {
            double[] c = point(g.center, 0, 0, w, h);
            double r = ((RadialGradient) g).radius * Math.min(w, h);
            if (r <= 0) {
                r = 1;
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double dx = x + 0.5 - c[0];
                    double dy = y + 0.5 - c[1];
                    out[y * w + x] = sample(ramp, stops, Math.sqrt(dx * dx + dy * dy) / r);
                }
            }
            return out;
        }
        double[] a = point(g.begin, -1, 0, w, h);
        double[] b = point(g.end, 1, 0, w, h);
        double vx = b[0] - a[0];
        double vy = b[1] - a[1];
        double len2 = vx * vx + vy * vy;
        if (len2 == 0) {
            len2 = 1;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double t = ((x + 0.5 - a[0]) * vx + (y + 0.5 - a[1]) * vy) / len2;
                out[y * w + x] = sample(ramp, stops, t);
            }
        }
        return out;
    }

    private static double[] point(Object alignment, double dx, double dy, int w, int h) {
        double ax = dx;
        double ay = dy;
        if (alignment instanceof Alignment) {
            ax = ((Alignment) alignment).x();
            ay = ((Alignment) alignment).y();
        }
        return new double[] {(ax + 1) / 2 * w, (ay + 1) / 2 * h};
    }

    private static double[] stops(Gradient g, int n) {
        double[] s = new double[n];
        if (g.stops instanceof java.util.List && ((java.util.List<?>) g.stops).size() == n) {
            java.util.List<?> l = (java.util.List<?>) g.stops;
            for (int i = 0; i < n; i++) {
                Object o = l.get(i);
                s[i] = o instanceof Number ? ((Number) o).doubleValue() : (n == 1 ? 0 : i / (double) (n - 1));
            }
            return s;
        }
        for (int i = 0; i < n; i++) {
            s[i] = n == 1 ? 0 : i / (double) (n - 1);
        }
        return s;
    }

    private static int sample(int[] ramp, double[] stops, double t) {
        if (t <= stops[0]) {
            return ramp[0];
        }
        int last = ramp.length - 1;
        if (t >= stops[last]) {
            return ramp[last];
        }
        for (int i = 1; i <= last; i++) {
            if (t <= stops[i]) {
                double span = stops[i] - stops[i - 1];
                double f = span <= 0 ? 1 : (t - stops[i - 1]) / span;
                return lerp(ramp[i - 1], ramp[i], f);
            }
        }
        return ramp[last];
    }

    private static int lerp(int c0, int c1, double f) {
        int r = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = (c0 >>> shift) & 0xff;
            int b = (c1 >>> shift) & 0xff;
            r |= ((int) Math.round(a + (b - a) * f) & 0xff) << shift;
        }
        return r;
    }
}
