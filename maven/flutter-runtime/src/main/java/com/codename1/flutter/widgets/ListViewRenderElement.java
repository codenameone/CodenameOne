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
    private boolean builtBottomSpacer;
    private Column lastColumn;

    private ScrollController controller;
    private final ScrollController.Client client = new ScrollController.Client() {
        @Override
        public void scrollToOffset(double offset) {
            requestControllerScroll(offset);
        }
    };

    /**
     * A controller offset asked for before the pane had real scroll geometry -- applied,
     * converted, the first time {@link ListPane#layoutContainer} sees a real size.
     */
    private Double pendingControllerOffsetDp;

    /// Whether a controller offset (explicit or the default {@code initialScrollOffset}
    /// of 0) has already been applied to the pane. Set the moment one is, even if the
    /// request happened to resolve to physical 0 -- that is still a choice made FOR this
    /// list, and {@link #applyInitialReverseScrollIfNeeded} must not then overwrite it
    /// with the no-controller default of opening on the reversed leading edge.
    private boolean explicitControllerScrollApplied;

    /// Whether the reverse list's own default starting position -- the leading edge of
    /// the reversed axis, where index 0 sits -- has been applied. Flutter opens a
    /// reversed list there with NO controller action at all, which is why this cannot
    /// simply be left to {@link #explicitControllerScrollApplied}.
    private boolean initialReverseScrollDone;

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

    /**
     * Flutter's {@code ListView.reverse}: the list runs from end to start -- bottom to
     * top for a vertical list, right to left for a horizontal one. Index 0 sits at that
     * reversed leading edge, and a controller's offset 0 is defined to be there too.
     */
    private boolean reverse() {
        return listView().getReverse();
    }

    /** The pane's largest physical scroll offset along its axis, or 0 before it is laid out. */
    private int maxScrollExtentPx() {
        if (pane == null) {
            return 0;
        }
        boolean h = horizontal();
        int content = h ? pane.getScrollDimension().getWidth() : pane.getScrollDimension().getHeight();
        int viewport = h ? pane.getWidth() : pane.getHeight();
        return Math.max(0, content - viewport);
    }

    /**
     * Flutter's logical scroll position (distance from index 0) and this pane's
     * physical one coincide when the list is NOT reversed. Reversed, index 0 sits at
     * the far physical end, so the physical position runs backwards from the pane's
     * maximum -- and that mirroring is its own inverse, so one helper converts both
     * ways: physical-from-logical here, and logical-from-physical in {@link
     * #reportScroll} and {@link #onScroll}.
     *
     * <p>Package-private and static so the conversion -- the whole difference between
     * a reversed list that opens on index 0 and one that opens on its last index -- can
     * be asserted directly.</p>
     */
    static double mirrorIfReverse(double px, double maxExtentPx, boolean reverse) {
        return reverse ? Math.max(0, maxExtentPx - px) : px;
    }

    /**
     * Converts a Flutter logical offset (distance from index 0, the reversed leading
     * edge included) into the physical device-pixel scroll this pane actually takes.
     */
    private int physicalScrollPx(double logicalOffsetDp) {
        return (int) Math.round(mirrorIfReverse(Dp.px(logicalOffsetDp), maxScrollExtentPx(), reverse()));
    }

    /** A controller asked to move this list to {@code offsetDp}; applied once sized. */
    private void requestControllerScroll(double offsetDp) {
        pendingControllerOffsetDp = offsetDp;
        applyPendingControllerScroll();
    }

    /** Applies a pending controller offset once the pane has real geometry to convert
     * it against; otherwise a no-op that {@link ListPane#layoutContainer} retries. */
    private void applyPendingControllerScroll() {
        if (pendingControllerOffsetDp == null || !(pane instanceof ScrollPane)) {
            return;
        }
        boolean h = horizontal();
        int dim = h ? pane.getWidth() : pane.getHeight();
        if (dim <= 0) {
            return;
        }
        double offsetDp = pendingControllerOffsetDp.doubleValue();
        pendingControllerOffsetDp = null;
        explicitControllerScrollApplied = true;
        ((ScrollPane) pane).scrollToPosition(physicalScrollPx(offsetDp), h);
    }

    /**
     * Flutter opens a reversed list on index 0 -- the reversed leading edge, which on a
     * plain vertical list is the BOTTOM -- with no controller action at all, so the pane
     * cannot simply default to physical 0 the way a non-reversed one does. Applied once,
     * the first time the pane has a real size, and only when nothing else (a controller's
     * own offset, explicit or the default zero) already placed it.
     */
    private void applyInitialReverseScrollIfNeeded() {
        if (initialReverseScrollDone || explicitControllerScrollApplied || !reverse()
                || !(pane instanceof ScrollPane)) {
            return;
        }
        boolean h = horizontal();
        int dim = h ? pane.getWidth() : pane.getHeight();
        if (dim <= 0) {
            return;
        }
        initialReverseScrollDone = true;
        int max = maxScrollExtentPx();
        if (max > 0) {
            ((ScrollPane) pane).scrollToPosition(max, h);
        }
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

    @Override
    protected com.codename1.ui.Container createPane(com.codename1.ui.layouts.Layout layout) {
        return new ListPane(layout);
    }

    /**
     * The scroll pane with one addition: once it has a real size, it retries a
     * controller offset that arrived before there was any geometry to convert it
     * against, and parks a reversed list on its default leading edge.
     */
    private final class ListPane extends ScrollPane {
        ListPane(com.codename1.ui.layouts.Layout layout) {
            super(layout);
        }

        @Override
        public void layoutContainer() {
            super.layoutContainer();
            applyPendingControllerScroll();
            applyInitialReverseScrollIfNeeded();
        }
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
        double maxExtent = Math.max(0, content - viewport);
        double logicalPx = mirrorIfReverse(scrollPx, maxExtent, reverse());
        controller.userScrolled(client, logicalPx / s, maxExtent / s, viewport / s);
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
        // The window math below is written against the LOGICAL scroll position -- the
        // distance from index 0 -- which for a reverse list runs backwards from the
        // pane's physical scrollY.
        int logicalScrollY = (int) Math.round(mirrorIfReverse(scrollY, maxScrollExtentPx(), reverse()));
        // The first row at or below the scroll position, by prefix sum.
        double y = 0;
        int first = 0;
        while (first < count && y + heightOf(first) <= logicalScrollY) {
            y += heightOf(first);
            first++;
        }
        int last = first;
        double bottom = y;
        while (last < count && bottom < logicalScrollY + viewport) {
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

    /**
     * Measures every row the last build materialized, from its laid-out component.
     *
     * <p>Reverse flips the physical order the rows were built in: the window runs
     * PHYSICALLY from {@code builtEnd-1} down to {@code builtStart}, behind the bottom
     * spacer rather than the top one, which is why {@code ci} counts down from
     * {@code builtEnd-1} instead of up from {@code builtStart}.</p>
     */
    private void recordHeights() {
        com.codename1.ui.Container rows = rowsContainer();
        if (rows == null) {
            return;
        }
        ensureHeights(listView().getItemCount());
        boolean rev = reverse();
        int offset = rev ? (builtBottomSpacer ? 1 : 0) : (builtTopSpacer ? 1 : 0);
        for (int i = builtStart; i < builtEnd && i < heights.length; i++) {
            int ci = rev ? offset + (builtEnd - 1 - i) : offset + (i - builtStart);
            if (ci < 0 || ci >= rows.getComponentCount()) {
                continue;
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
        builtBottomSpacer = estimable && end < count;
        if (builtBottomSpacer) {
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

    /**
     * Lays {@code items} out along the scroll axis, reversed end-to-end when the list
     * asks for it. Reverse flips the WHOLE physical order -- spacers included -- rather
     * than just the item indices: a windowed list's two spacers swap ends along with
     * everything between them, which is what keeps index 0 at the reversed leading edge
     * in both eager and windowed (builder) layouts.
     */
    private Widget wrap(ListView w, DartList<Widget> items) {
        DartList<Widget> ordered = w.getReverse() ? reversedCopy(items) : items;
        Widget line;
        if (horizontal()) {
            Row row = new Row();
            row.crossAxisAlignment(CrossAxisAlignment.stretch);
            row.mainAxisSize(MainAxisSize.min);
            row.children(ordered);
            line = row;
        } else {
            Column col = new Column();
            col.crossAxisAlignment(CrossAxisAlignment.stretch);
            col.mainAxisSize(MainAxisSize.min);
            col.children(ordered);
            lastColumn = col;
            line = col;
        }
        return padForScrollAxis(line, w.getPadding());
    }

    /**
     * A main-axis-reversed COPY of {@code items}. A copy, never an in-place reverse: in
     * children mode {@code items} is the widget's own children list, reused unchanged
     * across rebuilds, and reversing it in place would flip it back and forth on every
     * other build.
     */
    private static DartList<Widget> reversedCopy(DartList<Widget> items) {
        DartList<Widget> out = new DartList<Widget>();
        for (int i = items.size() - 1; i >= 0; i--) {
            out.add(items.get(i));
        }
        return out;
    }
}
