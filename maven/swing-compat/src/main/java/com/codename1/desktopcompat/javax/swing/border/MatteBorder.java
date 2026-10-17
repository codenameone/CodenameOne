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
package com.codename1.desktopcompat.javax.swing.border;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.Icon;

/// A border filled with a solid colour or tiled with an icon.
///
/// The icon variant paints by repeating `Icon.paintIcon` across each edge,
/// clipped to it; the tiles start at the border's top left corner.
public class MatteBorder extends EmptyBorder {

    protected Color color;

    protected Icon tileIcon;

    public MatteBorder(int top, int left, int bottom, int right, Color matteColor) {
        super(top, left, bottom, right);
        this.color = matteColor;
    }

    public MatteBorder(Insets borderInsets, Color matteColor) {
        super(borderInsets);
        this.color = matteColor;
    }

    public MatteBorder(int top, int left, int bottom, int right, Icon tileIcon) {
        super(top, left, bottom, right);
        this.tileIcon = tileIcon;
    }

    public MatteBorder(Insets borderInsets, Icon tileIcon) {
        super(borderInsets);
        this.tileIcon = tileIcon;
    }

    public MatteBorder(Icon tileIcon) {
        this(-1, -1, -1, -1, tileIcon);
    }

    /// The insets in force: those given, or the icon's size on every side
    /// for a border built from an icon alone.
    private Insets current() {
        if (tileIcon != null && top == -1 && bottom == -1 && left == -1 && right == -1) {
            int w = tileIcon.getIconWidth();
            int h = tileIcon.getIconHeight();
            return new Insets(h, w, h, w);
        }
        return new Insets(top, left, bottom, right);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets in = current();
        if (color != null) {
            Color old = g.getColor();
            g.setColor(color);
            g.fillRect(x, y, width - in.right, in.top);
            g.fillRect(x, y + in.top, in.left, height - in.top);
            g.fillRect(x + in.left, y + height - in.bottom, width - in.left, in.bottom);
            g.fillRect(x + width - in.right, y, in.right, height - in.bottom);
            g.setColor(old);
        } else if (tileIcon != null) {
            tile(c, g, x, y, x, y, width - in.right, in.top);
            tile(c, g, x, y, x, y + in.top, in.left, height - in.top);
            tile(c, g, x, y, x + in.left, y + height - in.bottom, width - in.left, in.bottom);
            tile(c, g, x, y, x + width - in.right, y, in.right, height - in.bottom);
        }
    }

    /// Paints the tiles that touch one edge rectangle, on the grid that
    /// starts at the border's origin.
    private void tile(Component c, Graphics g, int x, int y, int rx, int ry, int rw, int rh) {
        int tw = tileIcon.getIconWidth();
        int th = tileIcon.getIconHeight();
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return;
        }
        Graphics clipped = g.create();
        clipped.clipRect(rx, ry, rw, rh);
        int firstX = x + ((rx - x) / tw) * tw;
        int firstY = y + ((ry - y) / th) * th;
        for (int ty = firstY; ty < ry + rh; ty += th) {
            for (int tx = firstX; tx < rx + rw; tx += tw) {
                tileIcon.paintIcon(c, clipped, tx, ty);
            }
        }
        clipped.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets in = current();
        insets.set(in.top, in.left, in.bottom, in.right);
        return insets;
    }

    @Override
    public Insets getBorderInsets() {
        return current();
    }

    public Color getMatteColor() {
        return color;
    }

    public Icon getTileIcon() {
        return tileIcon;
    }

    @Override
    public boolean isBorderOpaque() {
        return color != null;
    }
}
