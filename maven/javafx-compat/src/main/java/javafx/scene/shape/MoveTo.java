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

/// Starts a new part of the outline at a point, without drawing to it.
public class MoveTo extends PathElement {

    private final DoubleProperty x = coordinate("x", 0);
    private final DoubleProperty y = coordinate("y", 0);

    /// Creates the element with every value zero.
    public MoveTo() {
    }

    /// Creates a move to a point.
    public MoveTo(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }

    /// Returns the x of the point.
    public final double getX() {
        return x.get();
    }

    /// Sets the x of the point.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x of the point.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the y of the point.
    public final double getY() {
        return y.get();
    }

    /// Sets the y of the point.
    public final void setY(double value) {
        y.set(value);
    }

    /// The y of the point.
    public final DoubleProperty yProperty() {
        return y;
    }

    @Override
    void addTo(FxPath path) {
        path.moveTo(originX(path) + getX(), originY(path) + getY());
    }
}
