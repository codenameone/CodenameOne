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

import java.util.List;

import javafx.beans.NamedArg;

/// A paint that blends colours outwards from a point to a circle. With
/// `proportional` set the centre and radius are fractions of the bounds of
/// the shape being filled.
public final class RadialGradient extends Paint {

    private final double focusAngle;
    private final double focusDistance;
    private final double centerX;
    private final double centerY;
    private final double radius;
    private final boolean proportional;
    private final CycleMethod cycleMethod;
    private final List<Stop> stops;

    /// Creates a gradient from stops given one by one.
    public RadialGradient(@NamedArg("focusAngle") double focusAngle, @NamedArg("focusDistance") double focusDistance,
            @NamedArg("centerX") double centerX, @NamedArg("centerY") double centerY,
            @NamedArg(value = "radius", defaultValue = "1") double radius,
            @NamedArg(value = "proportional", defaultValue = "true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod, @NamedArg("stops") Stop... stops) {
        this(focusAngle, focusDistance, centerX, centerY, radius, proportional, cycleMethod,
                LinearGradient.asList(stops));
    }

    /// Creates a gradient from a list of stops.
    public RadialGradient(@NamedArg("focusAngle") double focusAngle, @NamedArg("focusDistance") double focusDistance,
            @NamedArg("centerX") double centerX, @NamedArg("centerY") double centerY,
            @NamedArg(value = "radius", defaultValue = "1") double radius,
            @NamedArg(value = "proportional", defaultValue = "true") boolean proportional,
            @NamedArg("cycleMethod") CycleMethod cycleMethod, @NamedArg("stops") List<Stop> stops) {
        this.focusAngle = focusAngle;
        this.focusDistance = focusDistance;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.proportional = proportional;
        this.cycleMethod = cycleMethod == null ? CycleMethod.NO_CYCLE : cycleMethod;
        this.stops = Stop.normalize(stops);
    }

    /// Returns the angle in degrees from the centre to the focus point.
    public final double getFocusAngle() {
        return focusAngle;
    }

    /// Returns the distance of the focus point from the centre as a
    /// fraction of the radius.
    public final double getFocusDistance() {
        return focusDistance;
    }

    /// Returns the x of the centre.
    public final double getCenterX() {
        return centerX;
    }

    /// Returns the y of the centre.
    public final double getCenterY() {
        return centerY;
    }

    /// Returns the radius of the circle the last stop lies on.
    public final double getRadius() {
        return radius;
    }

    /// Returns whether centre and radius are fractions of the filled
    /// bounds.
    public final boolean isProportional() {
        return proportional;
    }

    /// Returns how the gradient continues outside the circle.
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
        if (obj instanceof RadialGradient) {
            RadialGradient o = (RadialGradient) obj;
            return Double.compare(focusAngle, o.focusAngle) == 0
                    && Double.compare(focusDistance, o.focusDistance) == 0
                    && Double.compare(centerX, o.centerX) == 0 && Double.compare(centerY, o.centerY) == 0
                    && Double.compare(radius, o.radius) == 0 && proportional == o.proportional
                    && cycleMethod == o.cycleMethod && stops.equals(o.stops);
        }
        return false;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(centerX) * 31 + Double.doubleToLongBits(centerY) * 17
                + Double.doubleToLongBits(radius) * 7 + Double.doubleToLongBits(focusAngle);
        return (int) (bits ^ (bits >>> 32)) * 37 + stops.hashCode() + (proportional ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("radial-gradient(center ").append(centerX).append(' ').append(centerY)
                .append(", radius ").append(radius);
        for (int i = 0; i < stops.size(); i++) {
            s.append(", ").append(stops.get(i));
        }
        return s.append(')').toString();
    }
}
