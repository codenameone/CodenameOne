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

/// A border of one colour and a fixed thickness, with square or rounded
/// corners.
///
/// The line is drawn as filled bands, or for rounded corners as nested one
/// pixel outlines, so it is not antialiased.
public class LineBorder extends AbstractBorder {

    private static final Border BLACK_LINE = new LineBorder(Color.black, 1);
    private static final Border GRAY_LINE = new LineBorder(Color.gray, 1);

    protected int thickness;

    protected Color lineColor;

    protected boolean roundedCorners;

    public static Border createBlackLineBorder() {
        return BLACK_LINE;
    }

    public static Border createGrayLineBorder() {
        return GRAY_LINE;
    }

    public LineBorder(Color color) {
        this(color, 1, false);
    }

    public LineBorder(Color color, int thickness) {
        this(color, thickness, false);
    }

    public LineBorder(Color color, int thickness, boolean roundedCorners) {
        lineColor = color;
        this.thickness = thickness;
        this.roundedCorners = roundedCorners;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (thickness <= 0 || lineColor == null) {
            return;
        }
        Color old = g.getColor();
        g.setColor(lineColor);
        if (roundedCorners) {
            for (int i = 0; i < thickness; i++) {
                int arc = (thickness - i) * 2;
                g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, arc, arc);
            }
        } else if (thickness * 2 >= Math.min(width, height)) {
            g.fillRect(x, y, width, height);
        } else {
            g.fillRect(x, y, width, thickness);
            g.fillRect(x, y + height - thickness, width, thickness);
            g.fillRect(x, y + thickness, thickness, height - thickness * 2);
            g.fillRect(x + width - thickness, y + thickness, thickness, height - thickness * 2);
        }
        g.setColor(old);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, thickness, thickness, thickness);
        return insets;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public int getThickness() {
        return thickness;
    }

    public boolean getRoundedCorners() {
        return roundedCorners;
    }

    @Override
    public boolean isBorderOpaque() {
        return !roundedCorners;
    }
}
