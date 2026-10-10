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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/// A mutable tree table node but for its columns: the parent, the list of
/// children and the user object, kept consistent on both sides of every
/// change.
///
/// A subclass says how many columns the node has and what is in them. As
/// it is, no column is editable and setting a value does nothing.
public abstract class AbstractMutableTreeTableNode implements MutableTreeTableNode {

    protected MutableTreeTableNode parent;
    protected final List<MutableTreeTableNode> children;
    protected Object userObject;
    protected boolean allowsChildren;

    /// A node without a user object that takes children.
    public AbstractMutableTreeTableNode() {
        this(null);
    }

    /// A node that takes children.
    public AbstractMutableTreeTableNode(Object userObject) {
        this(userObject, true);
    }

    public AbstractMutableTreeTableNode(Object userObject, boolean allowsChildren) {
        this.userObject = userObject;
        this.allowsChildren = allowsChildren;
        children = createChildrenList();
    }

    /// Makes the list the children are kept in; called once, while the
    /// node is being constructed.
    protected List<MutableTreeTableNode> createChildrenList() {
        return new ArrayList<MutableTreeTableNode>();
    }

    /// Makes `child` the last child of this node.
    public void add(MutableTreeTableNode child) {
        insert(child, getChildCount());
    }

    /// Makes `child` the child at `index`, taking it from the parent it
    /// had. A node that is a child of this one already is taken out
    /// first, and `index` then counts one less.
    @Override
    public void insert(MutableTreeTableNode child, int index) {
        if (!allowsChildren) {
            throw new IllegalStateException("this node cannot accept children");
        }
        int at = index;
        if (children.contains(child)) {
            children.remove(child);
            at--;
        }
        children.add(at, child);
        if (child.getParent() != this) {
            child.setParent(this);
        }
    }

    @Override
    public void remove(int index) {
        children.remove(index).setParent(null);
    }

    /// Takes `node` out of the tree. A node that is another node's child
    /// is taken from that one.
    @Override
    public void remove(MutableTreeTableNode node) {
        children.remove(node);
        node.setParent(null);
    }

    /// Takes this node out of its parent, which it must have.
    @Override
    public void removeFromParent() {
        parent.remove(this);
    }

    /// Moves this node below `newParent`, as its last child, or out of
    /// the tree for `null`.
    @Override
    public void setParent(MutableTreeTableNode newParent) {
        if (newParent != null && !newParent.getAllowsChildren()) {
            throw new IllegalArgumentException("newParent does not allow children");
        }
        if (parent != null && parent.getIndex(this) != -1) {
            parent.remove(this);
        }
        parent = newParent;
        if (parent != null && parent.getIndex(this) == -1) {
            parent.insert(this, parent.getChildCount());
        }
    }

    @Override
    public Object getUserObject() {
        return userObject;
    }

    @Override
    public void setUserObject(Object object) {
        userObject = object;
    }

    @Override
    public TreeTableNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    /// The index of a child, or -1 for a node that is not one.
    @Override
    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    @Override
    public TreeTableNode getParent() {
        return parent;
    }

    @Override
    public Enumeration<? extends MutableTreeTableNode> children() {
        return Collections.enumeration(children);
    }

    @Override
    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    /// Says whether the node takes children. One that stops taking them
    /// loses those it has.
    public void setAllowsChildren(boolean allowsChildren) {
        this.allowsChildren = allowsChildren;
        if (!allowsChildren) {
            children.clear();
        }
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    /// A node without children is a leaf.
    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    /// No column is editable.
    @Override
    public boolean isEditable(int column) {
        return false;
    }

    /// Does nothing.
    @Override
    public void setValueAt(Object aValue, int column) {
    }

    /// The user object's text, or the empty string without one.
    @Override
    public String toString() {
        return userObject == null ? "" : userObject.toString();
    }
}
