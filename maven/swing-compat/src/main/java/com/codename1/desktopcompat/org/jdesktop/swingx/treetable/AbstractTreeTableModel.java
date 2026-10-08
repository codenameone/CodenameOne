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
package com.codename1.desktopcompat.org.jdesktop.swingx.treetable;

import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.org.jdesktop.swingx.tree.TreeModelSupport;

/// The parts of a tree table model that do not depend on its nodes: the
/// root, the listeners, and answers for a model that is not editable and
/// whose columns are named like a spreadsheet's.
///
/// A subclass answers the tree questions -- child, child count, index of
/// a child -- the column count and the value of a node in a column, and
/// tells its listeners of changes through [#modelSupport].
public abstract class AbstractTreeTableModel implements TreeTableModel {

    /// The root, which may be `null` for an empty model.
    protected Object root;

    /// Keeps the listeners and sends the events.
    protected TreeModelSupport modelSupport;

    /// A model without a root.
    public AbstractTreeTableModel() {
        this(null);
    }

    public AbstractTreeTableModel(Object root) {
        this.root = root;
        this.modelSupport = new TreeModelSupport(this);
    }

    /// `Object` for every column.
    @Override
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

    /// The name a spreadsheet gives the column: A to Z, then AA, AB and
    /// so on.
    @Override
    public String getColumnName(int column) {
        StringBuilder name = new StringBuilder();
        int c = column;
        while (c >= 0) {
            name.insert(0, (char) ('A' + c % 26));
            c = c / 26 - 1;
        }
        return name.toString();
    }

    /// The first column, or -1 for a model without columns.
    @Override
    public int getHierarchicalColumn() {
        return getColumnCount() > 0 ? 0 : -1;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    /// No cell is editable.
    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    /// A node without children is a leaf.
    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    /// Does nothing: the model is not editable.
    @Override
    public void setValueAt(Object value, Object node, int column) {
    }

    /// Does nothing: the model is not editable.
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        modelSupport.addTreeModelListener(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        modelSupport.removeTreeModelListener(l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return modelSupport.getTreeModelListeners();
    }
}
