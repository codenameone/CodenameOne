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
package com.codename1.desktopcompat.org.jdesktop.swingx.tree;

import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.tree.TreeModel;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;

/// Keeps the listeners of a tree model and sends them the model's
/// events, each named for what happened to the model rather than for the
/// event that says so.
///
/// The model is the source of every event. Listeners are told in the
/// reverse of the order they were added in.
public final class TreeModelSupport {

    protected EventListenerList listeners;

    private final TreeModel treeModel;

    /// Support for the listeners of `model`, which must not be `null`.
    public TreeModelSupport(TreeModel model) {
        if (model == null) {
            throw new NullPointerException("model must not be null");
        }
        listeners = new EventListenerList();
        treeModel = model;
    }

    /// The model has another root, or everything below its root changed.
    /// A model without a root sends an event without a path.
    public void fireNewRoot() {
        Object root = treeModel.getRoot();
        fireTreeStructureChanged(root == null ? null : new TreePath(root));
    }

    /// The node at the end of the path became a leaf or stopped being
    /// one. Sent as a change of structure below the node, the only event
    /// that makes a tree ask again.
    public void firePathLeafStateChanged(TreePath path) {
        fireTreeStructureChanged(path);
    }

    /// Everything below the node at the end of the path changed; `null`
    /// for a model that has lost its root.
    public void fireTreeStructureChanged(TreePath subTreePath) {
        TreeModelListener[] ls = getTreeModelListeners();
        if (ls.length == 0) {
            return;
        }
        TreeModelEvent e = new TreeModelEvent(treeModel, subTreePath);
        for (int i = 0; i < ls.length; i++) {
            ls[i].treeStructureChanged(e);
        }
    }

    /// The node at the end of the path changed, but not its children.
    /// For the root that is an event with the root's path and no
    /// children; for any other node it names the node as a child of its
    /// parent.
    public void firePathChanged(TreePath path) {
        Object node = path.getLastPathComponent();
        TreePath parentPath = path.getParentPath();
        if (parentPath == null) {
            fireChildrenChanged(path, null, null);
        } else {
            Object parent = parentPath.getLastPathComponent();
            fireChildChanged(parentPath, treeModel.getIndexOfChild(parent, node), node);
        }
    }

    /// One child of the node at the end of `parentPath` changed.
    public void fireChildChanged(TreePath parentPath, int index, Object child) {
        fireChildrenChanged(parentPath, new int[]{index}, new Object[]{child});
    }

    /// Children of the node at the end of `parentPath` changed; the
    /// indices are in ascending order.
    public void fireChildrenChanged(TreePath parentPath, int[] indices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        if (ls.length == 0) {
            return;
        }
        TreeModelEvent e = new TreeModelEvent(treeModel, parentPath, indices, children);
        for (int i = 0; i < ls.length; i++) {
            ls[i].treeNodesChanged(e);
        }
    }

    /// A child was added to the node at the end of `parentPath`, at
    /// `index`.
    public void fireChildAdded(TreePath parentPath, int index, Object child) {
        fireChildrenAdded(parentPath, new int[]{index}, new Object[]{child});
    }

    /// A child that was at `index` was removed from the node at the end
    /// of `parentPath`.
    public void fireChildRemoved(TreePath parentPath, int index, Object child) {
        fireChildrenRemoved(parentPath, new int[]{index}, new Object[]{child});
    }

    /// Children were added to the node at the end of `parentPath`; the
    /// indices are where they are now, in ascending order.
    public void fireChildrenAdded(TreePath parentPath, int[] indices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        if (ls.length == 0) {
            return;
        }
        TreeModelEvent e = new TreeModelEvent(treeModel, parentPath, indices, children);
        for (int i = 0; i < ls.length; i++) {
            ls[i].treeNodesInserted(e);
        }
    }

    /// Children were removed from the node at the end of `parentPath`;
    /// the indices are where they were, in ascending order.
    public void fireChildrenRemoved(TreePath parentPath, int[] indices, Object[] children) {
        TreeModelListener[] ls = getTreeModelListeners();
        if (ls.length == 0) {
            return;
        }
        TreeModelEvent e = new TreeModelEvent(treeModel, parentPath, indices, children);
        for (int i = 0; i < ls.length; i++) {
            ls[i].treeNodesRemoved(e);
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(TreeModelListener.class, l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return listeners.getListeners(TreeModelListener.class);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(TreeModelListener.class, l);
    }
}
