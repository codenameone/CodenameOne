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
package com.codenameone.developerguide.desktopinterop.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/// A panel that paints itself: a disc with the first letter of a name.
// tag::desktopInteropSwingPaint[]
public class BadgePanel extends JPanel {
    private String initial = "?";

    public void setInitial(String name) {
        initial = name == null || name.length() == 0 ? "?" : name.substring(0, 1);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 120);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight()) - 16;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setColor(new Color(0x2f6fed));
        g2.fillOval(x, y, size, size);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(size / 2f));
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(initial, (getWidth() - metrics.stringWidth(initial)) / 2,
                (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
    }
}
// end::desktopInteropSwingPaint[]
