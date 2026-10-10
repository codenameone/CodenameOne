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

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;

/// Part of an ellipse, from a start angle over a length, both in degrees.
/// Angles are counted counter clockwise from the positive x axis, as
/// JavaFX counts them: a positive length sweeps towards the top of the
/// screen.
public class Arc extends Shape {

    private final DoubleProperty centerX = geometry("centerX", 0);
    private final DoubleProperty centerY = geometry("centerY", 0);
    private final DoubleProperty radiusX = geometry("radiusX", 0);
    private final DoubleProperty radiusY = geometry("radiusY", 0);
    private final DoubleProperty startAngle = geometry("startAngle", 0);
    private final DoubleProperty length = geometry("length", 0);
    private final ObjectProperty<ArcType> type = new FxObject<ArcType>(this, "type", ArcType.OPEN, Dirty.GEOMETRY);

    /// Creates an arc of no size.
    public Arc() {
    }

    /// Creates an arc of an ellipse.
    public Arc(double centerX, double centerY, double radiusX, double radiusY, double startAngle, double length) {
        this.centerX.set(centerX);
        this.centerY.set(centerY);
        this.radiusX.set(radiusX);
        this.radiusY.set(radiusY);
        this.startAngle.set(startAngle);
        this.length.set(length);
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

    /// Returns the angle the arc starts at, in degrees.
    public final double getStartAngle() {
        return startAngle.get();
    }

    /// Sets the angle the arc starts at, in degrees.
    public final void setStartAngle(double value) {
        startAngle.set(value);
    }

    /// The angle the arc starts at.
    public final DoubleProperty startAngleProperty() {
        return startAngle;
    }

    /// Returns the angle the arc covers, in degrees.
    public final double getLength() {
        return length.get();
    }

    /// Sets the angle the arc covers, in degrees.
    public final void setLength(double value) {
        length.set(value);
    }

    /// The angle the arc covers.
    public final DoubleProperty lengthProperty() {
        return length;
    }

    /// Returns how the ends are joined.
    public final ArcType getType() {
        ArcType t = type.get();
        return t == null ? ArcType.OPEN : t;
    }

    /// Sets how the ends are joined.
    public final void setType(ArcType value) {
        type.set(value);
    }

    /// How the ends are joined.
    public final ObjectProperty<ArcType> typeProperty() {
        return type;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        ArcType t = getType();
        if (t == ArcType.ROUND) {
            p.moveTo(getCenterX(), getCenterY());
        }
        p.addArc(getCenterX(), getCenterY(), Math.max(0, getRadiusX()), Math.max(0, getRadiusY()), getStartAngle(),
                getLength(), t == ArcType.ROUND);
        if (t != ArcType.OPEN) {
            p.closePath();
        }
        return p;
    }
}
