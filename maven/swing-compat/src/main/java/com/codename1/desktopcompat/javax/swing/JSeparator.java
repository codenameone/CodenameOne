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

/// A dividing line, horizontal or vertical, drawn in the foreground color
/// at half strength.
public class JSeparator extends JComponent implements SwingConstants {

    private int orientation;

    public JSeparator() {
        this(HORIZONTAL);
    }

    public JSeparator(int orientation) {
        check(orientation);
        this.orientation = orientation;
        setFocusable(false);
    }

    private static void check(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
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
        check(orientation);
        int old = this.orientation;
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
        return orientation == VERTICAL ? new Dimension(5, 0) : new Dimension(0, 5);
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? super.getMinimumSize() : getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return orientation == VERTICAL ? new Dimension(5, Short.MAX_VALUE) : new Dimension(Short.MAX_VALUE, 5);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color fg = getForeground();
        if (fg == null) {
            fg = Color.GRAY;
        }
        g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 96));
        if (orientation == VERTICAL) {
            g.fillRect(getWidth() / 2, 0, 1, getHeight());
        } else {
            g.fillRect(0, getHeight() / 2, getWidth(), 1);
        }
    }
}
