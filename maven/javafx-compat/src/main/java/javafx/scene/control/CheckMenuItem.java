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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;

/// A menu item that is either selected or not; its row shows a mark while
/// it is selected. Choosing the row flips the selection and then fires
/// the item. [#fire()] alone leaves the selection as it is.
public class CheckMenuItem extends MenuItem {

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);

    /// Creates an item with no text.
    public CheckMenuItem() {
        this(null, null);
    }

    /// Creates an item with a text.
    public CheckMenuItem(String text) {
        this(text, null);
    }

    /// Creates an item with a text and a graphic.
    public CheckMenuItem(String text, Node graphic) {
        super(text, graphic);
        styleClasses().add("check-menu-item");
    }

    /// Sets whether the item is selected.
    public final void setSelected(boolean value) {
        selected.set(value);
    }

    /// Returns whether the item is selected.
    public final boolean isSelected() {
        return selected.get();
    }

    /// Whether the item is selected.
    public final BooleanProperty selectedProperty() {
        return selected;
    }
}
