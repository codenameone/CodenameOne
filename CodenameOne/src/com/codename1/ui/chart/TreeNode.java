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
package com.codename1.ui.chart;

import java.util.ArrayList;
import java.util.List;

/// A node of hierarchical data for treemaps and sunbursts.
///
/// A leaf carries a value; a branch's value is the sum of its children.
///
/// ```java
/// TreeNode root = new TreeNode("Portfolio");
/// TreeNode tech = root.add(new TreeNode("Tech"));
/// tech.add(new TreeNode("ACME", 120));
/// tech.add(new TreeNode("Globex", 80));
/// root.add(new TreeNode("Energy", 60));
/// ```
public class TreeNode {
    private final String label;
    private double value;
    private Integer color;
    private TreeNode parent;
    private final List<TreeNode> children = new ArrayList<TreeNode>();
    // a branch's sum, kept until a change below it; a leaf never reads these
    private double sum;
    private boolean sumValid;
    // this node's position among its parent's children
    private int indexInParent;
    private ChangeListener listener;
    // how many times a branch summed its children; read by tests to hold the cache
    int sumComputations;

    /// Told about every change to the tree under the node it is set on: `setValue` and `add`
    /// on that node or any node below it.
    public interface ChangeListener {
        /// Called before a change, while the tree still holds the old values.
        ///
        /// #### Parameters
        ///
        /// - `node`: the node about to change
        void treeChanging(TreeNode node);

        /// Called after a change.
        ///
        /// #### Parameters
        ///
        /// - `node`: the node that changed
        void treeChanged(TreeNode node);
    }

    /// Creates a branch.
    ///
    /// #### Parameters
    ///
    /// - `label`: the node name
    public TreeNode(String label) {
        this(label, 0);
    }

    /// Creates a leaf.
    ///
    /// #### Parameters
    ///
    /// - `label`: the node name
    ///
    /// - `value`: the leaf value
    public TreeNode(String label, double value) {
        this.label = label;
        this.value = value;
    }

    /// Adds a child.
    ///
    /// #### Parameters
    ///
    /// - `child`: the child node
    ///
    /// #### Returns
    ///
    /// the child, for chaining further adds onto it
    public TreeNode add(TreeNode child) {
        for (TreeNode p = this; p != null; p = p.parent) {
            if (p == child) { //NOPMD CompareObjectsWithEquals - identity, a cycle is the same node
                throw new IllegalArgumentException("a node cannot be added under itself or its own descendant");
            }
        }
        TreeNode old = child.parent;
        if (old != null) {
            old.changing();
        }
        changing();
        if (old != null) {
            // a node lives in one place: moving it out of its old parent keeps that parent's
            // sum and the hierarchy a tree
            int at = child.indexInParent;
            old.children.remove(at);
            for (int i = at; i < old.children.size(); i++) {
                old.children.get(i).indexInParent = i;
            }
            child.parent = null;
            old.changed();
        }
        child.parent = this;
        child.indexInParent = children.size();
        children.add(child);
        changed();
        return child;
    }

    /// The node name.
    public String getLabel() {
        return label;
    }

    /// The leaf value, or the sum of the children's values for a branch.
    public double getValue() {
        if (children.isEmpty()) {
            return value > 0 && value < Double.POSITIVE_INFINITY ? value : 0; // NaN, infinities and negatives count as nothing
        }
        if (!sumValid) {
            double s = 0;
            for (TreeNode c : children) {
                s += c.getValue();
            }
            sum = s;
            sumValid = true;
            sumComputations++;
        }
        return sum;
    }

    /// Changes a leaf's value.
    ///
    /// #### Parameters
    ///
    /// - `v`: the new value
    ///
    /// #### Returns
    ///
    /// this node
    public TreeNode setValue(double v) {
        changing();
        value = v;
        changed();
        return this;
    }

    /// This node's position among its parent's children, or 0 for a node without a parent.
    ///
    /// #### Returns
    ///
    /// the child index
    public int getIndexInParent() {
        return parent == null ? 0 : indexInParent;
    }

    /// Sets the listener told about changes to this node and every node below it; null for
    /// none. A `HierarchySeries` sets one on its root so changes to the tree animate.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void setChangeListener(ChangeListener l) {
        listener = l;
    }

    /// The listener set by `setChangeListener`, or null.
    ///
    /// #### Returns
    ///
    /// the listener
    public ChangeListener getChangeListener() {
        return listener;
    }

    private void changing() {
        for (TreeNode p = this; p != null; p = p.parent) {
            if (p.listener != null) {
                p.listener.treeChanging(this);
            }
        }
    }

    // drops the cached sums on the way up, then tells every listener on the chain
    private void changed() {
        for (TreeNode p = this; p != null; p = p.parent) {
            p.sumValid = false;
        }
        for (TreeNode p = this; p != null; p = p.parent) {
            if (p.listener != null) {
                p.listener.treeChanged(this);
            }
        }
    }

    /// The colour fixed for this node, or null to derive it from the palette.
    public Integer getColor() {
        return color;
    }

    /// Fixes this node's colour; its descendants use shades of it.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the colour
    ///
    /// #### Returns
    ///
    /// this node
    public TreeNode setColor(int rgb) {
        color = Integer.valueOf(rgb);
        return this;
    }

    /// The parent, or null for the root.
    public TreeNode getParent() {
        return parent;
    }

    /// The number of children.
    public int getChildCount() {
        return children.size();
    }

    /// Child `i`.
    public TreeNode getChild(int i) {
        return children.get(i);
    }

    /// The node's depth below the root, 0 for the root.
    public int getDepth() {
        int d = 0;
        TreeNode p = parent;
        while (p != null) {
            d++;
            p = p.parent;
        }
        return d;
    }

    /// The path from the root, names joined with " / ".
    public String getPath() {
        if (parent == null) {
            return label;
        }
        String pp = parent.getPath();
        return pp == null || pp.length() == 0 ? label : pp + " / " + label;
    }
}
