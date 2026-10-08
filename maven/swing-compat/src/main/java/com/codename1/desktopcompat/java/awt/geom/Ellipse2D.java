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

/// The ellipse inscribed in a rectangular frame.
///
/// Its outline is four cubic Bezier curves, one per quarter, starting at the
/// middle of the right side and running through the bottom, left and top.
public abstract class Ellipse2D extends RectangularShape {

    /// How far a quarter-circle's control points sit from its ends, as a
    /// fraction of the radius: four thirds of the tangent of a sixteenth of
    /// a turn.
    static final double KAPPA = 4.0 / 3.0 * (Math.sqrt(2.0) - 1.0);

    /// An ellipse stored in single precision.
    public static class Float extends Ellipse2D {

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

        public void setFrame(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public void setFrame(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(x, y, width, height);
        }

        @Override
        public Object clone() {
            return new Float(x, y, width, height);
        }
    }

    /// An ellipse stored in double precision.
    public static class Double extends Ellipse2D {

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
        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(x, y, width, height);
        }

        @Override
        public Object clone() {
            return new Double(x, y, width, height);
        }
    }

    protected Ellipse2D() {
    }

    @Override
    public boolean contains(double x, double y) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        // in the frame where the ellipse is the circle of diameter one
        // about the origin
        double nx = (x - getX()) / w - 0.5;
        double ny = (y - getY()) / h - 0.5;
        return nx * nx + ny * ny < 0.25;
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        double ew = getWidth();
        double eh = getHeight();
        if (w <= 0.0 || h <= 0.0 || ew <= 0.0 || eh <= 0.0) {
            return false;
        }
        // the rectangle in the unit-circle frame, then its point nearest
        // the centre
        double nx0 = (x - getX()) / ew - 0.5;
        double nx1 = nx0 + w / ew;
        double ny0 = (y - getY()) / eh - 0.5;
        double ny1 = ny0 + h / eh;
        double nearX = nx0 > 0.0 ? nx0 : (nx1 < 0.0 ? nx1 : 0.0);
        double nearY = ny0 > 0.0 ? ny0 : (ny1 < 0.0 ? ny1 : 0.0);
        return nearX * nearX + nearY * nearY < 0.25;
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        // an ellipse is convex, so the corners decide
        return contains(x, y) && contains(x + w, y)
                && contains(x, y + h) && contains(x + w, y + h);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        SegmentIterator it = new SegmentIterator(PathIterator.WIND_NON_ZERO, at, 6);
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        double near = 0.5 - KAPPA / 2.0;
        double far = 0.5 + KAPPA / 2.0;
        it.move(x + w, y + h * 0.5);
        it.cubic(x + w, y + h * far, x + w * far, y + h, x + w * 0.5, y + h);
        it.cubic(x + w * near, y + h, x, y + h * far, x, y + h * 0.5);
        it.cubic(x, y + h * near, x + w * near, y, x + w * 0.5, y);
        it.cubic(x + w * far, y, x + w, y + h * near, x + w, y + h * 0.5);
        it.close();
        return it;
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
        if (obj instanceof Ellipse2D) {
            Ellipse2D e = (Ellipse2D) obj;
            return getX() == e.getX() && getY() == e.getY()
                    && getWidth() == e.getWidth() && getHeight() == e.getHeight();
        }
        return false;
    }

    /// Returns a copy of this ellipse. A subclass from elsewhere that does
    /// not override it gets a double precision ellipse back.
    @Override
    public Object clone() {
        return new Double(getX(), getY(), getWidth(), getHeight());
    }
}
