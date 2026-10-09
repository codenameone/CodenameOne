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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Rectangle;

/// The layout of a `JScrollPane`: the viewport, the two bars, the two
/// headers and the four corners.
///
/// A scroll pane has one from the start. An application replaces it with
/// a subclass to move the parts about: it overrides [#layoutContainer],
/// calls `super.layoutContainer` for the standard placement and then sets
/// the bounds of [#viewport], [#vsb], [#rowHead] and the others as it
/// likes. Those fields are the scroll pane's own parts, read from it
/// whenever one of them changes and before every layout.
///
/// A bar that is not needed is made invisible, as in Swing, so
/// `vsb.isVisible()` says whether there is one.
public class ScrollPaneLayout implements LayoutManager, ScrollPaneConstants {

    protected JViewport viewport;

    protected JScrollBar vsb;

    protected JScrollBar hsb;

    protected JViewport rowHead;

    protected JViewport colHead;

    protected Component lowerLeft;

    protected Component lowerRight;

    protected Component upperLeft;

    protected Component upperRight;

    protected int vsbPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int hsbPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    public ScrollPaneLayout() {
    }

    /// Reads the parts and the policies of `sp` into the fields.
    public void syncWithScrollPane(JScrollPane sp) {
        viewport = sp.getViewport();
        vsb = sp.getVerticalScrollBar();
        hsb = sp.getHorizontalScrollBar();
        rowHead = sp.getRowHeader();
        colHead = sp.getColumnHeader();
        lowerLeft = sp.getCorner(LOWER_LEFT_CORNER);
        lowerRight = sp.getCorner(LOWER_RIGHT_CORNER);
        upperLeft = sp.getCorner(UPPER_LEFT_CORNER);
        upperRight = sp.getCorner(UPPER_RIGHT_CORNER);
        vsbPolicy = sp.getVerticalScrollBarPolicy();
        hsbPolicy = sp.getHorizontalScrollBarPolicy();
    }

    /// Does nothing: the parts of a scroll pane are set on the scroll
    /// pane, which tells its layout.
    @Override
    public void addLayoutComponent(String s, Component c) {
    }

    @Override
    public void removeLayoutComponent(Component c) {
        if (c == viewport) {
            viewport = null;
        } else if (c == vsb) {
            vsb = null;
        } else if (c == hsb) {
            hsb = null;
        } else if (c == rowHead) {
            rowHead = null;
        } else if (c == colHead) {
            colHead = null;
        } else if (c == lowerLeft) {
            lowerLeft = null;
        } else if (c == lowerRight) {
            lowerRight = null;
        } else if (c == upperLeft) {
            upperLeft = null;
        } else if (c == upperRight) {
            upperRight = null;
        }
    }

    public int getVerticalScrollBarPolicy() {
        return vsbPolicy;
    }

    public int getHorizontalScrollBarPolicy() {
        return hsbPolicy;
    }

    public JViewport getViewport() {
        return viewport;
    }

    public JScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    public JScrollBar getVerticalScrollBar() {
        return vsb;
    }

    public JViewport getRowHeader() {
        return rowHead;
    }

    public JViewport getColumnHeader() {
        return colHead;
    }

    public Component getCorner(String key) {
        if (LOWER_LEFT_CORNER.equals(key)) {
            return lowerLeft;
        }
        if (LOWER_RIGHT_CORNER.equals(key)) {
            return lowerRight;
        }
        if (UPPER_LEFT_CORNER.equals(key)) {
            return upperLeft;
        }
        if (UPPER_RIGHT_CORNER.equals(key)) {
            return upperRight;
        }
        return null;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        if (parent instanceof JScrollPane) {
            return ((JScrollPane) parent).cn1PreferredSize();
        }
        return new Dimension(0, 0);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        if (parent instanceof JScrollPane) {
            return ((JScrollPane) parent).cn1MinimumSize();
        }
        return new Dimension(0, 0);
    }

    /// Places the parts of the scroll pane `parent`: the headers along the
    /// top and the leading side, the bars that are needed along the
    /// bottom and the trailing side, the viewport in what is left.
    @Override
    public void layoutContainer(Container parent) {
        if (parent instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) parent;
            syncWithScrollPane(sp);
            sp.cn1Layout();
        }
    }

    /// The layout a scroll pane has until the application gives it one.
    public static class UIResource extends ScrollPaneLayout
            implements com.codename1.desktopcompat.javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
