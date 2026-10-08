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

/// A rectangle given by a corner and a size, stored by a subclass in the
/// precision it chooses.
///
/// A point on the left or top edge is inside and one on the right or bottom
/// edge is not; a rectangle with no area contains and intersects nothing.
public abstract class Rectangle2D extends RectangularShape {

    public static final int OUT_LEFT = 1;
    public static final int OUT_TOP = 2;
    public static final int OUT_RIGHT = 4;
    public static final int OUT_BOTTOM = 8;

    /// A rectangle stored in single precision.
    public static class Float extends Rectangle2D {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public void setRect(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        @Override
        public void setRect(Rectangle2D r) {
            setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public int outcode(double x, double y) {
            return outcode(this.x, this.y, width, height, x, y);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Float(x, y, width, height);
        }

        @Override
        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest = r instanceof Float ? (Rectangle2D) new Float() : new Double();
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        @Override
        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = r instanceof Float ? (Rectangle2D) new Float() : new Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        @Override
        public Object clone() {
            return new Float(x, y, width, height);
        }

        @Override
        public String toString() {
            return "java.awt.geom.Rectangle2D$Float[x=" + x + ",y=" + y + ",w=" + width + ",h=" + height + "]";
        }
    }

    /// A rectangle stored in double precision.
    public static class Double extends Rectangle2D {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public void setRect(Rectangle2D r) {
            setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
        }

        @Override
        public int outcode(double x, double y) {
            return outcode(this.x, this.y, width, height, x, y);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Double(x, y, width, height);
        }

        @Override
        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        @Override
        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        @Override
        public Object clone() {
            return new Double(x, y, width, height);
        }

        @Override
        public String toString() {
            return "java.awt.geom.Rectangle2D$Double[x=" + x + ",y=" + y + ",w=" + width + ",h=" + height + "]";
        }
    }

    protected Rectangle2D() {
    }

    /// The `OUT_` flags of a point against a rectangle. A rectangle with no
    /// extent on an axis puts every point outside on both sides of it.
    static int outcode(double rx, double ry, double rw, double rh, double x, double y) {
        int out = 0;
        if (rw <= 0.0) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (x < rx) {
            out |= OUT_LEFT;
        } else if (x > rx + rw) {
            out |= OUT_RIGHT;
        }
        if (rh <= 0.0) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (y < ry) {
            out |= OUT_TOP;
        } else if (y > ry + rh) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    public abstract void setRect(double x, double y, double w, double h);

    public void setRect(Rectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /// Whether a line segment touches this rectangle, edges included.
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        int out2 = outcode(x2, y2);
        if (out2 == 0) {
            return true;
        }
        // walk the first end towards the second, one rectangle edge at a
        // time, until it is inside or both ends are beyond the same edge
        int out1 = outcode(x1, y1);
        while (out1 != 0) {
            if ((out1 & out2) != 0) {
                return false;
            }
            if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
                double x = (out1 & OUT_RIGHT) != 0 ? getX() + getWidth() : getX();
                y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
                x1 = x;
            } else {
                double y = (out1 & OUT_BOTTOM) != 0 ? getY() + getHeight() : getY();
                x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                y1 = y;
            }
            out1 = outcode(x1, y1);
        }
        return true;
    }

    public boolean intersectsLine(Line2D l) {
        return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    public abstract int outcode(double x, double y);

    public int outcode(Point2D p) {
        return outcode(p.getX(), p.getY());
    }

    @Override
    public void setFrame(double x, double y, double w, double h) {
        setRect(x, y, w, h);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Double(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return x >= x0 && y >= y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0.0 || h <= 0.0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight();
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0.0 || h <= 0.0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return x >= x0 && y >= y0 && x + w <= x0 + getWidth() && y + h <= y0 + getHeight();
    }

    public abstract Rectangle2D createIntersection(Rectangle2D r);

    public abstract Rectangle2D createUnion(Rectangle2D r);

    /// Stores the overlap of two rectangles. Rectangles that do not overlap
    /// give a result with a negative width or height.
    public static void intersect(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.max(src1.getMinX(), src2.getMinX());
        double y1 = Math.max(src1.getMinY(), src2.getMinY());
        double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
        dest.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public static void union(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.min(src1.getMinX(), src2.getMinX());
        double y1 = Math.min(src1.getMinY(), src2.getMinY());
        double x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dest.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    public void add(double newx, double newy) {
        double x1 = Math.min(getMinX(), newx);
        double x2 = Math.max(getMaxX(), newx);
        double y1 = Math.min(getMinY(), newy);
        double y2 = Math.max(getMaxY(), newy);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point2D pt) {
        add(pt.getX(), pt.getY());
    }

    public void add(Rectangle2D r) {
        double x1 = Math.min(getMinX(), r.getMinX());
        double x2 = Math.max(getMaxX(), r.getMaxX());
        double y1 = Math.min(getMinY(), r.getMinY());
        double y2 = Math.max(getMaxY(), r.getMaxY());
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 6);
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        it.move(x, y);
        it.line(x + w, y);
        it.line(x + w, y + h);
        it.line(x, y + h);
        it.line(x, y);
        it.close();
        return it;
    }

    /// A rectangle is already flat, so this is the plain outline.
    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getY());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getWidth());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getHeight());
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D) obj;
            return getX() == r.getX() && getY() == r.getY()
                    && getWidth() == r.getWidth() && getHeight() == r.getHeight();
        }
        return false;
    }
}
