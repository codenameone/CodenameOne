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
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager2;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.rt.CellTheme;
import java.util.HashMap;

/// The bar along the bottom of a window: a row of components, each added
/// with a [Constraint] that says whether it keeps a fixed width or shares
/// what is left over.
///
/// The bar lays its children out itself, from the left. A component
/// added with a `FIXED` constraint is as wide as the constraint's fixed
/// width, or as it prefers when that is negative; the `FILL` components
/// divide the remaining width equally. A component added without a
/// constraint is fixed at its preferred width, and one added with an
/// `Insets` object is that with those insets around it. The bar draws a
/// line along its top and a short separator before every component but
/// the first, in the theme's grid color.
///
/// ## What differs from SwingX
///
///  - There is no look and feel delegate, and the layout is not
///    replaceable: a layout manager set on the bar takes over and the
///    constraints stop meaning anything.
///  - The resize handle is recorded and never drawn; a device window is
///    not resized by a corner.
public class JXStatusBar extends JComponent {

    private static final int HPAD = 4;
    private static final int VPAD = 3;
    private static final int GAP = 9;

    private boolean resizeHandleEnabled = true;

    public JXStatusBar() {
        setLayout(new Row());
    }

    /// How a component of a status bar is sized.
    public static class Constraint {

        /// Whether a component keeps its width or takes a share of what
        /// the fixed ones leave.
        public enum ResizeBehavior {
            FILL,
            FIXED
        }

        private Insets insets;
        private ResizeBehavior resizeBehavior;
        private int fixedWidth;

        /// A fixed component at its preferred width.
        public Constraint() {
            this(ResizeBehavior.FIXED, null);
        }

        public Constraint(Insets insets) {
            this(ResizeBehavior.FIXED, insets);
        }

        /// A fixed component of the given width.
        public Constraint(int fixedWidth) {
            this(fixedWidth, null);
        }

        public Constraint(int fixedWidth, Insets insets) {
            if (fixedWidth < 0) {
                throw new IllegalArgumentException("fixedWidth must be >= 0");
            }
            this.fixedWidth = fixedWidth;
            this.insets = cn1Copy(insets);
            this.resizeBehavior = ResizeBehavior.FIXED;
        }

        public Constraint(ResizeBehavior resizeBehavior) {
            this(resizeBehavior, null);
        }

        public Constraint(ResizeBehavior resizeBehavior, Insets insets) {
            this.resizeBehavior = resizeBehavior == null ? ResizeBehavior.FIXED : resizeBehavior;
            this.insets = cn1Copy(insets);
            this.fixedWidth = -1;
        }

        /// Sets the width of a fixed component, or with a negative value
        /// its preferred width. A fill component ignores it.
        public void setFixedWidth(int width) {
            fixedWidth = width;
        }

        public ResizeBehavior getResizeBehavior() {
            return resizeBehavior;
        }

        public Insets getInsets() {
            return cn1Copy(insets);
        }

        /// The width of a fixed component; negative for its preferred
        /// width.
        public int getFixedWidth() {
            return fixedWidth;
        }

        private static Insets cn1Copy(Insets i) {
            return i == null ? new Insets(0, 0, 0, 0) : new Insets(i.top, i.left, i.bottom, i.right);
        }
    }

    /// The row layout described in the class comment.
    private static final class Row implements LayoutManager2 {

        private final HashMap<Component, Constraint> constraints = new HashMap<Component, Constraint>();

        @Override
        public void addLayoutComponent(Component comp, Object constraint) {
            if (constraint instanceof Constraint) {
                constraints.put(comp, (Constraint) constraint);
            } else if (constraint instanceof Insets) {
                constraints.put(comp, new Constraint((Insets) constraint));
            } else {
                constraints.remove(comp);
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            constraints.remove(comp);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }

        private static boolean fills(Constraint c) {
            return c != null && c.resizeBehavior == Constraint.ResizeBehavior.FILL;
        }

        /// The width a component asks for before the fill components
        /// share out the rest, without its insets.
        private static int wanted(Component comp, Constraint c) {
            if (c != null && !fills(c) && c.fixedWidth >= 0) {
                return c.fixedWidth;
            }
            return comp.getPreferredSize().width;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = 0;
            int h = 0;
            int shown = 0;
            int n = parent.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component comp = parent.getComponent(i);
                if (!comp.isVisible()) {
                    continue;
                }
                Constraint c = constraints.get(comp);
                int extraW = c == null ? 0 : c.insets.left + c.insets.right;
                int extraH = c == null ? 0 : c.insets.top + c.insets.bottom;
                w += wanted(comp, c) + extraW;
                h = Math.max(h, comp.getPreferredSize().height + extraH);
                shown++;
            }
            if (shown > 1) {
                w += GAP * (shown - 1);
            }
            return new Dimension(w + 2 * HPAD + in.left + in.right, h + 2 * VPAD + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int n = parent.getComponentCount();
            int available = parent.getWidth() - in.left - in.right - 2 * HPAD;
            int fillCount = 0;
            int shown = 0;
            for (int i = 0; i < n; i++) {
                Component comp = parent.getComponent(i);
                if (!comp.isVisible()) {
                    continue;
                }
                shown++;
                Constraint c = constraints.get(comp);
                if (c != null) {
                    available -= c.insets.left + c.insets.right;
                }
                if (fills(c)) {
                    fillCount++;
                } else {
                    available -= wanted(comp, c);
                }
            }
            if (shown > 1) {
                available -= GAP * (shown - 1);
            }
            available = Math.max(0, available);
            int share = fillCount > 0 ? available / fillCount : 0;
            int spare = fillCount > 0 ? available - share * fillCount : 0;
            int x = in.left + HPAD;
            int top = in.top + VPAD;
            int height = parent.getHeight() - in.top - in.bottom - 2 * VPAD;
            for (int i = 0; i < n; i++) {
                Component comp = parent.getComponent(i);
                if (!comp.isVisible()) {
                    continue;
                }
                Constraint c = constraints.get(comp);
                int w;
                if (fills(c)) {
                    fillCount--;
                    w = share + (fillCount == 0 ? spare : 0);
                } else {
                    w = wanted(comp, c);
                }
                int l = c == null ? 0 : c.insets.left;
                int r = c == null ? 0 : c.insets.right;
                int t = c == null ? 0 : c.insets.top;
                int b = c == null ? 0 : c.insets.bottom;
                comp.setBounds(x + l, top + t, w, Math.max(0, height - t - b));
                x += l + w + r + GAP;
            }
        }
    }

    /// Recorded only; no handle is drawn.
    public void setResizeHandleEnabled(boolean resizeHandleEnabled) {
        boolean old = this.resizeHandleEnabled;
        this.resizeHandleEnabled = resizeHandleEnabled;
        firePropertyChange("resizeHandleEnabled", old, resizeHandleEnabled);
    }

    public boolean isResizeHandleEnabled() {
        return resizeHandleEnabled;
    }

    /// Fills the background of an opaque bar, and draws the line along
    /// the top and the separators between the components.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color line = CellTheme.grid(null);
        g.setColor(line);
        g.drawLine(0, 0, getWidth() - 1, 0);
        boolean first = true;
        int n = getComponentCount();
        Object layout = getLayout();
        for (int i = 0; i < n; i++) {
            Component comp = getComponent(i);
            if (!comp.isVisible()) {
                continue;
            }
            if (!first) {
                int left = 0;
                if (layout instanceof Row) {
                    Constraint c = ((Row) layout).constraints.get(comp);
                    left = c == null ? 0 : c.insets.left;
                }
                int x = comp.getX() - left - GAP / 2 - 1;
                g.drawLine(x, VPAD + 1, x, getHeight() - VPAD - 1);
            }
            first = false;
        }
    }
}
