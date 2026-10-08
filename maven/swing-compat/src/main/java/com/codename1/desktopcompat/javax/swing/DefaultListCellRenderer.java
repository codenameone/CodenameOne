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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;

/// The renderer a list uses unless it is given another: a label showing
/// the icon, when the value is one, or its `toString()`.
public class DefaultListCellRenderer extends JLabel implements ListCellRenderer<Object> {

    public DefaultListCellRenderer() {
        setOpaque(true);
        setName("List.cellRenderer");
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        if (value instanceof Icon) {
            setIcon((Icon) value);
            setText("");
        } else {
            setIcon(null);
            setText(value == null ? "" : value.toString());
        }
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        return this;
    }

    /// The background first: the label behind this renderer is drawn by
    /// Codename One, which leaves the row's color to its owner.
    @Override
    protected void paintComponent(Graphics g) {
        Color bg = getBackground();
        if (isOpaque() && bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    /// The renderer the look and feel installs, as opposed to one an
    /// application set.
    public static class UIResource extends DefaultListCellRenderer {

        public UIResource() {
        }
    }
}
