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
package com.codename1.ui.table;

import java.util.HashMap;

/// The rows of a `TableModel`: filtered, sorted and optionally grouped by a
/// column, as an array of model row numbers.
///
/// A view entry of 0 or more is a model row; a negative one, `-(g + 1)`, is
/// the row of group `g`.
final class FlatRows extends GridRows {
    /// The key of the group of rows whose value is null.
    private static final Object NULL_KEY = new Object();

    private final TableModel model;
    private GridColumn groupColumn;
    private GridColumn[] columns = new GridColumn[0];

    // The data rows that pass the filter, in order.
    private int[] data = new int[0];
    private int dataSize;
    // How many rows the model had when the rows were last built.
    private int modelRows;

    private int[] view = new int[0];
    private int viewSize;
    private int[] modelToView;

    private Object[] groupKey = new Object[0];
    private int[] groupStart = new int[0];
    private int[] groupCount = new int[0];
    private Object[][] groupAggregates = new Object[0][];
    private int groups;
    private final HashMap<Object, Boolean> collapsed = new HashMap<Object, Boolean>();

    FlatRows(TableModel model) {
        this.model = model;
    }

    /// How many rows the model had at the last rebuild.
    int modelRowsBuilt() {
        return modelRows;
    }

    TableModel model() {
        return model;
    }

    /// The columns of the grid, all of them, for the aggregates.
    void setColumns(GridColumn[] columns) {
        this.columns = columns;
    }

    GridColumn getGroupColumn() {
        return groupColumn;
    }

    void setGroupColumn(GridColumn c) {
        if (c != groupColumn) { //NOPMD CompareObjectsWithEquals - a column is an identity
            collapsed.clear();
        }
        groupColumn = c;
    }

    @Override
    int size() {
        return viewSize;
    }

    @Override
    int columnCount() {
        return model.getColumnCount();
    }

    @Override
    String columnName(int modelColumn) {
        return model.getColumnName(modelColumn);
    }

    /// How many data rows pass the filter, open groups or not.
    int dataCount() {
        return dataSize;
    }

    @Override
    boolean isGroup(int row) {
        return view[row] < 0;
    }

    @Override
    String groupLabel(int row) {
        Object k = groupKey[-view[row] - 1];
        GridColumn gc = groupColumn;
        if (gc == null) {
            return "";
        }
        return gc.format(k == NULL_KEY ? null : k); //NOPMD CompareObjectsWithEquals - sentinel
    }

    @Override
    int groupCount(int row) {
        return view[row] < 0 ? groupCount[-view[row] - 1] : 0;
    }

    @Override
    int depth(int row) {
        return groupColumn != null && view[row] >= 0 ? 1 : 0;
    }

    @Override
    boolean isExpandable(int row) {
        return view[row] < 0;
    }

    @Override
    boolean isExpanded(int row) {
        return view[row] < 0 && !collapsed.containsKey(groupKey[-view[row] - 1]);
    }

    @Override
    int setExpanded(int row, boolean open) {
        if (view[row] >= 0 || open == isExpanded(row)) {
            return 0;
        }
        int g = -view[row] - 1;
        if (open) {
            collapsed.remove(groupKey[g]);
        } else {
            collapsed.put(groupKey[g], Boolean.TRUE);
        }
        int before = viewSize;
        buildView();
        return viewSize - before;
    }

    @Override
    Object value(int row, int modelColumn) {
        int v = view[row];
        if (v >= 0) {
            return model.getValueAt(v, modelColumn);
        }
        int g = -v - 1;
        if (groupColumn != null && groupColumn.getModelColumn() == modelColumn) {
            return groupKey[g] == NULL_KEY ? null : groupKey[g]; //NOPMD CompareObjectsWithEquals - sentinel
        }
        Object[] agg = groupAggregates[g];
        return agg != null && modelColumn < agg.length ? agg[modelColumn] : null;
    }

    @Override
    boolean isEditable(int row, int modelColumn) {
        return view[row] >= 0 && model.isCellEditable(view[row], modelColumn);
    }

    @Override
    void setValue(int row, int modelColumn, Object value) {
        if (view[row] >= 0) {
            model.setValueAt(view[row], modelColumn, value);
        }
    }

    @Override
    int modelRow(int row) {
        return view[row];
    }

    @Override
    Object key(int row) {
        int v = view[row];
        if (v >= 0) {
            return Integer.valueOf(v);
        }
        return new GroupKey(groupKey[-v - 1]);
    }

    @Override
    int find(Object key) {
        if (key instanceof Integer) {
            int r = ((Integer) key).intValue();
            int[] m = modelToView();
            return r >= 0 && r < m.length ? m[r] : -1;
        }
        if (key instanceof GroupKey) {
            Object k = ((GroupKey) key).key;
            for (int i = 0; i < viewSize; i++) {
                if (view[i] < 0 && equal(groupKey[-view[i] - 1], k)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// The view row of a model row, or -1 when it is filtered out or in a
    /// closed group.
    int viewRowOf(int modelRow) {
        int[] m = modelToView();
        return modelRow >= 0 && modelRow < m.length ? m[modelRow] : -1;
    }

    private int[] modelToView() {
        if (modelToView == null) {
            int[] m = new int[model.getRowCount()];
            for (int i = 0; i < m.length; i++) {
                m[i] = -1;
            }
            for (int i = 0; i < viewSize; i++) {
                int v = view[i];
                if (v >= 0 && v < m.length) {
                    m[v] = i;
                }
            }
            modelToView = m;
        }
        return modelToView;
    }

    @Override
    void rebuild() {
        int n = model.getRowCount();
        modelRows = n;
        int[] keep = new int[n];
        int m = 0;
        GridFilter f = filter;
        for (int r = 0; r < n; r++) {
            if (f != null) {
                probe.table(model, r);
                if (!f.accept(probe)) {
                    continue;
                }
            }
            keep[m++] = r;
        }
        final GridColumn[] keyColumns;
        final boolean[] keyAscending;
        if (groupColumn != null) {
            keyColumns = new GridColumn[sortColumns.length + 1];
            keyAscending = new boolean[keyColumns.length];
            keyColumns[0] = groupColumn;
            keyAscending[0] = groupAscending();
            System.arraycopy(sortColumns, 0, keyColumns, 1, sortColumns.length);
            System.arraycopy(sortAscending, 0, keyAscending, 1, sortColumns.length);
        } else {
            keyColumns = sortColumns;
            keyAscending = sortAscending;
        }
        if (keyColumns.length > 0 && m > 1) {
            // The values are read once, not once per comparison: a model
            // may compute them.
            final Object[][] keys = new Object[keyColumns.length][m];
            for (int k = 0; k < keyColumns.length; k++) {
                int col = keyColumns[k].getModelColumn();
                Object[] dest = keys[k];
                for (int i = 0; i < m; i++) {
                    dest[i] = model.getValueAt(keep[i], col);
                }
            }
            int[] pos = GridSorter.order(m, keys, comparators(keyColumns), keyAscending);
            int[] sorted = new int[m];
            for (int i = 0; i < m; i++) {
                sorted[i] = keep[pos[i]];
            }
            keep = sorted;
        }
        data = keep;
        dataSize = m;
        buildGroups();
        buildView();
    }

    private boolean groupAscending() {
        for (int i = 0; i < sortColumns.length; i++) {
            if (sortColumns[i] == groupColumn) { //NOPMD CompareObjectsWithEquals - a column is an identity
                return sortAscending[i];
            }
        }
        return true;
    }

    private void buildGroups() {
        groups = 0;
        if (groupColumn == null) {
            return;
        }
        int col = groupColumn.getModelColumn();
        Object[] keys = new Object[Math.max(4, dataSize / 8 + 1)];
        int[] start = new int[keys.length];
        Object last = null;
        boolean any = false;
        for (int i = 0; i < dataSize; i++) {
            Object v = model.getValueAt(data[i], col);
            if (!any || groupColumn.compare(v, last) != 0) {
                if (groups == keys.length) {
                    Object[] k2 = new Object[groups * 2];
                    System.arraycopy(keys, 0, k2, 0, groups);
                    keys = k2;
                    int[] s2 = new int[groups * 2];
                    System.arraycopy(start, 0, s2, 0, groups);
                    start = s2;
                }
                keys[groups] = v == null ? NULL_KEY : v;
                start[groups] = i;
                groups++;
                last = v;
                any = true;
            }
        }
        groupKey = keys;
        groupStart = start;
        groupCount = new int[groups];
        for (int g = 0; g < groups; g++) {
            int end = g + 1 < groups ? start[g + 1] : dataSize;
            groupCount[g] = end - start[g];
        }
        groupAggregates = new Object[groups][];
        int cols = model.getColumnCount();
        for (GridColumn gc : columns) {
            GridAggregate a = gc.getAggregate();
            int mc = gc.getModelColumn();
            if (a == null || mc < 0 || mc >= cols) {
                continue;
            }
            for (int g = 0; g < groups; g++) {
                Object acc = null;
                int end = groupStart[g] + groupCount[g];
                for (int i = groupStart[g]; i < end; i++) {
                    acc = a.fold(gc, acc, model.getValueAt(data[i], mc));
                }
                if (groupAggregates[g] == null) {
                    groupAggregates[g] = new Object[cols];
                }
                groupAggregates[g][mc] = a.finish(acc);
            }
        }
    }

    private void buildView() {
        modelToView = null;
        if (groupColumn == null) {
            view = data;
            viewSize = dataSize;
            return;
        }
        int n = groups;
        for (int g = 0; g < groups; g++) {
            if (!collapsed.containsKey(groupKey[g])) {
                n += groupCount[g];
            }
        }
        int[] v = new int[n];
        int k = 0;
        for (int g = 0; g < groups; g++) {
            v[k++] = -(g + 1);
            if (!collapsed.containsKey(groupKey[g])) {
                System.arraycopy(data, groupStart[g], v, k, groupCount[g]);
                k += groupCount[g];
            }
        }
        view = v;
        viewSize = n;
    }

    private static boolean equal(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /// A group's key as a selection remembers it, told apart from a model
    /// row number.
    static final class GroupKey {
        final Object key;

        GroupKey(Object key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GroupKey && equal(((GroupKey) o).key, key);
        }

        @Override
        public int hashCode() {
            return key == null ? 0 : key.hashCode();
        }
    }
}
