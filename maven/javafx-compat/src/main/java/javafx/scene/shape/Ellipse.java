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

/// An ellipse given by its centre and its two radii.
public class Ellipse extends Shape {

    private final DoubleProperty centerX = geometry("centerX", 0);
    private final DoubleProperty centerY = geometry("centerY", 0);
    private final DoubleProperty radiusX = geometry("radiusX", 0);
    private final DoubleProperty radiusY = geometry("radiusY", 0);

    /// Creates an ellipse of no size at the origin.
    public Ellipse() {
    }

    /// Creates an ellipse about the origin.
    public Ellipse(double radiusX, double radiusY) {
        this.radiusX.set(radiusX);
        this.radiusY.set(radiusY);
    }

    /// Creates an ellipse about a centre.
    public Ellipse(double centerX, double centerY, double radiusX, double radiusY) {
        this.centerX.set(centerX);
        this.centerY.set(centerY);
        this.radiusX.set(radiusX);
        this.radiusY.set(radiusY);
    }

    /// Returns the x of the centre.
    public final double getCenterX() {
        return centerX.get();
    }

    /// Sets the x of the centre.
    public final void setCenterX(double value) {
        centerX.set(value);
    }

    /// The x of the centre.
    public final DoubleProperty centerXProperty() {
        return centerX;
    }

    /// Returns the y of the centre.
    public final double getCenterY() {
        return centerY.get();
    }

    /// Sets the y of the centre.
    public final void setCenterY(double value) {
        centerY.set(value);
    }

    /// The y of the centre.
    public final DoubleProperty centerYProperty() {
        return centerY;
    }

    /// Returns the horizontal radius.
    public final double getRadiusX() {
        return radiusX.get();
    }

    /// Sets the horizontal radius.
    public final void setRadiusX(double value) {
        radiusX.set(value);
    }

    /// The horizontal radius.
    public final DoubleProperty radiusXProperty() {
        return radiusX;
    }

    /// Returns the vertical radius.
    public final double getRadiusY() {
        return radiusY.get();
    }

    /// Sets the vertical radius.
    public final void setRadiusY(double value) {
        radiusY.set(value);
    }

    /// The vertical radius.
    public final DoubleProperty radiusYProperty() {
        return radiusY;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.addEllipse(getCenterX(), getCenterY(), Math.max(0, getRadiusX()), Math.max(0, getRadiusY()));
        return p;
    }

    @Override
    FxPath offsetOutline(double distance) {
        double rx = Math.max(0, getRadiusX()) + distance;
        double ry = Math.max(0, getRadiusY()) + distance;
        if (!(rx > 0) || !(ry > 0)) {
            return null;
        }
        FxPath p = new FxPath();
        p.addEllipse(getCenterX(), getCenterY(), rx, ry);
        return p;
    }
}
