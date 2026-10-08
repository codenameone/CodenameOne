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

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;

/// A cell of a [ListView]. An application subclasses it and overrides
/// `updateItem` to decide what a row looks like.
public class ListCell<T> extends IndexedCell<T> {

    private final ReadOnlyObjectWrapper<ListView<T>> listView = new ReadOnlyObjectWrapper<ListView<T>>(this,
            "listView");
    private boolean stale = true;

    /// Creates a cell that belongs to no list yet.
    public ListCell() {
        getStyleClass().add("list-cell");
    }

    /// The list this cell belongs to.
    public final ReadOnlyObjectProperty<ListView<T>> listViewProperty() {
        return listView.getReadOnlyProperty();
    }

    /// Returns the list this cell belongs to.
    public final ListView<T> getListView() {
        return listView.get();
    }

    /// Tells the cell which list it belongs to; the list does.
    public final void updateListView(ListView<T> listView) {
        this.listView.set(listView);
    }

    /// Makes the next update show the item again even if it is the same.
    final void markStale() {
        stale = true;
    }

    @Override
    void indexChanged(int oldIndex, int newIndex) {
        ListView<T> view = getListView();
        ObservableList<T> items = view == null ? null : view.getItems();
        boolean valid = items != null && newIndex >= 0 && newIndex < items.size();
        if (valid) {
            T value = items.get(newIndex);
            if (stale || isEmpty() || oldIndex != newIndex || isItemChanged(getItem(), value)) {
                updateItem(value, false);
            }
        } else if (stale || !isEmpty()) {
            updateItem(null, true);
        }
        stale = false;
        MultipleSelectionModel<T> model = view == null ? null : view.getSelectionModel();
        updateSelected(valid && model != null && model.isSelected(newIndex));
    }
}
