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
package com.codename1.impl.pdf;

/// Rasterizes mesh shadings: free-form and lattice Gouraud triangles
/// (types 4 and 5) and Coons and tensor-product patches (types 6 and 7),
/// into an ARGB buffer covering part of the device.
final class PdfMesh {
    private final int sw;
    private final int sh;
    private final double x0;
    private final double y0;
    private final double step;
    private final double[] m;
    private final PdfColorSpace cs;
    private final PdfFunction[] fn;
    private final int alpha;
    private final int[] lut;

    // stream reading
    private byte[] d;
    private long bitPos;
    private int bpc;
    private int bpComp;
    private int bpFlag;
    private double[] decode;
    private int ncomp;

    private final int[] px; // NOPMD UnusedPrivateField - written per pixel in triangle()

    private PdfMesh(int[] px, int sw, int sh, double x0, double y0, double step, double[] m, PdfColorSpace cs,
            PdfFunction[] fn, int alpha) {
        this.px = px;
        this.sw = sw;
        this.sh = sh;
        this.x0 = x0;
        this.y0 = y0;
        this.step = step;
        this.m = m;
        this.cs = cs;
        this.fn = fn;
        this.alpha = alpha;
        if (fn != null) {
            lut = new int[256];
        } else {
            lut = null;
        }
    }

    /// Draws the mesh shading `s` of `type`, mapped to the device by `m`,
    /// into `px` (`sw` by `sh` cells of `step` device pixels starting at
    /// `(x0, y0)`), with alpha `alpha` (0-255).
    static void draw(PdfStream s, int type, PdfColorSpace cs, PdfFunction[] fn, double[] m, int[] px, int sw,
            int sh, double x0, double y0, double step, int alpha) {
        PdfMesh mesh = new PdfMesh(px, sw, sh, x0, y0, step, m, cs, fn, alpha);
        try {
            mesh.run(s, type);
        } catch (java.io.IOException e) {
            // an unreadable mesh draws nothing
        } catch (RuntimeException e) {
            // a malformed mesh draws what it could
        }
    }

    private void run(PdfStream s, int type) throws java.io.IOException {
        d = s.getData();
        bpc = s.getInt("BitsPerCoordinate", 16);
        bpComp = s.getInt("BitsPerComponent", 8);
        bpFlag = s.getInt("BitsPerFlag", 8);
        ncomp = fn != null ? 1 : cs.getComponents();
        java.util.List<Object> dl = s.getArray("Decode");
        decode = new double[4 + 2 * ncomp];
        for (int i = 0; i < decode.length; i++) {
            Object v = dl != null && i < dl.size() ? dl.get(i) : null;
            decode[i] = v instanceof Number ? ((Number) v).doubleValue() : (i % 2 == 0 ? 0 : 1);
        }
        if (fn != null) {
            double ta = decode[4];
            double tb = decode[5];
            for (int i = 0; i < 256; i++) {
                lut[i] = rgbOf(new double[] {ta + (tb - ta) * i / 255.0});
            }
        }
        if (bpc < 1 || bpc > 32 || bpComp < 1 || bpComp > 16) {
            return;
        }
        if (type == 4) {
            freeForm();
        } else if (type == 5) {
            // A row cannot hold more vertices than the data has bits for.
            lattice(Math.max(2, Math.min(s.getInt("VerticesPerRow", 2), d.length * 8 + 2)));
        } else {
            patches(type == 7);
        }
    }

    // ---- stream ----

    private boolean more() {
        return (bitPos >> 3) < d.length;
    }

    private long bits(int n) {
        long v = 0;
        for (int i = 0; i < n; i++) {
            long b = bitPos + i;
            int idx = (int) (b >> 3);
            int bit = idx < d.length ? (d[idx] >> (7 - (int) (b & 7))) & 1 : 0;
            v = (v << 1) | bit;
        }
        bitPos += n;
        return v;
    }

    private void align() {
        bitPos = (bitPos + 7) & ~7L;
    }

    private double value(int n, double lo, double hi) {
        long raw = bits(n);
        double max = (double) ((1L << n) - 1);
        return lo + raw * (hi - lo) / max;
    }

    /// Reads a point into `xy[o]`, `xy[o + 1]` in device space.
    private void point(double[] xy, int o) {
        double x = value(bpc, decode[0], decode[1]);
        double y = value(bpc, decode[2], decode[3]);
        xy[o] = m[0] * x + m[2] * y + m[4];
        xy[o + 1] = m[1] * x + m[3] * y + m[5];
    }

    /// Reads a color into `c[o .. o + ncomp)`.
    private void color(double[] c, int o) {
        for (int i = 0; i < ncomp; i++) {
            c[o + i] = value(bpComp, decode[4 + 2 * i], decode[5 + 2 * i]);
        }
    }

    private int rgbOf(double[] c) {
        if (fn == null) {
            return cs.toRgb(c);
        }
        double[] out;
        if (fn.length == 1) {
            out = fn[0].eval(c);
        } else {
            out = new double[fn.length];
            for (int i = 0; i < fn.length; i++) {
                out[i] = fn[i].eval(c)[0];
            }
        }
        return cs.toRgb(out);
    }

    // ---- triangles ----

    private void freeForm() {
        double[] xy = new double[6];
        double[] c = new double[3 * ncomp];
        int have = 0;
        while (more()) {
            int flag = (int) bits(bpFlag);
            double[] pxy = new double[2];
            double[] pc = new double[ncomp];
            point(pxy, 0);
            color(pc, 0);
            align();
            if (flag == 0 || have < 2) {
                int k = have < 3 ? have : 0;
                if (flag == 0 && have >= 3) {
                    have = 0;
                    k = 0;
                }
                xy[k * 2] = pxy[0];
                xy[k * 2 + 1] = pxy[1];
                System.arraycopy(pc, 0, c, k * ncomp, ncomp);
                have++;
                if (have == 3) {
                    triangle(xy, c);
                }
                continue;
            }
            if (flag == 1) {
                // (vb, vc, new)
                shift(xy, c, 1);
            } else {
                // (va, vc, new)
                xy[2] = xy[4];
                xy[3] = xy[5];
                System.arraycopy(c, 2 * ncomp, c, ncomp, ncomp);
            }
            xy[4] = pxy[0];
            xy[5] = pxy[1];
            System.arraycopy(pc, 0, c, 2 * ncomp, ncomp);
            triangle(xy, c);
        }
    }

    private void shift(double[] xy, double[] c, int by) {
        for (int k = 0; k + by < 3; k++) {
            xy[k * 2] = xy[(k + by) * 2];
            xy[k * 2 + 1] = xy[(k + by) * 2 + 1];
            System.arraycopy(c, (k + by) * ncomp, c, k * ncomp, ncomp);
        }
    }

    private void lattice(int perRow) {
        java.util.List<double[]> rows = new java.util.ArrayList<double[]>();
        java.util.List<double[]> cols = new java.util.ArrayList<double[]>();
        while (more()) {
            double[] xy = new double[perRow * 2];
            double[] c = new double[perRow * ncomp];
            for (int i = 0; i < perRow; i++) {
                point(xy, i * 2);
                color(c, i * ncomp);
            }
            if (!more() && (bitPos >> 3) > d.length) {
                break;
            }
            rows.add(xy);
            cols.add(c);
        }
        double[] txy = new double[6];
        double[] tc = new double[3 * ncomp];
        for (int r = 0; r + 1 < rows.size(); r++) {
            double[] a = rows.get(r);
            double[] b = rows.get(r + 1);
            double[] ac = cols.get(r);
            double[] bc = cols.get(r + 1);
            for (int i = 0; i + 1 < perRow; i++) {
                vertex(txy, tc, 0, a, ac, i);
                vertex(txy, tc, 1, a, ac, i + 1);
                vertex(txy, tc, 2, b, bc, i);
                triangle(txy, tc);
                vertex(txy, tc, 0, a, ac, i + 1);
                vertex(txy, tc, 1, b, bc, i + 1);
                vertex(txy, tc, 2, b, bc, i);
                triangle(txy, tc);
            }
        }
    }

    private void vertex(double[] txy, double[] tc, int k, double[] xy, double[] c, int i) {
        txy[k * 2] = xy[i * 2];
        txy[k * 2 + 1] = xy[i * 2 + 1];
        System.arraycopy(c, i * ncomp, tc, k * ncomp, ncomp);
    }

    /// Fills the triangle `xy` (device space), interpolating the colors
    /// `c` (three vertices of `ncomp` values).
    private void triangle(double[] xy, double[] c) {
        // To cell space.
        double ax = (xy[0] - x0) / step;
        double ay = (xy[1] - y0) / step;
        double bx = (xy[2] - x0) / step;
        double by = (xy[3] - y0) / step;
        double cx = (xy[4] - x0) / step;
        double cy = (xy[5] - y0) / step;
        double area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        if (area == 0) {
            return;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
        int maxX = Math.min(sw - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int minY = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
        int maxY = Math.min(sh - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        if (minX > maxX || minY > maxY) {
            return;
        }
        int ra = 0;
        int rb = 0;
        int rc = 0;
        if (fn == null) {
            double[] tmp = new double[ncomp];
            System.arraycopy(c, 0, tmp, 0, ncomp);
            ra = rgbOf(tmp);
            System.arraycopy(c, ncomp, tmp, 0, ncomp);
            rb = rgbOf(tmp);
            System.arraycopy(c, 2 * ncomp, tmp, 0, ncomp);
            rc = rgbOf(tmp);
        }
        double ta = 0;
        double tb = 0;
        double tcv = 0;
        if (fn != null) {
            double span = decode[5] - decode[4];
            ta = span == 0 ? 0 : (c[0] - decode[4]) / span;
            tb = span == 0 ? 0 : (c[1] - decode[4]) / span;
            tcv = span == 0 ? 0 : (c[2] - decode[4]) / span;
        }
        int a24 = alpha << 24;
        // A small tolerance closes the hairline gaps between neighbours.
        double eps = -0.02 * Math.abs(area);
        for (int y = minY; y <= maxY; y++) {
            double py = y + 0.5;
            for (int x = minX; x <= maxX; x++) {
                double pxv = x + 0.5;
                double w0 = (bx - pxv) * (cy - py) - (cx - pxv) * (by - py);
                double w1 = (cx - pxv) * (ay - py) - (ax - pxv) * (cy - py);
                double w2 = (ax - pxv) * (by - py) - (bx - pxv) * (ay - py);
                if (area < 0) {
                    w0 = -w0;
                    w1 = -w1;
                    w2 = -w2;
                }
                if (w0 < eps || w1 < eps || w2 < eps) {
                    continue;
                }
                double s = Math.abs(area);
                double l0 = w0 / s;
                double l1 = w1 / s;
                double l2 = 1 - l0 - l1;
                int rgb;
                if (fn != null) {
                    double t = l0 * ta + l1 * tb + l2 * tcv;
                    int i = (int) (Math.max(0, Math.min(1, t)) * 255 + 0.5);
                    rgb = lut[i];
                } else {
                    rgb = mix(ra, rb, rc, l0, l1, l2);
                }
                px[y * sw + x] = a24 | rgb;
            }
        }
    }

    private static int mix(int a, int b, int c, double l0, double l1, double l2) {
        int out = 0;
        for (int sft = 16; sft >= 0; sft -= 8) {
            double v = ((a >> sft) & 0xff) * l0 + ((b >> sft) & 0xff) * l1 + ((c >> sft) & 0xff) * l2;
            int iv = (int) (v + 0.5);
            out |= (iv < 0 ? 0 : iv > 255 ? 255 : iv) << sft;
        }
        return out;
    }

    // ---- patches ----

    /// Stream order of the twelve boundary points to the 4x4 grid index
    /// `j * 4 + i` of point `p(i, j)`.
    private static final int[] GRID = {0, 4, 8, 12, 13, 14, 15, 11, 7, 3, 2, 1};

    private void patches(boolean tensor) {
        double[] prevP = null;
        double[] prevC = null;
        int nPts = tensor ? 16 : 12;
        while (more()) {
            int flag = (int) bits(bpFlag);
            double[] p = new double[nPts * 2];
            double[] c = new double[4 * ncomp];
            int firstPt = 0;
            int firstCol = 0;
            if (flag != 0 && prevP != null) {
                // The first edge is an edge of the previous patch.
                int start = flag == 1 ? 3 : flag == 2 ? 6 : 9;
                for (int k = 0; k < 4; k++) {
                    int src = (start + k) % 12;
                    p[k * 2] = prevP[src * 2];
                    p[k * 2 + 1] = prevP[src * 2 + 1];
                }
                int cs0 = flag;
                System.arraycopy(prevC, cs0 * ncomp, c, 0, ncomp);
                System.arraycopy(prevC, ((cs0 + 1) % 4) * ncomp, c, ncomp, ncomp);
                firstPt = 4;
                firstCol = 2;
            } else if (flag != 0) {
                return;
            }
            for (int k = firstPt; k < nPts; k++) {
                point(p, k * 2);
            }
            for (int k = firstCol; k < 4; k++) {
                color(c, k * ncomp);
            }
            align();
            if ((bitPos >> 3) > d.length) {
                break;
            }
            patch(p, c, tensor);
            prevP = p;
            prevC = c;
        }
    }

    private void patch(double[] stream, double[] c, boolean tensor) {
        double[] gx = new double[16];
        double[] gy = new double[16];
        for (int k = 0; k < 12; k++) {
            gx[GRID[k]] = stream[k * 2];
            gy[GRID[k]] = stream[k * 2 + 1];
        }
        if (tensor) {
            // p11, p12, p22, p21
            int[] inner = {5, 9, 10, 6};
            for (int k = 0; k < 4; k++) {
                gx[inner[k]] = stream[(12 + k) * 2];
                gy[inner[k]] = stream[(12 + k) * 2 + 1];
            }
        } else {
            coonsInner(gx);
            coonsInner(gy);
        }
        double ext = 0;
        for (int k = 0; k < 16; k++) {
            ext = Math.max(ext, Math.abs(gx[k] - gx[0]) + Math.abs(gy[k] - gy[0]));
        }
        int n = Math.max(2, Math.min(40, (int) (ext / step / 6) + 2));
        double[] vx = new double[(n + 1) * (n + 1)];
        double[] vy = new double[vx.length];
        double[] vc = new double[vx.length * ncomp];
        double[] bu = new double[4];
        double[] bv = new double[4];
        for (int j = 0; j <= n; j++) {
            double v = j / (double) n;
            bern(v, bv);
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n;
                bern(u, bu);
                double sx = 0;
                double sy = 0;
                for (int b = 0; b < 4; b++) {
                    for (int a = 0; a < 4; a++) {
                        double w = bu[a] * bv[b];
                        sx += w * gx[b * 4 + a];
                        sy += w * gy[b * 4 + a];
                    }
                }
                int idx = j * (n + 1) + i;
                vx[idx] = sx;
                vy[idx] = sy;
                // c0 at p00, c1 at p03, c2 at p33, c3 at p30.
                for (int k = 0; k < ncomp; k++) {
                    vc[idx * ncomp + k] = (1 - u) * (1 - v) * c[k] + (1 - u) * v * c[ncomp + k]
                            + u * v * c[2 * ncomp + k] + u * (1 - v) * c[3 * ncomp + k];
                }
            }
        }
        double[] txy = new double[6];
        double[] tc = new double[3 * ncomp];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int a = j * (n + 1) + i;
                int b = a + 1;
                int cc = a + n + 1;
                int dd = cc + 1;
                tri(txy, tc, vx, vy, vc, a, b, cc);
                tri(txy, tc, vx, vy, vc, b, dd, cc);
            }
        }
    }

    private void tri(double[] txy, double[] tc, double[] vx, double[] vy, double[] vc, int a, int b, int c) {
        txy[0] = vx[a];
        txy[1] = vy[a];
        txy[2] = vx[b];
        txy[3] = vy[b];
        txy[4] = vx[c];
        txy[5] = vy[c];
        System.arraycopy(vc, a * ncomp, tc, 0, ncomp);
        System.arraycopy(vc, b * ncomp, tc, ncomp, ncomp);
        System.arraycopy(vc, c * ncomp, tc, 2 * ncomp, ncomp);
        triangle(txy, tc);
    }

    private static void bern(double t, double[] out) {
        double s = 1 - t;
        out[0] = s * s * s;
        out[1] = 3 * t * s * s;
        out[2] = 3 * t * t * s;
        out[3] = t * t * t;
    }

    /// The four inner control points of a Coons patch, as the PDF
    /// specification derives them from its boundary.
    private static void coonsInner(double[] g) {
        double p00 = g[0];
        double p01 = g[4];
        double p02 = g[8];
        double p03 = g[12];
        double p10 = g[1];
        double p13 = g[13];
        double p20 = g[2];
        double p23 = g[14];
        double p30 = g[3];
        double p31 = g[7];
        double p32 = g[11];
        double p33 = g[15];
        g[5] = (-4 * p00 + 6 * (p01 + p10) - 2 * (p03 + p30) + 3 * (p31 + p13) - p33) / 9;
        g[9] = (-4 * p03 + 6 * (p02 + p13) - 2 * (p00 + p33) + 3 * (p32 + p10) - p30) / 9;
        g[6] = (-4 * p30 + 6 * (p31 + p20) - 2 * (p33 + p00) + 3 * (p01 + p23) - p03) / 9;
        g[10] = (-4 * p33 + 6 * (p32 + p23) - 2 * (p30 + p03) + 3 * (p02 + p20) - p00) / 9;
    }
}
