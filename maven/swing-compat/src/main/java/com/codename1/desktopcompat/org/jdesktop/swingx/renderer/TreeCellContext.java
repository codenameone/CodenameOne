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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.rt.CellTheme;

/// The context of a tree cell, which also knows the node's default icon.
public class TreeCellContext extends CellContext {

    private static DefaultTreeCellRenderer cn1Defaults;

    protected Icon leafIcon;
    protected Icon closedIcon;
    protected Icon openIcon;

    public TreeCellContext() {
    }

    public void installContext(JTree component, Object value, int row, int column, boolean selected,
            boolean focused, boolean expanded, boolean leaf) {
        this.component = component;
        installState(value, row, column, selected, focused, expanded, leaf);
    }

    @Override
    public JTree getComponent() {
        return component instanceof JTree ? (JTree) component : null;
    }

    /// The path of the cell's row, or `null`.
    public TreePath getTreePath() {
        JTree t = getComponent();
        if (t == null || row < 0 || row >= t.getRowCount()) {
            return null;
        }
        return t.getPathForRow(row);
    }

    @Override
    public boolean isEditable() {
        JTree t = getComponent();
        TreePath path = getTreePath();
        return t != null && path != null && t.isPathEditable(path);
    }

    @Override
    protected Color getSelectionBackground() {
        return CellTheme.selectionBackground("Tree.selectionBackground");
    }

    @Override
    protected Color getSelectionForeground() {
        return CellTheme.selectionForeground("Tree.selectionForeground");
    }

    @Override
    protected String getUIPrefix() {
        return "Tree.";
    }

    private static DefaultTreeCellRenderer cn1Defaults() {
        if (cn1Defaults == null) {
            cn1Defaults = new DefaultTreeCellRenderer();
        }
        return cn1Defaults;
    }

    protected Icon getLeafIcon() {
        return leafIcon != null ? leafIcon : cn1Defaults().getDefaultLeafIcon();
    }

    protected Icon getOpenIcon() {
        return openIcon != null ? openIcon : cn1Defaults().getDefaultOpenIcon();
    }

    protected Icon getClosedIcon() {
        return closedIcon != null ? closedIcon : cn1Defaults().getDefaultClosedIcon();
    }

    @Override
    public Icon getIcon() {
        if (isLeaf()) {
            return getLeafIcon();
        }
        return isExpanded() ? getOpenIcon() : getClosedIcon();
    }

    @Override
    protected Border getFocusBorder() {
        return super.getFocusBorder();
    }
}
