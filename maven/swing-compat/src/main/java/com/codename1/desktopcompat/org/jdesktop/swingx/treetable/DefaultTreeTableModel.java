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

import com.codename1.desktopcompat.javax.swing.tree.TreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/// A tree table model over [TreeTableNode]s, with a list of column
/// identifiers that names the columns and says how many there are.
///
/// A node may have fewer columns than the model: its cells in the others
/// are empty and cannot be edited. Every method that takes a node wants
/// one that is in this model, the root or below it, and throws an
/// `IllegalArgumentException` for any other.
///
/// The list of identifiers is kept, not copied.
public class DefaultTreeTableModel extends AbstractTreeTableModel {

    protected List<?> columnIdentifiers;

    /// A model without a root and without columns.
    public DefaultTreeTableModel() {
        this(null, new ArrayList<Object>());
    }

    /// A model with one unnamed column.
    public DefaultTreeTableModel(TreeTableNode root) {
        this(root, null);
    }

    /// A model with a column for each identifier; `null` is one unnamed
    /// column.
    public DefaultTreeTableModel(TreeTableNode root, List<?> columnNames) {
        super(root);
        columnIdentifiers = cn1Identifiers(columnNames);
    }

    private static List<?> cn1Identifiers(List<?> identifiers) {
        if (identifiers != null) {
            return identifiers;
        }
        List<Object> one = new ArrayList<Object>();
        one.add(null);
        return one;
    }

    /// Replaces the columns; `null` is one unnamed column. Listeners are
    /// told that the whole model changed.
    public void setColumnIdentifiers(List<?> columnIdentifiers) {
        this.columnIdentifiers = cn1Identifiers(columnIdentifiers);
        modelSupport.fireNewRoot();
    }

    @Override
    public TreeTableNode getRoot() {
        return root instanceof TreeTableNode ? (TreeTableNode) root : null;
    }

    /// Whether the node is the root or below it.
    private boolean cn1Managed(Object node) {
        if (!(node instanceof TreeTableNode)) {
            return false;
        }
        TreeNode n = (TreeTableNode) node;
        while (n != null) {
            if (n == root) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private TreeTableNode cn1Cell(Object node, int column) {
        if (!cn1Managed(node)) {
            throw new IllegalArgumentException("node must be a valid node managed by this model");
        }
        if (column < 0 || column >= getColumnCount()) {
            throw new IllegalArgumentException("column must be a valid index");
        }
        return (TreeTableNode) node;
    }

    /// The node's value in the column, or `null` in a column the node
    /// does not have.
    @Override
    public Object getValueAt(Object node, int column) {
        TreeTableNode n = cn1Cell(node, column);
        return column >= n.getColumnCount() ? null : n.getValueAt(column);
    }

    /// Gives the node the value and tells the listeners that the node
    /// changed; nothing in a column the node does not have.
    @Override
    public void setValueAt(Object value, Object node, int column) {
        TreeTableNode n = cn1Cell(node, column);
        if (column < n.getColumnCount()) {
            n.setValueAt(value, column);
            modelSupport.firePathChanged(new TreePath(getPathToRoot(n)));
        }
    }

    @Override
    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    /// The text of the column's identifier, or the spreadsheet name for a
    /// column without one.
    @Override
    public String getColumnName(int column) {
        Object id = column >= 0 && column < columnIdentifiers.size() ? columnIdentifiers.get(column) : null;
        return id != null ? id.toString() : super.getColumnName(column);
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (!cn1Managed(parent)) {
            throw new IllegalArgumentException("parent must be a TreeTableNode managed by this model");
        }
        return ((TreeTableNode) parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        if (!cn1Managed(parent)) {
            throw new IllegalArgumentException("parent must be a TreeTableNode managed by this model");
        }
        return ((TreeTableNode) parent).getChildCount();
    }

    /// The index of `child` in `parent`, or -1 when either is not a node
    /// of this model.
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (cn1Managed(parent) && cn1Managed(child)) {
            return ((TreeTableNode) parent).getIndex((TreeTableNode) child);
        }
        return -1;
    }

    /// Whether the node says the column is editable; never in a column
    /// the node does not have.
    @Override
    public boolean isCellEditable(Object node, int column) {
        TreeTableNode n = cn1Cell(node, column);
        return column < n.getColumnCount() && n.isEditable(column);
    }

    @Override
    public boolean isLeaf(Object node) {
        if (!cn1Managed(node)) {
            throw new IllegalArgumentException("node must be a TreeTableNode managed by this model");
        }
        return ((TreeTableNode) node).isLeaf();
    }

    /// The nodes from the root down to `aNode`, which must be in this
    /// model.
    public TreeTableNode[] getPathToRoot(TreeTableNode aNode) {
        List<TreeTableNode> path = new ArrayList<TreeTableNode>();
        TreeTableNode node = aNode;
        while (node != root) {
            path.add(0, node);
            node = node.getParent();
        }
        path.add(0, node);
        return path.toArray(new TreeTableNode[path.size()]);
    }

    /// Replaces the root, with `null` for an empty model, and tells the
    /// listeners.
    public void setRoot(TreeTableNode root) {
        this.root = root;
        modelSupport.fireNewRoot();
    }

    /// Puts `newChild` into `parent` at `index` and tells the listeners.
    public void insertNodeInto(MutableTreeTableNode newChild, MutableTreeTableNode parent, int index) {
        parent.insert(newChild, index);
        modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), index, newChild);
    }

    /// Takes `node` out of its parent, which it must have, and tells the
    /// listeners.
    public void removeNodeFromParent(MutableTreeTableNode node) {
        TreeTableNode parent = node.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("node does not have a parent.");
        }
        int index = parent.getIndex(node);
        node.removeFromParent();
        modelSupport.fireChildRemoved(new TreePath(getPathToRoot(parent)), index, node);
    }

    /// Makes `newValue` the user object of the node at the end of a path
    /// that starts at this model's root, and tells the listeners.
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (path.getPathComponent(0) != root) {
            throw new IllegalArgumentException("invalid path");
        }
        Object last = path.getLastPathComponent();
        if (last instanceof TreeTableNode) {
            ((TreeTableNode) last).setUserObject(newValue);
            modelSupport.firePathChanged(path);
        }
    }

    /// Gives a node of this model another user object and tells the
    /// listeners.
    public void setUserObject(TreeTableNode node, Object userObject) {
        node.setUserObject(userObject);
        modelSupport.firePathChanged(new TreePath(getPathToRoot(node)));
    }
}
