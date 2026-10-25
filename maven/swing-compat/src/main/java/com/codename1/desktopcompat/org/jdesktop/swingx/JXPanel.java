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

import com.codename1.desktopcompat.java.awt.AlphaComposite;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Composite;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.Scrollable;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;

/// A panel that can be translucent, can have its background drawn by a
/// [Painter], and says how it wants to be sized inside a scroll pane.
///
/// ## Alpha
///
/// With an alpha below 1 the panel and everything in it is painted through
/// a composite of that opacity. Child widgets are drawn by Codename One,
/// which honors the opacity for what it draws through the same graphics.
///
/// ## Background painter
///
/// A painter replaces nothing: an opaque panel still fills its background
/// first, and the painter draws over that. It is given the panel's size,
/// or the size inside the border when
/// [#setPaintBorderInsets(boolean)] is off.
///
/// A painter that changes is not observed: call `repaint()` after
/// changing one.
public class JXPanel extends JPanel implements Scrollable {

    private float alpha = 1f;
    private boolean inheritAlpha = true;
    private Painter backgroundPainter;
    private boolean paintBorderInsets = true;
    private ScrollableSizeHint widthHint = ScrollableSizeHint.FIT;
    private ScrollableSizeHint heightHint = ScrollableSizeHint.FIT;

    public JXPanel() {
        super();
    }

    public JXPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    public JXPanel(LayoutManager layout) {
        super(layout);
    }

    public JXPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    // ------------------------------------------------------------ alpha

    public float getAlpha() {
        return alpha;
    }

    /// Sets the opacity, from 0 (invisible) to 1.
    public void setAlpha(float alpha) {
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("invalid alpha value " + alpha);
        }
        float old = this.alpha;
        this.alpha = alpha;
        if (old != alpha) {
            firePropertyChange("alpha", Float.valueOf(old), Float.valueOf(alpha));
            repaint();
        }
    }

    /// The opacity in use: this panel's, or the smaller of it and that of
    /// the nearest enclosing `JXPanel` when alpha is inherited.
    public float getEffectiveAlpha() {
        float a = alpha;
        if (inheritAlpha) {
            for (Container p = getParent(); p != null; p = p.getParent()) {
                if (p instanceof JXPanel) {
                    a = Math.min(((JXPanel) p).getEffectiveAlpha(), a);
                    break;
                }
            }
        }
        return a;
    }

    public boolean isInheritAlpha() {
        return inheritAlpha;
    }

    public void setInheritAlpha(boolean inheritAlpha) {
        boolean old = this.inheritAlpha;
        this.inheritAlpha = inheritAlpha;
        firePropertyChange("inheritAlpha", old, inheritAlpha);
        if (old != inheritAlpha) {
            repaint();
        }
    }

    // ------------------------------------------------------------ scrollable

    public final void setScrollableWidthHint(ScrollableSizeHint hint) {
        if (hint == null) {
            throw new NullPointerException("hint must not be null");
        }
        ScrollableSizeHint old = widthHint;
        widthHint = hint;
        if (old != hint) {
            revalidate();
            firePropertyChange("scrollableWidthHint", old, hint);
        }
    }

    public final void setScrollableHeightHint(ScrollableSizeHint hint) {
        if (hint == null) {
            throw new NullPointerException("hint must not be null");
        }
        ScrollableSizeHint old = heightHint;
        heightHint = hint;
        if (old != hint) {
            revalidate();
            firePropertyChange("scrollableHeightHint", old, hint);
        }
    }

    protected ScrollableSizeHint getScrollableWidthHint() {
        return widthHint;
    }

    protected ScrollableSizeHint getScrollableHeightHint() {
        return heightHint;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return heightHint.getTracksParentSize(this, SwingConstants.VERTICAL);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return widthHint.getTracksParentSize(this, SwingConstants.HORIZONTAL);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /// The visible length along the axis: one screenful.
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return Math.max(1, visibleRect.height);
        }
        return Math.max(1, visibleRect.width);
    }

    /// The first child's length along the axis, so that a unit is about
    /// one row of the panel; ten pixels when there is no child.
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int unit = 10;
        if (getComponentCount() > 0) {
            Component first = getComponent(0);
            int l = orientation == SwingConstants.VERTICAL ? first.getHeight() : first.getWidth();
            if (l > 0) {
                unit = l;
            }
        }
        return unit;
    }

    /// `true` is [ScrollableSizeHint#FIT], `false` is
    /// [ScrollableSizeHint#NONE].
    public void setScrollableTracksViewportHeight(boolean scrollableTracksViewportHeight) {
        setScrollableHeightHint(scrollableTracksViewportHeight ? ScrollableSizeHint.FIT : ScrollableSizeHint.NONE);
    }

    /// `true` is [ScrollableSizeHint#FIT], `false` is
    /// [ScrollableSizeHint#NONE].
    public void setScrollableTracksViewportWidth(boolean scrollableTracksViewportWidth) {
        setScrollableWidthHint(scrollableTracksViewportWidth ? ScrollableSizeHint.FIT : ScrollableSizeHint.NONE);
    }

    // ------------------------------------------------------------ painter

    /// Sets the painter of the background, or `null` for none.
    public void setBackgroundPainter(Painter p) {
        Painter old = backgroundPainter;
        backgroundPainter = p;
        firePropertyChange("backgroundPainter", old, p);
        repaint();
    }

    public Painter getBackgroundPainter() {
        return backgroundPainter;
    }

    public boolean isPaintBorderInsets() {
        return paintBorderInsets;
    }

    /// Whether the background painter draws under the border too.
    public void setPaintBorderInsets(boolean paintBorderInsets) {
        boolean old = this.paintBorderInsets;
        this.paintBorderInsets = paintBorderInsets;
        firePropertyChange("paintBorderInsets", old, paintBorderInsets);
        if (old != paintBorderInsets) {
            repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        float a = getEffectiveAlpha();
        if (a >= 1f || !(g instanceof Graphics2D)) {
            super.paint(g);
            return;
        }
        if (a <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        Composite saved = g2.getComposite();
        float base = 1f;
        if (saved instanceof AlphaComposite) {
            base = ((AlphaComposite) saved).getAlpha();
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, base * a));
        try {
            super.paint(g2);
        } finally {
            g2.setComposite(saved);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Painter p = backgroundPainter;
        if (p == null || !(g instanceof Graphics2D)) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            if (!paintBorderInsets) {
                Insets i = getInsets();
                g2.translate(i.left, i.top);
                w -= i.left + i.right;
                h -= i.top + i.bottom;
            }
            if (w > 0 && h > 0) {
                cn1Paint(p, g2, w, h);
            }
        } finally {
            g2.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private void cn1Paint(Painter p, Graphics2D g2, int w, int h) {
        p.paint(g2, this, w, h);
    }
}
