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
package com.codename1.desktopcompat.javax.swing.table;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;
import com.codename1.desktopcompat.javax.swing.border.LineBorder;
import com.codename1.desktopcompat.rt.CellTheme;

/// The renderer of table cells: a label, and so a Codename One label,
/// showing the value's string in the table's colors -- the selection
/// colors for a selected cell -- with a line around the cell that has the
/// focus.
///
/// One renderer paints every cell of its kind, so the methods that would
/// lay out or repaint a real component do nothing here. A subclass
/// changes what is shown by overriding [#setValue(Object)], or
/// `paintComponent` to draw the cell itself.
public class DefaultTableCellRenderer extends JLabel implements TableCellRenderer {

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private Color unselectedForeground;
    private Color unselectedBackground;
    private Border focusBorder;
    private Color focusColor;

    public DefaultTableCellRenderer() {
        super();
        setOpaque(true);
        setBorder(noFocusBorder);
        setName("Table.cellRenderer");
    }

    /// Sets the text color of unselected cells.
    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
        unselectedForeground = c;
    }

    /// Sets the background of unselected cells.
    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        unselectedBackground = c;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (table == null) {
            setValue(value);
            return this;
        }
        Color fg;
        Color bg;
        if (isSelected) {
            fg = table.getSelectionForeground();
            bg = table.getSelectionBackground();
        } else {
            fg = unselectedForeground != null ? unselectedForeground : table.getForeground();
            bg = unselectedBackground != null ? unselectedBackground : table.getBackground();
        }
        // Each of these reaches the Codename One label's style, so none is
        // repeated for a cell that looks like the one before it.
        if (!same(fg, isForegroundSet() ? getForeground() : null)) {
            super.setForeground(fg);
        }
        if (!same(bg, isBackgroundSet() ? getBackground() : null)) {
            super.setBackground(bg);
        }
        Font f = table.getFont();
        if (!same(f, isFontSet() ? getFont() : null)) {
            setFont(f);
        }
        if (hasFocus) {
            Color line = fg != null && bg != null ? CellTheme.mix(bg, fg, 0.6f) : Color.GRAY;
            if (focusBorder == null || !line.equals(focusColor)) {
                focusColor = line;
                focusBorder = new LineBorder(line, 1);
            }
            setBorder(focusBorder);
        } else {
            setBorder(noFocusBorder);
        }
        setValue(value);
        return this;
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void invalidate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void validate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void revalidate() {
    }

    /// Does nothing: the table paints the renderer when it paints itself.
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /// Does nothing: the table paints the renderer when it paints itself.
    @Override
    public void repaint(Rectangle r) {
    }

    /// Does nothing: the table paints the renderer when it paints itself.
    @Override
    public void repaint() {
    }

    /// Does nothing: nobody listens to a renderer's properties.
    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    /// Does nothing: nobody listens to a renderer's properties.
    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    /// Shows the value: its string, or nothing for `null`.
    protected void setValue(Object value) {
        setText(value == null ? "" : value.toString());
    }
}
