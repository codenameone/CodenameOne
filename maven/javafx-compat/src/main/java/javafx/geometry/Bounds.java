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

/// The immutable box around a node or any other object: a corner, a size
/// and, for three dimensional content, a depth.
public abstract class Bounds {

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double width;
    private final double height;
    private final double depth;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    /// Creates bounds from a corner and a size.
    protected Bounds(double minX, double minY, double minZ, double width, double height, double depth) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.maxX = minX + width;
        this.maxY = minY + height;
        this.maxZ = minZ + depth;
    }

    /// Returns the left edge.
    public final double getMinX() {
        return minX;
    }

    /// Returns the top edge.
    public final double getMinY() {
        return minY;
    }

    /// Returns the near edge.
    public final double getMinZ() {
        return minZ;
    }

    /// Returns the width.
    public final double getWidth() {
        return width;
    }

    /// Returns the height.
    public final double getHeight() {
        return height;
    }

    /// Returns the depth.
    public final double getDepth() {
        return depth;
    }

    /// Returns the right edge.
    public final double getMaxX() {
        return maxX;
    }

    /// Returns the bottom edge.
    public final double getMaxY() {
        return maxY;
    }

    /// Returns the far edge.
    public final double getMaxZ() {
        return maxZ;
    }

    /// Returns the horizontal center.
    public final double getCenterX() {
        return (getMaxX() + getMinX()) * 0.5;
    }

    /// Returns the vertical center.
    public final double getCenterY() {
        return (getMaxY() + getMinY()) * 0.5;
    }

    /// Returns the center in depth.
    public final double getCenterZ() {
        return (getMaxZ() + getMinZ()) * 0.5;
    }

    /// Returns whether a size is negative, which marks bounds of nothing.
    public abstract boolean isEmpty();

    /// Returns whether a point lies inside or on the edge.
    public abstract boolean contains(Point2D p);

    /// Returns whether a location lies inside or on the edge.
    public abstract boolean contains(double x, double y);

    /// Returns whether a location in space lies inside or on the edge.
    public abstract boolean contains(double x, double y, double z);

    /// Returns whether other bounds lie entirely inside these.
    public abstract boolean contains(Bounds b);

    /// Returns whether a rectangle lies entirely inside these bounds.
    public abstract boolean contains(double x, double y, double w, double h);

    /// Returns whether a box lies entirely inside these bounds.
    public abstract boolean contains(double x, double y, double z, double w, double h, double d);

    /// Returns whether other bounds overlap or touch these.
    public abstract boolean intersects(Bounds b);

    /// Returns whether a rectangle overlaps or touches these bounds.
    public abstract boolean intersects(double x, double y, double w, double h);

    /// Returns whether a box overlaps or touches these bounds.
    public abstract boolean intersects(double x, double y, double z, double w, double h, double d);
}
