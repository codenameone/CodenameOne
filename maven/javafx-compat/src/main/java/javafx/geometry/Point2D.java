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
package javafx.geometry;

import com.codename1.util.MathUtil;

import javafx.beans.NamedArg;

/// An immutable point in the plane, also used as a vector.
public class Point2D {

    /// The origin.
    public static final Point2D ZERO = new Point2D(0.0, 0.0);

    private final double x;
    private final double y;

    /// Creates a point.
    public Point2D(@NamedArg("x") double x, @NamedArg("y") double y) {
        this.x = x;
        this.y = y;
    }

    /// Returns the horizontal coordinate.
    public final double getX() {
        return x;
    }

    /// Returns the vertical coordinate.
    public final double getY() {
        return y;
    }

    /// Returns the distance to a location.
    public double distance(double x1, double y1) {
        double a = getX() - x1;
        double b = getY() - y1;
        return Math.sqrt(a * a + b * b);
    }

    /// Returns the distance to another point.
    public double distance(Point2D point) {
        return distance(point.getX(), point.getY());
    }

    /// Returns this point moved by an offset.
    public Point2D add(double x, double y) {
        return new Point2D(getX() + x, getY() + y);
    }

    /// Returns the sum of this vector and another.
    public Point2D add(Point2D point) {
        return add(point.getX(), point.getY());
    }

    /// Returns this point moved back by an offset.
    public Point2D subtract(double x, double y) {
        return new Point2D(getX() - x, getY() - y);
    }

    /// Returns this vector less another.
    public Point2D subtract(Point2D point) {
        return subtract(point.getX(), point.getY());
    }

    /// Returns this vector scaled by a factor.
    public Point2D multiply(double factor) {
        return new Point2D(getX() * factor, getY() * factor);
    }

    /// Returns the vector of the same direction and length one; the zero
    /// vector for the zero vector.
    public Point2D normalize() {
        final double mag = magnitude();
        if (mag == 0.0) {
            return new Point2D(0.0, 0.0);
        }
        return new Point2D(getX() / mag, getY() / mag);
    }

    /// Returns the point halfway to a location.
    public Point2D midpoint(double x, double y) {
        return new Point2D(x + (getX() - x) / 2.0, y + (getY() - y) / 2.0);
    }

    /// Returns the point halfway to another point.
    public Point2D midpoint(Point2D point) {
        return midpoint(point.getX(), point.getY());
    }

    /// Returns the angle in degrees, from 0 to 180, between this vector and
    /// the vector to a location.
    public double angle(double x, double y) {
        final double ax = getX();
        final double ay = getY();
        final double delta = (ax * x + ay * y) / Math.sqrt((ax * ax + ay * ay) * (x * x + y * y));
        if (delta > 1.0) {
            return 0.0;
        }
        if (delta < -1.0) {
            return 180.0;
        }
        return Math.toDegrees(MathUtil.acos(delta));
    }

    /// Returns the angle in degrees between this vector and another.
    public double angle(Point2D point) {
        return angle(point.getX(), point.getY());
    }

    /// Returns the angle in degrees at this point between the directions
    /// to two others.
    public double angle(Point2D p1, Point2D p2) {
        final double x = getX();
        final double y = getY();
        final double ax = p1.getX() - x;
        final double ay = p1.getY() - y;
        final double bx = p2.getX() - x;
        final double by = p2.getY() - y;
        final double delta = (ax * bx + ay * by) / Math.sqrt((ax * ax + ay * ay) * (bx * bx + by * by));
        if (delta > 1.0) {
            return 0.0;
        }
        if (delta < -1.0) {
            return 180.0;
        }
        return Math.toDegrees(MathUtil.acos(delta));
    }

    /// Returns the length of this vector.
    public double magnitude() {
        final double x = getX();
        final double y = getY();
        return Math.sqrt(x * x + y * y);
    }

    /// Returns the dot product of this vector and another.
    public double dotProduct(double x, double y) {
        return getX() * x + getY() * y;
    }

    /// Returns the dot product of this vector and another.
    public double dotProduct(Point2D vector) {
        return dotProduct(vector.getX(), vector.getY());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Point2D) {
            Point2D other = (Point2D) obj;
            return Geometry.same(getX(), other.getX()) && Geometry.same(getY(), other.getY());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Geometry.hash(Geometry.hash(7, getX()), getY());
    }

    @Override
    public String toString() {
        return "Point2D [x = " + getX() + ", y = " + getY() + "]";
    }
}
