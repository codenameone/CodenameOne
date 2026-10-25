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

/// Moves coordinates by a distance.
public class Translate extends Transform {

    private final DoubleProperty x = new TDouble(this, "x", 0);
    private final DoubleProperty y = new TDouble(this, "y", 0);

    /// Creates a translation by nothing.
    public Translate() {
    }

    /// Creates a translation.
    public Translate(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }

    /// Returns the horizontal distance.
    public final double getX() {
        return x.get();
    }

    /// Sets the horizontal distance.
    public final void setX(double value) {
        x.set(value);
    }

    /// The horizontal distance.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the vertical distance.
    public final double getY() {
        return y.get();
    }

    /// Sets the vertical distance.
    public final void setY(double value) {
        y.set(value);
    }

    /// The vertical distance.
    public final DoubleProperty yProperty() {
        return y;
    }

    @Override
    public double getTx() {
        return getX();
    }

    @Override
    public double getTy() {
        return getY();
    }
}
