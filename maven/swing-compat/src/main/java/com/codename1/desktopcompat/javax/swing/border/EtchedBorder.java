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

/// A two pixel groove (or ridge) drawn in a highlight and a shadow colour.
///
/// Colours not given are derived from the background of the component the
/// border is painted on, or from light gray when it has none.
public class EtchedBorder extends AbstractBorder {

    public static final int RAISED = 0;

    public static final int LOWERED = 1;

    protected int etchType;

    protected Color highlight;

    protected Color shadow;

    public EtchedBorder() {
        this(LOWERED);
    }

    public EtchedBorder(int etchType) {
        this(etchType, null, null);
    }

    public EtchedBorder(Color highlight, Color shadow) {
        this(LOWERED, highlight, shadow);
    }

    public EtchedBorder(int etchType, Color highlight, Color shadow) {
        this.etchType = etchType;
        this.highlight = highlight;
        this.shadow = shadow;
    }

    /// The colour highlights and shadows are derived from.
    static Color base(Component c) {
        Color bg = c == null ? null : c.getBackground();
        return bg == null ? Color.lightGray : bg;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color old = g.getColor();
        Color light = getHighlightColor(c);
        Color dark = getShadowColor(c);
        int w = width;
        int h = height;
        g.setColor(etchType == LOWERED ? dark : light);
        g.drawRect(x, y, w - 2, h - 2);
        g.setColor(etchType == LOWERED ? light : dark);
        g.drawLine(x + 1, y + h - 3, x + 1, y + 1);
        g.drawLine(x + 1, y + 1, x + w - 3, y + 1);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y + h - 1, x + w - 1, y);
        g.setColor(old);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(2, 2, 2, 2);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    public int getEtchType() {
        return etchType;
    }

    public Color getHighlightColor(Component c) {
        return highlight != null ? highlight : base(c).brighter();
    }

    public Color getHighlightColor() {
        return highlight;
    }

    public Color getShadowColor(Component c) {
        return shadow != null ? shadow : base(c).darker();
    }

    public Color getShadowColor() {
        return shadow;
    }
}
