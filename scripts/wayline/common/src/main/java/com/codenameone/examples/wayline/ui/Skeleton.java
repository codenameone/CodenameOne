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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/// What a list shows while what is in it is on its way: the outline of a few
/// rows, drawn as bars where the icon, the two lines and the value will be.
///
/// It says "this is loading" without a word and without moving, and it keeps
/// the screen from jumping when the rows arrive, because they land where the
/// outline was. The colour is the `WlSkeleton` style's.
final class Skeleton extends Component {
    /// How tall one row is drawn, in millimetres: a row of two lines.
    private static final float ROW_MM = 9.6f;

    private final int rows;

    Skeleton(int rows) {
        this.rows = rows;
        setUIID("WlSkeleton");
        setFocusable(false);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        return new Dimension(CN.convertToPixels(40f) + style.getHorizontalPadding(),
                rows * CN.convertToPixels(ROW_MM) + style.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        Style style = getStyle();
        int left = getX() + style.getPaddingLeftNoRTL();
        int width = getWidth() - style.getHorizontalPadding();
        int row = CN.convertToPixels(ROW_MM);
        int dot = CN.convertToPixels(4.4f);
        int tall = CN.convertToPixels(1.9f);
        int thin = CN.convertToPixels(1.5f);
        int gap = CN.convertToPixels(2.6f);
        if (width <= dot + gap) {
            return;
        }
        boolean rtl = isRTL();
        g.setAntiAliased(true);
        g.setColor(style.getFgColor());
        int lines = width - dot - gap;
        for (int iter = 0; iter < rows; iter++) {
            int top = getY() + style.getPaddingTop() + iter * row;
            int middle = top + row / 2;
            // Each row a little shorter than the one before: a list of
            // identical bars reads as a pattern, not as a list.
            int first = lines * (48 - iter % 3 * 7) / 100;
            int second = lines * (30 + iter % 2 * 9) / 100;
            int value = lines * 14 / 100;
            bar(g, rtl, left, width, 0, middle - dot / 2, dot, dot);
            bar(g, rtl, left, width, dot + gap, middle - tall - thin / 3, first, tall);
            bar(g, rtl, left, width, dot + gap, middle + thin * 2 / 3, second, thin);
            bar(g, rtl, left, width, width - value, middle - tall / 2, value, tall);
        }
    }

    /// One bar, `from` pixels in from the side a line of text starts at.
    private static void bar(Graphics g, boolean rtl, int left, int width, int from, int y,
            int w, int h) {
        int x = rtl ? left + width - from - w : left + from;
        g.fillRoundRect(x, y, w, h, h, h);
    }
}
