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
package com.codename1.image;

/// A shape made of straight lines and quadratic and cubic Bezier curves, for
/// [RasterGraphics#fill(RasterPath)] and [RasterGraphics#draw(RasterPath)].
///
/// A path is a list of subpaths, each started by [#moveTo(double, double)].
/// Filling closes every subpath implicitly; stroking draws a subpath closed
/// only after [#closePath()].
///
/// The static factories build the common shapes. Angles are in degrees,
/// counterclockwise from three o'clock, as on screen with `y` pointing down
/// (so a positive extent turns toward the top).
@com.codename1.impl.SharedWithBackend
public final class RasterPath {
    static final byte MOVE = 0;
    static final byte LINE = 1;
    static final byte QUAD = 2;
    static final byte CUBIC = 3;
    static final byte CLOSE = 4;

    /// An arc drawn alone, both ends open.
    public static final int ARC_OPEN = 0;
    /// An arc closed by the straight line between its ends.
    public static final int ARC_CHORD = 1;
    /// An arc closed through the center, a pie slice.
    public static final int ARC_PIE = 2;

    private static final double KAPPA = 0.5522847498307936;

    byte[] ops = new byte[16];
    int opCount;
    double[] coords = new double[32];
    int coordCount;
    private boolean open;

    private void op(byte o, int n) {
        if (opCount == ops.length) {
            byte[] b = new byte[ops.length * 2];
            System.arraycopy(ops, 0, b, 0, opCount);
            ops = b;
        }
        if (coordCount + n > coords.length) {
            double[] c = new double[Math.max(coords.length * 2, coordCount + n)];
            System.arraycopy(coords, 0, c, 0, coordCount);
            coords = c;
        }
        ops[opCount++] = o;
    }

    private void ensureOpen() {
        if (!open) {
            throw new IllegalStateException("moveTo must start a subpath");
        }
    }

    /// Starts a new subpath at a point.
    ///
    /// #### Parameters
    ///
    /// - `x`: x
    /// - `y`: y
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath moveTo(double x, double y) {
        op(MOVE, 2);
        coords[coordCount++] = x;
        coords[coordCount++] = y;
        open = true;
        return this;
    }

    /// Adds a straight line to a point.
    ///
    /// #### Parameters
    ///
    /// - `x`: x
    /// - `y`: y
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath lineTo(double x, double y) {
        ensureOpen();
        op(LINE, 2);
        coords[coordCount++] = x;
        coords[coordCount++] = y;
        return this;
    }

    /// Adds a quadratic Bezier curve.
    ///
    /// #### Parameters
    ///
    /// - `cx`: control point x
    /// - `cy`: control point y
    /// - `x`: end x
    /// - `y`: end y
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath quadTo(double cx, double cy, double x, double y) {
        ensureOpen();
        op(QUAD, 4);
        coords[coordCount++] = cx;
        coords[coordCount++] = cy;
        coords[coordCount++] = x;
        coords[coordCount++] = y;
        return this;
    }

    /// Adds a cubic Bezier curve.
    ///
    /// #### Parameters
    ///
    /// - `c1x`: first control point x
    /// - `c1y`: first control point y
    /// - `c2x`: second control point x
    /// - `c2y`: second control point y
    /// - `x`: end x
    /// - `y`: end y
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
        ensureOpen();
        op(CUBIC, 6);
        coords[coordCount++] = c1x;
        coords[coordCount++] = c1y;
        coords[coordCount++] = c2x;
        coords[coordCount++] = c2y;
        coords[coordCount++] = x;
        coords[coordCount++] = y;
        return this;
    }

    /// Closes the current subpath with a line back to its start.
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath closePath() {
        if (open) {
            op(CLOSE, 0);
            open = false;
        }
        return this;
    }

    /// Returns true when the path has no segments.
    public boolean isEmpty() {
        return opCount == 0;
    }

    /// Creates a closed polygon.
    ///
    /// #### Parameters
    ///
    /// - `xs`: x of each vertex
    /// - `ys`: y of each vertex
    /// - `n`: number of vertices to use
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath polygon(double[] xs, double[] ys, int n) {
        RasterPath p = polyline(xs, ys, n);
        return p.closePath();
    }

    /// Creates an open chain of lines.
    ///
    /// #### Parameters
    ///
    /// - `xs`: x of each vertex
    /// - `ys`: y of each vertex
    /// - `n`: number of vertices to use
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath polyline(double[] xs, double[] ys, int n) {
        RasterPath p = new RasterPath();
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                p.moveTo(xs[i], ys[i]);
            } else {
                p.lineTo(xs[i], ys[i]);
            }
        }
        return p;
    }

    /// Creates a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath rect(double x, double y, double w, double h) {
        return new RasterPath().moveTo(x, y).lineTo(x + w, y).lineTo(x + w, y + h).lineTo(x, y + h).closePath();
    }

    /// Creates a rectangle with elliptical corners.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    /// - `arcW`: horizontal diameter of a corner
    /// - `arcH`: vertical diameter of a corner
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath roundRect(double x, double y, double w, double h, double arcW, double arcH) {
        double rx = Math.min(Math.abs(arcW) / 2, Math.abs(w) / 2);
        double ry = Math.min(Math.abs(arcH) / 2, Math.abs(h) / 2);
        if (rx <= 0 || ry <= 0) {
            return rect(x, y, w, h);
        }
        double kx = rx * KAPPA;
        double ky = ry * KAPPA;
        RasterPath p = new RasterPath();
        p.moveTo(x + rx, y).lineTo(x + w - rx, y);
        p.curveTo(x + w - rx + kx, y, x + w, y + ry - ky, x + w, y + ry);
        p.lineTo(x + w, y + h - ry);
        p.curveTo(x + w, y + h - ry + ky, x + w - rx + kx, y + h, x + w - rx, y + h);
        p.lineTo(x + rx, y + h);
        p.curveTo(x + rx - kx, y + h, x, y + h - ry + ky, x, y + h - ry);
        p.lineTo(x, y + ry);
        p.curveTo(x, y + ry - ky, x + rx - kx, y, x + rx, y);
        return p.closePath();
    }

    /// Creates the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left
    /// - `y`: top
    /// - `w`: width
    /// - `h`: height
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath ellipse(double x, double y, double w, double h) {
        RasterPath p = arc(x, y, w, h, 0, 360, ARC_OPEN);
        return p.closePath();
    }

    /// Creates an arc of the ellipse inscribed in a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: left of the ellipse's bounds
    /// - `y`: top of the ellipse's bounds
    /// - `w`: width of the ellipse
    /// - `h`: height of the ellipse
    /// - `startDeg`: start angle in degrees
    /// - `extentDeg`: angular extent in degrees, negative for clockwise
    /// - `type`: `ARC_OPEN`, `ARC_CHORD` or `ARC_PIE`
    ///
    /// #### Returns
    ///
    /// the path
    public static RasterPath arc(double x, double y, double w, double h, double startDeg, double extentDeg,
                                 int type) {
        double cx = x + w / 2;
        double cy = y + h / 2;
        double rx = w / 2;
        double ry = h / 2;
        double ext = Math.max(-360, Math.min(360, extentDeg));
        int n = (int) Math.ceil(Math.abs(ext) / 90.0);
        if (n == 0) {
            n = 1;
        }
        double step = Math.toRadians(ext) / n;
        double a = Math.toRadians(startDeg);
        RasterPath p = new RasterPath();
        double sx = cx + rx * Math.cos(a);
        double sy = cy - ry * Math.sin(a);
        if (type == ARC_PIE) {
            p.moveTo(cx, cy).lineTo(sx, sy);
        } else {
            p.moveTo(sx, sy);
        }
        double k = 4.0 / 3.0 * Math.tan(step / 4);
        for (int i = 0; i < n; i++) {
            double a1 = a + step;
            double c0 = Math.cos(a);
            double s0 = Math.sin(a);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            p.curveTo(cx + rx * (c0 - k * s0), cy - ry * (s0 + k * c0),
                    cx + rx * (c1 + k * s1), cy - ry * (s1 - k * c1),
                    cx + rx * c1, cy - ry * s1);
            a = a1;
        }
        if (type == ARC_PIE || type == ARC_CHORD) {
            p.closePath();
        }
        return p;
    }

    /// Appends every subpath of another path to this one.
    ///
    /// #### Parameters
    ///
    /// - `other`: the path to append
    ///
    /// #### Returns
    ///
    /// this path
    public RasterPath append(RasterPath other) {
        int c = 0;
        for (int i = 0; i < other.opCount; i++) {
            byte o = other.ops[i];
            double[] k = other.coords;
            switch (o) {
                case MOVE:
                    moveTo(k[c], k[c + 1]);
                    c += 2;
                    break;
                case LINE:
                    lineTo(k[c], k[c + 1]);
                    c += 2;
                    break;
                case QUAD:
                    quadTo(k[c], k[c + 1], k[c + 2], k[c + 3]);
                    c += 4;
                    break;
                case CUBIC:
                    curveTo(k[c], k[c + 1], k[c + 2], k[c + 3], k[c + 4], k[c + 5]);
                    c += 6;
                    break;
                default:
                    closePath();
                    break;
            }
        }
        return this;
    }
}
