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

import java.util.Date;

/// The rows a `DataGrid` shows, in the order it shows them: the model's
/// rows filtered and sorted, with group rows or the open nodes of a tree.
///
/// A view row is an index into this, never into the model. Nothing here
/// copies the data: a flat view is an array of model row numbers, a tree a
/// list of the nodes that are open.
abstract class GridRows {
    /// The columns sorted by, most significant first, and their directions.
    GridColumn[] sortColumns = new GridColumn[0];
    boolean[] sortAscending = new boolean[0];
    GridFilter filter;
    final GridRow probe = new GridRow();

    abstract int size();

    abstract int columnCount();

    abstract String columnName(int modelColumn);

    /// Whether a view row is a group row rather than data.
    boolean isGroup(int row) {
        return false;
    }

    /// The label of a group row: its column's formatted key.
    String groupLabel(int row) {
        return "";
    }

    /// How many data rows a group row stands for.
    int groupCount(int row) {
        return 0;
    }

    /// How far in a row is drawn: a tree's depth, or 1 for the data rows
    /// under a group.
    abstract int depth(int row);

    abstract boolean isExpandable(int row);

    abstract boolean isExpanded(int row);

    /// Opens or closes a row in place.
    ///
    /// #### Returns
    ///
    /// how many rows were added, negative when removed
    abstract int setExpanded(int row, boolean open);

    abstract Object value(int row, int modelColumn);

    abstract boolean isEditable(int row, int modelColumn);

    abstract void setValue(int row, int modelColumn, Object value);

    /// What identifies a row across a rebuild: a model row number, a node or
    /// a group's key.
    abstract Object key(int row);

    /// The view row of a key, or -1 when it is not shown.
    abstract int find(Object key);

    /// Applies the filter, the sort and the grouping again.
    abstract void rebuild();

    /// The model row of a view row of a flat grid, -1 for any other row.
    int modelRow(int row) {
        return -1;
    }

    /// The node of a view row of a tree grid, null for any other row.
    Object node(int row) {
        return null;
    }

    final void setSort(GridColumn[] columns, boolean[] ascending) {
        sortColumns = columns;
        sortAscending = ascending;
    }

    // ------------------------------------------------------------- order

    /// The default order: null first, then numbers by value, dates by time,
    /// booleans false first, and anything else by its text, case folded
    /// character by character so a Turkish device sorts like any other.
    static int naturalCompare(Object a, Object b) {
        if (a == b) { //NOPMD CompareObjectsWithEquals - identity is the fast path, not equality
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Number && b instanceof Number) {
            if (isIntegral(a) && isIntegral(b)) {
                long x = ((Number) a).longValue();
                long y = ((Number) b).longValue();
                return x < y ? -1 : (x == y ? 0 : 1);
            }
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof Date && b instanceof Date) {
            long x = ((Date) a).getTime();
            long y = ((Date) b).getTime();
            return x < y ? -1 : (x == y ? 0 : 1);
        }
        if (a instanceof Boolean && b instanceof Boolean) {
            boolean x = ((Boolean) a).booleanValue();
            boolean y = ((Boolean) b).booleanValue();
            return x == y ? 0 : (x ? 1 : -1);
        }
        return compareText(a.toString(), b.toString());
    }

    private static boolean isIntegral(Object n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }

    static int compareText(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = a.charAt(i);
            char y = b.charAt(i);
            if (x != y) {
                char lx = Character.toLowerCase(x);
                char ly = Character.toLowerCase(y);
                if (lx != ly) {
                    return lx - ly;
                }
            }
        }
        if (a.length() != b.length()) {
            return a.length() - b.length();
        }
        // Equal but for case: settle it, so the order is total.
        return a.compareTo(b);
    }

    /// Compares two positions of the sort keys.
    static java.util.Comparator[] comparators(GridColumn[] cols) {
        java.util.Comparator[] out = new java.util.Comparator[cols.length];
        for (int i = 0; i < cols.length; i++) {
            out[i] = cols[i].getComparator();
        }
        return out;
    }

    interface Order {
        int compare(int a, int b);
    }

    /// Sorts `idx[from..to)` with a stable merge sort, which keeps rows that
    /// compare equal in the order they were in: the property that lets a
    /// sort by one column refine the sort by another.
    static void sort(int[] idx, int from, int to, Order order) {
        int n = to - from;
        if (n < 2) {
            return;
        }
        int[] buf = new int[n];
        mergeSort(idx, buf, from, to, order);
    }

    private static void mergeSort(int[] a, int[] buf, int from, int to, Order order) {
        int n = to - from;
        if (n <= 12) {
            for (int i = from + 1; i < to; i++) {
                int v = a[i];
                int j = i - 1;
                while (j >= from && order.compare(a[j], v) > 0) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = v;
            }
            return;
        }
        int mid = (from + to) >>> 1;
        mergeSort(a, buf, from, mid, order);
        mergeSort(a, buf, mid, to, order);
        if (order.compare(a[mid - 1], a[mid]) <= 0) {
            return;
        }
        System.arraycopy(a, from, buf, 0, n);
        int i = 0;
        int iEnd = mid - from;
        int j = iEnd;
        int k = from;
        while (i < iEnd && j < n) {
            if (order.compare(buf[j], buf[i]) < 0) {
                a[k++] = buf[j++];
            } else {
                a[k++] = buf[i++];
            }
        }
        while (i < iEnd) {
            a[k++] = buf[i++];
        }
        while (j < n) {
            a[k++] = buf[j++];
        }
    }
}
