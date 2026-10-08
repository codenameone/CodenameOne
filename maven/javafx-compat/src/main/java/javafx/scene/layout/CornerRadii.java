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
package javafx.scene.layout;

import javafx.beans.NamedArg;

/// The radii of the four corners of a rounded rectangle, in logical pixels
/// or as a fraction of the rectangle's size.
public class CornerRadii {

    /// Square corners.
    public static final CornerRadii EMPTY = new CornerRadii(0);

    private final double topLeft;
    private final double topRight;
    private final double bottomRight;
    private final double bottomLeft;
    private final boolean percent;

    /// Creates the same radius on every corner.
    public CornerRadii(@NamedArg("radius") double radius) {
        this(radius, radius, radius, radius, false);
    }

    /// Creates the same radius on every corner, optionally as a fraction
    /// of the size.
    public CornerRadii(@NamedArg("radius") double radius, @NamedArg("asPercent") boolean asPercent) {
        this(radius, radius, radius, radius, asPercent);
    }

    /// Creates a radius per corner, clockwise from the top left.
    public CornerRadii(@NamedArg("topLeft") double topLeft, @NamedArg("topRight") double topRight,
            @NamedArg("bottomRight") double bottomRight, @NamedArg("bottomLeft") double bottomLeft,
            @NamedArg("asPercent") boolean asPercent) {
        if (topLeft < 0 || topRight < 0 || bottomRight < 0 || bottomLeft < 0) {
            throw new IllegalArgumentException("No radii value may be < 0");
        }
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
        this.percent = asPercent;
    }

    /// Returns the horizontal radius of the top left corner.
    public final double getTopLeftHorizontalRadius() {
        return topLeft;
    }

    /// Returns the vertical radius of the top left corner.
    public final double getTopLeftVerticalRadius() {
        return topLeft;
    }

    /// Returns the vertical radius of the top right corner.
    public final double getTopRightVerticalRadius() {
        return topRight;
    }

    /// Returns the horizontal radius of the top right corner.
    public final double getTopRightHorizontalRadius() {
        return topRight;
    }

    /// Returns the horizontal radius of the bottom right corner.
    public final double getBottomRightHorizontalRadius() {
        return bottomRight;
    }

    /// Returns the vertical radius of the bottom right corner.
    public final double getBottomRightVerticalRadius() {
        return bottomRight;
    }

    /// Returns the vertical radius of the bottom left corner.
    public final double getBottomLeftVerticalRadius() {
        return bottomLeft;
    }

    /// Returns the horizontal radius of the bottom left corner.
    public final double getBottomLeftHorizontalRadius() {
        return bottomLeft;
    }

    /// Returns whether the top left radius is a fraction of the size.
    public final boolean isTopLeftHorizontalRadiusAsPercentage() {
        return percent;
    }

    /// Returns whether the top right radius is a fraction of the size.
    public final boolean isTopRightHorizontalRadiusAsPercentage() {
        return percent;
    }

    /// Returns whether the bottom right radius is a fraction of the size.
    public final boolean isBottomRightHorizontalRadiusAsPercentage() {
        return percent;
    }

    /// Returns whether the bottom left radius is a fraction of the size.
    public final boolean isBottomLeftHorizontalRadiusAsPercentage() {
        return percent;
    }

    /// Returns whether every corner has the same radius.
    public final boolean isUniform() {
        return topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CornerRadii)) {
            return false;
        }
        CornerRadii that = (CornerRadii) o;
        return Double.compare(topLeft, that.topLeft) == 0 && Double.compare(topRight, that.topRight) == 0
                && Double.compare(bottomRight, that.bottomRight) == 0
                && Double.compare(bottomLeft, that.bottomLeft) == 0 && percent == that.percent;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(topLeft) * 31 + Double.doubleToLongBits(topRight) * 17
                + Double.doubleToLongBits(bottomRight) * 7 + Double.doubleToLongBits(bottomLeft);
        return (int) (bits ^ (bits >>> 32)) + (percent ? 1 : 0);
    }

    @Override
    public String toString() {
        return "CornerRadii [" + topLeft + ", " + topRight + ", " + bottomRight + ", " + bottomLeft
                + (percent ? " %" : "") + "]";
    }
}
