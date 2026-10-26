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

/// A quadratic Bezier curve: two end points and one control point.
///
/// The area tested by `contains` and `intersects` is the region between the
/// curve and the straight line joining its ends, and is evaluated over the
/// flattened curve. `getBounds2D` answers the bounds of the three defining
/// points, which enclose the curve without being tight.
public abstract class QuadCurve2D implements Shape, Cloneable {

    /// A curve stored in single precision.
    public static class Float extends QuadCurve2D {

        public float x1;
        public float y1;
        public float ctrlx;
        public float ctrly;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
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
        public double getCtrlX() {
            return ctrlx;
        }

        @Override
        public double getCtrlY() {
            return ctrly;
        }

        @Override
        public Point2D getCtrlPt() {
            return new Point2D.Float(ctrlx, ctrly);
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
        public void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx = (float) ctrlx;
            this.ctrly = (float) ctrly;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setCurve(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            float left = Math.min(Math.min(x1, x2), ctrlx);
            float top = Math.min(Math.min(y1, y2), ctrly);
            float right = Math.max(Math.max(x1, x2), ctrlx);
            float bottom = Math.max(Math.max(y1, y2), ctrly);
            return new Rectangle2D.Float(left, top, right - left, bottom - top);
        }

        @Override
        public Object clone() {
            return new Float(x1, y1, ctrlx, ctrly, x2, y2);
        }
    }

    /// A curve stored in double precision.
    public static class Double extends QuadCurve2D {

        public double x1;
        public double y1;
        public double ctrlx;
        public double ctrly;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
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
        public double getCtrlX() {
            return ctrlx;
        }

        @Override
        public double getCtrlY() {
            return ctrly;
        }

        @Override
        public Point2D getCtrlPt() {
            return new Point2D.Double(ctrlx, ctrly);
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
        public void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            double left = Math.min(Math.min(x1, x2), ctrlx);
            double top = Math.min(Math.min(y1, y2), ctrly);
            double right = Math.max(Math.max(x1, x2), ctrlx);
            double bottom = Math.max(Math.max(y1, y2), ctrly);
            return new Rectangle2D.Double(left, top, right - left, bottom - top);
        }

        @Override
        public Object clone() {
            return new Double(x1, y1, ctrlx, ctrly, x2, y2);
        }
    }

    protected QuadCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getCtrlX();

    public abstract double getCtrlY();

    public abstract Point2D getCtrlPt();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setCurve(double x1, double y1, double ctrlx, double ctrly, double x2, double y2);

    public void setCurve(double[] coords, int offset) {
        setCurve(coords[offset], coords[offset + 1], coords[offset + 2], coords[offset + 3],
                coords[offset + 4], coords[offset + 5]);
    }

    public void setCurve(Point2D p1, Point2D cp, Point2D p2) {
        setCurve(p1.getX(), p1.getY(), cp.getX(), cp.getY(), p2.getX(), p2.getY());
    }

    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset].getX(), pts[offset].getY(), pts[offset + 1].getX(), pts[offset + 1].getY(),
                pts[offset + 2].getX(), pts[offset + 2].getY());
    }

    public void setCurve(QuadCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
    }

    /// The square of the distance of the control point from the segment
    /// joining the end points.
    public static double getFlatnessSq(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
        return Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly);
    }

    public static double getFlatness(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
        return Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly);
    }

    public static double getFlatnessSq(double[] coords, int offset) {
        return Line2D.ptSegDistSq(coords[offset], coords[offset + 1], coords[offset + 4], coords[offset + 5],
                coords[offset + 2], coords[offset + 3]);
    }

    public static double getFlatness(double[] coords, int offset) {
        return Line2D.ptSegDist(coords[offset], coords[offset + 1], coords[offset + 4], coords[offset + 5],
                coords[offset + 2], coords[offset + 3]);
    }

    public double getFlatnessSq() {
        return Line2D.ptSegDistSq(getX1(), getY1(), getX2(), getY2(), getCtrlX(), getCtrlY());
    }

    public double getFlatness() {
        return Line2D.ptSegDist(getX1(), getY1(), getX2(), getY2(), getCtrlX(), getCtrlY());
    }

    /// Splits this curve at its middle into two curves; either may be null
    /// or this curve itself.
    public void subdivide(QuadCurve2D left, QuadCurve2D right) {
        subdivide(this, left, right);
    }

    public static void subdivide(QuadCurve2D src, QuadCurve2D left, QuadCurve2D right) {
        double x1 = src.getX1();
        double y1 = src.getY1();
        double x2 = src.getX2();
        double y2 = src.getY2();
        double lx = (x1 + src.getCtrlX()) / 2.0;
        double ly = (y1 + src.getCtrlY()) / 2.0;
        double rx = (src.getCtrlX() + x2) / 2.0;
        double ry = (src.getCtrlY() + y2) / 2.0;
        double mx = (lx + rx) / 2.0;
        double my = (ly + ry) / 2.0;
        if (left != null) {
            left.setCurve(x1, y1, lx, ly, mx, my);
        }
        if (right != null) {
            right.setCurve(mx, my, rx, ry, x2, y2);
        }
    }

    /// Splits six coordinates into two curves of six; a destination may be
    /// null, and may be the source array where the ranges allow it.
    public static void subdivide(double[] src, int srcoff, double[] left, int leftoff, double[] right, int rightoff) {
        double x1 = src[srcoff];
        double y1 = src[srcoff + 1];
        double x2 = src[srcoff + 4];
        double y2 = src[srcoff + 5];
        double lx = (x1 + src[srcoff + 2]) / 2.0;
        double ly = (y1 + src[srcoff + 3]) / 2.0;
        double rx = (src[srcoff + 2] + x2) / 2.0;
        double ry = (src[srcoff + 3] + y2) / 2.0;
        double mx = (lx + rx) / 2.0;
        double my = (ly + ry) / 2.0;
        if (left != null) {
            left[leftoff] = x1;
            left[leftoff + 1] = y1;
            left[leftoff + 2] = lx;
            left[leftoff + 3] = ly;
            left[leftoff + 4] = mx;
            left[leftoff + 5] = my;
        }
        if (right != null) {
            right[rightoff] = mx;
            right[rightoff + 1] = my;
            right[rightoff + 2] = rx;
            right[rightoff + 3] = ry;
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
    }

    public static int solveQuadratic(double[] eqn) {
        return solveQuadratic(eqn, eqn);
    }

    /// Solves `eqn[2]*x*x + eqn[1]*x + eqn[0] = 0`, storing the real roots
    /// and returning how many there are, or -1 when the equation is a
    /// constant. A repeated root is stored twice, except a repeated root of
    /// zero, which is stored once.
    public static int solveQuadratic(double[] eqn, double[] res) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
        if (a == 0.0) {
            if (b == 0.0) {
                return -1;
            }
            res[0] = -c / b;
            return 1;
        }
        double d = b * b - 4.0 * a * c;
        if (d < 0.0) {
            return 0;
        }
        // add the root to b with matching sign so nothing cancels
        double q = -(b + (b < 0.0 ? -Math.sqrt(d) : Math.sqrt(d))) / 2.0;
        res[0] = q / a;
        if (q == 0.0) {
            return 1;
        }
        res[1] = c / q;
        return 2;
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
        it.quad(getCtrlX(), getCtrlY(), getX2(), getY2());
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
        return new Double(getX1(), getY1(), getCtrlX(), getCtrlY(), getX2(), getY2());
    }
}
