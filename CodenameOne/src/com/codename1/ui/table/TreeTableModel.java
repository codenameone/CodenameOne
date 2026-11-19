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

import com.codename1.ui.tree.TreeModel;

/// The data of a tree grid: a `TreeModel` whose every node also has a value
/// in each column.
///
/// `getChildren(null)` answers the roots. Children are asked for only when
/// a node is first opened, so a large or remote tree loads as it is
/// browsed. `isLeaf` is asked of every node shown, to decide whether it has
/// an arrow, and has to answer without loading the children.
///
/// Nodes are told apart by `equals` and `hashCode`, so two equal nodes are
/// one node to the grid: they open, close and are found together. A tree
/// that repeats a value, such as two leaves both named "Untitled", should
/// hand out node objects of its own rather than the bare values.
///
/// The model has no listeners: after a change call `DataGrid#refresh()`, or
/// `DataGrid#refreshNode(Object)` when only the children of one node
/// changed.
public interface TreeTableModel extends TreeModel {

    /// How many columns each node has.
    int getColumnCount();

    /// The name of a column, the default title of its header.
    String getColumnName(int column);

    /// The value of a node in a column.
    Object getValueAt(Object node, int column);

    /// Whether a node's value in a column may be edited.
    boolean isCellEditable(Object node, int column);

    /// Sets a node's value in a column.
    void setValueAt(Object node, int column, Object value);
}
