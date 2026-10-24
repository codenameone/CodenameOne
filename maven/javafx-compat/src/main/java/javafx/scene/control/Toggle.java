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
package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableMap;

/// Something that is either selected or not and can belong to a
/// [ToggleGroup], of which at most one member is selected.
public interface Toggle {

    /// Returns the group this toggle belongs to, or `null`.
    ToggleGroup getToggleGroup();

    /// Moves this toggle into a group, or out of any with `null`.
    void setToggleGroup(ToggleGroup toggleGroup);

    /// The group this toggle belongs to.
    ObjectProperty<ToggleGroup> toggleGroupProperty();

    /// Returns whether this toggle is selected.
    boolean isSelected();

    /// Selects or deselects this toggle.
    void setSelected(boolean selected);

    /// Whether this toggle is selected.
    BooleanProperty selectedProperty();

    /// Returns the object the application attached, or `null`.
    Object getUserData();

    /// Attaches an object of the application's.
    void setUserData(Object value);

    /// Returns the map of properties the application attached.
    ObservableMap<Object, Object> getProperties();
}
