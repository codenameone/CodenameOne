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

/// A quadratic curve from a start point to an end point, bent towards
/// one control point.
public class QuadCurve extends Shape {

    private final DoubleProperty startX = geometry("startX", 0);
    private final DoubleProperty startY = geometry("startY", 0);
    private final DoubleProperty controlX = geometry("controlX", 0);
    private final DoubleProperty controlY = geometry("controlY", 0);
    private final DoubleProperty endX = geometry("endX", 0);
    private final DoubleProperty endY = geometry("endY", 0);

    /// Creates a curve with every point at the origin.
    public QuadCurve() {
    }

    /// Creates a curve from its three points.
    public QuadCurve(double startX, double startY, double controlX, double controlY, double endX, double endY) {
        this.startX.set(startX);
        this.startY.set(startY);
        this.controlX.set(controlX);
        this.controlY.set(controlY);
        this.endX.set(endX);
        this.endY.set(endY);
    }

    /// Returns the x of the start.
    public final double getStartX() {
        return startX.get();
    }

    /// Sets the x of the start.
    public final void setStartX(double value) {
        startX.set(value);
    }

    /// The x of the start.
    public final DoubleProperty startXProperty() {
        return startX;
    }

    /// Returns the y of the start.
    public final double getStartY() {
        return startY.get();
    }

    /// Sets the y of the start.
    public final void setStartY(double value) {
        startY.set(value);
    }

    /// The y of the start.
    public final DoubleProperty startYProperty() {
        return startY;
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

    /// Returns the x of the end.
    public final double getEndX() {
        return endX.get();
    }

    /// Sets the x of the end.
    public final void setEndX(double value) {
        endX.set(value);
    }

    /// The x of the end.
    public final DoubleProperty endXProperty() {
        return endX;
    }

    /// Returns the y of the end.
    public final double getEndY() {
        return endY.get();
    }

    /// Sets the y of the end.
    public final void setEndY(double value) {
        endY.set(value);
    }

    /// The y of the end.
    public final DoubleProperty endYProperty() {
        return endY;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.moveTo(getStartX(), getStartY());
        p.quadTo(getControlX(), getControlY(), getEndX(), getEndY());
        return p;
    }
}
