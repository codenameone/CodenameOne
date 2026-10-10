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

import javafx.beans.property.DoubleProperty;

/// Draws a cubic curve to a point, bent towards two control points.
public class CubicCurveTo extends PathElement {

    private final DoubleProperty controlX1 = coordinate("controlX1", 0);
    private final DoubleProperty controlY1 = coordinate("controlY1", 0);
    private final DoubleProperty controlX2 = coordinate("controlX2", 0);
    private final DoubleProperty controlY2 = coordinate("controlY2", 0);
    private final DoubleProperty x = coordinate("x", 0);
    private final DoubleProperty y = coordinate("y", 0);

    /// Creates the element with every value zero.
    public CubicCurveTo() {
    }

    /// Creates a curve from its two control points and end point.
    public CubicCurveTo(double controlX1, double controlY1, double controlX2, double controlY2, double x, double y) {
        this.controlX1.set(controlX1);
        this.controlY1.set(controlY1);
        this.controlX2.set(controlX2);
        this.controlY2.set(controlY2);
        this.x.set(x);
        this.y.set(y);
    }

    /// Returns the x of the first control point.
    public final double getControlX1() {
        return controlX1.get();
    }

    /// Sets the x of the first control point.
    public final void setControlX1(double value) {
        controlX1.set(value);
    }

    /// The x of the first control point.
    public final DoubleProperty controlX1Property() {
        return controlX1;
    }

    /// Returns the y of the first control point.
    public final double getControlY1() {
        return controlY1.get();
    }

    /// Sets the y of the first control point.
    public final void setControlY1(double value) {
        controlY1.set(value);
    }

    /// The y of the first control point.
    public final DoubleProperty controlY1Property() {
        return controlY1;
    }

    /// Returns the x of the second control point.
    public final double getControlX2() {
        return controlX2.get();
    }

    /// Sets the x of the second control point.
    public final void setControlX2(double value) {
        controlX2.set(value);
    }

    /// The x of the second control point.
    public final DoubleProperty controlX2Property() {
        return controlX2;
    }

    /// Returns the y of the second control point.
    public final double getControlY2() {
        return controlY2.get();
    }

    /// Sets the y of the second control point.
    public final void setControlY2(double value) {
        controlY2.set(value);
    }

    /// The y of the second control point.
    public final DoubleProperty controlY2Property() {
        return controlY2;
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

    @Override
    void addTo(FxPath path) {
        double ox = originX(path);
        double oy = originY(path);
        path.curveTo(ox + getControlX1(), oy + getControlY1(), ox + getControlX2(), oy + getControlY2(), ox + getX(),
                oy + getY());
    }
}
