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

/// A cubic Bezier curve: two end points and two control points.
///
/// The area tested by `contains` and `intersects` is the region between the
/// curve and the straight line joining its ends, and is evaluated over the
/// flattened curve. `getBounds2D` answers the bounds of the four defining
/// points, which enclose the curve without being tight. `solveCubic` is not
/// provided.
public abstract class CubicCurve2D implements Shape, Cloneable {

    /// A curve stored in single precision.
    public static class Float extends CubicCurve2D {

        public float x1;
        public float y1;
        public float ctrlx1;
        public float ctrly1;
        public float ctrlx2;
        public float ctrly2;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float ctrlx1, float ctrly1,
                float ctrlx2, float ctrly2, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
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
        public double getCtrlX1() {
            return ctrlx1;
        }

        @Override
        public double getCtrlY1() {
            return ctrly1;
        }

        @Override
        public Point2D getCtrlP1() {
            return new Point2D.Float(ctrlx1, ctrly1);
        }

        @Override
        public double getCtrlX2() {
            return ctrlx2;
        }

        @Override
        public double getCtrlY2() {
            return ctrly2;
        }

        @Override
        public Point2D getCtrlP2() {
            return new Point2D.Float(ctrlx2, ctrly2);
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
        public void setCurve(double x1, double y1, double ctrlx1, double ctrly1,
                double ctrlx2, double ctrly2, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx1 = (float) ctrlx1;
            this.ctrly1 = (float) ctrly1;
            this.ctrlx2 = (float) ctrlx2;
            this.ctrly2 = (float) ctrly2;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setCurve(float x1, float y1, float ctrlx1, float ctrly1,
                float ctrlx2, float ctrly2, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            float left = Math.min(Math.min(x1, x2), Math.min(ctrlx1, ctrlx2));
            float top = Math.min(Math.min(y1, y2), Math.min(ctrly1, ctrly2));
            float right = Math.max(Math.max(x1, x2), Math.max(ctrlx1, ctrlx2));
            float bottom = Math.max(Math.max(y1, y2), Math.max(ctrly1, ctrly2));
            return new Rectangle2D.Float(left, top, right - left, bottom - top);
        }

        @Override
        public Object clone() {
            return new Float(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
        }
    }

    /// A curve stored in double precision.
    public static class Double extends CubicCurve2D {

        public double x1;
        public double y1;
        public double ctrlx1;
        public double ctrly1;
        public double ctrlx2;
        public double ctrly2;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double ctrlx1, double ctrly1,
                double ctrlx2, double ctrly2, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
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
        public double getCtrlX1() {
            return ctrlx1;
        }

        @Override
        public double getCtrlY1() {
            return ctrly1;
        }

        @Override
        public Point2D getCtrlP1() {
            return new Point2D.Double(ctrlx1, ctrly1);
        }

        @Override
        public double getCtrlX2() {
            return ctrlx2;
        }

        @Override
        public double getCtrlY2() {
            return ctrly2;
        }

        @Override
        public Point2D getCtrlP2() {
            return new Point2D.Double(ctrlx2, ctrly2);
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
        public void setCurve(double x1, double y1, double ctrlx1, double ctrly1,
                double ctrlx2, double ctrly2, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            double left = Math.min(Math.min(x1, x2), Math.min(ctrlx1, ctrlx2));
            double top = Math.min(Math.min(y1, y2), Math.min(ctrly1, ctrly2));
            double right = Math.max(Math.max(x1, x2), Math.max(ctrlx1, ctrlx2));
            double bottom = Math.max(Math.max(y1, y2), Math.max(ctrly1, ctrly2));
            return new Rectangle2D.Double(left, top, right - left, bottom - top);
        }

        @Override
        public Object clone() {
            return new Double(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
        }
    }

    protected CubicCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getCtrlX1();

    public abstract double getCtrlY1();

    public abstract Point2D getCtrlP1();

    public abstract double getCtrlX2();

    public abstract double getCtrlY2();

    public abstract Point2D getCtrlP2();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setCurve(double x1, double y1, double ctrlx1, double ctrly1,
            double ctrlx2, double ctrly2, double x2, double y2);

    public void setCurve(double[] coords, int offset) {
        setCurve(coords[offset], coords[offset + 1], coords[offset + 2], coords[offset + 3],
                coords[offset + 4], coords[offset + 5], coords[offset + 6], coords[offset + 7]);
    }

    public void setCurve(Point2D p1, Point2D cp1, Point2D cp2, Point2D p2) {
        setCurve(p1.getX(), p1.getY(), cp1.getX(), cp1.getY(), cp2.getX(), cp2.getY(), p2.getX(), p2.getY());
    }

    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset].getX(), pts[offset].getY(), pts[offset + 1].getX(), pts[offset + 1].getY(),
                pts[offset + 2].getX(), pts[offset + 2].getY(), pts[offset + 3].getX(), pts[offset + 3].getY());
    }

    public void setCurve(CubicCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(),
                c.getCtrlX2(), c.getCtrlY2(), c.getX2(), c.getY2());
    }

    /// The square of the larger distance of the two control points from the
    /// segment joining the end points.
    public static double getFlatnessSq(double x1, double y1, double ctrlx1, double ctrly1,
            double ctrlx2, double ctrly2, double x2, double y2) {
        return Math.max(Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx1, ctrly1),
                Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx2, ctrly2));
    }

    public static double getFlatness(double x1, double y1, double ctrlx1, double ctrly1,
            double ctrlx2, double ctrly2, double x2, double y2) {
        return Math.sqrt(getFlatnessSq(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2));
    }

    public static double getFlatnessSq(double[] coords, int offset) {
        return getFlatnessSq(coords[offset], coords[offset + 1], coords[offset + 2], coords[offset + 3],
                coords[offset + 4], coords[offset + 5], coords[offset + 6], coords[offset + 7]);
    }

    public static double getFlatness(double[] coords, int offset) {
        return Math.sqrt(getFlatnessSq(coords, offset));
    }

    public double getFlatnessSq() {
        return getFlatnessSq(getX1(), getY1(), getCtrlX1(), getCtrlY1(), getCtrlX2(), getCtrlY2(), getX2(), getY2());
    }

    public double getFlatness() {
        return Math.sqrt(getFlatnessSq());
    }

    /// Splits this curve at its middle into two curves; either may be null
    /// or this curve itself.
    public void subdivide(CubicCurve2D left, CubicCurve2D right) {
        subdivide(this, left, right);
    }

    public static void subdivide(CubicCurve2D src, CubicCurve2D left, CubicCurve2D right) {
        double[] c = {src.getX1(), src.getY1(), src.getCtrlX1(), src.getCtrlY1(),
            src.getCtrlX2(), src.getCtrlY2(), src.getX2(), src.getY2()};
        double[] l = new double[8];
        double[] r = new double[8];
        subdivide(c, 0, l, 0, r, 0);
        if (left != null) {
            left.setCurve(l, 0);
        }
        if (right != null) {
            right.setCurve(r, 0);
        }
    }

    /// Splits eight coordinates into two curves of eight; a destination may
    /// be null, and may be the source array where the ranges allow it.
    public static void subdivide(double[] src, int srcoff, double[] left, int leftoff, double[] right, int rightoff) {
        double x1 = src[srcoff];
        double y1 = src[srcoff + 1];
        double ax = src[srcoff + 2];
        double ay = src[srcoff + 3];
        double bx = src[srcoff + 4];
        double by = src[srcoff + 5];
        double x2 = src[srcoff + 6];
        double y2 = src[srcoff + 7];
        double midx = (ax + bx) / 2.0;
        double midy = (ay + by) / 2.0;
        double lax = (x1 + ax) / 2.0;
        double lay = (y1 + ay) / 2.0;
        double rbx = (bx + x2) / 2.0;
        double rby = (by + y2) / 2.0;
        double lbx = (lax + midx) / 2.0;
        double lby = (lay + midy) / 2.0;
        double rax = (midx + rbx) / 2.0;
        double ray = (midy + rby) / 2.0;
        double mx = (lbx + rax) / 2.0;
        double my = (lby + ray) / 2.0;
        if (left != null) {
            left[leftoff] = x1;
            left[leftoff + 1] = y1;
            left[leftoff + 2] = lax;
            left[leftoff + 3] = lay;
            left[leftoff + 4] = lbx;
            left[leftoff + 5] = lby;
            left[leftoff + 6] = mx;
            left[leftoff + 7] = my;
        }
        if (right != null) {
            right[rightoff] = mx;
            right[rightoff + 1] = my;
            right[rightoff + 2] = rax;
            right[rightoff + 3] = ray;
            right[rightoff + 4] = rbx;
            right[rightoff + 5] = rby;
            right[rightoff + 6] = x2;
            right[rightoff + 7] = y2;
        }
    }

    @Override
    public boolean contains(double x, double y) {
        return Outline.of(this).contains(x, y, PathIterator.WIND_EVEN_ODD);
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return Outline.of(this).intersects(x, y, w, h, PathIterator.WIND_EVEN_ODD);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return Outline.of(this).contains(x, y, w, h, PathIterator.WIND_EVEN_ODD);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 2);
        it.move(getX1(), getY1());
        it.cubic(getCtrlX1(), getCtrlY1(), getCtrlX2(), getCtrlY2(), getX2(), getY2());
        return it;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    /// Returns a copy of this curve. A subclass from elsewhere that does not
    /// override it gets a double precision curve back.
    @Override
    public Object clone() {
        return new Double(getX1(), getY1(), getCtrlX1(), getCtrlY1(), getCtrlX2(), getCtrlY2(), getX2(), getY2());
    }
}
