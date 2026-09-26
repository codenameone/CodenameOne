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

    public void addPath(Path path, Offset offset) {
        for (Segment s : path.segments) {
            segments.add(s);
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

    public Path shift(Offset offset) {
        Path p = new Path();
        for (Segment s : segments) {
            double[] c = s.coords.clone();
            for (int i = 0; i + 1 < c.length; i += 2) {
                c[i] += offset.dx();
                c[i + 1] += offset.dy();
            }
            p.segments.add(new Segment(s.verb, c));
        }
        return p;
    }
}
