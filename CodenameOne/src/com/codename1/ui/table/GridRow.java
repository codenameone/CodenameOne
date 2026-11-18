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

/// One row of the data under a `DataGrid`, as a filter sees it: a row of a
/// `TableModel` or a node of a `TreeTableModel`.
///
/// The grid hands the same object out for every row it asks about, so a
/// row is read where it is given and never kept.
public final class GridRow {
    private TableModel table;
    private TreeTableModel tree;
    private int modelRow = -1;
    private Object node;
    private int depth;

    GridRow() {
    }

    void table(TableModel m, int row) {
        table = m;
        tree = null;
        modelRow = row;
        node = null;
        depth = 0;
    }

    void tree(TreeTableModel m, Object n, int d) {
        tree = m;
        table = null;
        modelRow = -1;
        node = n;
        depth = d;
    }

    /// The value in column `modelColumn` of the model.
    public Object getValue(int modelColumn) {
        if (table != null) {
            return table.getValueAt(modelRow, modelColumn);
        }
        if (tree != null) {
            return tree.getValueAt(node, modelColumn);
        }
        return null;
    }

    /// The row of the `TableModel`, or -1 in a tree grid.
    public int getModelRow() {
        return modelRow;
    }

    /// The node of the `TreeTableModel`, or null in a flat grid.
    public Object getNode() {
        return node;
    }

    /// How deep the node is: 0 for a root, and always 0 in a flat grid.
    public int getDepth() {
        return depth;
    }
}
