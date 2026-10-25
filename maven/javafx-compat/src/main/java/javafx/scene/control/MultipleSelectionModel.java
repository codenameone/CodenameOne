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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ObservableList;

/// A selection of any number of rows, or of one when the mode is
/// [SelectionMode#SINGLE].
public abstract class MultipleSelectionModel<T> extends SelectionModel<T> {

    private final ObjectProperty<SelectionMode> selectionMode = new ObjectPropertyBase<SelectionMode>(
            SelectionMode.SINGLE) {
        @Override
        protected void invalidated() {
            if (get() == SelectionMode.SINGLE && !isEmpty()) {
                // One row survives a switch back to single selection.
                int kept = getSelectedIndex();
                clearSelection();
                select(kept);
            }
        }

        @Override
        public Object getBean() {
            return MultipleSelectionModel.this;
        }

        @Override
        public String getName() {
            return "selectionMode";
        }
    };

    /// Creates a model with nothing selected, in single selection mode.
    public MultipleSelectionModel() {
    }

    /// Sets how many rows may be selected.
    public final void setSelectionMode(SelectionMode value) {
        selectionMode.set(value);
    }

    /// Returns how many rows may be selected.
    public final SelectionMode getSelectionMode() {
        return selectionMode.get();
    }

    /// How many rows may be selected.
    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        return selectionMode;
    }

    /// Returns the selected indices, in ascending order. The list cannot
    /// be modified.
    public abstract ObservableList<Integer> getSelectedIndices();

    /// Returns the selected items, in the order of their indices. The
    /// list cannot be modified.
    public abstract ObservableList<T> getSelectedItems();

    /// Selects several indices, keeping what is selected.
    public abstract void selectIndices(int index, int... indices);

    /// Selects the indices from `start` up to, not including, `end`.
    public void selectRange(final int start, final int end) {
        if (start == end) {
            return;
        }
        boolean up = start < end;
        int low = up ? start : end;
        int high = up ? end : start;
        int count = high - low - 1;
        int[] rest = new int[count];
        int first = up ? low : high;
        int next = up ? low + 1 : high - 1;
        for (int i = 0; i < count; i++) {
            rest[i] = up ? next++ : next--;
        }
        selectIndices(first, rest);
    }

    /// Selects every row.
    public abstract void selectAll();

    @Override
    public abstract void selectFirst();

    @Override
    public abstract void selectLast();
}
