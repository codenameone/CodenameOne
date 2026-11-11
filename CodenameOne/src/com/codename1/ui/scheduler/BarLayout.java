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
package com.codename1.ui.scheduler;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// The layout of the bar over a schedule or a chart: what leads on the
/// left, what trails on the right and, when there are three parts, a middle
/// that takes the room between them.
///
/// On a phone the parts are wider than the bar. A row would then lay the
/// buttons of one part over the buttons of the other, so the bar becomes
/// two rows: the trailing part moves under the rest.
///
/// A layout is asked for its preferred size before its container has a
/// width, so it cannot decide by itself which of the two it is. The owner
/// of the bar says, from its own layout, how wide the bar will be.
final class BarLayout extends Layout {
    private boolean stacked;

    /// Chooses one row or two for a bar that is about to be `width` wide.
    /// True when that changed, and the bar has to be measured again.
    boolean fit(Container bar, int width) {
        int row = 0;
        int count = bar.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = bar.getComponentAt(i);
            if (!c.isHidden()) {
                row += outerWidth(c);
            }
        }
        Style st = bar.getStyle();
        boolean now = width > 0 && row > width - st.getHorizontalPadding();
        if (now == stacked) {
            return false;
        }
        stacked = now;
        return true;
    }

    boolean isStacked() {
        return stacked;
    }

    private static int outerWidth(Component c) {
        return c.getPreferredW() + c.getStyle().getHorizontalMargins();
    }

    private static int outerHeight(Component c) {
        return c.getPreferredH() + c.getStyle().getVerticalMargins();
    }

    private static void place(Component c, int x, int y, int w, int h) {
        Style st = c.getStyle();
        c.setX(x + st.getMarginLeftNoRTL());
        c.setY(y + st.getMarginTop());
        c.setWidth(Math.max(0, w - st.getHorizontalMargins()));
        c.setHeight(Math.max(0, h - st.getVerticalMargins()));
    }

    @Override
    public void layoutContainer(Container parent) {
        Style st = parent.getStyle();
        int x = st.getPaddingLeftNoRTL();
        int y = st.getPaddingTop();
        int w = parent.getLayoutWidth() - parent.getSideGap() - st.getHorizontalPadding();
        int h = parent.getLayoutHeight() - parent.getBottomGap() - st.getVerticalPadding();
        int count = parent.getComponentCount();
        if (count == 0) {
            return;
        }
        Component lead = parent.getComponentAt(0);
        Component trail = count > 1 ? parent.getComponentAt(count - 1) : null;
        Component middle = count > 2 ? parent.getComponentAt(1) : null;
        boolean rtl = parent.isRTL();
        int rowH = h;
        int rowW = w;
        if (stacked && trail != null) {
            int trailH = Math.min(h, outerHeight(trail));
            rowH = h - trailH;
            // Centred under the row when it fits, and from the leading edge
            // when it does not: a part that scrolls starts at its start.
            int tw = Math.min(w, outerWidth(trail));
            place(trail, x + (w - tw) / 2, y + rowH, tw, trailH);
        } else if (trail != null) {
            int tw = Math.min(w, outerWidth(trail));
            place(trail, rtl ? x : x + w - tw, y, tw, h);
            rowW = w - tw;
            if (rtl) {
                x += tw;
            }
        }
        int lw = Math.min(rowW, outerWidth(lead));
        place(lead, rtl ? x + rowW - lw : x, y, lw, rowH);
        if (middle != null) {
            place(middle, rtl ? x : x + lw, y, rowW - lw, rowH);
        }
    }

    @Override
    public Dimension getPreferredSize(Container parent) {
        Style st = parent.getStyle();
        int count = parent.getComponentCount();
        int rowW = 0;
        int rowH = 0;
        int trailW = 0;
        int trailH = 0;
        for (int i = 0; i < count; i++) {
            Component c = parent.getComponentAt(i);
            if (c.isHidden()) {
                continue;
            }
            if (stacked && count > 1 && i == count - 1) {
                trailW = outerWidth(c);
                trailH = outerHeight(c);
            } else {
                rowW += outerWidth(c);
                rowH = Math.max(rowH, outerHeight(c));
            }
        }
        return new Dimension(Math.max(rowW, trailW) + st.getHorizontalPadding(),
                rowH + trailH + st.getVerticalPadding());
    }
}
