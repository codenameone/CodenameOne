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
package com.codename1.desktopcompat.javax.swing.plaf.basic;

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.plaf.TreeUI;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;

/// The delegate of a tree, as far as an application reaches into it: a
/// subclass overrides [#paintRow] to draw behind or instead of a row.
///
/// Set on a tree with `JTree.setUI`, its `paintRow` is called for every
/// row the tree paints, with the rectangle of the row's text. What it does
/// as it comes is what the tree does without a delegate: the tint of a
/// selected row across the tree and the renderer's component. The arrow
/// of a node with children is drawn by the tree afterwards, over the row.
///
/// Nothing else of the JDK's class is here. The tree measures, lays out
/// and handles input itself, so the many protected fields and hooks the
/// JDK's delegate has for that would be read and never honoured.
public class BasicTreeUI extends TreeUI {

    /// The tree this delegate is set on, `null` when it is on none.
    protected JTree tree;

    public BasicTreeUI() {
    }

    @Override
    public void installUI(JComponent c) {
        if (c == null) {
            throw new NullPointerException("null component passed to BasicTreeUI.installUI()");
        }
        if (c instanceof JTree) {
            tree = (JTree) c;
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        tree = null;
    }

    /// Paints one row. `bounds` is the rectangle of the row's text, as
    /// `JTree.getPathBounds` answers it; `clipBounds` what is being
    /// painted and `insets` the tree's.
    protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path,
            int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
        if (tree != null) {
            tree.cn1PaintRow(g, path, row);
        }
    }

    /// Called by the tree for each row it paints. Not Swing API.
    public void cn1PaintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path,
            int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
        paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
    }

    @Override
    public Rectangle getPathBounds(JTree tree, TreePath path) {
        return tree == null ? null : tree.getPathBounds(path);
    }

    @Override
    public TreePath getPathForRow(JTree tree, int row) {
        return tree == null ? null : tree.getPathForRow(row);
    }

    @Override
    public int getRowForPath(JTree tree, TreePath path) {
        return tree == null ? -1 : tree.getRowForPath(path);
    }

    @Override
    public int getRowCount(JTree tree) {
        return tree == null ? 0 : tree.getRowCount();
    }

    @Override
    public TreePath getClosestPathForLocation(JTree tree, int x, int y) {
        return tree == null ? null : tree.getClosestPathForLocation(x, y);
    }

    @Override
    public boolean isEditing(JTree tree) {
        return tree != null && tree.isEditing();
    }

    @Override
    public boolean stopEditing(JTree tree) {
        return tree != null && tree.stopEditing();
    }

    @Override
    public void cancelEditing(JTree tree) {
        if (tree != null) {
            tree.cancelEditing();
        }
    }

    @Override
    public void startEditingAtPath(JTree tree, TreePath path) {
        if (tree != null) {
            tree.startEditingAtPath(path);
        }
    }

    @Override
    public TreePath getEditingPath(JTree tree) {
        return tree == null ? null : tree.getEditingPath();
    }
}
