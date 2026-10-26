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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.RowSorterEvent;
import com.codename1.desktopcompat.javax.swing.event.RowSorterListener;
import java.util.List;

/// Maps the rows a table shows to the rows of its model, so that the
/// table can show them sorted and filtered while the model stays as it
/// is. The table tells the sorter about every model change.
public abstract class RowSorter<M> {

    private final EventListenerList listenerList = new EventListenerList();

    public RowSorter() {
    }

    public abstract M getModel();

    public abstract void toggleSortOrder(int column);

    public abstract int convertRowIndexToModel(int index);

    public abstract int convertRowIndexToView(int index);

    public abstract void setSortKeys(List<? extends SortKey> keys);

    public abstract List<? extends SortKey> getSortKeys();

    public abstract int getViewRowCount();

    public abstract int getModelRowCount();

    public abstract void modelStructureChanged();

    public abstract void allRowsChanged();

    public abstract void rowsInserted(int firstRow, int endRow);

    public abstract void rowsDeleted(int firstRow, int endRow);

    public abstract void rowsUpdated(int firstRow, int endRow);

    public abstract void rowsUpdated(int firstRow, int endRow, int column);

    public void addRowSorterListener(RowSorterListener l) {
        listenerList.add(RowSorterListener.class, l);
    }

    public void removeRowSorterListener(RowSorterListener l) {
        listenerList.remove(RowSorterListener.class, l);
    }

    protected void fireSortOrderChanged() {
        fire(new RowSorterEvent(this));
    }

    protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
        fire(new RowSorterEvent(this, RowSorterEvent.Type.SORTED, lastRowIndexToModel));
    }

    private void fire(RowSorterEvent e) {
        RowSorterListener[] ls = listenerList.getListeners(RowSorterListener.class);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].sorterChanged(e);
        }
    }

    /// A column and the direction it is sorted in.
    public static class SortKey {

        private final int column;
        private final SortOrder sortOrder;

        public SortKey(int column, SortOrder sortOrder) {
            if (sortOrder == null) {
                throw new IllegalArgumentException("sort order must be non-null");
            }
            this.column = column;
            this.sortOrder = sortOrder;
        }

        public final int getColumn() {
            return column;
        }

        public final SortOrder getSortOrder() {
            return sortOrder;
        }

        @Override
        public int hashCode() {
            return 17 * (37 * 17 + column) + sortOrder.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SortKey) {
                SortKey k = (SortKey) o;
                return k.column == column && k.sortOrder == sortOrder;
            }
            return false;
        }
    }
}
