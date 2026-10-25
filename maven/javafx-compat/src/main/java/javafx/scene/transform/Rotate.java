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
package javafx.scene.transform;

import javafx.beans.property.DoubleProperty;

/// Rotates coordinates about a pivot point by an angle in degrees. A
/// positive angle turns clockwise on the screen, where y points down.
///
/// The rotation is in the plane only: the `axis` of JavaFX's `Rotate`,
/// its axis constants and the pivot's z are absent.
public class Rotate extends Transform {

    private final DoubleProperty angle = new TDouble(this, "angle", 0);
    private final DoubleProperty pivotX = new TDouble(this, "pivotX", 0);
    private final DoubleProperty pivotY = new TDouble(this, "pivotY", 0);

    /// Creates a rotation by nothing.
    public Rotate() {
    }

    /// Creates a rotation about the origin.
    public Rotate(double angle) {
        this.angle.set(angle);
    }

    /// Creates a rotation about a pivot.
    public Rotate(double angle, double pivotX, double pivotY) {
        this.angle.set(angle);
        this.pivotX.set(pivotX);
        this.pivotY.set(pivotY);
    }

    /// Returns the angle in degrees.
    public final double getAngle() {
        return angle.get();
    }

    /// Sets the angle in degrees.
    public final void setAngle(double value) {
        angle.set(value);
    }

    /// The angle in degrees.
    public final DoubleProperty angleProperty() {
        return angle;
    }

    /// Returns the x of the point rotated about.
    public final double getPivotX() {
        return pivotX.get();
    }

    /// Sets the x of the point rotated about.
    public final void setPivotX(double value) {
        pivotX.set(value);
    }

    /// The x of the point rotated about.
    public final DoubleProperty pivotXProperty() {
        return pivotX;
    }

    /// Returns the y of the point rotated about.
    public final double getPivotY() {
        return pivotY.get();
    }

    /// Sets the y of the point rotated about.
    public final void setPivotY(double value) {
        pivotY.set(value);
    }

    /// The y of the point rotated about.
    public final DoubleProperty pivotYProperty() {
        return pivotY;
    }

    private double cos() {
        return Math.cos(Math.toRadians(getAngle()));
    }

    private double sin() {
        return Math.sin(Math.toRadians(getAngle()));
    }

    @Override
    public double getMxx() {
        return cos();
    }

    @Override
    public double getMxy() {
        return -sin();
    }

    @Override
    public double getMyx() {
        return sin();
    }

    @Override
    public double getMyy() {
        return cos();
    }

    @Override
    public double getTx() {
        return getPivotX() - getPivotX() * cos() + getPivotY() * sin();
    }

    @Override
    public double getTy() {
        return getPivotY() - getPivotX() * sin() - getPivotY() * cos();
    }
}
