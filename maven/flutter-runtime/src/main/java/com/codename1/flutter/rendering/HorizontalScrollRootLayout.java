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
import com.codename1.ui.plaf.Style;

/**
 * {@link ScrollRootLayout}'s horizontal twin: the content is laid out with a
 * tight viewport HEIGHT and an unbounded horizontal main axis, and the content
 * extent is reported as the preferred size so CN1's tensile scrolling takes
 * over once the content is wider than the viewport.
 *
 * <p>Used by horizontally scrolling Flutter boundaries — {@code PageView}, and
 * {@code ListView(scrollDirection: Axis.horizontal)}.</p>
 */
public class HorizontalScrollRootLayout extends FlutterRootLayout {

    /**
     * The content height used by the last real layout pass. As in the vertical
     * case, getPreferredSize must measure at the SAME extent layoutContainer
     * used, or the two passes alternate and the UI bounces.
     */
    private int lastLayoutHeight = -1;

    public HorizontalScrollRootLayout(RenderHost host) {
        super(host);
    }

    @Override
    public void layoutContainer(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return;
        }
        Style s = parent.getStyle();
        int height = parent.getHeight() - s.getVerticalPadding();
        if (height < 0) {
            height = 0;
        }
        lastLayoutHeight = height;
        root.layout(contentConstraints(height));
        root.position(s.getPaddingLeftNoRTL(), s.getPaddingTop());
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        RenderElement root = host().rootRenderElement();
        if (root == null) {
            return new Dimension(0, 0);
        }
        Style s = parent.getStyle();
        int height = lastLayoutHeight > 0 ? lastLayoutHeight : parent.getHeight() - s.getVerticalPadding();
        Size sz = root.layout(height > 0
                ? contentConstraints(height)
                : BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        int w = (int) Math.ceil(sz.width()) + s.getHorizontalPadding();
        int h = (int) Math.ceil(sz.height()) + s.getVerticalPadding();
        return new Dimension(w, h);
    }

    /**
     * Tight viewport height, unbounded width — the Flutter viewport contract
     * for a horizontal scrollable.
     */
    public static BoxConstraints contentConstraints(double height) {
        return new BoxConstraints(0, Double.POSITIVE_INFINITY, height, height);
    }
}
