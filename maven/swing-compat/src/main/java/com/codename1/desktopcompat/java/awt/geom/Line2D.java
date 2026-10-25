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
package com.codename1.desktopcompat.java.awt.geom;

import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;

/// A line segment between two points. It has no area, so it contains
/// nothing, but it can be tested against rectangles and other segments.
public abstract class Line2D implements Shape, Cloneable {

    /// A segment stored in single precision.
    public static class Float extends Line2D {

        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Float(Point2D p1, Point2D p2) {
            this.x1 = (float) p1.getX();
            this.y1 = (float) p1.getY();
            this.x2 = (float) p2.getX();
            this.y2 = (float) p2.getY();
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public Point2D getP1() {
            return new Point2D.Float(x1, y1);
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public Point2D getP2() {
            return new Point2D.Float(x2, y2);
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2),
                    Math.abs(x2 - x1), Math.abs(y2 - y1));
        }

        @Override
        public Object clone() {
            return new Float(x1, y1, x2, y2);
        }
    }

    /// A segment stored in double precision.
    public static class Double extends Line2D {

        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Double(Point2D p1, Point2D p2) {
            this.x1 = p1.getX();
            this.y1 = p1.getY();
            this.x2 = p2.getX();
            this.y2 = p2.getY();
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public Point2D getP1() {
            return new Point2D.Double(x1, y1);
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public Point2D getP2() {
            return new Point2D.Double(x2, y2);
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2),
                    Math.abs(x2 - x1), Math.abs(y2 - y1));
        }

        @Override
        public Object clone() {
            return new Double(x1, y1, x2, y2);
        }
    }

    protected Line2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setLine(Line2D l) {
        setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    /// Tells which way the segment from `(x1, y1)` to `(x2, y2)` has to turn
    /// about its first end to point at `(px, py)`: 1 when that is the turn
    /// taking the positive x axis towards the negative y axis, -1 for the
    /// opposite turn. For a point on the segment's line the answer is 0 when
    /// it lies between the ends, -1 when it lies before the first end and 1
    /// when it lies past the second.
    public static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double rx = px - x1;
        double ry = py - y1;
        double side = rx * dy - ry * dx;
        if (side == 0.0) {
            // on the line: order the point along it instead
            side = rx * dx + ry * dy;
            if (side > 0.0) {
                side = (rx - dx) * dx + (ry - dy) * dy;
                if (side < 0.0) {
                    side = 0.0;
                }
            }
        }
        return side < 0.0 ? -1 : (side > 0.0 ? 1 : 0);
    }

    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public int relativeCCW(Point2D p) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    /// Whether two segments share a point: each must have the other's ends
    /// on opposite sides of it, or on it.
    public static boolean linesIntersect(double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4) {
        return relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0
                && relativeCCW(x3, y3, x4, y4, x1, y1) * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0;
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2, getX1(), getY1(), getX2(), getY2());
    }

    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(), getX1(), getY1(), getX2(), getY2());
    }

    /// The square of the distance from a point to the nearest point of a
    /// segment.
    public static double ptSegDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double rx = px - x1;
        double ry = py - y1;
        double along = rx * dx + ry * dy;
        double projSq;
        if (along <= 0.0) {
            // nearest to the first end
            projSq = 0.0;
        } else {
            // measure from the second end instead
            rx = dx - rx;
            ry = dy - ry;
            along = rx * dx + ry * dy;
            projSq = along <= 0.0 ? 0.0 : along * along / (dx * dx + dy * dy);
        }
        double distSq = rx * rx + ry * ry - projSq;
        return distSq < 0.0 ? 0.0 : distSq;
    }

    public static double ptSegDist(double x1, double y1, double x2, double y2, double px, double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDistSq(Point2D pt) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptSegDist(double px, double py) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDist(Point2D pt) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    /// The square of the distance from a point to the infinite line through
    /// two points.
    public static double ptLineDistSq(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double rx = px - x1;
        double ry = py - y1;
        double along = rx * dx + ry * dy;
        double distSq = rx * rx + ry * ry - along * along / (dx * dx + dy * dy);
        return distSq < 0.0 ? 0.0 : distSq;
    }

    public static double ptLineDist(double x1, double y1, double x2, double y2, double px, double py) {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDistSq(Point2D pt) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptLineDist(double px, double py) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDist(Point2D pt) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    /// Always false: a segment has no interior.
    @Override
    public boolean contains(double x, double y) {
        return false;
    }

    /// Always false: a segment has no interior.
    @Override
    public boolean contains(Point2D p) {
        return false;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return new Rectangle2D.Double(x, y, w, h).intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    /// Always false: a segment has no interior.
    @Override
    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    /// Always false: a segment has no interior.
    @Override
    public boolean contains(Rectangle2D r) {
        return false;
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 2);
        it.move(getX1(), getY1());
        it.line(getX2(), getY2());
        return it;
    }

    /// A segment is already flat, so this is the plain outline.
    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    /// Returns a copy of this segment. A subclass from elsewhere that does
    /// not override it gets a double precision segment back.
    @Override
    public Object clone() {
        return new Double(getX1(), getY1(), getX2(), getY2());
    }
}
