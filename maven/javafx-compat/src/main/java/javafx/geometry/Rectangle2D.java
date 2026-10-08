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

import javafx.beans.NamedArg;

/// An immutable rectangle given by its upper left corner and its size.
public class Rectangle2D {

    /// The rectangle at the origin with no size.
    public static final Rectangle2D EMPTY = new Rectangle2D(0, 0, 0, 0);

    private final double minX;
    private final double minY;
    private final double width;
    private final double height;
    private final double maxX;
    private final double maxY;

    /// Creates a rectangle.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the width or the height is negative
    public Rectangle2D(@NamedArg("minX") double minX, @NamedArg("minY") double minY,
            @NamedArg("width") double width, @NamedArg("height") double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Both width and height must be >= 0");
        }
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.maxX = minX + width;
        this.maxY = minY + height;
    }

    /// Returns the left edge.
    public double getMinX() {
        return minX;
    }

    /// Returns the top edge.
    public double getMinY() {
        return minY;
    }

    /// Returns the width.
    public double getWidth() {
        return width;
    }

    /// Returns the height.
    public double getHeight() {
        return height;
    }

    /// Returns the right edge.
    public double getMaxX() {
        return maxX;
    }

    /// Returns the bottom edge.
    public double getMaxY() {
        return maxY;
    }

    /// Returns whether a point lies inside or on the edge.
    public boolean contains(Point2D p) {
        if (p == null) {
            return false;
        }
        return contains(p.getX(), p.getY());
    }

    /// Returns whether a location lies inside or on the edge.
    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    /// Returns whether another rectangle lies entirely inside this one.
    public boolean contains(Rectangle2D r) {
        if (r == null) {
            return false;
        }
        return r.minX >= minX && r.minY >= minY && r.maxX <= maxX && r.maxY <= maxY;
    }

    /// Returns whether a rectangle lies entirely inside this one.
    public boolean contains(double x, double y, double w, double h) {
        return x >= minX && y >= minY && w <= maxX - x && h <= maxY - y;
    }

    /// Returns whether another rectangle overlaps or touches this one.
    public boolean intersects(Rectangle2D r) {
        if (r == null) {
            return false;
        }
        return r.maxX > minX && r.maxY > minY && r.minX < maxX && r.minY < maxY;
    }

    /// Returns whether a rectangle overlaps this one.
    public boolean intersects(double x, double y, double w, double h) {
        return x < maxX && y < maxY && x + w > minX && y + h > minY;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D other = (Rectangle2D) obj;
            return Geometry.same(minX, other.minX) && Geometry.same(minY, other.minY)
                    && Geometry.same(width, other.width) && Geometry.same(height, other.height);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Geometry.hash(Geometry.hash(Geometry.hash(Geometry.hash(7, minX), minY), width), height);
    }

    @Override
    public String toString() {
        return "Rectangle2D [minX = " + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY + ", width="
                + width + ", height=" + height + "]";
    }
}
