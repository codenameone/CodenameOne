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

/// Slants coordinates: x moves in proportion to the distance from the
/// pivot's y, and y in proportion to the distance from the pivot's x.
public class Shear extends Transform {

    private final DoubleProperty x = new TDouble(this, "x", 0);
    private final DoubleProperty y = new TDouble(this, "y", 0);
    private final DoubleProperty pivotX = new TDouble(this, "pivotX", 0);
    private final DoubleProperty pivotY = new TDouble(this, "pivotY", 0);

    /// Creates a shear that changes nothing.
    public Shear() {
    }

    /// Creates a shear about the origin.
    public Shear(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }

    /// Creates a shear about a pivot.
    public Shear(double x, double y, double pivotX, double pivotY) {
        this.x.set(x);
        this.y.set(y);
        this.pivotX.set(pivotX);
        this.pivotY.set(pivotY);
    }

    /// Returns how far x moves per unit of y.
    public final double getX() {
        return x.get();
    }

    /// Sets how far x moves per unit of y.
    public final void setX(double value) {
        x.set(value);
    }

    /// How far x moves per unit of y.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns how far y moves per unit of x.
    public final double getY() {
        return y.get();
    }

    /// Sets how far y moves per unit of x.
    public final void setY(double value) {
        y.set(value);
    }

    /// How far y moves per unit of x.
    public final DoubleProperty yProperty() {
        return y;
    }

    /// Returns the x of the point that stays in place.
    public final double getPivotX() {
        return pivotX.get();
    }

    /// Sets the x of the point that stays in place.
    public final void setPivotX(double value) {
        pivotX.set(value);
    }

    /// The x of the point that stays in place.
    public final DoubleProperty pivotXProperty() {
        return pivotX;
    }

    /// Returns the y of the point that stays in place.
    public final double getPivotY() {
        return pivotY.get();
    }

    /// Sets the y of the point that stays in place.
    public final void setPivotY(double value) {
        pivotY.set(value);
    }

    /// The y of the point that stays in place.
    public final DoubleProperty pivotYProperty() {
        return pivotY;
    }

    @Override
    public double getMxy() {
        return getX();
    }

    @Override
    public double getMyx() {
        return getY();
    }

    @Override
    public double getTx() {
        return -getX() * getPivotY();
    }

    @Override
    public double getTy() {
        return -getY() * getPivotX();
    }
}
