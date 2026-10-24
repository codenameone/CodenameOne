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

/// Bounds given directly by a corner and a size.
public class BoundingBox extends Bounds {

    /// Creates a box in space.
    public BoundingBox(@NamedArg("minX") double minX, @NamedArg("minY") double minY, @NamedArg("minZ") double minZ,
            @NamedArg("width") double width, @NamedArg("height") double height, @NamedArg("depth") double depth) {
        super(minX, minY, minZ, width, height, depth);
    }

    /// Creates a flat box.
    public BoundingBox(@NamedArg("minX") double minX, @NamedArg("minY") double minY,
            @NamedArg("width") double width, @NamedArg("height") double height) {
        super(minX, minY, 0, width, height, 0);
    }

    @Override
    public boolean isEmpty() {
        return getMaxX() < getMinX() || getMaxY() < getMinY() || getMaxZ() < getMinZ();
    }

    @Override
    public boolean contains(Point2D p) {
        if (p == null) {
            return false;
        }
        return contains(p.getX(), p.getY(), 0.0f);
    }

    @Override
    public boolean contains(double x, double y) {
        return contains(x, y, 0.0f);
    }

    @Override
    public boolean contains(double x, double y, double z) {
        if (isEmpty()) {
            return false;
        }
        return x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY() && z >= getMinZ()
                && z <= getMaxZ();
    }

    @Override
    public boolean contains(Bounds b) {
        if (b == null || b.isEmpty()) {
            return false;
        }
        return contains(b.getMinX(), b.getMinY(), b.getMinZ(), b.getWidth(), b.getHeight(), b.getDepth());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) && contains(x + w, y + h);
    }

    @Override
    public boolean contains(double x, double y, double z, double w, double h, double d) {
        return contains(x, y, z) && contains(x + w, y + h, z + d);
    }

    @Override
    public boolean intersects(Bounds b) {
        if (b == null || b.isEmpty()) {
            return false;
        }
        return intersects(b.getMinX(), b.getMinY(), b.getMinZ(), b.getWidth(), b.getHeight(), b.getDepth());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return intersects(x, y, 0, w, h, 0);
    }

    @Override
    public boolean intersects(double x, double y, double z, double w, double h, double d) {
        if (isEmpty() || w < 0 || h < 0 || d < 0) {
            return false;
        }
        return x + w >= getMinX() && y + h >= getMinY() && z + d >= getMinZ() && x <= getMaxX() && y <= getMaxY()
                && z <= getMaxZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof BoundingBox) {
            BoundingBox other = (BoundingBox) obj;
            return Geometry.same(getMinX(), other.getMinX()) && Geometry.same(getMinY(), other.getMinY())
                    && Geometry.same(getMinZ(), other.getMinZ()) && Geometry.same(getWidth(), other.getWidth())
                    && Geometry.same(getHeight(), other.getHeight()) && Geometry.same(getDepth(), other.getDepth());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = Geometry.hash(Geometry.hash(Geometry.hash(7, getMinX()), getMinY()), getMinZ());
        return Geometry.hash(Geometry.hash(Geometry.hash(hash, getWidth()), getHeight()), getDepth());
    }

    @Override
    public String toString() {
        return "BoundingBox [minX:" + getMinX() + ", minY:" + getMinY() + ", minZ:" + getMinZ() + ", width:"
                + getWidth() + ", height:" + getHeight() + ", depth:" + getDepth() + ", maxX:" + getMaxX()
                + ", maxY:" + getMaxY() + ", maxZ:" + getMaxZ() + "]";
    }
}
