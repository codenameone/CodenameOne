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
package com.codename1.flutter.widgets;

import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Component;
import com.codename1.ui.events.ScrollListener;

import dart.core.DartList;

/**
 * Scroll boundary for {@link ListView}. In builder mode it WINDOWS the list: only the items in (and a
 * little around) the viewport are materialized, with empty {@link SizedBox} spacers standing in for
 * the off-screen items so the scroll geometry is preserved. As the user scrolls, the visible window is
 * recomputed and this element is rebuilt, so the number of live components stays roughly constant
 * regardless of {@code itemCount}. Children mode (a fixed list of children) still builds all.
 *
 * <p>Row heights are tracked per index. A row is measured once it has been built and laid out;
 * rows not yet built are estimated from the average of the measured ones. The window's first
 * index and both spacers come from prefix sums over those heights. It used to take ONE average
 * from the first window and apply it to every index, so a list whose later rows were taller or
 * shorter than the first few drifted: the window skipped real rows, showed the wrong ones for
 * the scroll position, and reported a wrong total extent.</p>
 *
 * <p>The list's {@link ScrollController}, if any, is attached for the element's lifetime: user
 * scrolls update its offset and notify its listeners, and its jumpTo/animateTo move the list.</p>
 */
public class ListViewRenderElement extends ScrollRenderElement {

    private static final int INITIAL = 24;
    private static final int BUFFER = 8;

    private Component pane;
    private int winStart;
    private int winCount = INITIAL;

    /** Measured row heights in physical px, by index; 0 means not measured yet. */
    private double[] heights = new double[0];
    private double measuredSum;
    private int measuredCount;
    /** What the last build materialized: rows [builtStart, builtEnd), after a top spacer or not. */
    private int builtStart;
    private int builtEnd;
    private boolean builtTopSpacer;
    private Column lastColumn;

    private ScrollController controller;
    private final ScrollController.Client client = new ScrollController.Client() {
        @Override
        public void scrollToOffset(double offset) {
            if (pane instanceof ScrollPane) {
                ((ScrollPane) pane).scrollToPosition((int) Math.round(Dp.px(offset)), horizontal());
            }
        }
    };

    public ListViewRenderElement(ListView widget) {
        super(widget);
    }

    private ListView listView() {
        return (ListView) widget();
    }

    @Override
    protected boolean shrinkWrap() {
        return listView().getShrinkWrap();
    }

    @Override
    protected boolean horizontal() {
        return listView().getScrollDirection() == com.codename1.flutter.Axis.horizontal;
    }

    /**
     * Windowing is vertical-only: the scroll math and the spacers are written
     * against item HEIGHTS. Horizontal lists in practice hold a handful of
     * items (a row of cards), so they are built eagerly rather than windowed.
     */
    private boolean windowed() {
        return listView().isBuilderMode() && !horizontal();
    }

    @Override
    public void mount(com.codename1.flutter.Element parent, int slot) {
        super.mount(parent, slot);
        attachController(listView().getController());
    }

    @Override
    public void update(Widget newWidget) {
        ScrollController next = ((ListView) newWidget).getController();
        if (next != controller) {
            attachController(next);
        }
        super.update(newWidget);
    }

    @Override
    public void unmount() {
        attachController(null);
        super.unmount();
    }

    private void attachController(ScrollController next) {
        if (controller != null) {
            controller.detach(client);
        }
        controller = next;
        if (controller != null) {
            controller.attach(client);
        }
    }

    @Override
    protected Component createComponent() {
        Component c = super.createComponent();
        pane = c;
        if (c != null) {
            c.addScrollListener(new ScrollListener() {
                @Override
                public void scrollChanged(int scrollX, int scrollY, int oldX, int oldY) {
                    reportScroll(horizontal() ? scrollX : scrollY);
                    onScroll(scrollY);
                }
            });
        }
        return c;
    }

    /** Tells the attached controller where the user scrolled to. */
    private void reportScroll(int scrollPx) {
        if (controller == null || pane == null) {
            return;
        }
        boolean h = horizontal();
        double scale = Dp.scale();
        double s = scale > 0 ? scale : 1;
        double content = h ? pane.getScrollDimension().getWidth() : pane.getScrollDimension().getHeight();
        double viewport = h ? pane.getWidth() : pane.getHeight();
        controller.userScrolled(scrollPx / s, Math.max(0, content - viewport) / s, viewport / s);
    }

    /** Recomputes the visible window on scroll and rebuilds when it changed. */
    private void onScroll(int scrollY) {
        if (pane == null || !windowed()) {
            return;
        }
        recordHeights();
        long count = listView().getItemCount();
        ensureHeights(count);
        int viewport = pane.getHeight();
        // The first row at or below the scroll position, by prefix sum.
        double y = 0;
        int first = 0;
        while (first < count && y + heightOf(first) <= scrollY) {
            y += heightOf(first);
            first++;
        }
        int last = first;
        double bottom = y;
        while (last < count && bottom < scrollY + viewport) {
            bottom += heightOf(last);
            last++;
        }
        int start = Math.max(0, first - BUFFER);
        int cnt = (int) Math.min(count - start, (long) (last - start) + BUFFER);
        // Throttle: the BUFFER of extra items above/below already covers small scrolls, so only
        // rebuild once the window has drifted by half the buffer. This keeps the viewport always
        // populated while avoiding a rebuild on every scroll frame (which would itself cause jank).
        boolean drifted = Math.abs(start - winStart) >= BUFFER / 2;
        boolean grew = cnt > winCount;
        if (drifted || grew) {
            winStart = start;
            winCount = Math.max(cnt, winCount);
            markNeedsBuild();
        }
    }

    private void ensureHeights(long count) {
        if (heights.length != count) {
            double[] next = new double[(int) count];
            System.arraycopy(heights, 0, next, 0, (int) Math.min(heights.length, count));
            heights = next;
            measuredSum = 0;
            measuredCount = 0;
            for (double h : heights) {
                if (h > 0) {
                    measuredSum += h;
                    measuredCount++;
                }
            }
        }
    }

    /** A row's height: measured if it has been, else the average of those that have. */
    private double heightOf(int i) {
        if (i < heights.length && heights[i] > 0) {
            return heights[i];
        }
        return measuredCount > 0 ? measuredSum / measuredCount : Dp.px(64);
    }

    /** Measures every row the last build materialized, from its laid-out component. */
    private void recordHeights() {
        com.codename1.ui.Container rows = rowsContainer();
        if (rows == null) {
            return;
        }
        ensureHeights(listView().getItemCount());
        int offset = builtTopSpacer ? 1 : 0;
        for (int i = builtStart; i < builtEnd && i < heights.length; i++) {
            int ci = offset + (i - builtStart);
            if (ci >= rows.getComponentCount()) {
                break;
            }
            Component c = rows.getComponentAt(ci);
            double h = c.getHeight() + c.getStyle().getVerticalMargins();
            if (h <= 0) {
                continue;
            }
            if (heights[i] > 0) {
                measuredSum += h - heights[i];
            } else {
                measuredSum += h;
                measuredCount++;
            }
            heights[i] = h;
        }
    }

    /** The component laying out the rows: the one rendered for the last built Column. */
    private com.codename1.ui.Container rowsContainer() {
        final com.codename1.ui.Container[] found = {null};
        final Column target = lastColumn;
        if (target == null) {
            return null;
        }
        visitChildren(new dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.Element>() {
            @Override
            public void call(com.codename1.flutter.Element e) {
                if (found[0] != null) {
                    return;
                }
                if (e.widget() == target && e instanceof com.codename1.flutter.RenderElement) {
                    Component c = ((com.codename1.flutter.RenderElement) e).component();
                    if (c instanceof com.codename1.ui.Container) {
                        found[0] = (com.codename1.ui.Container) c;
                    }
                    return;
                }
                e.visitChildren(this);
            }
        });
        return found[0];
    }

    @Override
    protected Widget buildContent() {
        ListView w = listView();
        DartList<Widget> items = new DartList<Widget>();
        if (!w.isBuilderMode()) {
            items = w.getChildren() == null ? new DartList<Widget>() : w.getChildren();
            return wrap(w, items);
        }
        if (!windowed()) {
            long n = w.getItemCount();
            for (long i = 0; i < n; i++) {
                items.add(w.getItemBuilder().call(this, i));
            }
            return wrap(w, items);
        }
        // Heights of rows built last time are known now; record them before rebuilding.
        recordHeights();
        long count = w.getItemCount();
        ensureHeights(count);
        int start = winStart;
        if (start >= count) {
            start = (int) Math.max(0, count - 1);
        }
        int end = (int) Math.min(count, start + (long) winCount);
        boolean estimable = measuredCount > 0;
        // top spacer for the items scrolled off above (only once some row height is known)
        builtTopSpacer = estimable && start > 0;
        if (builtTopSpacer) {
            double above = 0;
            for (int i = 0; i < start; i++) {
                above += heightOf(i);
            }
            items.add(spacer(above));
        }
        for (long i = start; i < end; i++) {
            items.add(w.getItemBuilder().call(this, i));
        }
        builtStart = start;
        builtEnd = end;
        // bottom spacer for the items below the window
        if (estimable && end < count) {
            double below = 0;
            for (int i = end; i < count; i++) {
                below += heightOf(i);
            }
            items.add(spacer(below));
        }
        return wrap(w, items);
    }

    private Widget spacer(double physicalHeight) {
        SizedBox s = new SizedBox();
        double scale = Dp.scale();
        s.height(scale > 0 ? physicalHeight / scale : physicalHeight);
        return s;
    }

    private Widget wrap(ListView w, DartList<Widget> items) {
        Widget line;
        if (horizontal()) {
            Row row = new Row();
            row.crossAxisAlignment(CrossAxisAlignment.stretch);
            row.mainAxisSize(MainAxisSize.min);
            row.children(items);
            line = row;
        } else {
            Column col = new Column();
            col.crossAxisAlignment(CrossAxisAlignment.stretch);
            col.mainAxisSize(MainAxisSize.min);
            col.children(items);
            lastColumn = col;
            line = col;
        }
        return padForScrollAxis(line, w.getPadding());
    }
}
