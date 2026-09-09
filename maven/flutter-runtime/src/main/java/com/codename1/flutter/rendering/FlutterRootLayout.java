/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.flutter.RenderElement;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/**
 * The CN1 layout installed on the single flat container hosting a Flutter
 * subtree. CN1 sees one container whose children are the subtree's leaf
 * components (Labels, buttons ...); this layout runs the Flutter constraint
 * pass on the render-element tree and writes the resulting absolute bounds
 * onto those components.
 */
public class FlutterRootLayout extends Layout {

    private final RenderHost host;

    public FlutterRootLayout(RenderHost host) {
        this.host = host;
    }

    public RenderHost host() {
        return host;
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return;
        }
        Style s = parent.getStyle();
        root.layout(constraintsFor(parent));
        root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
    }

    /**
     * The constraints this pass hands the subtree. By default the pane's own box, which
     * is right for a top-level host: CN1 owns that container's size.
     *
     * <p>A nested host (an effect's pane) overrides this, because there the Flutter pass
     * already decided the subtree's constraints and the pane's component size may not
     * reflect them yet.</p>
     */
    protected BoxConstraints constraintsFor(Container parent) {
        Style s = parent.getStyle();
        int width = parent.getLayoutWidth() - parent.getSideGap() - s.getHorizontalPadding();
        int height = parent.getLayoutHeight() - parent.getBottomGap() - s.getVerticalPadding();
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        return BoxConstraints.tight(width, height);
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host.rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        // Dry pass with loose unbounded constraints. It goes through dryLayout, which keeps
        // its own cache slot: these constraints differ from layoutContainer's tight ones, so
        // sharing one slot made the two passes evict each other on every box in the tree.
        Size sz = root.dryLayout(BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        Style s = parent.getStyle();
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    @Override
    public boolean isOverlapSupported() {
        return true;
    }
}
