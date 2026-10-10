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

/// A bevel whose corners are softened: three pixels wide, with the corner
/// pixels left to the background.
public class SoftBevelBorder extends BevelBorder {

    public SoftBevelBorder(int bevelType) {
        super(bevelType);
    }

    public SoftBevelBorder(int bevelType, Color highlight, Color shadow) {
        super(bevelType, highlight, shadow);
    }

    public SoftBevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor,
            Color shadowOuterColor, Color shadowInnerColor) {
        super(bevelType, highlightOuterColor, highlightInnerColor, shadowOuterColor, shadowInnerColor);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color old = g.getColor();
        boolean raised = bevelType == RAISED;
        Color outerTop = raised ? getHighlightOuterColor(c) : getShadowOuterColor(c);
        Color innerTop = raised ? getHighlightInnerColor(c) : getShadowInnerColor(c);
        Color outerBottom = raised ? getShadowOuterColor(c) : getHighlightOuterColor(c);
        Color innerBottom = raised ? getShadowInnerColor(c) : getHighlightInnerColor(c);
        g.translate(x, y);
        g.setColor(outerTop);
        g.drawLine(0, 0, width - 2, 0);
        g.drawLine(0, 0, 0, height - 2);
        g.drawLine(1, 1, 1, 1);
        g.setColor(innerTop);
        g.drawLine(2, 1, width - 2, 1);
        g.drawLine(1, 2, 1, height - 2);
        g.drawLine(2, 2, 2, 2);
        g.drawLine(0, height - 1, 0, height - 2);
        g.drawLine(width - 1, 0, width - 1, 0);
        g.setColor(outerBottom);
        g.drawLine(2, height - 1, width - 1, height - 1);
        g.drawLine(width - 1, 2, width - 1, height - 1);
        g.setColor(innerBottom);
        g.drawLine(width - 2, height - 2, width - 2, height - 2);
        g.translate(-x, -y);
        g.setColor(old);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = 3;
        insets.left = 3;
        insets.bottom = 3;
        insets.right = 3;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
