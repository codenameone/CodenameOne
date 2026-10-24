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
package javafx.scene.shape;

import com.codename1.fxcompat.runtime.FxPath;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;

/// Draws part of an ellipse to a point, as an SVG arc does: of the
/// ellipses with the given radii and rotation that pass through the
/// current point and the end point, `largeArcFlag` picks the longer or the
/// shorter way round and `sweepFlag` the direction, clockwise on the screen
/// when set. The arc is drawn as cubic curves.
///
/// Radii too small to reach the end point are scaled up until they do; a
/// zero radius gives a straight line.
public class ArcTo extends PathElement {

    private final DoubleProperty radiusX = coordinate("radiusX", 0);
    private final DoubleProperty radiusY = coordinate("radiusY", 0);
    private final DoubleProperty xAxisRotation = coordinate("XAxisRotation", 0);
    private final DoubleProperty x = coordinate("x", 0);
    private final DoubleProperty y = coordinate("y", 0);
    private final BooleanProperty largeArcFlag = flag("largeArcFlag");
    private final BooleanProperty sweepFlag = flag("sweepFlag");

    /// Creates the element with every value zero.
    public ArcTo() {
    }

    /// Creates an arc from its radii, rotation, end point and flags.
    public ArcTo(double radiusX, double radiusY, double xAxisRotation,
            double x, double y, boolean largeArcFlag, boolean sweepFlag) {
        this.radiusX.set(radiusX);
        this.radiusY.set(radiusY);
        this.xAxisRotation.set(xAxisRotation);
        this.x.set(x);
        this.y.set(y);
        this.largeArcFlag.set(largeArcFlag);
        this.sweepFlag.set(sweepFlag);
    }

    /// Returns the horizontal radius of the ellipse.
    public final double getRadiusX() {
        return radiusX.get();
    }

    /// Sets the horizontal radius of the ellipse.
    public final void setRadiusX(double value) {
        radiusX.set(value);
    }

    /// The horizontal radius of the ellipse.
    public final DoubleProperty radiusXProperty() {
        return radiusX;
    }

    /// Returns the vertical radius of the ellipse.
    public final double getRadiusY() {
        return radiusY.get();
    }

    /// Sets the vertical radius of the ellipse.
    public final void setRadiusY(double value) {
        radiusY.set(value);
    }

    /// The vertical radius of the ellipse.
    public final DoubleProperty radiusYProperty() {
        return radiusY;
    }

    /// Returns the rotation of the ellipse in degrees.
    public final double getXAxisRotation() {
        return xAxisRotation.get();
    }

    /// Sets the rotation of the ellipse in degrees.
    public final void setXAxisRotation(double value) {
        xAxisRotation.set(value);
    }

    /// The rotation of the ellipse in degrees.
    public final DoubleProperty XAxisRotationProperty() {
        return xAxisRotation;
    }

    /// Returns the x of the end point.
    public final double getX() {
        return x.get();
    }

    /// Sets the x of the end point.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x of the end point.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the y of the end point.
    public final double getY() {
        return y.get();
    }

    /// Sets the y of the end point.
    public final void setY(double value) {
        y.set(value);
    }

    /// The y of the end point.
    public final DoubleProperty yProperty() {
        return y;
    }

    /// Returns whether the longer of the two arcs is drawn.
    public final boolean isLargeArcFlag() {
        return largeArcFlag.get();
    }

    /// Sets whether the longer of the two arcs is drawn.
    public final void setLargeArcFlag(boolean value) {
        largeArcFlag.set(value);
    }

    /// Whether the longer of the two arcs is drawn.
    public final BooleanProperty largeArcFlagProperty() {
        return largeArcFlag;
    }

    /// Returns whether the arc is drawn clockwise.
    public final boolean isSweepFlag() {
        return sweepFlag.get();
    }

    /// Sets whether the arc is drawn clockwise.
    public final void setSweepFlag(boolean value) {
        sweepFlag.set(value);
    }

    /// Whether the arc is drawn clockwise.
    public final BooleanProperty sweepFlagProperty() {
        return sweepFlag;
    }

    @Override
    void addTo(FxPath path) {
        path.arcTo(getRadiusX(), getRadiusY(), getXAxisRotation(), isLargeArcFlag(), isSweepFlag(),
                originX(path) + getX(), originY(path) + getY());
    }
}
