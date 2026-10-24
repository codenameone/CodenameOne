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

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.Fonts;

/// A panel with a title bar over a content container.
///
/// The bar holds an optional component on the left, the title, and an
/// optional component on the right. Its background is the theme's table
/// header color unless a title painter is set, which then draws the whole
/// bar; the title is a Codename One label, bold unless a title font is
/// set. The panel's own layout is a border layout with the bar in the
/// north and the content container in the center, and is not to be
/// replaced: add to [#getContentContainer()].
///
/// There is no look and feel delegate, so the bar has no platform
/// gradient of its own.
public class JXTitledPanel extends JXPanel {

    public static final String LEFT_DECORATION = "JXTitledPanel.leftDecoration";
    public static final String RIGHT_DECORATION = "JXTitledPanel.rightDecoration";

    private static final int PAD = 6;

    private final Bar bar = new Bar();
    private String title = "";
    private Container contentContainer;
    private JComponent leftDecoration;
    private JComponent rightDecoration;
    private Font titleFont;
    private Color titleForeground;
    private Painter titlePainter;

    public JXTitledPanel() {
        this(" ");
    }

    public JXTitledPanel(String title) {
        this(title, new JXPanel());
    }

    public JXTitledPanel(String title, Container content) {
        super(new BorderLayout());
        add(bar, BorderLayout.NORTH);
        setTitle(title);
        setContentContainer(content);
    }

    /// The title bar.
    private final class Bar extends JComponent {

        private final JLabel label = new JLabel();

        Bar() {
            label.setFont(Fonts.defaultFont().deriveFont(Font.BOLD));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            add(label);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = label.getPreferredSize();
            int w = d.width + 2 * PAD;
            int h = d.height;
            if (leftDecoration != null) {
                Dimension l = leftDecoration.getPreferredSize();
                w += l.width + PAD;
                h = Math.max(h, l.height);
            }
            if (rightDecoration != null) {
                Dimension r = rightDecoration.getPreferredSize();
                w += r.width + PAD;
                h = Math.max(h, r.height);
            }
            return new Dimension(w, Math.max(CellTheme.touch() ? 40 : 26, h + PAD));
        }

        @Override
        public void doLayout() {
            int left = PAD;
            int right = getWidth() - PAD;
            int h = getHeight();
            if (leftDecoration != null) {
                Dimension l = leftDecoration.getPreferredSize();
                int lh = Math.min(h, l.height);
                leftDecoration.setBounds(left, (h - lh) / 2, l.width, lh);
                left += l.width + PAD;
            }
            if (rightDecoration != null) {
                Dimension r = rightDecoration.getPreferredSize();
                int rh = Math.min(h, r.height);
                right -= r.width;
                rightDecoration.setBounds(right, (h - rh) / 2, r.width, rh);
                right -= PAD;
            }
            label.setBounds(left, 0, Math.max(0, right - left), h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Painter p = titlePainter;
            if (p != null && g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    paintWith(p, g2);
                } finally {
                    g2.dispose();
                }
                return;
            }
            g.setColor(CellTheme.headerBackground(null));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        @SuppressWarnings("unchecked")
        private void paintWith(Painter p, Graphics2D g2) {
            p.paint(g2, JXTitledPanel.this, getWidth(), getHeight());
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title == null ? "" : title;
        bar.label.setText(this.title);
        firePropertyChange("title", old, this.title);
        revalidate();
    }

    /// The container under the title bar; a [JXPanel] unless one was set.
    public Container getContentContainer() {
        if (contentContainer == null) {
            setContentContainer(new JXPanel());
        }
        return contentContainer;
    }

    public void setContentContainer(Container contentPanel) {
        Container old = contentContainer;
        if (old != null) {
            remove(old);
        }
        contentContainer = contentPanel;
        if (contentPanel != null) {
            add(contentPanel, BorderLayout.CENTER);
        }
        firePropertyChange("contentContainer", old, contentPanel);
        revalidate();
    }

    /// Puts a component at the right end of the title bar; `null` removes
    /// the one there.
    public void setRightDecoration(JComponent decoration) {
        JComponent old = rightDecoration;
        if (old != null) {
            bar.remove(old);
        }
        rightDecoration = decoration;
        if (decoration != null) {
            bar.add(decoration);
        }
        firePropertyChange("rightDecoration", old, decoration);
        bar.revalidate();
    }

    public JComponent getRightDecoration() {
        return rightDecoration;
    }

    /// Puts a component at the left end of the title bar, before the
    /// title; `null` removes the one there.
    public void setLeftDecoration(JComponent decoration) {
        JComponent old = leftDecoration;
        if (old != null) {
            bar.remove(old);
        }
        leftDecoration = decoration;
        if (decoration != null) {
            bar.add(decoration);
        }
        firePropertyChange("leftDecoration", old, decoration);
        bar.revalidate();
    }

    public JComponent getLeftDecoration() {
        return leftDecoration;
    }

    /// The font set for the title, or `null` for the default bold one.
    public Font getTitleFont() {
        return titleFont;
    }

    public void setTitleFont(Font titleFont) {
        Font old = this.titleFont;
        this.titleFont = titleFont;
        bar.label.setFont(titleFont != null ? titleFont : Fonts.defaultFont().deriveFont(Font.BOLD));
        firePropertyChange("titleFont", old, titleFont);
        revalidate();
    }

    /// Sets what draws the title bar's background, or `null` for the
    /// theme's color.
    public void setTitlePainter(Painter p) {
        Painter old = titlePainter;
        titlePainter = p;
        firePropertyChange("titlePainter", old, p);
        bar.repaint();
    }

    public Painter getTitlePainter() {
        return titlePainter;
    }

    /// The color set for the title, or `null` for the theme's.
    public Color getTitleForeground() {
        return titleForeground;
    }

    public void setTitleForeground(Color titleForeground) {
        Color old = this.titleForeground;
        this.titleForeground = titleForeground;
        bar.label.setForeground(titleForeground);
        firePropertyChange("titleForeground", old, titleForeground);
        bar.repaint();
    }
}
