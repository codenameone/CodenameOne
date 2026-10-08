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

/// Draws a horizontal line to an x.
public class HLineTo extends PathElement {

    private final DoubleProperty x = coordinate("x", 0);

    /// Creates the element with every value zero.
    public HLineTo() {
    }

    /// Creates a horizontal line to an x.
    public HLineTo(double x) {
        this.x.set(x);
    }

    /// Returns the x the line ends at.
    public final double getX() {
        return x.get();
    }

    /// Sets the x the line ends at.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x the line ends at.
    public final DoubleProperty xProperty() {
        return x;
    }

    @Override
    void addTo(FxPath path) {
        path.lineTo(originX(path) + getX(), path.currentY());
    }
}
