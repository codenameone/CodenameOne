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

import java.util.ArrayList;
import java.util.List;

/**
 * A mutable path built from move/line/curve segments — Flutter's dart:ui
 * {@code Path}. For this milestone the path records its subpath commands
 * structurally (so painters can be transpiled and driven); faithful
 * rasterization is deferred to the render layer.
 */
public final class Path {

    /** A single recorded path command: a verb plus its raw coordinate operands. */
    public static final class Segment {
        public final String verb;
        public final double[] coords;

        Segment(String verb, double[] coords) {
            this.verb = verb;
            this.coords = coords;
        }
    }

    private final List<Segment> segments = new ArrayList<Segment>();
    private double currentX;
    private double currentY;

    public Path() {
    }

    public List<Segment> segments() {
        return segments;
    }

    public void moveTo(double x, double y) {
        currentX = x;
        currentY = y;
        segments.add(new Segment("moveTo", new double[] {x, y}));
    }

    public void lineTo(double x, double y) {
        currentX = x;
        currentY = y;
        segments.add(new Segment("lineTo", new double[] {x, y}));
    }

    public void cubicTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        currentX = x3;
        currentY = y3;
        segments.add(new Segment("cubicTo", new double[] {x1, y1, x2, y2, x3, y3}));
    }

    public void quadraticBezierTo(double x1, double y1, double x2, double y2) {
        currentX = x2;
        currentY = y2;
        segments.add(new Segment("quadraticBezierTo", new double[] {x1, y1, x2, y2}));
    }

    public void conicTo(double x1, double y1, double x2, double y2, double w) {
        currentX = x2;
        currentY = y2;
        segments.add(new Segment("conicTo", new double[] {x1, y1, x2, y2, w}));
    }

    public void arcTo(Rect rect, double startAngle, double sweepAngle, boolean forceMoveTo) {
        segments.add(new Segment("arcTo",
                new double[] {rect.left(), rect.top(), rect.right(), rect.bottom(),
                        startAngle, sweepAngle, forceMoveTo ? 1 : 0}));
    }

    public void arcToPoint(Offset arcEnd, Radius radius, double rotation,
                           boolean largeArc, boolean clockwise) {
        currentX = arcEnd.dx();
        currentY = arcEnd.dy();
        segments.add(new Segment("arcToPoint",
                new double[] {arcEnd.dx(), arcEnd.dy(), radius == null ? 0 : radius.x(),
                        radius == null ? 0 : radius.y(), rotation,
                        largeArc ? 1 : 0, clockwise ? 1 : 0}));
    }

    public void relativeMoveTo(double dx, double dy) {
        moveTo(currentX + dx, currentY + dy);
    }

    public void relativeLineTo(double dx, double dy) {
        lineTo(currentX + dx, currentY + dy);
    }

    public void addRect(Rect rect) {
        segments.add(new Segment("addRect",
                new double[] {rect.left(), rect.top(), rect.right(), rect.bottom()}));
    }

    public void addOval(Rect oval) {
        segments.add(new Segment("addOval",
                new double[] {oval.left(), oval.top(), oval.right(), oval.bottom()}));
    }

    public void addRRect(RRect rrect) {
        Rect r = rrect.outerRect();
        segments.add(new Segment("addRRect",
                new double[] {r.left(), r.top(), r.right(), r.bottom()}));
    }

    public void addPolygon(List<Offset> points, boolean close) {
        boolean first = true;
        for (Offset p : points) {
            if (first) {
                moveTo(p.dx(), p.dy());
                first = false;
            } else {
                lineTo(p.dx(), p.dy());
            }
        }
        if (close) {
            close();
        }
    }

    /**
     * {@code Path.addPath(path, offset)} -- appends a copy of {@code path}'s
     * segments translated by {@code offset}, as Flutter's dart:ui does.
     */
    public void addPath(Path path, Offset offset) {
        double dx = offset.dx();
        double dy = offset.dy();
        for (Segment s : path.segments) {
            segments.add(new Segment(s.verb, translateCoords(s.verb, s.coords, dx, dy)));
        }
    }

    /**
     * {@code Path.addPath(path, offset, {matrix4})} -- like {@link
     * #addPath(Path, Offset)}, but when {@code matrix4} is given the source
     * segments are also transformed by its 2D affine part. {@code matrix4} is
     * Flutter's column-major 4x4 {@code Float64List} (as produced by {@code
     * vector_math}'s {@code Matrix4.storage}); only the 2x2 linear part
     * (entries 0, 1, 4, 5) and the translation column (entries 12, 13) are
     * used -- this class has no projective path support. Matching dart:ui's
     * native {@code CanvasPath.addPathWithMatrix}, {@code offset} is folded
     * into the matrix's own translation rather than applied separately.
     */
    public void addPath(Path path, Offset offset, double[] matrix4) {
        if (matrix4 == null) {
            addPath(path, offset);
            return;
        }
        if (matrix4.length != 16) {
            throw new IllegalArgumentException("matrix4 must have 16 entries");
        }
        double m00 = matrix4[0];
        double m10 = matrix4[1];
        double m01 = matrix4[4];
        double m11 = matrix4[5];
        double tx = matrix4[12] + offset.dx();
        double ty = matrix4[13] + offset.dy();
        for (Segment s : path.segments) {
            segments.add(new Segment(s.verb,
                    transformCoords(s.verb, s.coords, m00, m10, m01, m11, tx, ty)));
        }
    }

    public void close() {
        segments.add(new Segment("close", new double[0]));
    }

    public void reset() {
        segments.clear();
        currentX = 0;
        currentY = 0;
    }

    public boolean contains(Offset point) {
        return false;
    }

    /**
     * {@code Path.shift(offset)} -- a copy of this path translated by {@code
     * offset}. Only the position-valued operands of each segment move; a
     * segment's shape parameters (an arc's angles and radii, a conic's
     * weight, the boolean flags) are invariant under translation.
     */
    public Path shift(Offset offset) {
        Path p = new Path();
        double dx = offset.dx();
        double dy = offset.dy();
        for (Segment s : segments) {
            p.segments.add(new Segment(s.verb, translateCoords(s.verb, s.coords, dx, dy)));
        }
        return p;
    }

    /**
     * The {@code coords} offsets, for every verb {@link #segments} can hold,
     * where a position (x, y) pair starts -- the only operands a translation
     * or an affine transform may touch. Everything else in a segment's
     * {@code coords} is a shape parameter that such a transform leaves alone:
     * {@code arcTo}'s {@code startAngle}/{@code sweepAngle} and
     * {@code forceMoveTo} flag, {@code arcToPoint}'s {@code radius},
     * {@code rotation} and {@code largeArc}/{@code clockwise} flags, and
     * {@code conicTo}'s weight {@code w} (a ratio, invariant under any affine
     * map of the control points). {@code close} has no operands at all.
     */
    private static int[] positionPairStarts(String verb) {
        switch (verb) {
            case "moveTo":
            case "lineTo":
                // {x, y}
                return new int[] {0};
            case "arcToPoint":
                // {arcEnd.dx, arcEnd.dy, radius.x, radius.y, rotation, largeArc, clockwise}
                return new int[] {0};
            case "quadraticBezierTo":
                // {x1, y1, x2, y2}
                return new int[] {0, 2};
            case "conicTo":
                // {x1, y1, x2, y2, w}
                return new int[] {0, 2};
            case "arcTo":
                // {left, top, right, bottom, startAngle, sweepAngle, forceMoveTo}
                return new int[] {0, 2};
            case "addRect":
            case "addOval":
            case "addRRect":
                // {left, top, right, bottom}
                return new int[] {0, 2};
            case "cubicTo":
                // {x1, y1, x2, y2, x3, y3}
                return new int[] {0, 2, 4};
            case "close":
                // no operands
                return new int[0];
            default:
                return new int[0];
        }
    }

    private static double[] translateCoords(String verb, double[] coords, double dx, double dy) {
        double[] c = coords.clone();
        for (int i : positionPairStarts(verb)) {
            c[i] += dx;
            c[i + 1] += dy;
        }
        return c;
    }

    private static double[] transformCoords(String verb, double[] coords,
            double m00, double m10, double m01, double m11, double tx, double ty) {
        double[] c = coords.clone();
        for (int i : positionPairStarts(verb)) {
            double x = c[i];
            double y = c[i + 1];
            c[i] = m00 * x + m01 * y + tx;
            c[i + 1] = m10 * x + m11 * y + ty;
        }
        return c;
    }
}
