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
package javafx.scene.layout;

import java.util.ArrayList;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;

/// The base of the constraints a `GridPane` keeps for a row or a column.
/// A change to a constraint asks every pane that uses it for layout.
public abstract class ConstraintsBase {

    /// For a minimum or maximum: the same as the preferred size.
    public static final double CONSTRAIN_TO_PREF = Double.NEGATIVE_INFINITY;

    private final ArrayList<Parent> users = new ArrayList<Parent>();

    ConstraintsBase() {
    }

    /// Asks the panes that use this constraint for layout.
    protected void requestLayout() {
        for (int i = 0; i < users.size(); i++) {
            users.get(i).requestLayout();
        }
    }

    void add(Parent pane) {
        users.add(pane);
    }

    void remove(Parent pane) {
        users.remove(pane);
    }

    /// The minimum size set, or `Region.USE_COMPUTED_SIZE`.
    abstract double minSize();

    /// The preferred size set, or `Region.USE_COMPUTED_SIZE`.
    abstract double prefSize();

    /// The maximum size set, or `Region.USE_COMPUTED_SIZE`.
    abstract double maxSize();

    /// The share of the pane in percent, or a negative number.
    abstract double percent();

    /// The grow priority set, or `null`.
    abstract Priority grow();

    /// Whether children are stretched across the row or column.
    abstract boolean fill();

    /// A number whose change asks for layout.
    static final class Num extends SimpleDoubleProperty {
        private final ConstraintsBase owner;

        Num(ConstraintsBase owner, String name, double initial) {
            super(owner, name, initial);
            this.owner = owner;
        }

        @Override
        protected void invalidated() {
            owner.requestLayout();
        }
    }

    /// A flag whose change asks for layout.
    static final class Flag extends SimpleBooleanProperty {
        private final ConstraintsBase owner;

        Flag(ConstraintsBase owner, String name, boolean initial) {
            super(owner, name, initial);
            this.owner = owner;
        }

        @Override
        protected void invalidated() {
            owner.requestLayout();
        }
    }

    /// A value whose change asks for layout.
    static final class Ref<T> extends SimpleObjectProperty<T> {
        private final ConstraintsBase owner;

        Ref(ConstraintsBase owner, String name) {
            super(owner, name);
            this.owner = owner;
        }

        @Override
        protected void invalidated() {
            owner.requestLayout();
        }
    }
}
