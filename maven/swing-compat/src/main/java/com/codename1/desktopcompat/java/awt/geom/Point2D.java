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

/// A location in two dimensional space, stored by a subclass in the
/// precision it chooses.
public abstract class Point2D implements Cloneable {

    /// A point stored in single precision.
    public static class Float extends Point2D {

        public float x;

        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Object clone() {
            return new Float(x, y);
        }

        @Override
        public String toString() {
            return "Point2D.Float[" + x + ", " + y + "]";
        }
    }

    /// A point stored in double precision.
    public static class Double extends Point2D {

        public double x;

        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Object clone() {
            return new Double(x, y);
        }

        @Override
        public String toString() {
            return "Point2D.Double[" + x + ", " + y + "]";
        }
    }

    protected Point2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    public static double distanceSq(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public double distanceSq(double px, double py) {
        return distanceSq(getX(), getY(), px, py);
    }

    public double distanceSq(Point2D pt) {
        return distanceSq(getX(), getY(), pt.getX(), pt.getY());
    }

    public double distance(double px, double py) {
        return Math.sqrt(distanceSq(getX(), getY(), px, py));
    }

    public double distance(Point2D pt) {
        return Math.sqrt(distanceSq(getX(), getY(), pt.getX(), pt.getY()));
    }

    /// Returns a copy of this point. A subclass outside this package that
    /// does not override it gets a double precision point back.
    @Override
    public Object clone() {
        return new Double(getX(), getY());
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX()) * 31 + java.lang.Double.doubleToLongBits(getY());
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point2D) {
            Point2D p = (Point2D) obj;
            return getX() == p.getX() && getY() == p.getY();
        }
        return false;
    }
}
