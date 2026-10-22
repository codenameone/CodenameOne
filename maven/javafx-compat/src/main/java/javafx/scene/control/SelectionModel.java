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

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/// The selection of a control that shows a list of items: which index is
/// selected and which item that is. A subclass decides how many items can
/// be selected at once and where the items come from.
public abstract class SelectionModel<T> {

    private final ReadOnlyIntegerWrapper selectedIndex = new ReadOnlyIntegerWrapper(this, "selectedIndex", -1);
    private final ReadOnlyObjectWrapper<T> selectedItem = new ReadOnlyObjectWrapper<T>(this, "selectedItem");

    /// Creates a selection model with nothing selected.
    public SelectionModel() {
    }

    /// The selected index, or -1; in a multiple selection the one selected
    /// last.
    public final ReadOnlyIntegerProperty selectedIndexProperty() {
        return selectedIndex.getReadOnlyProperty();
    }

    /// Sets the selected index; for subclasses.
    protected final void setSelectedIndex(int value) {
        selectedIndex.set(value);
    }

    /// Returns the selected index, or -1.
    public final int getSelectedIndex() {
        return selectedIndex.get();
    }

    /// The selected item, or `null`.
    public final ReadOnlyObjectProperty<T> selectedItemProperty() {
        return selectedItem.getReadOnlyProperty();
    }

    /// Sets the selected item; for subclasses.
    protected final void setSelectedItem(T value) {
        selectedItem.set(value);
    }

    /// Returns the selected item, or `null`.
    public final T getSelectedItem() {
        return selectedItem.get();
    }

    /// Clears the selection and selects one index.
    public abstract void clearAndSelect(int index);

    /// Selects an index; an index outside the items is ignored.
    public abstract void select(int index);

    /// Selects the first index holding an item.
    public abstract void select(T obj);

    /// Removes one index from the selection.
    public abstract void clearSelection(int index);

    /// Removes everything from the selection.
    public abstract void clearSelection();

    /// Returns whether an index is selected.
    public abstract boolean isSelected(int index);

    /// Returns whether nothing is selected.
    public abstract boolean isEmpty();

    /// Selects the index before the selected one.
    public abstract void selectPrevious();

    /// Selects the index after the selected one.
    public abstract void selectNext();

    /// Selects the first index.
    public abstract void selectFirst();

    /// Selects the last index.
    public abstract void selectLast();
}
