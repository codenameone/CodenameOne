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

/// A two pixel bevel that makes its component look raised or sunken.
///
/// Colours not given are derived from the background of the component the
/// border is painted on, or from light gray when it has none.
public class BevelBorder extends AbstractBorder {

    public static final int RAISED = 0;

    public static final int LOWERED = 1;

    protected int bevelType;

    protected Color highlightOuter;

    protected Color highlightInner;

    protected Color shadowInner;

    protected Color shadowOuter;

    public BevelBorder(int bevelType) {
        this.bevelType = bevelType;
    }

    public BevelBorder(int bevelType, Color highlight, Color shadow) {
        this(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
    }

    public BevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor, Color shadowOuterColor,
            Color shadowInnerColor) {
        this(bevelType);
        this.highlightOuter = highlightOuterColor;
        this.highlightInner = highlightInnerColor;
        this.shadowOuter = shadowOuterColor;
        this.shadowInner = shadowInnerColor;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (bevelType == RAISED) {
            paintRaisedBevel(c, g, x, y, width, height);
        } else if (bevelType == LOWERED) {
            paintLoweredBevel(c, g, x, y, width, height);
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(2, 2, 2, 2);
        return insets;
    }

    public Color getHighlightOuterColor(Component c) {
        return highlightOuter != null ? highlightOuter : EtchedBorder.highlight(c, 2);
    }

    public Color getHighlightInnerColor(Component c) {
        return highlightInner != null ? highlightInner : EtchedBorder.highlight(c, 1);
    }

    public Color getShadowInnerColor(Component c) {
        return shadowInner != null ? shadowInner : EtchedBorder.base(c).darker();
    }

    public Color getShadowOuterColor(Component c) {
        return shadowOuter != null ? shadowOuter : EtchedBorder.base(c).darker().darker();
    }

    public Color getHighlightOuterColor() {
        return highlightOuter;
    }

    public Color getHighlightInnerColor() {
        return highlightInner;
    }

    public Color getShadowInnerColor() {
        return shadowInner;
    }

    public Color getShadowOuterColor() {
        return shadowOuter;
    }

    public int getBevelType() {
        return bevelType;
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    /// Two nested L shapes on the top left and two on the bottom right.
    private static void bevel(Graphics g, int x, int y, int w, int h, Color topOuter, Color topInner,
            Color bottomOuter, Color bottomInner) {
        Color old = g.getColor();
        g.setColor(topOuter);
        g.drawLine(x, y, x, y + h - 2);
        g.drawLine(x + 1, y, x + w - 2, y);
        g.setColor(topInner);
        g.drawLine(x + 1, y + 1, x + 1, y + h - 3);
        g.drawLine(x + 2, y + 1, x + w - 3, y + 1);
        g.setColor(bottomOuter);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 2);
        g.setColor(bottomInner);
        g.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
        g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 3);
        g.setColor(old);
    }

    protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
        bevel(g, x, y, width, height, getHighlightOuterColor(c), getHighlightInnerColor(c), getShadowOuterColor(c),
                getShadowInnerColor(c));
    }

    protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
        bevel(g, x, y, width, height, getShadowInnerColor(c), getShadowOuterColor(c), getHighlightOuterColor(c),
                getHighlightInnerColor(c));
    }
}
