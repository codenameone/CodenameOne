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

import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;

import java.util.ArrayList;
import java.util.List;

/// A path flattened to straight segments in device coordinates.
///
/// Each entry of `lines` is one subpath as `x0, y0, x1, y1, ...`; `closed` says whether the
/// subpath was closed.
final class Polylines {
    final List<double[]> lines = new ArrayList<double[]>();
    final List<Boolean> closed = new ArrayList<Boolean>();
    boolean evenOdd;

    private static final int MAX_DEPTH = 16;

    private double[] cur = new double[64];
    private int n;

    static Polylines flatten(GeneralPath path, Matrix m, double tolerance) {
        Polylines out = new Polylines();
        PathIterator it = path.getPathIterator();
        out.evenOdd = it.getWindingRule() == PathIterator.WIND_EVEN_ODD;
        float[] c = new float[6];
        double lx = 0;
        double ly = 0;
        double sx = 0;
        double sy = 0;
        double tol2 = tolerance * tolerance;
        while (!it.isDone()) {
            int t = it.currentSegment(c);
            switch (t) {
                case PathIterator.SEG_MOVETO:
                    out.end(false);
                    lx = m.mapX(c[0], c[1]);
                    ly = m.mapY(c[0], c[1]);
                    sx = lx;
                    sy = ly;
                    out.add(lx, ly);
                    break;
                case PathIterator.SEG_LINETO:
                    lx = m.mapX(c[0], c[1]);
                    ly = m.mapY(c[0], c[1]);
                    out.add(lx, ly);
                    break;
                case PathIterator.SEG_QUADTO: {
                    double x1 = m.mapX(c[0], c[1]);
                    double y1 = m.mapY(c[0], c[1]);
                    double x2 = m.mapX(c[2], c[3]);
                    double y2 = m.mapY(c[2], c[3]);
                    out.cubic(lx, ly, lx + 2.0 / 3 * (x1 - lx), ly + 2.0 / 3 * (y1 - ly),
                            x2 + 2.0 / 3 * (x1 - x2), y2 + 2.0 / 3 * (y1 - y2), x2, y2, tol2, 0);
                    lx = x2;
                    ly = y2;
                    break;
                }
                case PathIterator.SEG_CUBICTO: {
                    double x3 = m.mapX(c[4], c[5]);
                    double y3 = m.mapY(c[4], c[5]);
                    out.cubic(lx, ly, m.mapX(c[0], c[1]), m.mapY(c[0], c[1]),
                            m.mapX(c[2], c[3]), m.mapY(c[2], c[3]), x3, y3, tol2, 0);
                    lx = x3;
                    ly = y3;
                    break;
                }
                case PathIterator.SEG_CLOSE:
                    out.end(true);
                    lx = sx;
                    ly = sy;
                    break;
                default:
                    break;
            }
            it.next();
        }
        out.end(false);
        return out;
    }

    private void add(double x, double y) {
        if (n > 0 && cur[n - 2] == x && cur[n - 1] == y) {
            return;
        }
        if (n + 2 > cur.length) {
            double[] nc = new double[cur.length * 2];
            System.arraycopy(cur, 0, nc, 0, n);
            cur = nc;
        }
        cur[n++] = x;
        cur[n++] = y;
    }

    private void end(boolean close) {
        if (n >= 2) {
            double[] l = new double[n];
            System.arraycopy(cur, 0, l, 0, n);
            lines.add(l);
            closed.add(Boolean.valueOf(close));
        }
        n = 0;
    }

    private void cubic(double x0, double y0, double x1, double y1, double x2, double y2,
            double x3, double y3, double tol2, int depth) {
        double dx = x3 - x0;
        double dy = y3 - y0;
        double d1 = Math.abs((x1 - x3) * dy - (y1 - y3) * dx);
        double d2 = Math.abs((x2 - x3) * dy - (y2 - y3) * dx);
        double dd = (d1 + d2) * (d1 + d2);
        if (depth >= MAX_DEPTH || dd <= tol2 * (dx * dx + dy * dy)
                || (dx * dx + dy * dy < 1e-12 && Math.abs(x1 - x0) + Math.abs(y1 - y0)
                + Math.abs(x2 - x0) + Math.abs(y2 - y0) < tol2)) {
            add(x3, y3);
            return;
        }
        double x01 = (x0 + x1) / 2;
        double y01 = (y0 + y1) / 2;
        double x12 = (x1 + x2) / 2;
        double y12 = (y1 + y2) / 2;
        double x23 = (x2 + x3) / 2;
        double y23 = (y2 + y3) / 2;
        double xa = (x01 + x12) / 2;
        double ya = (y01 + y12) / 2;
        double xb = (x12 + x23) / 2;
        double yb = (y12 + y23) / 2;
        double xm = (xa + xb) / 2;
        double ym = (ya + yb) / 2;
        cubic(x0, y0, x01, y01, xa, ya, xm, ym, tol2, depth + 1);
        cubic(xm, ym, xb, yb, x23, y23, x3, y3, tol2, depth + 1);
    }

    /// Bounds as `minX, minY, maxX, maxY`, or null when empty.
    double[] bounds() {
        double[] b = null;
        for (double[] l : lines) {
            for (int j = 0; j < l.length; j += 2) {
                if (b == null) {
                    b = new double[]{l[j], l[j + 1], l[j], l[j + 1]};
                } else {
                    b[0] = Math.min(b[0], l[j]);
                    b[1] = Math.min(b[1], l[j + 1]);
                    b[2] = Math.max(b[2], l[j]);
                    b[3] = Math.max(b[3], l[j + 1]);
                }
            }
        }
        return b;
    }

    /// True when the point is inside under this path's fill rule.
    boolean contains(double px, double py) {
        int wind = 0;
        for (double[] l : lines) {
            int cnt = l.length / 2;
            for (int j = 0; j < cnt; j++) {
                double x0 = l[j * 2];
                double y0 = l[j * 2 + 1];
                int k = (j + 1) % cnt;
                double x1 = l[k * 2];
                double y1 = l[k * 2 + 1];
                if ((y0 <= py) != (y1 <= py)) {
                    double x = x0 + (py - y0) * (x1 - x0) / (y1 - y0);
                    if (x > px) {
                        wind += y1 > y0 ? 1 : -1;
                    }
                }
            }
        }
        return evenOdd ? (wind & 1) != 0 : wind != 0;
    }
}
