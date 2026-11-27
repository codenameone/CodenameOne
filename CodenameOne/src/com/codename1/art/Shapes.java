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

/// Paths for the basic shapes.
public final class Shapes {
    private static final double K = 0.5522847498307936;

    private Shapes() {
    }

    /// A rectangle with corners rounded by `rx` horizontally and `ry` vertically.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `rx`: horizontal corner radius
    /// - `ry`: vertical corner radius
    public static GeneralPath roundRect(double x, double y, double w, double h, double rx, double ry) {
        GeneralPath p = new GeneralPath();
        rx = Math.max(0, Math.min(rx, w / 2));
        ry = Math.max(0, Math.min(ry, h / 2));
        if (rx == 0 || ry == 0) {
            p.moveTo(x, y);
            p.lineTo(x + w, y);
            p.lineTo(x + w, y + h);
            p.lineTo(x, y + h);
            p.closePath();
            return p;
        }
        double kx = rx * K;
        double ky = ry * K;
        p.moveTo(x + rx, y);
        p.lineTo(x + w - rx, y);
        p.curveTo(x + w - rx + kx, y, x + w, y + ry - ky, x + w, y + ry);
        p.lineTo(x + w, y + h - ry);
        p.curveTo(x + w, y + h - ry + ky, x + w - rx + kx, y + h, x + w - rx, y + h);
        p.lineTo(x + rx, y + h);
        p.curveTo(x + rx - kx, y + h, x, y + h - ry + ky, x, y + h - ry);
        p.lineTo(x, y + ry);
        p.curveTo(x, y + ry - ky, x + rx - kx, y, x + rx, y);
        p.closePath();
        return p;
    }

    /// A rectangle with each corner rounded by its own radius, clockwise from top left.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `r`: four radii: top left, top right, bottom right, bottom left
    public static GeneralPath roundRect(double x, double y, double w, double h, double[] r) {
        double max = Math.min(w, h) / 2;
        double tl = Math.max(0, Math.min(r[0], max));
        double tr = Math.max(0, Math.min(r[1], max));
        double br = Math.max(0, Math.min(r[2], max));
        double bl = Math.max(0, Math.min(r[3], max));
        GeneralPath p = new GeneralPath();
        p.moveTo(x + tl, y);
        p.lineTo(x + w - tr, y);
        if (tr > 0) {
            p.curveTo(x + w - tr + tr * K, y, x + w, y + tr - tr * K, x + w, y + tr);
        }
        p.lineTo(x + w, y + h - br);
        if (br > 0) {
            p.curveTo(x + w, y + h - br + br * K, x + w - br + br * K, y + h, x + w - br, y + h);
        }
        p.lineTo(x + bl, y + h);
        if (bl > 0) {
            p.curveTo(x + bl - bl * K, y + h, x, y + h - bl + bl * K, x, y + h - bl);
        }
        p.lineTo(x, y + tl);
        if (tl > 0) {
            p.curveTo(x, y + tl - tl * K, x + tl - tl * K, y, x + tl, y);
        }
        p.closePath();
        return p;
    }

    /// An ellipse.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `rx`: horizontal radius
    /// - `ry`: vertical radius
    public static GeneralPath ellipse(double cx, double cy, double rx, double ry) {
        GeneralPath p = new GeneralPath();
        double kx = rx * K;
        double ky = ry * K;
        p.moveTo(cx + rx, cy);
        p.curveTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry);
        p.curveTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy);
        p.curveTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry);
        p.curveTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy);
        p.closePath();
        return p;
    }

    /// A polygon or polyline through points given as `x0, y0, x1, y1, ...`.
    ///
    /// #### Parameters
    ///
    /// - `pts`: the points
    /// - `close`: true to close it
    public static GeneralPath polygon(double[] pts, boolean close) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i + 1 < pts.length; i += 2) {
            if (i == 0) {
                p.moveTo(pts[0], pts[1]);
            } else {
                p.lineTo(pts[i], pts[i + 1]);
            }
        }
        if (close && pts.length >= 4) {
            p.closePath();
        }
        return p;
    }

    /// A regular star or polygon.
    ///
    /// #### Parameters
    ///
    /// - `cx`: center x
    /// - `cy`: center y
    /// - `outer`: outer radius
    /// - `inner`: inner radius; equal to `outer` for a plain polygon
    /// - `points`: number of points, at least 3
    public static GeneralPath star(double cx, double cy, double outer, double inner, int points) {
        int n = Math.max(3, points);
        boolean starred = inner != outer;
        int verts = starred ? n * 2 : n;
        double[] pts = new double[verts * 2];
        for (int i = 0; i < verts; i++) {
            double r = starred && (i & 1) != 0 ? inner : outer;
            double a = -Math.PI / 2 + Math.PI * 2 * i / verts;
            pts[i * 2] = cx + Math.cos(a) * r;
            pts[i * 2 + 1] = cy + Math.sin(a) * r;
        }
        return polygon(pts, true);
    }

    /// Returns a copy of `path` with every point mapped through `m`.
    ///
    /// #### Parameters
    ///
    /// - `path`: the source
    /// - `m`: the transform
    public static GeneralPath transform(GeneralPath path, Matrix m) {
        GeneralPath out = new GeneralPath(path.getWindingRule());
        com.codename1.ui.geom.PathIterator it = path.getPathIterator();
        float[] c = new float[6];
        while (!it.isDone()) {
            int t = it.currentSegment(c);
            switch (t) {
                case com.codename1.ui.geom.PathIterator.SEG_MOVETO:
                    out.moveTo(m.mapX(c[0], c[1]), m.mapY(c[0], c[1]));
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_LINETO:
                    out.lineTo(m.mapX(c[0], c[1]), m.mapY(c[0], c[1]));
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_QUADTO:
                    out.quadTo(m.mapX(c[0], c[1]), m.mapY(c[0], c[1]),
                            m.mapX(c[2], c[3]), m.mapY(c[2], c[3]));
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_CUBICTO:
                    out.curveTo(m.mapX(c[0], c[1]), m.mapY(c[0], c[1]),
                            m.mapX(c[2], c[3]), m.mapY(c[2], c[3]),
                            m.mapX(c[4], c[5]), m.mapY(c[4], c[5]));
                    break;
                default:
                    out.closePath();
                    break;
            }
            it.next();
        }
        return out;
    }
}
