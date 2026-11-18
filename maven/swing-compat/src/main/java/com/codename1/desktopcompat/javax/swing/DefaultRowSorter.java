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

import com.codename1.desktopcompat.rt.Comparators;
import com.codename1.ui.table.GridSorter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A row sorter that sorts by up to three columns and filters, reading
/// the model through a [ModelWrapper].
///
/// Values are compared by the column's comparator. The default one never
/// trusts two values to be of one type: numbers are compared as numbers,
/// two values of the same comparable class by `compareTo`, and anything
/// else by its string. Strings are compared by `String.compareTo`; there
/// is no locale collation on a device.
///
/// Every change of the rows sorts all of them again.
public abstract class DefaultRowSorter<M, I> extends RowSorter<M> {

    private ModelWrapper<M, I> modelWrapper;
    private int[] viewToModel;
    private int[] modelToView;
    private List<SortKey> sortKeys = new ArrayList<SortKey>();
    private Comparator<?>[] comparators;
    private boolean[] unsortable;
    private RowFilter<? super M, ? super I> filter;
    private int maxSortKeys = 3;
    private boolean sortsOnUpdates;
    private int modelRowCount;

    public DefaultRowSorter() {
    }

    protected final void setModelWrapper(ModelWrapper<M, I> modelWrapper) {
        if (modelWrapper == null) {
            throw new IllegalArgumentException("modelWrapper most be non-null");
        }
        ModelWrapper<M, I> last = this.modelWrapper;
        this.modelWrapper = modelWrapper;
        if (last != null) {
            modelStructureChanged();
        } else {
            modelRowCount = modelWrapper.getRowCount();
        }
    }

    protected final ModelWrapper<M, I> getModelWrapper() {
        return modelWrapper;
    }

    @Override
    public final M getModel() {
        return modelWrapper == null ? null : modelWrapper.getModel();
    }

    private void checkColumn(int column) {
        if (modelWrapper == null || column < 0 || column >= modelWrapper.getColumnCount()) {
            throw new IndexOutOfBoundsException("column beyond range of TableModel");
        }
    }

    public void setSortable(int column, boolean sortable) {
        checkColumn(column);
        if (unsortable == null) {
            unsortable = new boolean[modelWrapper.getColumnCount()];
        }
        unsortable[column] = !sortable;
    }

    public boolean isSortable(int column) {
        checkColumn(column);
        return unsortable == null || column >= unsortable.length || !unsortable[column];
    }

    @Override
    public void setSortKeys(List<? extends SortKey> sortKeys) {
        List<SortKey> old = this.sortKeys;
        List<SortKey> now = new ArrayList<SortKey>();
        if (sortKeys != null) {
            for (int i = 0; i < sortKeys.size(); i++) {
                SortKey key = sortKeys.get(i);
                if (key == null) {
                    throw new IllegalArgumentException("Invalid SortKey");
                }
                checkColumn(key.getColumn());
                now.add(key);
            }
        }
        this.sortKeys = now;
        if (!now.equals(old)) {
            fireSortOrderChanged();
            sort();
        }
    }

    @Override
    public List<? extends SortKey> getSortKeys() {
        return Collections.unmodifiableList(sortKeys);
    }

    public void setMaxSortKeys(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Invalid max");
        }
        maxSortKeys = max;
    }

    public int getMaxSortKeys() {
        return maxSortKeys;
    }

    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        this.sortsOnUpdates = sortsOnUpdates;
    }

    public boolean getSortsOnUpdates() {
        return sortsOnUpdates;
    }

    public void setRowFilter(RowFilter<? super M, ? super I> filter) {
        this.filter = filter;
        sort();
    }

    public RowFilter<? super M, ? super I> getRowFilter() {
        return filter;
    }

    /// Makes `column` the first sort key: ascending, or the other way
    /// round if it was the first key already.
    @Override
    public void toggleSortOrder(int column) {
        checkColumn(column);
        if (!isSortable(column)) {
            return;
        }
        List<SortKey> keys = new ArrayList<SortKey>(sortKeys);
        int at = -1;
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).getColumn() == column) {
                at = i;
                break;
            }
        }
        if (at == 0) {
            SortOrder flipped = keys.get(0).getSortOrder() == SortOrder.ASCENDING
                    ? SortOrder.DESCENDING : SortOrder.ASCENDING;
            keys.set(0, new SortKey(column, flipped));
        } else {
            if (at > 0) {
                keys.remove(at);
            }
            keys.add(0, new SortKey(column, SortOrder.ASCENDING));
        }
        while (keys.size() > maxSortKeys) {
            keys.remove(keys.size() - 1);
        }
        setSortKeys(keys);
    }

    @Override
    public int convertRowIndexToView(int index) {
        if (modelToView == null) {
            if (index < 0 || index >= modelRowCount) {
                throw new IndexOutOfBoundsException("Invalid index");
            }
            return index;
        }
        return modelToView[index];
    }

    @Override
    public int convertRowIndexToModel(int index) {
        if (viewToModel == null) {
            if (index < 0 || index >= modelRowCount) {
                throw new IndexOutOfBoundsException("Invalid index");
            }
            return index;
        }
        return viewToModel[index];
    }

    private boolean sorted() {
        for (int i = 0; i < sortKeys.size(); i++) {
            if (sortKeys.get(i).getSortOrder() != SortOrder.UNSORTED) {
                return true;
            }
        }
        return false;
    }

    /// Filters and sorts every row again, and tells the listeners if the
    /// rows shown or their order may have changed.
    public void sort() {
        if (modelWrapper == null) {
            return;
        }
        int[] before = viewToModel;
        modelRowCount = modelWrapper.getRowCount();
        if (filter == null && !sorted()) {
            viewToModel = null;
            modelToView = null;
            if (before != null) {
                fireRowSorterChanged(before);
            }
            return;
        }
        List<Integer> rows = new ArrayList<Integer>(modelRowCount);
        Row entry = filter == null ? null : new Row();
        for (int i = 0; i < modelRowCount; i++) {
            if (entry != null) {
                entry.modelIndex = i;
                if (!filter.include(entry)) {
                    continue;
                }
            }
            rows.add(Integer.valueOf(i));
        }
        int n = rows.size();
        int[] kept = new int[n];
        for (int i = 0; i < n; i++) {
            kept[i] = rows.get(i).intValue();
        }
        if (sorted()) {
            kept = sortRows(kept);
        }
        viewToModel = kept;
        modelToView = new int[modelRowCount];
        for (int i = 0; i < modelToView.length; i++) {
            modelToView[i] = -1;
        }
        for (int i = 0; i < viewToModel.length; i++) {
            modelToView[viewToModel[i]] = i;
        }
        fireRowSorterChanged(before);
    }

    /// Orders the rows a filter kept with the core grid's sort
    /// (`com.codename1.ui.table.GridSorter`): every key is read from the
    /// model once per row rather than once per comparison, and the sort is
    /// stable, so equal rows keep the model's order as Swing's do. A null
    /// sorts first, and last when its key descends, as Swing's does by
    /// negating the comparison.
    private int[] sortRows(int[] kept) {
        int n = kept.length;
        ArrayList<SortKey> active = new ArrayList<SortKey>();
        for (int i = 0; i < sortKeys.size(); i++) {
            if (sortKeys.get(i).getSortOrder() != SortOrder.UNSORTED) {
                active.add(sortKeys.get(i));
            }
        }
        int k = active.size();
        Object[][] keys = new Object[k][n];
        Comparator[] comparators = new Comparator[k];
        boolean[] ascending = new boolean[k];
        for (int j = 0; j < k; j++) {
            SortKey key = active.get(j);
            int column = key.getColumn();
            boolean strings = useToString(column);
            comparators[j] = strings ? Comparators.STRINGS : getComparator(column);
            ascending[j] = key.getSortOrder() != SortOrder.DESCENDING;
            Object[] dest = keys[j];
            for (int i = 0; i < n; i++) {
                dest[i] = strings ? modelWrapper.getStringValueAt(kept[i], column)
                        : modelWrapper.getValueAt(kept[i], column);
            }
        }
        int[] order = GridSorter.order(n, keys, comparators, ascending);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = kept[order[i]];
        }
        return out;
    }

    /// Whether the column's values are compared by their strings.
    protected boolean useToString(int column) {
        return getComparator(column) == Comparators.STRINGS;
    }

    public void setComparator(int column, Comparator<?> comparator) {
        checkColumn(column);
        if (comparators == null) {
            comparators = new Comparator<?>[modelWrapper.getColumnCount()];
        }
        comparators[column] = comparator;
    }

    /// The comparator set for the column, or the one that compares
    /// strings.
    public Comparator<?> getComparator(int column) {
        checkColumn(column);
        if (comparators != null && column < comparators.length && comparators[column] != null) {
            return comparators[column];
        }
        return Comparators.STRINGS;
    }

    @Override
    public int getViewRowCount() {
        return viewToModel != null ? viewToModel.length : modelRowCount;
    }

    @Override
    public int getModelRowCount() {
        return modelWrapper == null ? 0 : modelWrapper.getRowCount();
    }

    /// The columns are different: comparators and sortable flags are
    /// forgotten, and so are the sort keys of columns that are gone.
    @Override
    public void modelStructureChanged() {
        comparators = null;
        unsortable = null;
        int columns = modelWrapper == null ? 0 : modelWrapper.getColumnCount();
        List<SortKey> kept = new ArrayList<SortKey>();
        for (int i = 0; i < sortKeys.size(); i++) {
            if (sortKeys.get(i).getColumn() < columns) {
                kept.add(sortKeys.get(i));
            }
        }
        boolean keysChanged = kept.size() != sortKeys.size();
        sortKeys = kept;
        if (keysChanged) {
            fireSortOrderChanged();
        }
        sort();
    }

    @Override
    public void allRowsChanged() {
        sort();
    }

    private void checkRange(int firstRow, int endRow) {
        if (firstRow > endRow || firstRow < 0 || endRow < 0) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
    }

    @Override
    public void rowsInserted(int firstRow, int endRow) {
        checkRange(firstRow, endRow);
        if (endRow >= getModelRowCount()) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
        sort();
    }

    @Override
    public void rowsDeleted(int firstRow, int endRow) {
        checkRange(firstRow, endRow);
        if (firstRow >= modelRowCount) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
        sort();
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow) {
        checkRange(firstRow, endRow);
        if (firstRow >= modelRowCount) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
        if (sortsOnUpdates) {
            sort();
        }
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow, int column) {
        checkColumn(column);
        rowsUpdated(firstRow, endRow);
    }

    /// The row a filter is shown.
    private final class Row extends RowFilter.Entry<M, I> {

        int modelIndex;

        @Override
        public M getModel() {
            return modelWrapper.getModel();
        }

        @Override
        public int getValueCount() {
            return modelWrapper.getColumnCount();
        }

        @Override
        public Object getValue(int index) {
            return modelWrapper.getValueAt(modelIndex, index);
        }

        @Override
        public String getStringValue(int index) {
            return modelWrapper.getStringValueAt(modelIndex, index);
        }

        @Override
        public I getIdentifier() {
            return modelWrapper.getIdentifier(modelIndex);
        }
    }

    /// How the sorter reads its model.
    protected abstract static class ModelWrapper<M, I> {

        protected ModelWrapper() {
        }

        public abstract M getModel();

        public abstract int getColumnCount();

        public abstract int getRowCount();

        public abstract Object getValueAt(int row, int column);

        public String getStringValueAt(int row, int column) {
            Object o = getValueAt(row, column);
            if (o == null) {
                return "";
            }
            return o.toString();
        }

        public abstract I getIdentifier(int row);
    }
}
