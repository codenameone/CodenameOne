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

/// Draws a quadratic curve to a point, bent towards a control point.
public class QuadCurveTo extends PathElement {

    private final DoubleProperty controlX = coordinate("controlX", 0);
    private final DoubleProperty controlY = coordinate("controlY", 0);
    private final DoubleProperty x = coordinate("x", 0);
    private final DoubleProperty y = coordinate("y", 0);

    /// Creates the element with every value zero.
    public QuadCurveTo() {
    }

    /// Creates a curve from its control point and end point.
    public QuadCurveTo(double controlX, double controlY, double x, double y) {
        this.controlX.set(controlX);
        this.controlY.set(controlY);
        this.x.set(x);
        this.y.set(y);
    }

    /// Returns the x of the control point.
    public final double getControlX() {
        return controlX.get();
    }

    /// Sets the x of the control point.
    public final void setControlX(double value) {
        controlX.set(value);
    }

    /// The x of the control point.
    public final DoubleProperty controlXProperty() {
        return controlX;
    }

    /// Returns the y of the control point.
    public final double getControlY() {
        return controlY.get();
    }

    /// Sets the y of the control point.
    public final void setControlY(double value) {
        controlY.set(value);
    }

    /// The y of the control point.
    public final DoubleProperty controlYProperty() {
        return controlY;
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
        path.quadTo(ox + getControlX(), oy + getControlY(), ox + getX(), oy + getY());
    }
}
