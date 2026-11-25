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

import java.util.Arrays;

/// Scan converts flattened paths into anti-aliased coverage.
///
/// Each pixel row is sampled on four sub-scanlines; along each sub-scanline coverage is
/// exact, so edges are smooth in both directions. Both fill rules are supported.
final class Rasterizer {
    private static final int SUB = 4;

    private Rasterizer() {
    }

    /// Fills `p` and returns its coverage, clipped to `clip` (x, y, w, h).
    static Mask fill(Polylines p, int[] clip) {
        double[] b = p.bounds();
        if (b == null) {
            return new Mask(clip[0], clip[1], 0, 0);
        }
        int x0 = Math.max(clip[0], (int) Math.floor(b[0]));
        int y0 = Math.max(clip[1], (int) Math.floor(b[1]));
        int x1 = Math.min(clip[0] + clip[2], (int) Math.ceil(b[2]) + 1);
        int y1 = Math.min(clip[1] + clip[3], (int) Math.ceil(b[3]) + 1);
        if (x1 <= x0 || y1 <= y0) {
            return new Mask(x0, y0, 0, 0);
        }
        Mask m = new Mask(x0, y0, x1 - x0, y1 - y0);
        int ec = 0;
        for (int i = 0; i < p.lines.size(); i++) {
            ec += p.lines.get(i).length / 2;
        }
        double[] ex0 = new double[ec];
        double[] ey0 = new double[ec];
        double[] ex1 = new double[ec];
        double[] ey1 = new double[ec];
        int[] dir = new int[ec];
        int n = 0;
        for (int i = 0; i < p.lines.size(); i++) {
            double[] l = p.lines.get(i);
            int cnt = l.length / 2;
            for (int j = 0; j < cnt; j++) {
                int k = (j + 1) % cnt;
                double ay = l[j * 2 + 1];
                double by = l[k * 2 + 1];
                if (ay == by) {
                    continue;
                }
                if (ay < by) {
                    ex0[n] = l[j * 2];
                    ey0[n] = ay;
                    ex1[n] = l[k * 2];
                    ey1[n] = by;
                    dir[n] = 1;
                } else {
                    ex0[n] = l[k * 2];
                    ey0[n] = by;
                    ex1[n] = l[j * 2];
                    ey1[n] = ay;
                    dir[n] = -1;
                }
                n++;
            }
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = Integer.valueOf(i);
        }
        final double[] sortKey = ey0;
        Arrays.sort(order, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(sortKey[o1.intValue()], sortKey[o2.intValue()]);
            }
        });
        int[] active = new int[n];
        int activeN = 0;
        int next = 0;
        double[] xs = new double[n];
        int[] ds = new int[n];
        float[] acc = new float[m.w + 2];
        int w = m.w;
        for (int row = y0; row < y1; row++) {
            Arrays.fill(acc, 0);
            boolean any = false;
            for (int s = 0; s < SUB; s++) {
                double sy = row + (s + 0.5) / SUB;
                while (next < n && ey0[order[next].intValue()] <= sy) {
                    active[activeN++] = order[next].intValue();
                    next++;
                }
                int cn = 0;
                for (int i = 0; i < activeN; i++) {
                    int e = active[i];
                    if (ey1[e] <= sy) {
                        active[i] = active[--activeN];
                        i--;
                        continue;
                    }
                    if (ey0[e] > sy) {
                        continue;
                    }
                    xs[cn] = ex0[e] + (sy - ey0[e]) * (ex1[e] - ex0[e]) / (ey1[e] - ey0[e]);
                    ds[cn] = dir[e];
                    cn++;
                }
                if (cn < 2) {
                    continue;
                }
                sortCrossings(xs, ds, cn);
                int wind = 0;
                for (int i = 0; i < cn - 1; i++) {
                    wind += ds[i];
                    boolean inside = p.evenOdd ? (wind & 1) != 0 : wind != 0;
                    if (inside) {
                        any = true;
                        span(acc, xs[i] - x0, xs[i + 1] - x0, w);
                    }
                }
            }
            if (!any) {
                continue;
            }
            int base = (row - y0) * w;
            for (int i = 0; i < w; i++) {
                float v = acc[i] / SUB;
                if (v > 0) {
                    int c = (int) (v * 255 + 0.5f);
                    m.a[base + i] = (byte) (c > 255 ? 255 : c);
                }
            }
        }
        return m;
    }

    private static void span(float[] acc, double a, double b, int w) {
        if (b <= 0 || a >= w) {
            return;
        }
        if (a < 0) {
            a = 0;
        }
        if (b > w) {
            b = w;
        }
        int ia = (int) a;
        int ib = (int) b;
        if (ia == ib) {
            acc[ia] += (float) (b - a);
            return;
        }
        acc[ia] += (float) (ia + 1 - a);
        for (int i = ia + 1; i < ib; i++) {
            acc[i] += 1;
        }
        if (ib < w) {
            acc[ib] += (float) (b - ib);
        }
    }

    private static void sortCrossings(double[] xs, int[] ds, int n) {
        for (int i = 1; i < n; i++) {
            double x = xs[i];
            int d = ds[i];
            int j = i - 1;
            while (j >= 0 && xs[j] > x) {
                xs[j + 1] = xs[j];
                ds[j + 1] = ds[j];
                j--;
            }
            xs[j + 1] = x;
            ds[j + 1] = d;
        }
    }
}
