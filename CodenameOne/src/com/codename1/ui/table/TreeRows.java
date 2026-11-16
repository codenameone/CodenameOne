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
import java.util.List;

/// The open nodes of a `TreeTableModel`, flattened into rows in the order
/// they are shown: the row engine of a tree `DataGrid`, for any other view
/// of a tree that needs rows.
///
/// The rows are empty until `refresh()` reads the roots. Nodes are told
/// apart by `equals` and `hashCode` (see `TreeTableModel`). A row is a node
/// and its depth, 0 for a root; opening a row inserts the rows of its open
/// descendants after it, and closing it removes them.
///
/// Children are asked of the model the first time a node is opened and
/// kept; opening a node inserts its rows and closing one removes them, so
/// either costs what the node shows and not what the tree holds. A filter is
/// the exception: deciding which paths lead to a match reads the whole tree.
public final class TreeRows extends GridRows {
    private static final Object[] NONE = new Object[0];

    private final TreeTableModel model;
    private Object[] nodes = new Object[16];
    private int[] depths = new int[16];
    private int size;
    private HashMap<Object, Integer> index;

    private final HashMap<Object, Boolean> expanded = new HashMap<Object, Boolean>();
    // What the model answered, in its own order.
    private final HashMap<Object, Object[]> loaded = new HashMap<Object, Object[]>();
    // The same, sorted and filtered; cleared by every rebuild.
    private final HashMap<Object, Object[]> shown = new HashMap<Object, Object[]>();
    // While a filter is set: the nodes that match or lead to a match.
    private HashMap<Object, Boolean> visible;
    private final Object rootKey = new Object();

    /// Rows over a tree model, empty until `refresh()`.
    public TreeRows(TreeTableModel model) {
        this.model = model;
    }

    /// The model the rows show.
    public TreeTableModel getModel() {
        return model;
    }

    /// How many rows there are.
    public int getRowCount() {
        return size;
    }

    /// The node a row shows.
    public Object getNode(int row) {
        return nodes[row];
    }

    /// How deep a row is: 0 for a root.
    public int getDepth(int row) {
        return depths[row];
    }

    /// Whether a row can open: its node is not a leaf.
    public boolean isRowExpandable(int row) {
        return isExpandable(row);
    }

    /// Whether a row is open.
    public boolean isRowExpanded(int row) {
        return isExpanded(row);
    }

    /// Opens or closes a row.
    ///
    /// #### Returns
    ///
    /// how many rows were inserted after it, or removed after it when
    /// negative; 0 when nothing moved
    public int setRowExpanded(int row, boolean open) {
        return setExpanded(row, open);
    }

    /// The row showing a node, or -1 when it has none.
    public int getRow(Object node) {
        return find(node);
    }

    /// Whether a node is open, shown or not.
    public boolean isNodeExpanded(Object node) {
        return expanded.containsKey(node);
    }

    /// Opens or closes a node whether it is shown or not; one that is not
    /// shown opens the next time it is.
    ///
    /// #### Returns
    ///
    /// the rows inserted or, when negative, removed after the node's row
    public int setNodeExpanded(Object node, boolean open) {
        int row = find(node);
        if (row >= 0) {
            return setExpanded(row, open);
        }
        if (open) {
            expanded.put(node, Boolean.TRUE);
        } else {
            expanded.remove(node);
        }
        return 0;
    }

    /// Asks the model for the whole tree again and flattens it, keeping
    /// which nodes are open.
    public void refresh() {
        forget(null);
    }

    /// Asks the model again for the children of one node and flattens the
    /// tree, keeping which nodes are open.
    public void refreshNode(Object node) {
        forget(node);
    }

    TreeTableModel model() {
        return model;
    }

    @Override
    int size() {
        return size;
    }

    @Override
    int columnCount() {
        return model.getColumnCount();
    }

    @Override
    String columnName(int modelColumn) {
        return model.getColumnName(modelColumn);
    }

    @Override
    int depth(int row) {
        return depths[row];
    }

    @Override
    boolean isExpandable(int row) {
        Object n = nodes[row];
        if (visible != null) {
            return children(n).length > 0;
        }
        return !model.isLeaf(n);
    }

    @Override
    boolean isExpanded(int row) {
        return open(nodes[row]);
    }

    private boolean open(Object n) {
        if (visible != null) {
            // A filter shows every path to a match open.
            return children(n).length > 0;
        }
        return expanded.containsKey(n);
    }

    boolean isExpandedNode(Object n) {
        return isNodeExpanded(n);
    }

    @Override
    int setExpanded(int row, boolean open) {
        Object n = nodes[row];
        if (open == expanded.containsKey(n)) {
            return 0;
        }
        if (open) {
            expanded.put(n, Boolean.TRUE);
        } else {
            expanded.remove(n);
        }
        if (visible != null || model.isLeaf(n)) {
            return 0;
        }
        index = null;
        if (open) {
            // The rows after this one step aside while its children are
            // appended, and come back after them.
            int tail = size - row - 1;
            Object[] tn = new Object[tail];
            int[] td = new int[tail];
            System.arraycopy(nodes, row + 1, tn, 0, tail);
            System.arraycopy(depths, row + 1, td, 0, tail);
            size = row + 1;
            appendChildren(n, depths[row] + 1);
            int added = size - row - 1;
            ensure(size + tail);
            System.arraycopy(tn, 0, nodes, size, tail);
            System.arraycopy(td, 0, depths, size, tail);
            size += tail;
            return added;
        }
        int d = depths[row];
        int end = row + 1;
        while (end < size && depths[end] > d) {
            end++;
        }
        int removed = end - row - 1;
        System.arraycopy(nodes, end, nodes, row + 1, size - end);
        System.arraycopy(depths, end, depths, row + 1, size - end);
        for (int i = size - removed; i < size; i++) {
            nodes[i] = null;
        }
        size -= removed;
        return -removed;
    }

    private void ensure(int cap) {
        if (cap > nodes.length) {
            int c = Math.max(cap, nodes.length * 2);
            Object[] n2 = new Object[c];
            System.arraycopy(nodes, 0, n2, 0, size);
            nodes = n2;
            int[] d2 = new int[c];
            System.arraycopy(depths, 0, d2, 0, size);
            depths = d2;
        }
    }

    private void add(Object n, int d) {
        ensure(size + 1);
        nodes[size] = n;
        depths[size] = d;
        size++;
    }

    private void appendChildren(Object parent, int depth) {
        Object[] kids = children(parent);
        for (Object kid : kids) {
            Object k = kid;
            add(k, depth);
            if (open(k)) {
                appendChildren(k, depth + 1);
            }
        }
    }

    /// The children of a node as shown: sorted, and filtered to the paths
    /// that lead to a match. `null` is the parent of the roots.
    Object[] children(Object parent) {
        Object k = parent == null ? rootKey : parent;
        Object[] s = shown.get(k);
        if (s != null) {
            return s;
        }
        Object[] raw = load(parent);
        if (visible != null) {
            int m = 0;
            Object[] f = new Object[raw.length];
            for (Object n : raw) {
                if (visible.containsKey(n)) {
                    f[m++] = n;
                }
            }
            s = new Object[m];
            System.arraycopy(f, 0, s, 0, m);
        } else {
            s = new Object[raw.length];
            System.arraycopy(raw, 0, s, 0, raw.length);
        }
        sortSiblings(s);
        shown.put(k, s);
        return s;
    }

    private Object[] load(Object parent) {
        Object k = parent == null ? rootKey : parent;
        Object[] raw = loaded.get(k);
        if (raw == null) {
            if (parent != null && model.isLeaf(parent)) {
                raw = NONE;
            } else {
                List<?> v = model.getChildren(parent);
                raw = v == null ? NONE : v.toArray();
            }
            loaded.put(k, raw);
        }
        return raw;
    }

    private void sortSiblings(final Object[] s) {
        final GridColumn[] cols = sortColumns;
        if (cols.length == 0 || s.length < 2) {
            return;
        }
        final boolean[] asc = sortAscending;
        final Object[][] keys = new Object[cols.length][s.length];
        for (int c = 0; c < cols.length; c++) {
            for (int i = 0; i < s.length; i++) {
                keys[c][i] = model.getValueAt(s[i], cols[c].getModelColumn());
            }
        }
        int[] pos = GridSorter.order(s.length, keys, comparators(cols), asc);
        Object[] copy = new Object[s.length];
        System.arraycopy(s, 0, copy, 0, s.length);
        for (int i = 0; i < pos.length; i++) {
            s[i] = copy[pos[i]];
        }
    }

    /// Forgets what the model answered for a node's children, or for the
    /// whole tree when `node` is null, and flattens again.
    void forget(Object node) {
        if (node == null) {
            loaded.clear();
        } else {
            loaded.remove(node);
        }
        rebuild();
    }

    @Override
    Object value(int row, int modelColumn) {
        return model.getValueAt(nodes[row], modelColumn);
    }

    @Override
    boolean isEditable(int row, int modelColumn) {
        return model.isCellEditable(nodes[row], modelColumn);
    }

    @Override
    void setValue(int row, int modelColumn, Object value) {
        model.setValueAt(nodes[row], modelColumn, value);
    }

    @Override
    Object node(int row) {
        return nodes[row];
    }

    @Override
    Object key(int row) {
        return nodes[row];
    }

    @Override
    int find(Object key) {
        if (index == null) {
            index = new HashMap<Object, Integer>();
            for (int i = 0; i < size; i++) {
                index.put(nodes[i], Integer.valueOf(i));
            }
        }
        Integer i = index.get(key);
        return i == null ? -1 : i.intValue();
    }

    @Override
    void rebuild() {
        shown.clear();
        visible = null;
        if (filter != null) {
            HashMap<Object, Boolean> vis = new HashMap<Object, Boolean>();
            mark(null, 0, vis, filter);
            visible = vis;
        }
        for (int i = 0; i < size; i++) {
            nodes[i] = null;
        }
        size = 0;
        index = null;
        appendChildren(null, 0);
    }

    // Marks the nodes under `parent` that match or lead to a match, and
    // answers whether any did.
    private boolean mark(Object parent, int depth, HashMap<Object, Boolean> vis, GridFilter f) {
        Object[] kids = load(parent);
        boolean any = false;
        for (Object kid : kids) {
            Object k = kid;
            boolean below = !model.isLeaf(k) && mark(k, depth + 1, vis, f);
            probe.tree(model, k, depth);
            if (below || f.accept(probe)) {
                vis.put(k, Boolean.TRUE);
                any = true;
            }
        }
        return any;
    }
}
