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
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;
import com.codename1.desktopcompat.javax.swing.border.LineBorder;
import com.codename1.desktopcompat.rt.CellTheme;

/// The cell a renderer is asked to draw: its component, value, position
/// and state, and the colors, font and border the cell's kind of
/// component wants for it.
///
/// Colors come from the component and, through it, from the Codename One
/// theme; there is no look and feel table to read them from. The drop
/// target colors are `null` because the layer has no drag and drop.
public class CellContext {

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    protected transient JComponent component;
    protected transient Object value;
    protected transient int row;
    protected transient int column;
    protected transient boolean selected;
    protected transient boolean focused;
    protected transient boolean expanded;
    protected transient boolean leaf;
    protected transient boolean dropOn;

    private Border cn1FocusBorder;
    private Color cn1FocusColor;

    public CellContext() {
    }

    protected void installState(Object value, int row, int column, boolean selected, boolean focused,
            boolean expanded, boolean leaf) {
        this.value = value;
        this.row = row;
        this.column = column;
        this.selected = selected;
        this.focused = focused;
        this.expanded = expanded;
        this.leaf = leaf;
    }

    /// Puts another value in the context and answers the one it had.
    public Object replaceValue(Object value) {
        Object old = this.value;
        this.value = value;
        return old;
    }

    public JComponent getComponent() {
        return component;
    }

    public Object getValue() {
        return value;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public boolean isEditable() {
        return false;
    }

    /// The icon the cell has by default; only a tree cell has one.
    public Icon getIcon() {
        return null;
    }

    protected boolean isDropOn() {
        return dropOn;
    }

    protected Color getForeground() {
        return component != null ? component.getForeground() : null;
    }

    protected Color getBackground() {
        return component != null ? component.getBackground() : null;
    }

    protected Color getSelectionBackground() {
        return null;
    }

    protected Color getSelectionForeground() {
        return null;
    }

    /// A line in a color between the cell's text and background colors.
    protected Border getFocusBorder() {
        Color fg = selected ? getSelectionForeground() : getForeground();
        Color bg = selected ? getSelectionBackground() : getBackground();
        Color line = fg != null && bg != null ? CellTheme.mix(bg, fg, 0.6f) : Color.GRAY;
        if (cn1FocusBorder == null || !line.equals(cn1FocusColor)) {
            cn1FocusColor = line;
            cn1FocusBorder = new LineBorder(line, 1);
        }
        return cn1FocusBorder;
    }

    protected Border getBorder() {
        if (isFocused()) {
            return getFocusBorder();
        }
        return noFocusBorder;
    }

    protected Color getFocusForeground() {
        return null;
    }

    protected Color getFocusBackground() {
        return null;
    }

    protected Color getDropCellForeground() {
        return null;
    }

    protected Color getDropCellBackground() {
        return null;
    }

    protected String getUIKey(String key) {
        return getUIPrefix() + key;
    }

    protected String getUIPrefix() {
        return "";
    }

    protected Font getFont() {
        return component != null ? component.getFont() : null;
    }

    public String getCellRendererName() {
        return getUIPrefix() + "cellRenderer";
    }
}
