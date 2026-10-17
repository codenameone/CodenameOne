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

/// The widths of the four sides of a border, in logical pixels.
public final class BorderWidths {

    /// The width that stands for "take it from the stroke"; treated as 1.
    public static final double AUTO = -1;

    /// One logical pixel on every side.
    public static final BorderWidths DEFAULT = new BorderWidths(1, 1, 1, 1);

    /// No width on any side.
    public static final BorderWidths EMPTY = new BorderWidths(0, 0, 0, 0);

    private final double top;
    private final double right;
    private final double bottom;
    private final double left;
    private final boolean topAsPercentage;
    private final boolean rightAsPercentage;
    private final boolean bottomAsPercentage;
    private final boolean leftAsPercentage;

    /// Creates the same width on every side.
    public BorderWidths(@NamedArg("width") double width) {
        this(width, width, width, width);
    }

    /// Creates a width per side.
    public BorderWidths(@NamedArg("top") double top, @NamedArg("right") double right,
            @NamedArg("bottom") double bottom, @NamedArg("left") double left) {
        this(top, right, bottom, left, false, false, false, false);
    }

    /// Creates a width per side, each a length or a share written 0 to 1
    /// of the side it is measured against.
    public BorderWidths(@NamedArg("top") double top, @NamedArg("right") double right,
            @NamedArg("bottom") double bottom, @NamedArg("left") double left,
            @NamedArg("topAsPercentage") boolean topAsPercentage,
            @NamedArg("rightAsPercentage") boolean rightAsPercentage,
            @NamedArg("bottomAsPercentage") boolean bottomAsPercentage,
            @NamedArg("leftAsPercentage") boolean leftAsPercentage) {
        if ((top < 0 && top != AUTO) || (right < 0 && right != AUTO) || (bottom < 0 && bottom != AUTO)
                || (left < 0 && left != AUTO)) {
            throw new IllegalArgumentException("None of the widths can be < 0");
        }
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.topAsPercentage = topAsPercentage;
        this.rightAsPercentage = rightAsPercentage;
        this.bottomAsPercentage = bottomAsPercentage;
        this.leftAsPercentage = leftAsPercentage;
    }

    /// Returns whether the top width is a share, 0 to 1.
    public final boolean isTopAsPercentage() {
        return topAsPercentage;
    }

    /// Returns whether the right width is a share, 0 to 1.
    public final boolean isRightAsPercentage() {
        return rightAsPercentage;
    }

    /// Returns whether the bottom width is a share, 0 to 1.
    public final boolean isBottomAsPercentage() {
        return bottomAsPercentage;
    }

    /// Returns whether the left width is a share, 0 to 1.
    public final boolean isLeftAsPercentage() {
        return leftAsPercentage;
    }

    /// Returns the width of the top side.
    public final double getTop() {
        return top;
    }

    /// Returns the width of the right side.
    public final double getRight() {
        return right;
    }

    /// Returns the width of the bottom side.
    public final double getBottom() {
        return bottom;
    }

    /// Returns the width of the left side.
    public final double getLeft() {
        return left;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderWidths)) {
            return false;
        }
        BorderWidths that = (BorderWidths) o;
        return Double.compare(top, that.top) == 0 && Double.compare(right, that.right) == 0
                && Double.compare(bottom, that.bottom) == 0 && Double.compare(left, that.left) == 0
                && topAsPercentage == that.topAsPercentage && rightAsPercentage == that.rightAsPercentage
                && bottomAsPercentage == that.bottomAsPercentage && leftAsPercentage == that.leftAsPercentage;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(top) * 31 + Double.doubleToLongBits(right) * 17
                + Double.doubleToLongBits(bottom) * 7 + Double.doubleToLongBits(left);
        return (int) (bits ^ (bits >>> 32));
    }
}
