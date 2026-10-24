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

/// The four distances inside the edges of a rectangle.
public class Insets {

    /// No distance on any side.
    public static final Insets EMPTY = new Insets(0, 0, 0, 0);

    private final double top;
    private final double right;
    private final double bottom;
    private final double left;

    /// Creates insets with a distance for each side.
    public Insets(@NamedArg("top") double top, @NamedArg("right") double right, @NamedArg("bottom") double bottom,
            @NamedArg("left") double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /// Creates insets with the same distance on every side.
    public Insets(@NamedArg("topRightBottomLeft") double topRightBottomLeft) {
        this(topRightBottomLeft, topRightBottomLeft, topRightBottomLeft, topRightBottomLeft);
    }

    /// Returns the distance at the top.
    public final double getTop() {
        return top;
    }

    /// Returns the distance at the right.
    public final double getRight() {
        return right;
    }

    /// Returns the distance at the bottom.
    public final double getBottom() {
        return bottom;
    }

    /// Returns the distance at the left.
    public final double getLeft() {
        return left;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Insets) {
            Insets other = (Insets) obj;
            return Geometry.same(top, other.top) && Geometry.same(right, other.right)
                    && Geometry.same(bottom, other.bottom) && Geometry.same(left, other.left);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Geometry.hash(Geometry.hash(Geometry.hash(Geometry.hash(17, top), right), bottom), left);
    }

    @Override
    public String toString() {
        return "Insets [top=" + top + ", right=" + right + ", bottom=" + bottom + ", left=" + left + "]";
    }
}
