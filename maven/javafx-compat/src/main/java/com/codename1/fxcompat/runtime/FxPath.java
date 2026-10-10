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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

/// A path in logical coordinates: the one geometry every shape, canvas
/// call, background and border is reduced to before it is drawn.
///
/// The path keeps double precision and knows how to flatten itself, cut
/// itself into dashes, answer whether a point is inside and produce the
/// Codename One path of itself under an affine matrix.
public final class FxPath {

    /// Starts a new sub path; one point.
    public static final byte MOVE = 0;
    /// A straight line; one point.
    public static final byte LINE = 1;
    /// A quadratic curve; a control point and an end point.
    public static final byte QUAD = 2;
    /// A cubic curve; two control points and an end point.
    public static final byte CUBIC = 3;
    /// Closes the current sub path; no points.
    public static final byte CLOSE = 4;

    private static final double KAPPA = 0.5522847498307936;

    private byte[] commands = new byte[8];
    private double[] points = new double[32];
    private int commandCount;
    private int pointCount;
    private boolean evenOdd;
    private double startX;
    private double startY;
    private double lastX;
    private double lastY;

    /// Creates an empty path filled by the non-zero rule.
    public FxPath() {
    }

    /// Sets whether the inside is decided by the even-odd rule.
    public void setEvenOdd(boolean evenOdd) {
        this.evenOdd = evenOdd;
    }

    /// Returns whether the inside is decided by the even-odd rule.
    public boolean isEvenOdd() {
        return evenOdd;
    }

    /// Removes every segment.
    public void reset() {
        commandCount = 0;
        pointCount = 0;
        lastX = 0;
        lastY = 0;
        startX = 0;
        startY = 0;
    }

    /// Returns whether the path has no segments.
    public boolean isEmpty() {
        return commandCount == 0;
    }

    /// Returns the number of commands.
    public int commandCount() {
        return commandCount;
    }

    /// Returns a command, one of the constants of this class.
    public byte command(int index) {
        return commands[index];
    }

    /// Returns the coordinates, x and y alternating, in command order.
    public double[] points() {
        double[] copy = new double[pointCount];
        System.arraycopy(points, 0, copy, 0, pointCount);
        return copy;
    }

    /// Returns the x of the current point.
    public double currentX() {
        return lastX;
    }

    /// Returns the y of the current point.
    public double currentY() {
        return lastY;
    }

    private void add(byte command, int coordinates) {
        if (commandCount == commands.length) {
            byte[] grown = new byte[commandCount * 2];
            System.arraycopy(commands, 0, grown, 0, commandCount);
            commands = grown;
        }
        commands[commandCount++] = command;
        if (pointCount + coordinates > points.length) {
            double[] grown = new double[Math.max(points.length * 2, pointCount + coordinates)];
            System.arraycopy(points, 0, grown, 0, pointCount);
            points = grown;
        }
    }

    /// Starts a new sub path.
    public void moveTo(double x, double y) {
        add(MOVE, 2);
        points[pointCount++] = x;
        points[pointCount++] = y;
        lastX = x;
        lastY = y;
        startX = x;
        startY = y;
    }

    /// Adds a line; on an empty path it starts a sub path instead.
    public void lineTo(double x, double y) {
        if (commandCount == 0) {
            moveTo(x, y);
            return;
        }
        add(LINE, 2);
        points[pointCount++] = x;
        points[pointCount++] = y;
        lastX = x;
        lastY = y;
    }

    /// Adds a quadratic curve.
    public void quadTo(double cx, double cy, double x, double y) {
        if (commandCount == 0) {
            moveTo(cx, cy);
        }
        add(QUAD, 4);
        points[pointCount++] = cx;
        points[pointCount++] = cy;
        points[pointCount++] = x;
        points[pointCount++] = y;
        lastX = x;
        lastY = y;
    }

    /// Adds a cubic curve.
    public void curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
        if (commandCount == 0) {
            moveTo(c1x, c1y);
        }
        add(CUBIC, 6);
        points[pointCount++] = c1x;
        points[pointCount++] = c1y;
        points[pointCount++] = c2x;
        points[pointCount++] = c2y;
        points[pointCount++] = x;
        points[pointCount++] = y;
        lastX = x;
        lastY = y;
    }

    /// Closes the current sub path with a line to its start.
    public void closePath() {
        if (commandCount == 0 || commands[commandCount - 1] == CLOSE) {
            return;
        }
        add(CLOSE, 0);
        lastX = startX;
        lastY = startY;
    }

    /// Adds a closed rectangle.
    public void addRect(double x, double y, double w, double h) {
        moveTo(x, y);
        lineTo(x + w, y);
        lineTo(x + w, y + h);
        lineTo(x, y + h);
        closePath();
    }

    /// Adds a closed rectangle with a radius per corner, clockwise from
    /// the top left. Radii are reduced so neighbours never overlap.
    public void addRoundRect(double x, double y, double w, double h, double topLeft, double topRight,
            double bottomRight, double bottomLeft) {
        if (w <= 0 || h <= 0) {
            return;
        }
        double tl = Math.max(0, topLeft);
        double tr = Math.max(0, topRight);
        double br = Math.max(0, bottomRight);
        double bl = Math.max(0, bottomLeft);
        double f = 1;
        f = shrink(f, w, tl + tr);
        f = shrink(f, w, bl + br);
        f = shrink(f, h, tl + bl);
        f = shrink(f, h, tr + br);
        tl *= f;
        tr *= f;
        br *= f;
        bl *= f;
        if (tl == 0 && tr == 0 && br == 0 && bl == 0) {
            addRect(x, y, w, h);
            return;
        }
        double k = 1 - KAPPA;
        moveTo(x + tl, y);
        lineTo(x + w - tr, y);
        if (tr > 0) {
            curveTo(x + w - tr * k, y, x + w, y + tr * k, x + w, y + tr);
        }
        lineTo(x + w, y + h - br);
        if (br > 0) {
            curveTo(x + w, y + h - br * k, x + w - br * k, y + h, x + w - br, y + h);
        }
        lineTo(x + bl, y + h);
        if (bl > 0) {
            curveTo(x + bl * k, y + h, x, y + h - bl * k, x, y + h - bl);
        }
        lineTo(x, y + tl);
        if (tl > 0) {
            curveTo(x, y + tl * k, x + tl * k, y, x + tl, y);
        }
        closePath();
    }

    /// Adds a closed rectangle whose four corners are quarters of one
    /// ellipse with the given radii, each at most half the side it lies
    /// on. A radius that is not positive gives square corners.
    public void addRoundRect(double x, double y, double w, double h, double radiusX, double radiusY) {
        double rx = Math.min(Math.max(0, radiusX), w / 2);
        double ry = Math.min(Math.max(0, radiusY), h / 2);
        if (!(rx > 0) || !(ry > 0)) {
            addRect(x, y, w, h);
            return;
        }
        double kx = rx * (1 - KAPPA);
        double ky = ry * (1 - KAPPA);
        moveTo(x + rx, y);
        lineTo(x + w - rx, y);
        curveTo(x + w - kx, y, x + w, y + ky, x + w, y + ry);
        lineTo(x + w, y + h - ry);
        curveTo(x + w, y + h - ky, x + w - kx, y + h, x + w - rx, y + h);
        lineTo(x + rx, y + h);
        curveTo(x + kx, y + h, x, y + h - ky, x, y + h - ry);
        lineTo(x, y + ry);
        curveTo(x, y + ky, x + kx, y, x + rx, y);
        closePath();
    }

    private static double shrink(double factor, double available, double wanted) {
        if (wanted > available && wanted > 0) {
            return Math.min(factor, available / wanted);
        }
        return factor;
    }

    /// Adds a closed ellipse.
    public void addEllipse(double cx, double cy, double rx, double ry) {
        double ox = rx * KAPPA;
        double oy = ry * KAPPA;
        moveTo(cx + rx, cy);
        curveTo(cx + rx, cy + oy, cx + ox, cy + ry, cx, cy + ry);
        curveTo(cx - ox, cy + ry, cx - rx, cy + oy, cx - rx, cy);
        curveTo(cx - rx, cy - oy, cx - ox, cy - ry, cx, cy - ry);
        curveTo(cx + ox, cy - ry, cx + rx, cy - oy, cx + rx, cy);
        closePath();
    }

    /// Adds an elliptical arc as cubic curves. Angles are in degrees,
    /// counter clockwise from the positive x axis as JavaFX measures them
    /// (y pointing down, so a positive extent sweeps towards the top).
    /// With `connect` the arc is joined to the current point by a line;
    /// otherwise it starts a new sub path.
    public void addArc(double cx, double cy, double rx, double ry, double startDegrees, double extentDegrees,
            boolean connect) {
        double extent = extentDegrees;
        if (extent > 360) {
            extent = 360;
        } else if (extent < -360) {
            extent = -360;
        }
        double start = Math.toRadians(-startDegrees);
        double sweep = Math.toRadians(-extent);
        int pieces = (int) Math.ceil(Math.abs(sweep) / (Math.PI / 2) - 1e-9);
        if (pieces < 1) {
            pieces = 1;
        }
        double step = sweep / pieces;
        double t = 4.0 / 3.0 * Math.tan(step / 4);
        double a = start;
        double px = cx + rx * Math.cos(a);
        double py = cy + ry * Math.sin(a);
        if (connect && commandCount > 0) {
            lineTo(px, py);
        } else {
            moveTo(px, py);
        }
        for (int i = 0; i < pieces; i++) {
            double b = a + step;
            double cosA = Math.cos(a);
            double sinA = Math.sin(a);
            double cosB = Math.cos(b);
            double sinB = Math.sin(b);
            curveTo(cx + rx * (cosA - t * sinA), cy + ry * (sinA + t * cosA), cx + rx * (cosB + t * sinB),
                    cy + ry * (sinB - t * cosB), cx + rx * cosB, cy + ry * sinB);
            a = b;
        }
    }

    /// Adds an elliptical arc from the current point to an end point, in
    /// the form SVG path data and `ArcTo` give it: two radii, the
    /// rotation of the ellipse's x axis in degrees, whether the larger of
    /// the two possible arcs is meant and whether it is drawn in the
    /// direction of growing angles (clockwise on a screen). The arc is
    /// converted to cubic curves of at most a quarter turn each.
    ///
    /// As SVG specifies: an arc to the current point adds nothing, a zero
    /// radius gives a straight line, and radii too small to reach the end
    /// point are scaled up until they do.
    public void arcTo(double radiusX, double radiusY, double xAxisRotationDegrees, boolean largeArc, boolean sweep,
            double x, double y) {
        if (commandCount == 0) {
            moveTo(x, y);
            return;
        }
        double x0 = lastX;
        double y0 = lastY;
        if (Double.compare(x0, x) == 0 && Double.compare(y0, y) == 0) {
            return;
        }
        double rx = Math.abs(radiusX);
        double ry = Math.abs(radiusY);
        if (!(rx > 0) || !(ry > 0)) {
            lineTo(x, y);
            return;
        }
        double phi = Math.toRadians(xAxisRotationDegrees);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double dx2 = (x0 - x) / 2;
        double dy2 = (y0 - y) / 2;
        double x1 = cosPhi * dx2 + sinPhi * dy2;
        double y1 = -sinPhi * dx2 + cosPhi * dy2;
        double lambda = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry);
        if (lambda > 1) {
            double grow = Math.sqrt(lambda);
            rx *= grow;
            ry *= grow;
        }
        double numerator = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1;
        double denominator = rx * rx * y1 * y1 + ry * ry * x1 * x1;
        double factor = denominator > 0 && numerator > 0 ? Math.sqrt(numerator / denominator) : 0;
        if (largeArc == sweep) {
            factor = -factor;
        }
        double cx1 = factor * (rx * y1 / ry);
        double cy1 = factor * -(ry * x1 / rx);
        double cx = cosPhi * cx1 - sinPhi * cy1 + (x0 + x) / 2;
        double cy = sinPhi * cx1 + cosPhi * cy1 + (y0 + y) / 2;
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double theta = MathUtil.atan2(uy, ux);
        double delta = MathUtil.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }
        int pieces = (int) Math.ceil(Math.abs(delta) / (Math.PI / 2) - 1e-9);
        if (pieces < 1) {
            pieces = 1;
        }
        double step = delta / pieces;
        double t = 4.0 / 3.0 * Math.tan(step / 4);
        double a = theta;
        for (int i = 0; i < pieces; i++) {
            double b = a + step;
            double cosA = Math.cos(a);
            double sinA = Math.sin(a);
            double cosB = Math.cos(b);
            double sinB = Math.sin(b);
            // Points and tangents on the unrotated ellipse, then rotated.
            double p1x = rx * (cosA - t * sinA);
            double p1y = ry * (sinA + t * cosA);
            double p2x = rx * (cosB + t * sinB);
            double p2y = ry * (sinB - t * cosB);
            double ex = i == pieces - 1 ? x : cx + cosPhi * rx * cosB - sinPhi * ry * sinB;
            double ey = i == pieces - 1 ? y : cy + sinPhi * rx * cosB + cosPhi * ry * sinB;
            curveTo(cx + cosPhi * p1x - sinPhi * p1y, cy + sinPhi * p1x + cosPhi * p1y,
                    cx + cosPhi * p2x - sinPhi * p2y, cy + sinPhi * p2x + cosPhi * p2y, ex, ey);
            a = b;
        }
    }

    /// Returns a copy of this path.
    public FxPath copy() {
        return transformed(new double[] {1, 0, 0, 1, 0, 0});
    }

    /// Returns the x the current sub path started at.
    public double startX() {
        return startX;
    }

    /// Returns the y the current sub path started at.
    public double startY() {
        return startY;
    }

    /// Appends every segment of another path.
    public void append(FxPath other) {
        int p = 0;
        for (int i = 0; i < other.commandCount; i++) {
            double[] o = other.points;
            switch (other.commands[i]) {
                case MOVE:
                    moveTo(o[p], o[p + 1]);
                    p += 2;
                    break;
                case LINE:
                    lineTo(o[p], o[p + 1]);
                    p += 2;
                    break;
                case QUAD:
                    quadTo(o[p], o[p + 1], o[p + 2], o[p + 3]);
                    p += 4;
                    break;
                case CUBIC:
                    curveTo(o[p], o[p + 1], o[p + 2], o[p + 3], o[p + 4], o[p + 5]);
                    p += 6;
                    break;
                default:
                    closePath();
                    break;
            }
        }
    }

    private static int segments(double length, double tolerance) {
        int n = (int) Math.ceil(Math.sqrt(length / Math.max(tolerance, 0.01)));
        return n < 2 ? 2 : (n > 96 ? 96 : n);
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /// Returns this path with every curve replaced by lines that stay
    /// within a tolerance of it.
    public FxPath flatten(double tolerance) {
        FxPath out = new FxPath();
        out.evenOdd = evenOdd;
        int p = 0;
        double x = 0;
        double y = 0;
        for (int i = 0; i < commandCount; i++) {
            switch (commands[i]) {
                case MOVE:
                    x = points[p];
                    y = points[p + 1];
                    out.moveTo(x, y);
                    p += 2;
                    break;
                case LINE:
                    x = points[p];
                    y = points[p + 1];
                    out.lineTo(x, y);
                    p += 2;
                    break;
                case QUAD: {
                    double cx = points[p];
                    double cy = points[p + 1];
                    double ex = points[p + 2];
                    double ey = points[p + 3];
                    int n = segments(dist(x, y, cx, cy) + dist(cx, cy, ex, ey), tolerance);
                    for (int s = 1; s <= n; s++) {
                        double t = (double) s / n;
                        double u = 1 - t;
                        out.lineTo(u * u * x + 2 * u * t * cx + t * t * ex, u * u * y + 2 * u * t * cy + t * t * ey);
                    }
                    x = ex;
                    y = ey;
                    p += 4;
                    break;
                }
                case CUBIC: {
                    double c1x = points[p];
                    double c1y = points[p + 1];
                    double c2x = points[p + 2];
                    double c2y = points[p + 3];
                    double ex = points[p + 4];
                    double ey = points[p + 5];
                    int n = segments(dist(x, y, c1x, c1y) + dist(c1x, c1y, c2x, c2y) + dist(c2x, c2y, ex, ey),
                            tolerance);
                    for (int s = 1; s <= n; s++) {
                        double t = (double) s / n;
                        double u = 1 - t;
                        double a = u * u * u;
                        double b = 3 * u * u * t;
                        double c = 3 * u * t * t;
                        double d = t * t * t;
                        out.lineTo(a * x + b * c1x + c * c2x + d * ex, a * y + b * c1y + c * c2y + d * ey);
                    }
                    x = ex;
                    y = ey;
                    p += 6;
                    break;
                }
                default:
                    out.closePath();
                    x = out.lastX;
                    y = out.lastY;
                    break;
            }
        }
        return out;
    }

    /// Returns the bounds of the path as `{minX, minY, maxX, maxY}`, or
    /// `null` for an empty path. Curves are measured by their flattened
    /// form, so the result is tight.
    public double[] bounds() {
        if (pointCount == 0) {
            return null;
        }
        FxPath flat = hasCurves() ? flatten(0.05) : this;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < flat.pointCount; i += 2) {
            double x = flat.points[i];
            double y = flat.points[i + 1];
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        return new double[] {minX, minY, maxX, maxY};
    }

    private boolean hasCurves() {
        for (int i = 0; i < commandCount; i++) {
            if (commands[i] == QUAD || commands[i] == CUBIC) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether a point is inside the filled path. Open sub paths
    /// count as closed, as they do when filling.
    public boolean contains(double x, double y) {
        FxPath flat = hasCurves() ? flatten(0.1) : this;
        int winding = 0;
        int crossings = 0;
        int p = 0;
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        boolean open = false;
        for (int i = 0; i <= flat.commandCount; i++) {
            byte command = i < flat.commandCount ? flat.commands[i] : MOVE;
            if (command == LINE) {
                double nx = flat.points[p];
                double ny = flat.points[p + 1];
                p += 2;
                int w = cross(cx, cy, nx, ny, x, y);
                winding += w;
                crossings += w == 0 ? 0 : 1;
                cx = nx;
                cy = ny;
                continue;
            }
            if (open) {
                int w = cross(cx, cy, sx, sy, x, y);
                winding += w;
                crossings += w == 0 ? 0 : 1;
                cx = sx;
                cy = sy;
                open = command == CLOSE;
            }
            if (command == MOVE && i < flat.commandCount) {
                sx = flat.points[p];
                sy = flat.points[p + 1];
                p += 2;
                cx = sx;
                cy = sy;
                open = true;
            }
        }
        return evenOdd ? (crossings & 1) != 0 : winding != 0;
    }

    private static int cross(double x1, double y1, double x2, double y2, double px, double py) {
        if (y1 <= py) {
            if (y2 > py && (x2 - x1) * (py - y1) - (px - x1) * (y2 - y1) > 0) {
                return 1;
            }
        } else if (y2 <= py && (x2 - x1) * (py - y1) - (px - x1) * (y2 - y1) < 0) {
            return -1;
        }
        return 0;
    }

    /// Returns whether a point is within a distance of the outline, which
    /// is how a stroke is hit tested.
    public boolean nearOutline(double x, double y, double distance) {
        FxPath flat = hasCurves() ? flatten(0.1) : this;
        int p = 0;
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        for (int i = 0; i < flat.commandCount; i++) {
            byte command = flat.commands[i];
            if (command == MOVE) {
                sx = flat.points[p];
                sy = flat.points[p + 1];
                cx = sx;
                cy = sy;
                p += 2;
                if (dist(cx, cy, x, y) <= distance) {
                    return true;
                }
            } else {
                double nx = command == CLOSE ? sx : flat.points[p];
                double ny = command == CLOSE ? sy : flat.points[p + 1];
                if (command == LINE) {
                    p += 2;
                }
                if (segmentDistance(cx, cy, nx, ny, x, y) <= distance) {
                    return true;
                }
                cx = nx;
                cy = ny;
            }
        }
        return false;
    }

    private static double segmentDistance(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = dx * dx + dy * dy;
        double t = len == 0 ? 0 : ((px - x1) * dx + (py - y1) * dy) / len;
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return dist(x1 + t * dx, y1 + t * dy, px, py);
    }

    /// Returns the outline cut into dashes: `pattern` alternates the
    /// lengths of dashes and gaps, and `offset` is where in the pattern
    /// the outline starts. A pattern with no positive length answers this
    /// path.
    public FxPath dashed(double[] pattern, double offset) {
        if (pattern == null || pattern.length == 0) {
            return this;
        }
        double total = 0;
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i] < 0) {
                return this;
            }
            total += pattern[i];
        }
        if (!(total > 0)) {
            return this;
        }
        int entries = pattern.length % 2 == 0 ? pattern.length : pattern.length * 2;
        double cycle = pattern.length % 2 == 0 ? total : total * 2;
        FxPath flat = flatten(0.1);
        FxPath out = new FxPath();
        int p = 0;
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        int index = 0;
        double remaining = 0;
        boolean on = true;
        boolean pen = false;
        for (int i = 0; i < flat.commandCount; i++) {
            byte command = flat.commands[i];
            if (command == MOVE) {
                sx = flat.points[p];
                sy = flat.points[p + 1];
                p += 2;
                cx = sx;
                cy = sy;
                double phase = offset - Math.floor(offset / cycle) * cycle;
                index = 0;
                while (phase > 0 && phase >= pattern[index % pattern.length]) {
                    phase -= pattern[index % pattern.length];
                    index = (index + 1) % entries;
                }
                remaining = pattern[index % pattern.length] - phase;
                on = index % 2 == 0;
                pen = false;
                continue;
            }
            double nx = command == CLOSE ? sx : flat.points[p];
            double ny = command == CLOSE ? sy : flat.points[p + 1];
            if (command == LINE) {
                p += 2;
            }
            double len = dist(cx, cy, nx, ny);
            double done = 0;
            int guard = 0;
            while (len - done > 1e-9 && guard++ < 100000) {
                double take = Math.min(remaining, len - done);
                double t0 = len == 0 ? 0 : done / len;
                double t1 = len == 0 ? 0 : (done + take) / len;
                if (on) {
                    if (!pen) {
                        out.moveTo(cx + (nx - cx) * t0, cy + (ny - cy) * t0);
                        pen = true;
                    }
                    out.lineTo(cx + (nx - cx) * t1, cy + (ny - cy) * t1);
                }
                done += take;
                remaining -= take;
                if (remaining <= 1e-9) {
                    index = (index + 1) % entries;
                    remaining = pattern[index % pattern.length];
                    on = index % 2 == 0;
                    pen = false;
                }
            }
            cx = nx;
            cy = ny;
        }
        return out;
    }

    /// Returns the Codename One path of this path under the affine matrix
    /// `{a, b, c, d, tx, ty}`, where `x' = a*x + c*y + tx` and
    /// `y' = b*x + d*y + ty`.
    public GeneralPath toDevice(double[] m) {
        GeneralPath out = new GeneralPath();
        out.setWindingRule(evenOdd ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        int p = 0;
        for (int i = 0; i < commandCount; i++) {
            switch (commands[i]) {
                case MOVE:
                    out.moveTo(tx(m, points[p], points[p + 1]), ty(m, points[p], points[p + 1]));
                    p += 2;
                    break;
                case LINE:
                    out.lineTo(tx(m, points[p], points[p + 1]), ty(m, points[p], points[p + 1]));
                    p += 2;
                    break;
                case QUAD:
                    out.quadTo(tx(m, points[p], points[p + 1]), ty(m, points[p], points[p + 1]),
                            tx(m, points[p + 2], points[p + 3]), ty(m, points[p + 2], points[p + 3]));
                    p += 4;
                    break;
                case CUBIC:
                    out.curveTo(tx(m, points[p], points[p + 1]), ty(m, points[p], points[p + 1]),
                            tx(m, points[p + 2], points[p + 3]), ty(m, points[p + 2], points[p + 3]),
                            tx(m, points[p + 4], points[p + 5]), ty(m, points[p + 4], points[p + 5]));
                    p += 6;
                    break;
                default:
                    out.closePath();
                    break;
            }
        }
        return out;
    }

    /// Returns this path with every point mapped through the affine
    /// matrix `{a, b, c, d, tx, ty}`.
    public FxPath transformed(double[] m) {
        FxPath out = new FxPath();
        out.evenOdd = evenOdd;
        out.commands = new byte[Math.max(8, commandCount)];
        System.arraycopy(commands, 0, out.commands, 0, commandCount);
        out.commandCount = commandCount;
        out.points = new double[Math.max(32, pointCount)];
        for (int i = 0; i < pointCount; i += 2) {
            out.points[i] = tx(m, points[i], points[i + 1]);
            out.points[i + 1] = ty(m, points[i], points[i + 1]);
        }
        out.pointCount = pointCount;
        out.lastX = tx(m, lastX, lastY);
        out.lastY = ty(m, lastX, lastY);
        out.startX = tx(m, startX, startY);
        out.startY = ty(m, startX, startY);
        return out;
    }

    private static double tx(double[] m, double x, double y) {
        return m[0] * x + m[2] * y + m[4];
    }

    private static double ty(double[] m, double x, double y) {
        return m[1] * x + m[3] * y + m[5];
    }
}
