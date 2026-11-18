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
        child.parent = this;
        children.add(child);
        return child;
    }

    /// The node name.
    public String getLabel() {
        return label;
    }

    /// The leaf value, or the sum of the children's values for a branch.
    public double getValue() {
        if (children.isEmpty()) {
            return Math.max(0, value);
        }
        double s = 0;
        for (TreeNode c : children) {
            s += c.getValue();
        }
        return s;
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
        value = v;
        return this;
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
