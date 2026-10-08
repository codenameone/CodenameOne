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

/// A selection of at most one index. A subclass supplies the items
/// through [#getModelItem(int)] and [#getItemCount()].
public abstract class SingleSelectionModel<T> extends SelectionModel<T> {

    /// Creates a selection model with nothing selected.
    public SingleSelectionModel() {
    }

    /// Clears the selection.
    @Override
    public void clearSelection() {
        updateSelectedIndex(-1);
    }

    /// Clears the selection if the index is the selected one.
    @Override
    public void clearSelection(int index) {
        if (getSelectedIndex() == index) {
            clearSelection();
        }
    }

    /// Returns whether nothing is selected.
    @Override
    public boolean isEmpty() {
        return getItemCount() == 0 || getSelectedIndex() == -1;
    }

    /// Returns whether the index is the selected one.
    @Override
    public boolean isSelected(int index) {
        return getSelectedIndex() == index;
    }

    /// Selects the index; the same as [#select(int)] here.
    @Override
    public void clearAndSelect(int index) {
        select(index);
    }

    /// Selects the first index holding the item. An item that is not
    /// among the items becomes the selected item all the same, with the
    /// selected index left as it was; `null` clears the selection.
    @Override
    public void select(T obj) {
        if (obj == null) {
            setSelectedIndex(-1);
            setSelectedItem(null);
            return;
        }
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            T value = getModelItem(i);
            if (value != null && value.equals(obj)) {
                select(i);
                return;
            }
        }
        setSelectedItem(obj);
    }

    /// Selects the index; -1 clears the selection and any other index
    /// outside the items is ignored.
    @Override
    public void select(int index) {
        if (index == -1) {
            clearSelection();
            return;
        }
        int count = getItemCount();
        if (count == 0 || index < 0 || index >= count) {
            return;
        }
        updateSelectedIndex(index);
    }

    /// Selects the index before the selected one, if there is one.
    @Override
    public void selectPrevious() {
        if (getSelectedIndex() == 0) {
            return;
        }
        select(getSelectedIndex() - 1);
    }

    /// Selects the index after the selected one, if there is one.
    @Override
    public void selectNext() {
        select(getSelectedIndex() + 1);
    }

    /// Selects the first index, if there are items.
    @Override
    public void selectFirst() {
        if (getItemCount() > 0) {
            select(0);
        }
    }

    /// Selects the last index, if there are items.
    @Override
    public void selectLast() {
        int count = getItemCount();
        if (count > 0 && getSelectedIndex() < count - 1) {
            select(count - 1);
        }
    }

    /// Returns the item at an index, or `null` for an index outside the
    /// items.
    protected abstract T getModelItem(int index);

    /// Returns the number of items.
    protected abstract int getItemCount();

    private void updateSelectedIndex(int newIndex) {
        setSelectedIndex(newIndex);
        setSelectedItem(getModelItem(newIndex));
    }
}
