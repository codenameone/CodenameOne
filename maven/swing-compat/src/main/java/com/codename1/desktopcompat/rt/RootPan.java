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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.javax.swing.JRootPane;
import com.codename1.desktopcompat.javax.swing.RootPaneContainer;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

/// What holds the peer of a window that fills the display, and lets the
/// user pan it when the display is smaller than the window can get.
///
/// A desktop window that is made smaller than the minimum size of its
/// content cuts the content off, and the user makes it larger again. A
/// display cannot be made larger, so here the window is never given less
/// than that minimum: in an axis in which the display is smaller the
/// window keeps its minimum size and this container scrolls, by dragging,
/// flinging and the wheel as everything else on the device does. In an
/// axis in which the content fits, which is every axis of a layout that
/// can shrink, the window has exactly the size of the display and nothing
/// scrolls.
///
/// The minimum is the one the root pane's layout answers, that is the
/// content's and the menu bar's. A minimum size set on the window itself
/// is a wish about a desktop window and is not read.
///
/// One answer of the desktop's is not taken as it is. A component with
/// neither a layout nor a minimum size of its own answers the size it
/// has at the moment, whatever that is; a window that was laid out once
/// at the size the application asked for would then never be smaller
/// again. While the least size is asked for, see [#measuring()], such a
/// component answers what it needs instead: nothing when it has no
/// children, and the extent of its children when it places them itself.
public final class RootPan extends Container {

    private static boolean measuring;

    private final Window window;

    public RootPan(Window w, Component peer) {
        super(new Fit());
        window = w;
        setScrollableX(false);
        setScrollableY(false);
        getAllStyles().setPadding(0, 0, 0, 0);
        getAllStyles().setMargin(0, 0, 0, 0);
        getAllStyles().setBgTransparency(0);
        add(peer);
    }

    /// Whether a least size is being asked for at the moment, which is
    /// when a component with no minimum of its own answers what it needs
    /// and not the size it happens to have.
    public static boolean measuring() {
        return measuring;
    }

    /// The width of the display in logical pixels: the widest a window
    /// is laid out without being panned, which is where a row of
    /// components that can wrap does so.
    public static int displayWidth() {
        if (!com.codename1.ui.Display.isInitialized()) {
            return Integer.MAX_VALUE;
        }
        return Units.toLogical(com.codename1.ui.Display.getInstance().getDisplayWidth());
    }

    /// The least size the window is laid out at, in device pixels: the
    /// minimum size of its content, or nothing when the window has none.
    int[] least() {
        com.codename1.desktopcompat.java.awt.Dimension d = null;
        if (window instanceof RootPaneContainer) {
            JRootPane root = ((RootPaneContainer) window).getRootPane();
            if (root != null) {
                d = minimumOf(root);
            }
        }
        if (d == null) {
            return new int[]{0, 0};
        }
        return new int[]{Units.toDevice(d.width), Units.toDevice(d.height)};
    }

    /// The minimum size of `root`, asked for with [#measuring()] on.
    private static com.codename1.desktopcompat.java.awt.Dimension minimumOf(JRootPane root) {
        boolean was = measuring;
        measuring = true;
        try {
            return root.getMinimumSize();
        } finally {
            measuring = was;
        }
    }


    @Override
    public Dimension getScrollDimension() {
        if (getComponentCount() > 0) {
            Component c = getComponentAt(0);
            return new Dimension(Math.max(getWidth(), c.getWidth()), Math.max(getHeight(), c.getHeight()));
        }
        return new Dimension(getWidth(), getHeight());
    }

    /// Gives the window's peer the size of this container or the least
    /// size of the window, whichever is larger, axis by axis.
    private static final class Fit extends Layout {

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0 || !(parent instanceof RootPan)) {
                return;
            }
            RootPan pan = (RootPan) parent;
            int[] least = pan.least();
            int w = Math.max(parent.getWidth(), least[0]);
            int h = Math.max(parent.getHeight(), least[1]);
            boolean x = w > parent.getWidth();
            boolean y = h > parent.getHeight();
            if (parent.isScrollableX() != x) {
                parent.setScrollableX(x);
            }
            if (parent.isScrollableY() != y) {
                parent.setScrollableY(y);
            }
            int sx = Math.max(0, Math.min(parent.getScrollX(), w - parent.getWidth()));
            int sy = Math.max(0, Math.min(parent.getScrollY(), h - parent.getHeight()));
            if (sx != parent.getScrollX() || sy != parent.getScrollY()) {
                pan.panTo(sx, sy);
            }
            Component c = parent.getComponentAt(0);
            c.setX(0);
            c.setY(0);
            c.setWidth(w);
            c.setHeight(h);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            if (parent.getComponentCount() == 0) {
                return new Dimension(0, 0);
            }
            return parent.getComponentAt(0).getPreferredSize();
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    /// Pans to a position in device pixels, as a drag would.
    public void panTo(int x, int y) {
        if (x != getScrollX()) {
            setScrollX(x);
        }
        if (y != getScrollY()) {
            setScrollY(y);
        }
    }
}
