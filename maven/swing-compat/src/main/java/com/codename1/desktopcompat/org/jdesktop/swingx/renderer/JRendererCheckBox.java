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
package com.codename1.desktopcompat.org.jdesktop.swingx.renderer;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JCheckBox;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;

/// The check box the check box provider renders with. A [Painter] set on
/// it draws under the box.
public class JRendererCheckBox extends JCheckBox implements PainterAware {

    protected Painter painter;

    public JRendererCheckBox() {
        super();
    }

    @Override
    public Painter getPainter() {
        return painter;
    }

    @Override
    public void setPainter(Painter painter) {
        this.painter = painter;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (painter != null && g instanceof Graphics2D) {
            paintComponentWithPainter((Graphics2D) g);
        } else {
            super.paintComponent(g);
        }
    }

    protected void paintComponentWithPainter(Graphics2D g) {
        Color bg = getBackground();
        if (bg != null && isBackgroundSet()) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        Graphics2D scratch = (Graphics2D) g.create();
        try {
            cn1Paint(scratch);
        } finally {
            scratch.dispose();
        }
        if (bg != null) {
            super.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0));
        }
        try {
            super.paintComponent(g);
        } finally {
            if (bg != null) {
                super.setBackground(bg);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cn1Paint(Graphics2D g) {
        painter.paint(g, this, getWidth(), getHeight());
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void invalidate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void validate() {
    }

    /// Does nothing: the renderer is in no layout.
    @Override
    public void revalidate() {
    }

    /// Does nothing: the owner paints the renderer when it paints itself.
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /// Does nothing: the owner paints the renderer when it paints itself.
    @Override
    public void repaint(Rectangle r) {
    }

    /// Does nothing: the owner paints the renderer when it paints itself.
    @Override
    public void repaint() {
    }
}
