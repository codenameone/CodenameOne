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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;

/// Two borders nested, the inside one drawn within the insets of the
/// outside one.
public class CompoundBorder extends AbstractBorder {

    protected Border outsideBorder;

    protected Border insideBorder;

    public CompoundBorder() {
        this.outsideBorder = null;
        this.insideBorder = null;
    }

    public CompoundBorder(Border outsideBorder, Border insideBorder) {
        this.outsideBorder = outsideBorder;
        this.insideBorder = insideBorder;
    }

    @Override
    public boolean isBorderOpaque() {
        return (outsideBorder == null || outsideBorder.isBorderOpaque())
                && (insideBorder == null || insideBorder.isBorderOpaque());
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int px = x;
        int py = y;
        int pw = width;
        int ph = height;
        if (outsideBorder != null) {
            outsideBorder.paintBorder(c, g, px, py, pw, ph);
            Insets in = outsideBorder.getBorderInsets(c);
            px += in.left;
            py += in.top;
            pw = pw - in.right - in.left;
            ph = ph - in.bottom - in.top;
        }
        if (insideBorder != null) {
            insideBorder.paintBorder(c, g, px, py, pw, ph);
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = 0;
        insets.left = 0;
        insets.right = 0;
        insets.bottom = 0;
        if (outsideBorder != null) {
            Insets in = outsideBorder.getBorderInsets(c);
            insets.top += in.top;
            insets.left += in.left;
            insets.right += in.right;
            insets.bottom += in.bottom;
        }
        if (insideBorder != null) {
            Insets in = insideBorder.getBorderInsets(c);
            insets.top += in.top;
            insets.left += in.left;
            insets.right += in.right;
            insets.bottom += in.bottom;
        }
        return insets;
    }

    public Border getOutsideBorder() {
        return outsideBorder;
    }

    public Border getInsideBorder() {
        return insideBorder;
    }
}
