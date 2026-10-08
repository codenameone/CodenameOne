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

/// An immutable width and height.
public class Dimension2D {

    private final double width;
    private final double height;

    /// Creates a size.
    public Dimension2D(@NamedArg("width") double width, @NamedArg("height") double height) {
        this.width = width;
        this.height = height;
    }

    /// Returns the width.
    public final double getWidth() {
        return width;
    }

    /// Returns the height.
    public final double getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Dimension2D) {
            Dimension2D other = (Dimension2D) obj;
            return Geometry.same(getWidth(), other.getWidth()) && Geometry.same(getHeight(), other.getHeight());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Geometry.hash(Geometry.hash(7, getWidth()), getHeight());
    }

    @Override
    public String toString() {
        return "Dimension2D [width = " + getWidth() + ", height = " + getHeight() + "]";
    }
}
