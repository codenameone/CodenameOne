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

import java.awt.geom.Path2D;

/// A rectangle with an elliptical radius per corner, the shape of every CSS
/// box edge (border, padding, shadow). Immutable.
///
/// Radii are stored as eight values in CSS order: top-left x and y,
/// top-right x and y, bottom-right x and y, bottom-left x and y. The
/// constructor normalises them the way CSS does:
///
/// - a corner with a zero (or negative) radius on either axis is square;
/// - when the radii of two adjacent corners together exceed the side they
///   share, **all** radii are scaled by the same factor, the smallest
///   `side / (sum of the two radii)`, so the corners never overlap. A
///   1000px radius on a 100x40 box therefore becomes 20px: a pill.
///
/// Geometry is kept in `double` and never rounded; fractional boxes are
/// antialiased by the caller.
public final class RoundedBox {
    /// Index of the top-left corner, for [#radiusX(int)] and friends.
    public static final int TOP_LEFT = 0;
    public static final int TOP_RIGHT = 1;
    public static final int BOTTOM_RIGHT = 2;
    public static final int BOTTOM_LEFT = 3;

    /// Index of the top side, for [#sidePath(int)].
    public static final int TOP = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT = 3;

    /// The parametric angle, in degrees, at which each corner's arc starts
    /// when the outline is walked clockwise. Angles are measured in screen
    /// space (y down), so 270 is straight up.
    private static final double[] ARC_START = {180, 270, 0, 90};

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final double[] radii;

    /// Creates a box and normalises its radii.
    ///
    /// #### Parameters
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width; a value that is not positive makes the box empty
    ///
    /// - `height`: height; a value that is not positive makes the box empty
    ///
    /// - `radii`: eight radii, or `null` for square corners
    public RoundedBox(double x, double y, double width, double height, double[] radii) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        double[] r = new double[8];
        if (radii != null && width > 0 && height > 0) {
            for (int c = 0; c < 4; c++) {
                double rx = radii[c * 2];
                double ry = radii[c * 2 + 1];
                if (rx > 0 && ry > 0) {
                    r[c * 2] = rx;
                    r[c * 2 + 1] = ry;
                }
            }
            double f = 1;
            f = Math.min(f, ratio(width, r[0] + r[2]));
            f = Math.min(f, ratio(height, r[3] + r[5]));
            f = Math.min(f, ratio(width, r[4] + r[6]));
            f = Math.min(f, ratio(height, r[7] + r[1]));
            if (f < 1) {
                for (int i = 0; i < 8; i++) {
                    r[i] *= f;
                }
            }
        }
        this.radii = r;
    }

    private static double ratio(double side, double sum) {
        return sum > side ? side / sum : 1;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    /// The horizontal radius of a corner after normalisation.
    public double radiusX(int corner) {
        return radii[corner * 2];
    }

    /// The vertical radius of a corner after normalisation.
    public double radiusY(int corner) {
        return radii[corner * 2 + 1];
    }

    /// The eight normalised radii. A copy.
    public double[] getRadii() {
        return radii.clone();
    }

    /// Whether the box encloses no area.
    public boolean isEmpty() {
        return !(width > 0) || !(height > 0);
    }

    /// The box moved by `(dx, dy)`.
    public RoundedBox translate(double dx, double dy) {
        return new RoundedBox(x + dx, y + dy, width, height, radii);
    }

    /// The box shrunk by a distance per side, as the padding edge is from
    /// the border edge. Each radius shrinks by the inset of the side it
    /// meets and stops at zero, which is the CSS rule for the inner border
    /// radius; the result is then normalised again.
    public RoundedBox inset(double top, double right, double bottom, double left) {
        double[] r = {
            radii[0] - left, radii[1] - top,
            radii[2] - right, radii[3] - top,
            radii[4] - right, radii[5] - bottom,
            radii[6] - left, radii[7] - bottom
        };
        return new RoundedBox(x + left, y + top, width - left - right, height - top - bottom, r);
    }

    /// The box grown by a CSS shadow spread distance (shrunk for a negative
    /// one). A square corner stays square. A rounded corner grows by the
    /// spread, except that a radius smaller than the spread grows by less,
    /// `spread * (1 + (r / spread - 1)^3)`, which is the CSS rule that keeps
    /// a barely rounded box from getting a visibly round shadow.
    public RoundedBox grow(double spread) {
        double[] r = new double[8];
        for (int c = 0; c < 4; c++) {
            double rx = radii[c * 2];
            double ry = radii[c * 2 + 1];
            if (rx > 0 && ry > 0) {
                r[c * 2] = grownRadius(rx, spread);
                r[c * 2 + 1] = grownRadius(ry, spread);
            }
        }
        return new RoundedBox(x - spread, y - spread, width + 2 * spread, height + 2 * spread, r);
    }

    private static double grownRadius(double r, double spread) {
        if (spread <= 0) {
            return Math.max(0, r + spread);
        }
        if (r >= spread) {
            return r + spread;
        }
        double k = r / spread - 1;
        return r + spread * (1 + k * k * k);
    }

    private double centerX(int corner) {
        switch (corner) {
            case TOP_LEFT:
                return x + radii[0];
            case TOP_RIGHT:
                return x + width - radii[2];
            case BOTTOM_RIGHT:
                return x + width - radii[4];
            default:
                return x + radii[6];
        }
    }

    private double centerY(int corner) {
        switch (corner) {
            case TOP_LEFT:
                return y + radii[1];
            case TOP_RIGHT:
                return y + radii[3];
            case BOTTOM_RIGHT:
                return y + height - radii[5];
            default:
                return y + height - radii[7];
        }
    }

    private double pointX(int corner, double deg) {
        return centerX(corner) + radii[corner * 2] * Math.cos(Math.toRadians(deg));
    }

    private double pointY(int corner, double deg) {
        return centerY(corner) + radii[corner * 2 + 1] * Math.sin(Math.toRadians(deg));
    }

    /// The closed outline, clockwise from the start of the top edge. Empty
    /// when the box is.
    public Path2D.Double toPath() {
        Path2D.Double p = new Path2D.Double();
        appendTo(p);
        return p;
    }

    /// Appends the closed outline to a path, as its own subpath.
    public void appendTo(Path2D p) {
        if (isEmpty()) {
            return;
        }
        p.moveTo(pointX(TOP_LEFT, 270), pointY(TOP_LEFT, 270));
        for (int i = 1; i <= 4; i++) {
            int c = i % 4;
            double a = ARC_START[c];
            p.lineTo(pointX(c, a), pointY(c, a));
            arc(p, c, a, a + 90);
        }
        p.closePath();
    }

    /// The ring between two boxes as one even-odd path: `outer` with `inner`
    /// cut out of it. This is the border area when `inner` is the padding
    /// edge of `outer`.
    public static Path2D.Double ring(RoundedBox outer, RoundedBox inner) {
        Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        outer.appendTo(p);
        inner.appendTo(p);
        return p;
    }

    /// The open path along one side, from the middle of the corner arc that
    /// precedes it to the middle of the corner arc that follows it, walking
    /// clockwise. A dashed or dotted border is laid out along this path, so
    /// that each side owns half of each of its corners.
    public Path2D.Double sidePath(int side) {
        Path2D.Double p = new Path2D.Double();
        int c0 = side;
        int c1 = (side + 1) % 4;
        double a0 = ARC_START[c0];
        double a1 = ARC_START[c1];
        p.moveTo(pointX(c0, a0 + 45), pointY(c0, a0 + 45));
        arc(p, c0, a0 + 45, a0 + 90);
        p.lineTo(pointX(c1, a1), pointY(c1, a1));
        arc(p, c1, a1, a1 + 45);
        return p;
    }

    /// Appends an elliptical arc of at most 90 degrees as one cubic. The
    /// control points are the exact tangent directions at both ends scaled
    /// by `4/3 * tan(sweep / 4)`, the standard approximation, whose radial
    /// error for a quarter arc is under 0.03 percent of the radius.
    private void arc(Path2D p, int corner, double fromDeg, double toDeg) {
        double rx = radii[corner * 2];
        double ry = radii[corner * 2 + 1];
        if (!(rx > 0) || !(ry > 0)) {
            return;
        }
        double cx = centerX(corner);
        double cy = centerY(corner);
        double a0 = Math.toRadians(fromDeg);
        double a1 = Math.toRadians(toDeg);
        double k = 4.0 / 3.0 * Math.tan((a1 - a0) / 4);
        double c0 = Math.cos(a0);
        double s0 = Math.sin(a0);
        double c1 = Math.cos(a1);
        double s1 = Math.sin(a1);
        p.curveTo(
                cx + rx * (c0 - k * s0), cy + ry * (s0 + k * c0),
                cx + rx * (c1 + k * s1), cy + ry * (s1 - k * c1),
                cx + rx * c1, cy + ry * s1);
    }
}
