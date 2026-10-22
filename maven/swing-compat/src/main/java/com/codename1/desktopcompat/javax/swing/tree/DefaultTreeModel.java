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
package com.codename1.desktopcompat.javax.swing.tree;

import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import java.util.EventListener;

/// A tree model over [TreeNode]s. Change the nodes through
/// [#insertNodeInto] and [#removeNodeFromParent], or change them directly
/// and then say so with one of the `node...` methods, so that the tree
/// hears of it.
public class DefaultTreeModel implements TreeModel {

    protected TreeNode root;
    protected EventListenerList listenerList = new EventListenerList();
    protected boolean asksAllowsChildren;

    public DefaultTreeModel(TreeNode root) {
        this(root, false);
    }

    public DefaultTreeModel(TreeNode root, boolean asksAllowsChildren) {
        this.root = root;
        this.asksAllowsChildren = asksAllowsChildren;
    }

    public void setAsksAllowsChildren(boolean newValue) {
        asksAllowsChildren = newValue;
    }

    public boolean asksAllowsChildren() {
        return asksAllowsChildren;
    }

    public void setRoot(TreeNode root) {
        Object oldRoot = this.root;
        this.root = root;
        if (root == null && oldRoot != null) {
            fireTreeStructureChanged(this, null);
        } else {
            nodeStructureChanged(root);
        }
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (!(parent instanceof TreeNode) || !(child instanceof TreeNode)) {
            return -1;
        }
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof TreeNode ? ((TreeNode) parent).getChildAt(index) : null;
    }

    @Override
    public int getChildCount(Object parent) {
        return parent instanceof TreeNode ? ((TreeNode) parent).getChildCount() : 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (!(node instanceof TreeNode)) {
            return true;
        }
        if (asksAllowsChildren) {
            return !((TreeNode) node).getAllowsChildren();
        }
        return ((TreeNode) node).isLeaf();
    }

    /// Everything below the root may have changed.
    public void reload() {
        reload(root);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        Object node = path.getLastPathComponent();
        if (node instanceof MutableTreeNode) {
            ((MutableTreeNode) node).setUserObject(newValue);
            nodeChanged((MutableTreeNode) node);
        }
    }

    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        parent.insert(newChild, index);
        nodesWereInserted(parent, new int[]{index});
    }

    public void removeNodeFromParent(MutableTreeNode node) {
        TreeNode parent = node.getParent();
        if (!(parent instanceof MutableTreeNode)) {
            throw new IllegalArgumentException("node does not have a parent.");
        }
        int[] childIndex = {parent.getIndex(node)};
        Object[] removed = {node};
        ((MutableTreeNode) parent).remove(childIndex[0]);
        nodesWereRemoved(parent, childIndex, removed);
    }

    public void nodeChanged(TreeNode node) {
        if (listenerList != null && node != null) {
            TreeNode parent = node.getParent();
            if (parent != null) {
                int index = parent.getIndex(node);
                if (index != -1) {
                    nodesChanged(parent, new int[]{index});
                }
            } else if (node == getRoot()) {
                nodesChanged(node, null);
            }
        }
    }

    /// Everything below `node` may have changed.
    public void reload(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    public void nodesWereInserted(TreeNode node, int[] childIndices) {
        if (listenerList != null && node != null && childIndices != null && childIndices.length > 0) {
            Object[] newChildren = new Object[childIndices.length];
            for (int i = 0; i < childIndices.length; i++) {
                newChildren[i] = node.getChildAt(childIndices[i]);
            }
            fireTreeNodesInserted(this, getPathToRoot(node), childIndices, newChildren);
        }
    }

    public void nodesWereRemoved(TreeNode node, int[] childIndices, Object[] removedChildren) {
        if (node != null && childIndices != null) {
            fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren);
        }
    }

    public void nodesChanged(TreeNode node, int[] childIndices) {
        if (node != null) {
            if (childIndices != null) {
                if (childIndices.length > 0) {
                    Object[] changed = new Object[childIndices.length];
                    for (int i = 0; i < childIndices.length; i++) {
                        changed[i] = node.getChildAt(childIndices[i]);
                    }
                    fireTreeNodesChanged(this, getPathToRoot(node), childIndices, changed);
                }
            } else if (node == getRoot()) {
                fireTreeNodesChanged(this, getPathToRoot(node), null, null);
            }
        }
    }

    public void nodeStructureChanged(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    public TreeNode[] getPathToRoot(TreeNode aNode) {
        return getPathToRoot(aNode, 0);
    }

    /// The nodes from the model's root down to `aNode`, with `depth` free
    /// places after them.
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        int n = 0;
        boolean reached = false;
        for (TreeNode t = aNode; t != null; t = t.getParent()) {
            n++;
            if (t == root) {
                reached = true;
                break;
            }
        }
        if (aNode == null) {
            return depth == 0 ? null : new TreeNode[depth];
        }
        TreeNode[] out = new TreeNode[n + depth];
        int at = n;
        for (TreeNode t = aNode; t != null && at > 0; t = t.getParent()) {
            out[--at] = t;
            if (reached && t == root) {
                break;
            }
        }
        return out;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return listenerList.getListeners(TreeModelListener.class);
    }

    protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        TreeModelEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeModelEvent(source, path, childIndices, children);
            }
            ls[i].treeNodesChanged(e);
        }
    }

    protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        TreeModelEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeModelEvent(source, path, childIndices, children);
            }
            ls[i].treeNodesInserted(e);
        }
    }

    protected void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        TreeModelEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeModelEvent(source, path, childIndices, children);
            }
            ls[i].treeNodesRemoved(e);
        }
    }

    protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        TreeModelEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeModelEvent(source, path, childIndices, children);
            }
            ls[i].treeStructureChanged(e);
        }
    }

    private void fireTreeStructureChanged(Object source, TreePath path) {
        TreeModelListener[] ls = getTreeModelListeners();
        TreeModelEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeModelEvent(source, path);
            }
            ls[i].treeStructureChanged(e);
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
