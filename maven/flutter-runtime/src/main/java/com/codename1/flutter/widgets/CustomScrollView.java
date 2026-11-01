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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Clip;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;

import dart.core.DartList;
import dart.runtime.Funcs;

/// A scroll view built from a list of slivers -- Flutter's {@code CustomScrollView}.
///
/// The slivers are laid out as boxes in a {@link ListView}. The persistent headers
/// ({@link SliverPersistentHeader}, {@link SliverAppBar}) that LEAD the sliver list are
/// handled by the scroll protocol: the view listens to its scroll offset, reserves each
/// header's maxExtent in the scrolling content, and paints the headers over it with the
/// shrinkOffset that offset gives them. A pinned header shrinks from maxExtent to
/// minExtent and stays; a floating one scrolls away and comes back as soon as the user
/// scrolls in reverse; a plain one scrolls away with a growing shrinkOffset. A header
/// that follows ordinary slivers does the same once the content scrolls it up to the
/// pinned headers above it: its top is read from the laid-out slot it reserves, so it
/// scrolls with the content until then and sticks at the offset Flutter sticks it.
public class CustomScrollView extends StatefulWidget {

    private DartList<Widget> slivers;
    private boolean shrinkWrap;
    private ScrollController controller;
    private com.codename1.flutter.Axis scrollDirection;
    private boolean reverse;

    public void slivers(DartList<Widget> v) {
        this.slivers = v;
    }

    public void controller(Object v) {
        this.controller = v instanceof ScrollController ? (ScrollController) v : null;
    }

    public void scrollDirection(Object v) {
        this.scrollDirection = v instanceof com.codename1.flutter.Axis ? (com.codename1.flutter.Axis) v : null;
    }

    public void reverse(boolean v) {
        this.reverse = v;
    }

    public void shrinkWrap(boolean v) {
        this.shrinkWrap = v;
    }

    public void physics(Object v) {
    }

    public void cacheExtent(double v) {
    }

    public void primary(Object v) {
    }

    public void clipBehavior(Clip v) {
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new ViewState();
    }

    /// A header's painted extent and the shrinkOffset it is built with, for scroll
    /// offset {@code offset} when the header's top in the content is {@code top}.
    /// {@code floatShown} is how much of a floating header the reverse scrolling has
    /// brought back. Returns {extent, shrinkOffset}.
    static double[] headerGeometry(double min, double max, double top, double offset,
                                   boolean pinned, boolean floating, double floatShown) {
        double scrolled = Math.max(0, offset - top);
        double extent = max - scrolled;
        if (floating) {
            extent = Math.max(extent, Math.min(max, floatShown));
        }
        if (pinned) {
            extent = Math.max(extent, min);
        }
        extent = Math.max(0, Math.min(max, extent));
        return new double[] {extent, max - extent};
    }

    /// One leading persistent header, as the scroll protocol sees it.
    static final class Header {
        final SliverPersistentHeaderDelegate delegate;
        final boolean pinned;
        final boolean floating;

        Header(SliverPersistentHeaderDelegate delegate, boolean pinned, boolean floating) {
            this.delegate = delegate;
            this.pinned = pinned;
            this.floating = floating;
        }
    }

    static Header headerOf(Widget w) {
        if (w instanceof SliverPersistentHeader) {
            SliverPersistentHeader h = (SliverPersistentHeader) w;
            return h.getDelegate() == null ? null : new Header(h.getDelegate(), h.isPinned(), h.isFloating());
        }
        if (w instanceof SliverAppBar) {
            SliverAppBar a = (SliverAppBar) w;
            return new Header(a.headerDelegate(), a.isPinned(), a.isFloating());
        }
        return null;
    }

    static final class ViewState extends State<CustomScrollView> {
        private ScrollController own;
        private ScrollController listening;
        private double lastOffset;
        /// Per leading header: how much of a floating header is shown.
        private double[] floatShown = new double[0];
        private final Funcs.VoidFunc0 onScroll = new Funcs.VoidFunc0() {
            @Override
            public void call() {
                scrolled();
            }
        };

        ScrollController controller() {
            CustomScrollView w = widget();
            if (w.controller != null) {
                return w.controller;
            }
            if (own == null) {
                own = new ScrollController();
            }
            return own;
        }

        private void listen() {
            ScrollController c = controller();
            if (c != listening) {
                if (listening != null) {
                    listening.removeListener(onScroll);
                }
                c.addListener(onScroll);
                listening = c;
            }
        }

        @Override
        public void initState() {
            listen();
        }

        @Override
        public void didUpdateWidget(CustomScrollView oldWidget) {
            listen();
        }

        @Override
        public void dispose() {
            if (listening != null) {
                listening.removeListener(onScroll);
            }
            if (own != null) {
                own.dispose();
            }
        }

        private double offset() {
            try {
                return controller().offset();
            } catch (RuntimeException e) {
                return lastOffset;
            }
        }

        void scrolled() {
            final double now = offset();
            final double delta = now - lastOffset;
            if (delta == 0) {
                return;
            }
            setState(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    for (int i = 0; i < floatShown.length; i++) {
                        floatShown[i] = Math.max(0, floatShown[i] - delta);
                    }
                    lastOffset = now;
                }
            });
        }

        /// Per header: the context of its slot in the scrolling content, from which the
        /// header's top in the content is read once it has been laid out.
        private BuildContext[] slots = new BuildContext[0];

        /// The top of {@code slot} in the scrolling content, or -1 while it has not been
        /// laid out: its y summed up to the component that scrolls.
        static double contentTop(BuildContext slot) {
            if (!(slot instanceof com.codename1.flutter.Element)) {
                return -1;
            }
            com.codename1.flutter.RenderElement r =
                    com.codename1.flutter.RenderElement.findRenderElement((com.codename1.flutter.Element) slot);
            if (r == null) {
                return -1;
            }
            com.codename1.ui.Component c = r.component();
            if (c == null || c.getParent() == null) {
                // Not on a Codename One component (yet): the render tree's own layout,
                // the slot's offset summed up to the list.
                double y = r.relY();
                for (com.codename1.flutter.Element e = r.parent(); e != null; e = e.parent()) {
                    if (e instanceof ListViewRenderElement) {
                        return y;
                    }
                    if (e instanceof com.codename1.flutter.RenderElement) {
                        y += ((com.codename1.flutter.RenderElement) e).relY();
                    }
                }
                return -1;
            }
            double y = 0;
            while (c != null && c.getParent() != null) {
                y += c.getY();
                if (c.getParent().isScrollableY()) {
                    return y;
                }
                c = c.getParent();
            }
            return -1;
        }

        /// The headers, lead count and geometry the last build used, so a layout pass
        /// can tell whether the slot positions it just computed disagree with them.
        private java.util.List<Header> lastHeaders = new java.util.ArrayList<Header>();
        private int lastLead;
        private double[][] lastGeometry;

        /// Per header: {extent, shrinkOffset, paint y, stuck (1/0)}. A header that
        /// follows ordinary slivers has its top read from its slot: from the render
        /// tree's layout when {@code fromLayout} (the pass that just ran), otherwise
        /// from the components.
        private double[][] geometry(java.util.List<Header> headers, int lead, boolean fromLayout) {
            double off = offset();
            int n = headers.size();
            double[] extent = new double[n];
            double[] shrink = new double[n];
            double[] paintY = new double[n];
            double[] stuck = new double[n];
            double pinnedAbove = 0;
            double leadTop = 0;
            for (int i = 0; i < n; i++) {
                Header h = headers.get(i);
                double max = h.delegate.maxExtent();
                double top;
                if (i < lead) {
                    top = leadTop;
                    leadTop += max;
                } else {
                    top = fromLayout ? renderTop(slots[i]) : contentTop(slots[i]);
                }
                if (top < 0 || top - off >= pinnedAbove) {
                    extent[i] = max;
                    continue;
                }
                double[] g = headerGeometry(h.delegate.minExtent(), max, top - pinnedAbove, off,
                        h.pinned, h.floating, floatShown[i]);
                extent[i] = g[0];
                shrink[i] = g[1];
                paintY[i] = pinnedAbove;
                stuck[i] = 1;
                if (h.pinned || h.floating) {
                    pinnedAbove += g[0];
                }
            }
            return new double[][] {extent, shrink, paintY, stuck};
        }

        /// Whether the layout that just ran puts a header that follows ordinary slivers
        /// somewhere other than the last build assumed -- on the first frame the build
        /// could not know any such slot's position.
        boolean layoutDisagrees() {
            if (lastGeometry == null || lastLead >= lastHeaders.size()
                    || slots.length != lastHeaders.size()) {
                return false;
            }
            double[][] now = geometry(lastHeaders, lastLead, true);
            for (int k = 0; k < now.length; k++) {
                if (!java.util.Arrays.equals(now[k], lastGeometry[k])) {
                    return true;
                }
            }
            return false;
        }

        /// The top of {@code slot} in the list's content from the render tree alone,
        /// valid as soon as the list has been laid out, or -1.
        static double renderTop(BuildContext slot) {
            if (!(slot instanceof com.codename1.flutter.Element)) {
                return -1;
            }
            com.codename1.flutter.RenderElement r =
                    com.codename1.flutter.RenderElement.findRenderElement((com.codename1.flutter.Element) slot);
            if (r == null) {
                return -1;
            }
            double y = r.relY();
            for (com.codename1.flutter.Element e = r.parent(); e != null; e = e.parent()) {
                if (e instanceof ListViewRenderElement) {
                    return y;
                }
                if (e instanceof com.codename1.flutter.RenderElement) {
                    y += ((com.codename1.flutter.RenderElement) e).relY();
                }
            }
            return -1;
        }

        @Override
        public Widget build(final BuildContext context) {
            final CustomScrollView w = widget();
            final Widget body = buildBody(context);
            boolean trailing = false;
            if (w.slivers != null && w.scrollDirection != com.codename1.flutter.Axis.horizontal && !w.reverse) {
                boolean ordinary = false;
                for (int i = 0; i < w.slivers.size(); i++) {
                    if (headerOf(w.slivers.get(i)) == null) {
                        ordinary = true;
                    } else if (ordinary) {
                        trailing = true;
                    }
                }
            }
            if (!trailing) {
                return body;
            }
            return new SlotPass(body, new Funcs.Func0<Widget>() {
                @Override
                public Widget call() {
                    return layoutDisagrees() ? buildBody(context) : null;
                }
            });
        }

        Widget buildBody(final BuildContext context) {
            CustomScrollView w = widget();
            DartList<Widget> all = w.slivers != null ? w.slivers : new DartList<Widget>();
            boolean vertical = w.scrollDirection != com.codename1.flutter.Axis.horizontal;
            final java.util.List<Header> headers = new java.util.ArrayList<Header>();
            // index into all -> header index, -1 for an ordinary sliver
            int[] headerAt = new int[all.size()];
            // How many headers lead the list: their tops are known without layout.
            int lead = -1;
            for (int i = 0; i < all.size(); i++) {
                Header h = vertical && !w.reverse ? headerOf(all.get(i)) : null;
                if (h == null) {
                    headerAt[i] = -1;
                    if (lead < 0) {
                        lead = headers.size();
                    }
                } else {
                    headerAt[i] = headers.size();
                    headers.add(h);
                }
            }
            if (lead < 0) {
                lead = headers.size();
            }
            if (floatShown.length != headers.size()) {
                double[] next = new double[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    next[i] = headers.get(i).delegate.maxExtent();
                }
                floatShown = next;
                slots = new BuildContext[headers.size()];
            }
            // Where each header's top is in the content, and whether it is stuck: scrolled
            // to (or past) the bottom of the pinned headers above it, so it is painted over
            // the list with the extent and shrinkOffset that give it, as Flutter's sliver
            // protocol does wherever in the list the header sits.
            double[][] geo = geometry(headers, lead, false);
            final double[] extent = geo[0];
            final double[] shrink = geo[1];
            final double[] paintY = geo[2];
            final boolean[] stuck = new boolean[headers.size()];
            for (int i = 0; i < stuck.length; i++) {
                stuck[i] = geo[3][i] != 0;
            }
            lastHeaders = headers;
            lastLead = lead;
            lastGeometry = geo;
            DartList<Widget> content = new DartList<Widget>();
            for (int i = 0; i < all.size(); i++) {
                final int hi = headerAt[i];
                if (hi < 0) {
                    content.add(all.get(i));
                    continue;
                }
                final Header h = headers.get(hi);
                // The header's slot reserves maxExtent in the content; it holds the header
                // itself until the header sticks, and is left empty after.
                Builder slot = new Builder();
                slot.builder(new Funcs.Func1<BuildContext, Widget>() {
                    @Override
                    public Widget call(BuildContext ctx) {
                        slots[hi] = ctx;
                        SizedBox box = new SizedBox();
                        box.height(h.delegate.maxExtent());
                        if (!stuck[hi]) {
                            box.child(h.delegate.build(ctx, 0.0, false));
                        }
                        return box;
                    }
                });
                content.add(slot);
            }
            ListView list = new ListView();
            list.children(content);
            list.controller(controller());
            if (w.scrollDirection != null) {
                list.scrollDirection(w.scrollDirection);
            }
            if (w.reverse) {
                list.reverse(true);
            }
            if (w.shrinkWrap) {
                list.shrinkWrap(true);
            }
            DartList<Widget> layers = new DartList<Widget>();
            layers.add(list);
            for (int i = 0; i < headers.size(); i++) {
                if (!stuck[i] || extent[i] <= 0) {
                    continue;
                }
                SizedBox box = new SizedBox();
                box.height(extent[i]);
                ClipRect clip = new ClipRect();
                clip.child(headers.get(i).delegate.build(context, shrink[i], shrink[i] > 0));
                box.child(clip);
                Positioned pos = new Positioned();
                pos.left(0);
                pos.top(paintY[i]);
                pos.right(0);
                pos.child(box);
                layers.add(pos);
            }
            if (headers.isEmpty()) {
                return list;
            }
            Stack stack = new Stack();
            stack.children(layers);
            return stack;
        }
    }

    /// Lays out the scroll view's body and, when that layout puts a header that follows
    /// ordinary slivers somewhere the body's build did not assume, rebuilds the body
    /// once and lays it out again -- in the same pass, so such a header is right on the
    /// first frame instead of after the next rebuild.
    static final class SlotPass extends Widget {
        final Widget body;
        final Funcs.Func0<Widget> rebuildIfStale;

        SlotPass(Widget body, Funcs.Func0<Widget> rebuildIfStale) {
            this.body = body;
            this.rebuildIfStale = rebuildIfStale;
        }

        @Override
        public com.codename1.flutter.Element createElement() {
            return new SlotPassElement(this);
        }
    }

    static final class SlotPassElement extends com.codename1.flutter.SingleChildRenderElement {
        /// The body rebuilt by the last layout, until the next configuration arrives.
        private Widget relaidOut;

        SlotPassElement(SlotPass w) {
            super(w);
        }

        @Override
        public void update(Widget newWidget) {
            relaidOut = null;
            super.update(newWidget);
        }

        @Override
        protected Widget childWidget() {
            return relaidOut != null ? relaidOut : ((SlotPass) widget()).body;
        }

        @Override
        protected com.codename1.flutter.rendering.Size performLayout(
                com.codename1.flutter.rendering.BoxConstraints constraints) {
            com.codename1.flutter.RenderElement child = renderChild();
            if (child == null) {
                return constraints.smallest();
            }
            com.codename1.flutter.rendering.Size cs = child.layout(constraints);
            if (!isDryPass()) {
                Widget next = ((SlotPass) widget()).rebuildIfStale.call();
                if (next != null) {
                    relaidOut = next;
                    syncChildren();
                    child = renderChild();
                    if (child == null) {
                        return constraints.smallest();
                    }
                    cs = child.layout(constraints);
                }
            }
            setChildOffset(child, 0, 0);
            return constraints.constrain(cs);
        }
    }
}
