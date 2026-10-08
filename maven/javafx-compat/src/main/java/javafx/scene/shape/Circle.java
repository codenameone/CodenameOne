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
import javafx.scene.paint.Paint;

/// A circle given by its centre and radius.
public class Circle extends Shape {

    private final DoubleProperty centerX = geometry("centerX", 0);
    private final DoubleProperty centerY = geometry("centerY", 0);
    private final DoubleProperty radius = geometry("radius", 0);

    /// Creates a circle of radius zero at the origin.
    public Circle() {
    }

    /// Creates a circle about the origin.
    public Circle(double radius) {
        this.radius.set(radius);
    }

    /// Creates a circle about the origin with a fill.
    public Circle(double radius, Paint fill) {
        this.radius.set(radius);
        fillProperty().set(fill);
    }

    /// Creates a circle about a centre.
    public Circle(double centerX, double centerY, double radius) {
        this.centerX.set(centerX);
        this.centerY.set(centerY);
        this.radius.set(radius);
    }

    /// Creates a circle about a centre with a fill.
    public Circle(double centerX, double centerY, double radius, Paint fill) {
        this.centerX.set(centerX);
        this.centerY.set(centerY);
        this.radius.set(radius);
        fillProperty().set(fill);
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

    /// Returns the radius.
    public final double getRadius() {
        return radius.get();
    }

    /// Sets the radius.
    public final void setRadius(double value) {
        radius.set(value);
    }

    /// The radius.
    public final DoubleProperty radiusProperty() {
        return radius;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        double r = Math.max(0, getRadius());
        p.addEllipse(getCenterX(), getCenterY(), r, r);
        return p;
    }

    @Override
    FxPath offsetOutline(double distance) {
        double r = Math.max(0, getRadius()) + distance;
        if (!(r > 0)) {
            return null;
        }
        FxPath p = new FxPath();
        p.addEllipse(getCenterX(), getCenterY(), r, r);
        return p;
    }
}
