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

/// A cubic curve from a start point to an end point, bent towards two
/// control points.
public class CubicCurve extends Shape {

    private final DoubleProperty startX = geometry("startX", 0);
    private final DoubleProperty startY = geometry("startY", 0);
    private final DoubleProperty controlX1 = geometry("controlX1", 0);
    private final DoubleProperty controlY1 = geometry("controlY1", 0);
    private final DoubleProperty controlX2 = geometry("controlX2", 0);
    private final DoubleProperty controlY2 = geometry("controlY2", 0);
    private final DoubleProperty endX = geometry("endX", 0);
    private final DoubleProperty endY = geometry("endY", 0);

    /// Creates a curve with every point at the origin.
    public CubicCurve() {
    }

    /// Creates a curve from its four points.
    public CubicCurve(double startX, double startY, double controlX1, double controlY1, double controlX2,
            double controlY2, double endX, double endY) {
        this.startX.set(startX);
        this.startY.set(startY);
        this.controlX1.set(controlX1);
        this.controlY1.set(controlY1);
        this.controlX2.set(controlX2);
        this.controlY2.set(controlY2);
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
        p.curveTo(getControlX1(), getControlY1(), getControlX2(), getControlY2(), getEndX(), getEndY());
        return p;
    }
}
