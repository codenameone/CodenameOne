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

import java.util.List;

/// Turns a flattened path into the outline of its stroke.
///
/// Every segment becomes a quad, and every join and cap its own polygon. All of them are
/// oriented the same way and filled with the non-zero rule, so their union is the stroke.
final class Stroker {
    private Stroker() {
    }

    static Polylines stroke(Polylines in, StrokeStyle st, double scale) {
        double hw = st.getWidth() * scale / 2;
        Polylines out = new Polylines();
        out.evenOdd = false;
        if (hw <= 0) {
            return out;
        }
        Polylines src = in;
        float[] dash = st.getDash();
        if (dash != null && dash.length > 0) {
            src = dash(in, dash, st.getDashOffset(), scale);
        }
        int join = st.getJoin();
        int cap = st.getCap();
        double miter = st.getMiterLimit();
        for (int li = 0; li < src.lines.size(); li++) {
            double[] l = src.lines.get(li);
            boolean closed = src.closed.get(li).booleanValue();
            int cnt = l.length / 2;
            if (cnt == 1) {
                if (cap == StrokeStyle.CAP_ROUND) {
                    circle(out, l[0], l[1], hw);
                } else if (cap == StrokeStyle.CAP_SQUARE) {
                    poly(out, new double[]{l[0] - hw, l[1] - hw, l[0] + hw, l[1] - hw,
                            l[0] + hw, l[1] + hw, l[0] - hw, l[1] + hw});
                }
                continue;
            }
            int segs = closed ? cnt : cnt - 1;
            for (int i = 0; i < segs; i++) {
                int j = (i + 1) % cnt;
                double x0 = l[i * 2];
                double y0 = l[i * 2 + 1];
                double x1 = l[j * 2];
                double y1 = l[j * 2 + 1];
                double dx = x1 - x0;
                double dy = y1 - y0;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len == 0) {
                    continue;
                }
                double nx = -dy / len * hw;
                double ny = dx / len * hw;
                double ex0 = x0;
                double ey0 = y0;
                double ex1 = x1;
                double ey1 = y1;
                if (!closed && cap == StrokeStyle.CAP_SQUARE) {
                    if (i == 0) {
                        ex0 -= dx / len * hw;
                        ey0 -= dy / len * hw;
                    }
                    if (i == segs - 1) {
                        ex1 += dx / len * hw;
                        ey1 += dy / len * hw;
                    }
                }
                poly(out, new double[]{ex0 + nx, ey0 + ny, ex1 + nx, ey1 + ny,
                        ex1 - nx, ey1 - ny, ex0 - nx, ey0 - ny});
            }
            int firstJoin = closed ? 0 : 1;
            int lastJoin = closed ? cnt : cnt - 1;
            for (int i = firstJoin; i < lastJoin; i++) {
                int p = (i - 1 + cnt) % cnt;
                int q = (i + 1) % cnt;
                joint(out, l[p * 2], l[p * 2 + 1], l[i * 2], l[i * 2 + 1], l[q * 2], l[q * 2 + 1],
                        hw, join, miter);
            }
            if (!closed && cap == StrokeStyle.CAP_ROUND) {
                circle(out, l[0], l[1], hw);
                circle(out, l[(cnt - 1) * 2], l[(cnt - 1) * 2 + 1], hw);
            }
        }
        return out;
    }

    private static void joint(Polylines out, double ax, double ay, double bx, double by,
            double cx, double cy, double hw, int join, double miterLimit) {
        if (join == StrokeStyle.JOIN_ROUND) {
            circle(out, bx, by, hw);
            return;
        }
        double d1x = bx - ax;
        double d1y = by - ay;
        double d2x = cx - bx;
        double d2y = cy - by;
        double l1 = Math.sqrt(d1x * d1x + d1y * d1y);
        double l2 = Math.sqrt(d2x * d2x + d2y * d2y);
        if (l1 == 0 || l2 == 0) {
            return;
        }
        double cross = d1x * d2y - d1y * d2x;
        double side = cross > 0 ? -1 : 1;
        double n1x = -d1y / l1 * hw * side;
        double n1y = d1x / l1 * hw * side;
        double n2x = -d2y / l2 * hw * side;
        double n2y = d2x / l2 * hw * side;
        if (join == StrokeStyle.JOIN_MITER) {
            double cos = (d1x * d2x + d1y * d2y) / (l1 * l2);
            double ratio = 1 / Math.sqrt(Math.max(1e-12, (1 + cos) / 2));
            if (ratio <= miterLimit) {
                double mx = n1x + n2x;
                double my = n1y + n2y;
                double ml = Math.sqrt(mx * mx + my * my);
                if (ml > 0) {
                    double len = hw * ratio;
                    poly(out, new double[]{bx, by, bx + n1x, by + n1y,
                            bx + mx / ml * len, by + my / ml * len, bx + n2x, by + n2y});
                    return;
                }
            }
        }
        poly(out, new double[]{bx, by, bx + n1x, by + n1y, bx + n2x, by + n2y});
    }

    private static void circle(Polylines out, double cx, double cy, double r) {
        int segs = Math.max(8, Math.min(128, (int) (r * 2)));
        double[] p = new double[segs * 2];
        for (int i = 0; i < segs; i++) {
            double a = Math.PI * 2 * i / segs;
            p[i * 2] = cx + Math.cos(a) * r;
            p[i * 2 + 1] = cy + Math.sin(a) * r;
        }
        poly(out, p);
    }

    private static void poly(Polylines out, double[] p) {
        double area = 0;
        int n = p.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
        }
        if (area < 0) {
            double[] r = new double[p.length];
            for (int i = 0; i < n; i++) {
                r[i * 2] = p[(n - 1 - i) * 2];
                r[i * 2 + 1] = p[(n - 1 - i) * 2 + 1];
            }
            p = r;
        }
        out.lines.add(p);
        out.closed.add(Boolean.TRUE);
    }

    static Polylines dash(Polylines in, float[] dash, float offset, double scale) {
        Polylines out = new Polylines();
        double total = 0;
        for (int i = 0; i < dash.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            total += Math.max(0, dash[i]) * scale;
        }
        if (total <= 0) {
            return in;
        }
        List<double[]> lines = in.lines;
        for (int li = 0; li < lines.size(); li++) {
            double[] l = lines.get(li);
            boolean closed = in.closed.get(li).booleanValue();
            int cnt = l.length / 2;
            int segs = closed ? cnt : cnt - 1;
            double pos = (offset * scale) % total;
            if (pos < 0) {
                pos += total;
            }
            int di = 0;
            while (pos >= dash[di] * scale) {
                pos -= dash[di] * scale;
                di = (di + 1) % dash.length;
            }
            double remain = dash[di] * scale - pos;
            boolean on = di % 2 == 0;
            double[] buf = new double[16];
            int bn = 0;
            if (on && cnt > 0) {
                buf[bn++] = l[0];
                buf[bn++] = l[1];
            }
            for (int i = 0; i < segs; i++) {
                int j = (i + 1) % cnt;
                double x0 = l[i * 2];
                double y0 = l[i * 2 + 1];
                double x1 = l[j * 2];
                double y1 = l[j * 2 + 1];
                double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
                double t = 0;
                int guard = 0;
                while (len - t > remain && guard++ < 100000) {
                    t += remain;
                    double px = x0 + (x1 - x0) * t / len;
                    double py = y0 + (y1 - y0) * t / len;
                    if (on) {
                        buf = push(buf, bn, px, py);
                        bn += 2;
                        emit(out, buf, bn);
                        bn = 0;
                    } else {
                        buf = push(buf, 0, px, py);
                        bn = 2;
                    }
                    on = !on;
                    di = (di + 1) % dash.length;
                    remain = Math.max(1e-6, dash[di] * scale);
                }
                remain -= len - t;
                if (on) {
                    buf = push(buf, bn, x1, y1);
                    bn += 2;
                }
            }
            if (on) {
                emit(out, buf, bn);
            }
        }
        return out;
    }

    private static double[] push(double[] buf, int bn, double x, double y) {
        if (bn + 2 > buf.length) {
            double[] nb = new double[buf.length * 2];
            System.arraycopy(buf, 0, nb, 0, bn);
            buf = nb;
        }
        buf[bn] = x;
        buf[bn + 1] = y;
        return buf;
    }

    private static void emit(Polylines out, double[] buf, int bn) {
        if (bn >= 4) {
            double[] l = new double[bn];
            System.arraycopy(buf, 0, l, 0, bn);
            out.lines.add(l);
            out.closed.add(Boolean.FALSE);
        }
    }
}
