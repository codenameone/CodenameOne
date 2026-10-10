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

/// A rectangle whose corners are quarter ellipses of a given width and
/// height.
///
/// The arc size is the full width and height of the ellipse a corner is cut
/// from, and is limited to the size of the rectangle. The outline starts on
/// the left side below the top-left corner and runs through the bottom,
/// right and top.
public abstract class RoundRectangle2D extends RectangularShape {

    /// How far a corner's control points sit from the corner of the frame,
    /// as a fraction of the corner radius.
    private static final double CORNER = 1.0 - Ellipse2D.KAPPA;

    /// A rounded rectangle stored in single precision.
    public static class Float extends RoundRectangle2D {

        public float x;
        public float y;
        public float width;
        public float height;
        public float arcwidth;
        public float archeight;

        public Float() {
        }

        public Float(float x, float y, float w, float h, float arcw, float arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRoundRect(float x, float y, float w, float h, float arcw, float arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        @Override
        public void setRoundRect(double x, double y, double w, double h, double arcw, double arch) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
            this.arcwidth = (float) arcw;
            this.archeight = (float) arch;
        }

        @Override
        public void setRoundRect(RoundRectangle2D rr) {
            setRoundRect(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight(), rr.getArcWidth(), rr.getArcHeight());
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(x, y, width, height);
        }

        @Override
        public Object clone() {
            return new Float(x, y, width, height, arcwidth, archeight);
        }
    }

    /// A rounded rectangle stored in double precision.
    public static class Double extends RoundRectangle2D {

        public double x;
        public double y;
        public double width;
        public double height;
        public double arcwidth;
        public double archeight;

        public Double() {
        }

        public Double(double x, double y, double w, double h, double arcw, double arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRoundRect(double x, double y, double w, double h, double arcw, double arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        @Override
        public void setRoundRect(RoundRectangle2D rr) {
            setRoundRect(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight(), rr.getArcWidth(), rr.getArcHeight());
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public Object clone() {
            return new Double(x, y, width, height, arcwidth, archeight);
        }
    }

    protected RoundRectangle2D() {
    }

    public abstract double getArcWidth();

    public abstract double getArcHeight();

    public abstract void setRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight);

    public void setRoundRect(RoundRectangle2D rr) {
        setRoundRect(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight(), rr.getArcWidth(), rr.getArcHeight());
    }

    /// Moves and resizes the frame, keeping the arc size.
    @Override
    public void setFrame(double x, double y, double w, double h) {
        setRoundRect(x, y, w, h, getArcWidth(), getArcHeight());
    }

    /// The horizontal corner radius actually used.
    private double radiusX() {
        return Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
    }

    /// The vertical corner radius actually used.
    private double radiusY() {
        return Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
    }

    /// How far outside the corner ellipses the nearest point of a box lies,
    /// where the box overlaps the frame: below one it reaches the shape.
    ///
    /// The shape is every point within the corner ellipse of the core
    /// rectangle left when the frame is inset by the radii, so the measure
    /// is the box's gap to that core on each axis, scaled by the radius.
    private double cornerMeasure(double x0, double y0, double x1, double y1) {
        double rx = radiusX();
        double ry = radiusY();
        if (rx <= 0.0 || ry <= 0.0) {
            return 0.0;
        }
        double coreX0 = getX() + rx;
        double coreX1 = getX() + getWidth() - rx;
        double coreY0 = getY() + ry;
        double coreY1 = getY() + getHeight() - ry;
        double gapX = Math.max(0.0, Math.max(coreX0 - x1, x0 - coreX1)) / rx;
        double gapY = Math.max(0.0, Math.max(coreY0 - y1, y0 - coreY1)) / ry;
        return gapX * gapX + gapY * gapY;
    }

    @Override
    public boolean contains(double x, double y) {
        if (isEmpty()) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        if (x < x0 || y < y0 || x >= x0 + getWidth() || y >= y0 + getHeight()) {
            return false;
        }
        return cornerMeasure(x, y, x, y) <= 1.0;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0.0 || h <= 0.0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        if (x + w <= x0 || y + h <= y0 || x >= x0 + getWidth() || y >= y0 + getHeight()) {
            return false;
        }
        return cornerMeasure(x, y, x + w, y + h) < 1.0;
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0.0 || h <= 0.0) {
            return false;
        }
        // the shape is convex, so the corners decide
        return contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(x + w, y + h);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 10);
        double x0 = getX();
        double y0 = getY();
        double x1 = x0 + getWidth();
        double y1 = y0 + getHeight();
        double rx = radiusX();
        double ry = radiusY();
        double cx = rx * CORNER;
        double cy = ry * CORNER;
        it.move(x0, y0 + ry);
        it.line(x0, y1 - ry);
        it.cubic(x0, y1 - cy, x0 + cx, y1, x0 + rx, y1);
        it.line(x1 - rx, y1);
        it.cubic(x1 - cx, y1, x1, y1 - cy, x1, y1 - ry);
        it.line(x1, y0 + ry);
        it.cubic(x1, y0 + cy, x1 - cx, y0, x1 - rx, y0);
        it.line(x0 + rx, y0);
        it.cubic(x0 + cx, y0, x0, y0 + cy, x0, y0 + ry);
        it.close();
        return it;
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getY());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getWidth());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getHeight());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getArcWidth());
        bits = bits * 31 + java.lang.Double.doubleToLongBits(getArcHeight());
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RoundRectangle2D) {
            RoundRectangle2D r = (RoundRectangle2D) obj;
            return getX() == r.getX() && getY() == r.getY()
                    && getWidth() == r.getWidth() && getHeight() == r.getHeight()
                    && getArcWidth() == r.getArcWidth() && getArcHeight() == r.getArcHeight();
        }
        return false;
    }

    /// Returns a copy of this shape. A subclass from elsewhere that does not
    /// override it gets a double precision rounded rectangle back.
    @Override
    public Object clone() {
        return new Double(getX(), getY(), getWidth(), getHeight(), getArcWidth(), getArcHeight());
    }
}
