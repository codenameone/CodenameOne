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

import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.geom.Dimension;

/// The peer of a viewport: a Codename One container that scrolls the peer
/// of the view, so dragging, flinging and the mouse wheel behave as they
/// do everywhere else on the device.
///
/// The view's peer stays at the origin and Codename One's scroll position
/// moves it; the area that can be scrolled is the size of that peer.
/// Codename One hands `paint` a context already shifted by the scroll
/// position, which is undone for the viewport's own painting and applied
/// again for the children.
public class ViewportPeer extends ContainerPeer {

    /// Hears about the scroll position changing, in device pixels.
    public interface Listener {
        void scrolled(int x, int y);
    }

    private final Listener listener;
    private boolean silent;

    public ViewportPeer(Container owner, Listener l) {
        super(owner);
        listener = l;
        addScrollListener(new ScrollListener() {
            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                if (!silent) {
                    listener.scrolled(scrollX, scrollY);
                }
            }
        });
    }

    /// Scrolls without telling the listener.
    public void scrollTo(int x, int y) {
        silent = true;
        try {
            if (x != getScrollX()) {
                setScrollX(x);
            }
            if (y != getScrollY()) {
                setScrollY(y);
            }
        } finally {
            silent = false;
        }
    }

    @Override
    public Dimension getScrollDimension() {
        if (getComponentCount() > 0) {
            com.codename1.ui.Component view = getComponentAt(0);
            return new Dimension(view.getWidth(), view.getHeight());
        }
        return new Dimension(getWidth(), getHeight());
    }

    @Override
    public void paint(Graphics g) {
        boolean scrolls = isScrollable();
        int sx = scrolls ? getScrollX() : 0;
        int sy = scrolls ? getScrollY() : 0;
        g.translate(sx, sy);
        try {
            support().paintOwner(g);
        } finally {
            g.translate(-sx, -sy);
        }
    }

    @Override
    public void paintNativeChildren(Graphics g) {
        boolean scrolls = isScrollable();
        int sx = scrolls ? getScrollX() : 0;
        int sy = scrolls ? getScrollY() : 0;
        g.translate(-sx, -sy);
        try {
            super.paintNativeChildren(g);
        } finally {
            g.translate(sx, sy);
        }
    }
}
