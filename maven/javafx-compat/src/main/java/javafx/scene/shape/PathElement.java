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

import java.util.ArrayList;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxPath;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/// One step of a [Path]: a move, a line, a curve, an arc or the closing
/// of the outline.
///
/// The coordinates of an element are absolute by default. With
/// `absolute` off they are distances from the point the previous element
/// ended at; that holds for control points too.
public abstract class PathElement {

    private final ArrayList<Path> owners = new ArrayList<Path>(1);
    private final BooleanProperty absolute = new SimpleBooleanProperty(this, "absolute", true) {
        @Override
        protected void invalidated() {
            changed();
        }
    };

    /// Creates an element.
    public PathElement() {
    }

    final void addOwner(Path path) {
        owners.add(path);
    }

    final void removeOwner(Path path) {
        owners.remove(path);
    }

    final void changed() {
        for (int i = 0; i < owners.size(); i++) {
            owners.get(i).cn1Invalidated(Dirty.GEOMETRY);
        }
    }

    /// Creates a coordinate property that reshapes the paths using this
    /// element when it changes.
    final DoubleProperty coordinate(String name, double initial) {
        return new SimpleDoubleProperty(this, name, initial) {
            @Override
            protected void invalidated() {
                changed();
            }
        };
    }

    /// Creates a flag property that reshapes the paths using this
    /// element when it changes.
    final BooleanProperty flag(String name) {
        return new SimpleBooleanProperty(this, name, false) {
            @Override
            protected void invalidated() {
                changed();
            }
        };
    }

    /// Adds this element to an outline whose current point is where the
    /// previous element ended.
    abstract void addTo(FxPath path);

    /// The x a relative coordinate is counted from.
    final double originX(FxPath path) {
        return isAbsolute() ? 0 : path.currentX();
    }

    /// The y a relative coordinate is counted from.
    final double originY(FxPath path) {
        return isAbsolute() ? 0 : path.currentY();
    }

    /// Returns whether the coordinates are absolute.
    public final boolean isAbsolute() {
        return absolute.get();
    }

    /// Sets whether the coordinates are absolute rather than distances
    /// from the end of the previous element.
    public final void setAbsolute(boolean value) {
        absolute.set(value);
    }

    /// Whether the coordinates are absolute.
    public final BooleanProperty absoluteProperty() {
        return absolute;
    }
}
