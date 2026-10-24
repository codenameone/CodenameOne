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
import javafx.geometry.Side;

/// Where a background image is placed in its region: a distance from one
/// side for each direction. A distance that is a percentage, written
/// 0 to 1, places that share of the image at that share of the region,
/// so 0.5 centres it.
public class BackgroundPosition {

    /// The top left corner.
    public static final BackgroundPosition DEFAULT = new BackgroundPosition(Side.LEFT, 0, true, Side.TOP, 0, true);

    /// The middle.
    public static final BackgroundPosition CENTER =
            new BackgroundPosition(Side.LEFT, 0.5, true, Side.TOP, 0.5, true);

    private final Side horizontalSide;
    private final Side verticalSide;
    private final double horizontalPosition;
    private final double verticalPosition;
    private final boolean horizontalAsPercentage;
    private final boolean verticalAsPercentage;

    /// Creates a position. The horizontal side is `LEFT` or `RIGHT`, the
    /// vertical one `TOP` or `BOTTOM`; `null` is the left and the top.
    public BackgroundPosition(@NamedArg("horizontalSide") Side horizontalSide,
            @NamedArg("horizontalPosition") double horizontalPosition,
            @NamedArg("horizontalAsPercentage") boolean horizontalAsPercentage,
            @NamedArg("verticalSide") Side verticalSide, @NamedArg("verticalPosition") double verticalPosition,
            @NamedArg("verticalAsPercentage") boolean verticalAsPercentage) {
        if (horizontalSide == Side.TOP || horizontalSide == Side.BOTTOM) {
            throw new IllegalArgumentException("The horizontalSide must be LEFT or RIGHT");
        }
        if (verticalSide == Side.LEFT || verticalSide == Side.RIGHT) {
            throw new IllegalArgumentException("The verticalSide must be TOP or BOTTOM");
        }
        this.horizontalSide = horizontalSide == null ? Side.LEFT : horizontalSide;
        this.verticalSide = verticalSide == null ? Side.TOP : verticalSide;
        this.horizontalPosition = horizontalPosition;
        this.verticalPosition = verticalPosition;
        this.horizontalAsPercentage = horizontalAsPercentage;
        this.verticalAsPercentage = verticalAsPercentage;
    }

    /// Returns the side the horizontal distance is from.
    public final Side getHorizontalSide() {
        return horizontalSide;
    }

    /// Returns the side the vertical distance is from.
    public final Side getVerticalSide() {
        return verticalSide;
    }

    /// Returns the horizontal distance.
    public final double getHorizontalPosition() {
        return horizontalPosition;
    }

    /// Returns the vertical distance.
    public final double getVerticalPosition() {
        return verticalPosition;
    }

    /// Returns whether the horizontal distance is a share, 0 to 1.
    public final boolean isHorizontalAsPercentage() {
        return horizontalAsPercentage;
    }

    /// Returns whether the vertical distance is a share, 0 to 1.
    public final boolean isVerticalAsPercentage() {
        return verticalAsPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackgroundPosition)) {
            return false;
        }
        BackgroundPosition p = (BackgroundPosition) o;
        return horizontalSide == p.horizontalSide && verticalSide == p.verticalSide
                && horizontalPosition == p.horizontalPosition && verticalPosition == p.verticalPosition
                && horizontalAsPercentage == p.horizontalAsPercentage
                && verticalAsPercentage == p.verticalAsPercentage;
    }

    @Override
    public int hashCode() {
        return (int) (horizontalPosition * 31 + verticalPosition * 17) + horizontalSide.hashCode() * 7
                + verticalSide.hashCode() + (horizontalAsPercentage ? 3 : 0) + (verticalAsPercentage ? 5 : 0);
    }
}
