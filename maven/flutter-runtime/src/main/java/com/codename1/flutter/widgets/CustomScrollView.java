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
/// scrolls in reverse; a plain one scrolls away with a growing shrinkOffset. A
/// persistent header that follows an ordinary sliver is built at its full extent and
/// scrolls with the content: its position would need the extents of the box slivers
/// before it, which this layout does not report.
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

        @Override
        public Widget build(BuildContext context) {
            CustomScrollView w = widget();
            DartList<Widget> all = w.slivers != null ? w.slivers : new DartList<Widget>();
            java.util.List<Header> headers = new java.util.ArrayList<Header>();
            int lead = 0;
            boolean vertical = w.scrollDirection != com.codename1.flutter.Axis.horizontal;
            while (vertical && !w.reverse && lead < all.size()) {
                Header h = headerOf(all.get(lead));
                if (h == null) {
                    break;
                }
                headers.add(h);
                lead++;
            }
            if (floatShown.length != headers.size()) {
                double[] next = new double[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    next[i] = headers.get(i).delegate.maxExtent();
                }
                floatShown = next;
            }
            DartList<Widget> content = new DartList<Widget>();
            double reserved = 0;
            for (Header h : headers) {
                reserved += h.delegate.maxExtent();
            }
            if (reserved > 0) {
                SizedBox spacer = new SizedBox();
                spacer.height(reserved);
                content.add(spacer);
            }
            for (int i = lead; i < all.size(); i++) {
                content.add(all.get(i));
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
            if (headers.isEmpty()) {
                return list;
            }
            double off = offset();
            DartList<Widget> painted = new DartList<Widget>();
            double top = 0;
            for (int i = 0; i < headers.size(); i++) {
                Header h = headers.get(i);
                double max = h.delegate.maxExtent();
                double[] g = headerGeometry(h.delegate.minExtent(), max, top, off,
                        h.pinned, h.floating, floatShown[i]);
                top += max;
                if (g[0] <= 0) {
                    continue;
                }
                SizedBox box = new SizedBox();
                box.height(g[0]);
                ClipRect clip = new ClipRect();
                clip.child(h.delegate.build(context, g[1], g[1] > 0));
                box.child(clip);
                painted.add(box);
            }
            Column column = new Column();
            column.mainAxisSize(MainAxisSize.min);
            column.children(painted);
            Positioned pos = new Positioned();
            pos.left(0);
            pos.top(0);
            pos.right(0);
            pos.child(column);
            Stack stack = new Stack();
            stack.children(DartList.of((Widget) list, pos));
            return stack;
        }
    }
}
