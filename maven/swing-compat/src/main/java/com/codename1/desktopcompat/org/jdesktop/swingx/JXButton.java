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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.G2D;
import com.codename1.desktopcompat.rt.Peer;

/// A button whose background and content can be drawn by painters.
///
/// Without painters it is the Codename One button of [JButton]. Otherwise
/// it is drawn in two layers:
///
///  - the background: the background painter, or without one the
///    button's own background and border from the theme;
///  - the content: the foreground painter, or without one the button's
///    icon and text, centered, in the button's font and text color.
///
/// The button keeps the size of the Codename One button, and it is still
/// that button that takes the clicks.
///
/// ## What differs from SwingX
///
///  - The content drawn over a background painter is always centered,
///    with the icon before the text; the alignment and text position
///    properties apply to the unpainted button only.
///  - A painter that changes is not observed: call `repaint()`.
public class JXButton extends JButton {

    private Painter backgroundPainter;
    private Painter foregroundPainter;
    private boolean paintBorderInsets = true;

    public JXButton() {
        super();
    }

    public JXButton(String text) {
        super(text);
    }

    public JXButton(Action a) {
        super(a);
    }

    public JXButton(Icon icon) {
        super(icon);
    }

    public JXButton(String text, Icon icon) {
        super(text, icon);
    }

    public Painter getBackgroundPainter() {
        return backgroundPainter;
    }

    public void setBackgroundPainter(Painter p) {
        Painter old = backgroundPainter;
        backgroundPainter = p;
        firePropertyChange("backgroundPainter", old, p);
        repaint();
    }

    public Painter getForegroundPainter() {
        return foregroundPainter;
    }

    public void setForegroundPainter(Painter painter) {
        Painter old = foregroundPainter;
        foregroundPainter = painter;
        firePropertyChange("foregroundPainter", old, painter);
        repaint();
    }

    public boolean isPaintBorderInsets() {
        return paintBorderInsets;
    }

    /// Whether the painters draw under the border too.
    public void setPaintBorderInsets(boolean paintBorderInsets) {
        boolean old = this.paintBorderInsets;
        this.paintBorderInsets = paintBorderInsets;
        firePropertyChange("paintBorderInsets", old, paintBorderInsets);
        if (old != paintBorderInsets) {
            repaint();
        }
    }

    @SuppressWarnings("unchecked")
    private void run(Painter p, Graphics g) {
        Graphics copy = g.create();
        try {
            if (copy instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) copy;
                int w = getWidth();
                int h = getHeight();
                if (!paintBorderInsets) {
                    Insets i = getInsets();
                    g2.translate(i.left, i.top);
                    w -= i.left + i.right;
                    h -= i.top + i.bottom;
                }
                if (w > 0 && h > 0) {
                    p.paint(g2, this, w, h);
                }
            }
        } finally {
            copy.dispose();
        }
    }

    /// Draws the background and border the theme gives the button, without
    /// its icon and text.
    private void paintThemeBackground(Graphics g) {
        if (!(g instanceof G2D) || !com.codename1.ui.Display.isInitialized()) {
            return;
        }
        com.codename1.ui.Component p = cn1Peer();
        if (!(p instanceof Peer)) {
            return;
        }
        ((G2D) g).paintNative(p.getX(), p.getY(), new ThemeBackground(p, (Peer) p));
    }

    /// Paints a peer's styled background and its border.
    private static final class ThemeBackground implements G2D.NativePainter {
        private final com.codename1.ui.Component component;
        private final Peer peer;

        ThemeBackground(com.codename1.ui.Component component, Peer peer) {
            this.component = component;
            this.peer = peer;
        }

        @Override
        public void paint(com.codename1.ui.Graphics ng) {
            peer.support().paintStyleBackground(ng);
            com.codename1.ui.plaf.Border b = component.getStyle().getBorder();
            if (b != null && !b.isBackgroundPainter()) {
                b.paint(ng, component);
            }
        }
    }

    private Color textColor() {
        if (!isEnabled()) {
            return Color.GRAY;
        }
        if (!isForegroundSet() && com.codename1.ui.Display.isInitialized()) {
            return new Color(cn1Peer().getStyle().getFgColor() & 0xffffff);
        }
        return getForeground();
    }

    private void paintContent(Graphics g) {
        String text = getText();
        Icon icon = getIcon();
        Font f = getFont() != null ? getFont() : Fonts.defaultFont();
        FontMetrics fm = Fonts.metrics(f);
        boolean hasText = text != null && text.length() > 0;
        int tw = hasText ? fm.stringWidth(text) : 0;
        int iw = icon != null ? icon.getIconWidth() : 0;
        int gap = icon != null && hasText ? getIconTextGap() : 0;
        int x = (getWidth() - iw - gap - tw) / 2;
        if (icon != null) {
            icon.paintIcon(this, g, x, (getHeight() - icon.getIconHeight()) / 2);
        }
        if (hasText) {
            g.setFont(f);
            Color c = textColor();
            if (c != null) {
                g.setColor(c);
            }
            g.drawString(text, x + iw + gap, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundPainter == null && foregroundPainter == null) {
            super.paintComponent(g);
            return;
        }
        if (backgroundPainter != null) {
            run(backgroundPainter, g);
        } else {
            paintThemeBackground(g);
        }
        if (foregroundPainter != null) {
            run(foregroundPainter, g);
        } else {
            Graphics copy = g.create();
            try {
                paintContent(copy);
            } finally {
                copy.dispose();
            }
        }
    }
}
