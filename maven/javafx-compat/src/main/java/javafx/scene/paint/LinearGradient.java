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
package javafx.scene.paint;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.NamedArg;

/// A paint that blends colours along a line. With `proportional` set the
/// line is given in fractions of the bounds of the shape being filled;
/// otherwise in the shape's own coordinates.
public final class LinearGradient extends Paint {

    private final double startX;
    private final double startY;
    private final double endX;
    private final double endY;
    private final boolean proportional;
    private final CycleMethod cycleMethod;
    private final List<Stop> stops;

    /// Creates a gradient from stops given one by one.
    public LinearGradient(@NamedArg("startX") double startX, @NamedArg("startY") double startY,
            @NamedArg(value = "endX", defaultValue = "1") double endX,
            @NamedArg(value = "endY", defaultValue = "1") double endY,
            @NamedArg(value = "proportional", defaultValue = "true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod, @NamedArg("stops") Stop... stops) {
        this(startX, startY, endX, endY, proportional, cycleMethod, asList(stops));
    }

    /// Creates a gradient from a list of stops.
    public LinearGradient(@NamedArg("startX") double startX, @NamedArg("startY") double startY,
            @NamedArg(value = "endX", defaultValue = "1") double endX,
            @NamedArg(value = "endY", defaultValue = "1") double endY,
            @NamedArg(value = "proportional", defaultValue = "true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod, @NamedArg("stops") List<Stop> stops) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.proportional = proportional;
        this.cycleMethod = cycleMethod == null ? CycleMethod.NO_CYCLE : cycleMethod;
        this.stops = Stop.normalize(stops);
    }

    static List<Stop> asList(Stop[] stops) {
        ArrayList<Stop> list = new ArrayList<Stop>();
        if (stops != null) {
            for (int i = 0; i < stops.length; i++) {
                list.add(stops[i]);
            }
        }
        return list;
    }

    /// Returns the x of the start of the line.
    public final double getStartX() {
        return startX;
    }

    /// Returns the y of the start of the line.
    public final double getStartY() {
        return startY;
    }

    /// Returns the x of the end of the line.
    public final double getEndX() {
        return endX;
    }

    /// Returns the y of the end of the line.
    public final double getEndY() {
        return endY;
    }

    /// Returns whether the line is in fractions of the filled bounds.
    public final boolean isProportional() {
        return proportional;
    }

    /// Returns how the gradient continues outside the line.
    public final CycleMethod getCycleMethod() {
        return cycleMethod;
    }

    /// Returns the stops, sorted and covering 0 to 1; unmodifiable.
    public final List<Stop> getStops() {
        return stops;
    }

    @Override
    public final boolean isOpaque() {
        for (int i = 0; i < stops.size(); i++) {
            if (!stops.get(i).getColor().isOpaque()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof LinearGradient) {
            LinearGradient o = (LinearGradient) obj;
            return Double.compare(startX, o.startX) == 0 && Double.compare(startY, o.startY) == 0
                    && Double.compare(endX, o.endX) == 0 && Double.compare(endY, o.endY) == 0
                    && proportional == o.proportional && cycleMethod == o.cycleMethod && stops.equals(o.stops);
        }
        return false;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(startX) * 31 + Double.doubleToLongBits(startY) * 17
                + Double.doubleToLongBits(endX) * 7 + Double.doubleToLongBits(endY);
        return (int) (bits ^ (bits >>> 32)) * 37 + stops.hashCode() + (proportional ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("linear-gradient(from ").append(startX).append(' ').append(startY)
                .append(" to ").append(endX).append(' ').append(endY);
        for (int i = 0; i < stops.size(); i++) {
            s.append(", ").append(stops.get(i));
        }
        return s.append(')').toString();
    }
}
