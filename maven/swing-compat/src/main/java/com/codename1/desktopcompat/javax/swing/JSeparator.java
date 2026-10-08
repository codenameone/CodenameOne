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
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;

/// A dividing line, horizontal or vertical, drawn by this class one
/// logical pixel thick in a gray half way between the foreground and the
/// background. It has no Codename One widget behind it.
///
/// Not supported: there is no UI delegate, so `getUI` and `setUI` are
/// absent and the line has no highlight beside it.
public class JSeparator extends JComponent implements SwingConstants {

    private int orientation;

    public JSeparator() {
        this(HORIZONTAL);
    }

    public JSeparator(int orientation) {
        checkOrientation(orientation);
        this.orientation = orientation;
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        if (this.orientation == orientation) {
            return;
        }
        int old = this.orientation;
        checkOrientation(orientation);
        this.orientation = orientation;
        firePropertyChange("orientation", old, orientation);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return orientation == VERTICAL ? new Dimension(2, 0) : new Dimension(0, 2);
    }

    /// The colour of the line: half way from the foreground to the
    /// background, where a missing foreground is black and a missing
    /// background white.
    private Color lineColor() {
        Color fg = getForeground();
        Color bg = getBackground();
        if (fg == null) {
            fg = Color.BLACK;
        }
        if (bg == null) {
            bg = Color.WHITE;
        }
        return new Color((fg.getRed() + bg.getRed()) / 2, (fg.getGreen() + bg.getGreen()) / 2,
                (fg.getBlue() + bg.getBlue()) / 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(lineColor());
        if (orientation == VERTICAL) {
            g.fillRect(0, 0, 1, getHeight());
        } else {
            g.fillRect(0, 0, getWidth(), 1);
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",orientation=" + (orientation == HORIZONTAL ? "HORIZONTAL" : "VERTICAL");
    }
}
