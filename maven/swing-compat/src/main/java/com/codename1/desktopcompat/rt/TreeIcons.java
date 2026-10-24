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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.javax.swing.Icon;

/// The icons a tree gives its nodes when the application sets none: a
/// closed folder, an open folder and a sheet of paper for a leaf.
///
/// They are drawn, not loaded, in tones of the text color of the component
/// they are painted on over its background, so they read in a light and in
/// a dark theme and need no resource in the application.
public final class TreeIcons implements Icon {

    private static final int SIZE = 16;
    private static final int CLOSED_KIND = 0;
    private static final int OPEN_KIND = 1;
    private static final int LEAF_KIND = 2;

    /// A folder that is closed.
    public static final Icon CLOSED = new TreeIcons(CLOSED_KIND);
    /// A folder that is open.
    public static final Icon OPEN = new TreeIcons(OPEN_KIND);
    /// A sheet of paper.
    public static final Icon LEAF = new TreeIcons(LEAF_KIND);

    private final int kind;

    private TreeIcons(int kind) {
        this.kind = kind;
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Color fg = c != null ? c.getForeground() : null;
        Color bg = c != null ? c.getBackground() : null;
        if (fg == null) {
            fg = Color.DARK_GRAY;
        }
        if (bg == null) {
            bg = Color.WHITE;
        }
        Color line = CellTheme.mix(bg, fg, 0.7f);
        Color fill = CellTheme.mix(bg, fg, 0.3f);
        Color saved = g.getColor();
        if (kind == LEAF_KIND) {
            // A sheet with its top right corner folded.
            g.setColor(CellTheme.mix(bg, fg, 0.06f));
            g.fillRect(x + 3, y + 1, 10, 14);
            g.setColor(line);
            g.drawLine(x + 3, y + 1, x + 9, y + 1);
            g.drawLine(x + 3, y + 1, x + 3, y + 14);
            g.drawLine(x + 3, y + 14, x + 12, y + 14);
            g.drawLine(x + 12, y + 4, x + 12, y + 14);
            g.drawLine(x + 9, y + 1, x + 12, y + 4);
            g.drawLine(x + 9, y + 1, x + 9, y + 4);
            g.drawLine(x + 9, y + 4, x + 12, y + 4);
        } else {
            // The tab, the back, and the front: lower when the folder is open.
            g.setColor(line);
            g.fillRect(x + 1, y + 2, 6, 3);
            g.fillRect(x + 1, y + 4, 14, 10);
            g.setColor(fill);
            if (kind == OPEN_KIND) {
                g.fillPolygon(new int[]{x + 3, x + 16, x + 14, x + 1}, new int[]{y + 8, y + 8, y + 14, y + 14}, 4);
            } else {
                g.fillRect(x + 2, y + 6, 12, 7);
            }
        }
        g.setColor(saved);
    }
}
