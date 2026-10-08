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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JViewport;

/// What a component that handles its own pointer input needs from the
/// scroll pane around it.
///
/// A table or tree takes the pointer events over it, so the scroll pane
/// never sees a finger drag that starts on a row. On a touch screen such a
/// drag means "scroll", not "extend the selection": the component leaves
/// its selection alone and hands the events on with [#forward].
public final class ScrollDelegate {

    /// How thick the scroll bars of a scroll pane are on a desktop port.
    private static final int DESKTOP_BAR = 14;

    private static int forcedBar = -1;

    private ScrollDelegate() {
    }

    /// How much room a scroll bar of a scroll pane takes, in logical
    /// pixels: a bar to drag with the mouse on a desktop port, and none on
    /// a touch device, where the content itself is dragged and Codename
    /// One draws an indicator over it.
    public static int barThickness() {
        if (forcedBar >= 0) {
            return forcedBar;
        }
        return com.codename1.ui.Display.isInitialized() && com.codename1.ui.Display.getInstance().isDesktop()
                ? DESKTOP_BAR : 0;
    }

    /// Fixes the answer of [#barThickness]; a negative value lets the
    /// device decide again. For tests.
    public static void setBarThickness(int thickness) {
        forcedBar = thickness;
    }

    /// The nearest scroll pane around `c`, or `null`.
    public static JScrollPane enclosing(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof JScrollPane) {
                return (JScrollPane) p;
            }
        }
        return null;
    }

    /// Sends a copy of a mouse event of `source` to the scroll pane around
    /// it, in the pane's coordinates. Answers `false` when there is none.
    public static boolean forward(Component source, MouseEvent e) {
        JScrollPane pane = enclosing(source);
        if (pane == null) {
            return false;
        }
        int x = e.getX();
        int y = e.getY();
        for (Component c = source; c != null && c != pane; c = c.getParent()) {
            x += c.getX();
            y += c.getY();
        }
        pane.dispatchEvent(new MouseEvent(pane, e.getID(), e.getWhen(), e.getModifiers(), x, y, e.getClickCount(),
                e.isPopupTrigger(), e.getButton()));
        return true;
    }

    /// Scrolls the viewport around `c` as little as it takes to show the
    /// rectangle, given in the coordinates of `c`. Does nothing when `c`
    /// is in no viewport.
    public static void reveal(Component c, Rectangle r) {
        int x = r.x;
        int y = r.y;
        Component view = c;
        Container parent = c.getParent();
        while (parent != null && !(parent instanceof JViewport)) {
            x += view.getX();
            y += view.getY();
            view = parent;
            parent = parent.getParent();
        }
        if (parent == null) {
            return;
        }
        JViewport viewport = (JViewport) parent;
        if (!viewport.isValid()) {
            // The view may just have grown: scroll within its new size.
            viewport.validate();
        }
        Point at = viewport.getViewPosition();
        Dimension extent = viewport.getExtentSize();
        int nx = along(at.x, extent.width, x, r.width, view.getWidth());
        int ny = along(at.y, extent.height, y, r.height, view.getHeight());
        if (nx != at.x || ny != at.y) {
            viewport.setViewPosition(new Point(nx, ny));
        }
    }

    /// The scroll position along one axis that shows `[start, start +
    /// length)`: unchanged if it shows already, else moved the shorter
    /// way, and with the start winning when the span is longer than the
    /// viewport.
    private static int along(int position, int extent, int start, int length, int viewLength) {
        int p = position;
        if (start < p) {
            p = start;
        } else if (start + length > p + extent) {
            p = Math.min(start, start + length - extent);
        }
        int max = Math.max(0, viewLength - extent);
        return Math.max(0, Math.min(p, max));
    }
}
