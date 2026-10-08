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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.rt.CellTheme;

/// The renderer of tree rows: a label, and so a Codename One label,
/// showing the node's text in the theme's colors.
///
/// There are no stock folder and document icons: the three icons are
/// `null` until an application sets them, and a row without one is just
/// its text. The tree draws the expand handles itself.
public class DefaultTreeCellRenderer extends JLabel implements TreeCellRenderer {

    protected boolean selected;
    protected boolean hasFocus;
    protected transient Icon closedIcon;
    protected transient Icon leafIcon;
    protected transient Icon openIcon;
    protected Color textSelectionColor;
    protected Color textNonSelectionColor;
    protected Color backgroundSelectionColor;
    protected Color backgroundNonSelectionColor;
    protected Color borderSelectionColor;

    private Color shownForeground;

    public DefaultTreeCellRenderer() {
        setOpaque(false);
        textSelectionColor = CellTheme.selectionForeground("Tree.selectionForeground");
        textNonSelectionColor = CellTheme.foreground("Tree.textForeground");
        backgroundSelectionColor = CellTheme.selectionBackground("Tree.selectionBackground");
        backgroundNonSelectionColor = null;
        borderSelectionColor = CellTheme.mix(backgroundSelectionColor, textSelectionColor, 0.6f);
        setName("Tree.cellRenderer");
    }

    public Icon getDefaultOpenIcon() {
        return null;
    }

    public Icon getDefaultClosedIcon() {
        return null;
    }

    public Icon getDefaultLeafIcon() {
        return null;
    }

    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    public Icon getOpenIcon() {
        return openIcon;
    }

    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    public Icon getClosedIcon() {
        return closedIcon;
    }

    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    public Icon getLeafIcon() {
        return leafIcon;
    }

    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }

    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        String text = tree == null ? String.valueOf(value)
                : tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
        this.hasFocus = hasFocus;
        this.selected = sel;
        if (!text.equals(getText())) {
            setText(text);
        }
        Color fg = sel ? textSelectionColor : textNonSelectionColor;
        // This reaches the Codename One label's style, so it is not
        // repeated for a row that looks like the one before it.
        if (fg != null && !fg.equals(shownForeground)) {
            shownForeground = fg;
            super.setForeground(fg);
        }
        if (tree != null) {
            Font f = tree.getFont();
            if (f != null && !f.equals(isFontSet() ? getFont() : null)) {
                setFont(f);
            }
            if (isEnabled() != tree.isEnabled()) {
                setEnabled(tree.isEnabled());
            }
        }
        Icon icon = leaf ? leafIcon : expanded ? openIcon : closedIcon;
        if (icon != getIcon()) {
            setIcon(icon);
        }
        return this;
    }

    /// Fills the row with the background color of its state, if there is
    /// one, and paints the label over it.
    @Override
    public void paint(Graphics g) {
        Color bg = selected ? backgroundSelectionColor : backgroundNonSelectionColor;
        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paint(g);
        if (hasFocus && selected && borderSelectionColor != null) {
            g.setColor(borderSelectionColor);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void validate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void invalidate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void revalidate() {
    }

    /// Does nothing: the tree paints the renderer when it paints itself.
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /// Does nothing: the tree paints the renderer when it paints itself.
    @Override
    public void repaint(Rectangle r) {
    }

    /// Does nothing: the tree paints the renderer when it paints itself.
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
}
