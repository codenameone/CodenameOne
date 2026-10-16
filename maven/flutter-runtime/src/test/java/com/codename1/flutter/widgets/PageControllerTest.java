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

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.AnimatedBuilder;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;
import dart.runtime.Funcs;
import dart.runtime.RefLong;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PageController is a LIVE scroll model, not a stored page number.
 *
 * <p>This pins the contract the gallery's home carousel is built on. Every card is
 * wrapped in {@code AnimatedBuilder(animation: controller)} and scaled by
 * {@code controller.page - index}; when the controller never notified and always
 * reported its initial page, every card rendered at the scale it had on the first
 * frame — measured on the running app as a centred card drawn at 0.868 of its size
 * instead of 1.0, because the builder kept taking the {@code haveDimensions == false}
 * branch.</p>
 */
class PageControllerTest {

    private BuildOwner owner;
    private RenderHost host;

    private PageView carousel(PageController controller, int pages, Boolean snapping,
                              Funcs.VoidFunc1<RefLong> onPageChanged) {
        DartList<Widget> children = new DartList<Widget>();
        for (int i = 0; i < pages; i++) {
            children.add(new ProbeBox(10, 10));
        }
        PageView v = new PageView();
        v.controller(controller);
        v.children(children);
        if (snapping != null) {
            v.pageSnapping(snapping);
        }
        if (onPageChanged != null) {
            v.onPageChanged(onPageChanged);
        }
        return v;
    }

    private RenderElement mountAndLayout(Widget root, double w, double h) {
        owner = new BuildOwner();
        host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(w, h));
        return r;
    }

    @Test
    void anUnattachedControllerReportsItsInitialPage() {
        PageController c = new PageController();
        c.initialPage(2);
        assertEquals(2.0, c.page(), 0.0001);
        assertFalse(c.hasClients());
        assertFalse(c.position().haveDimensions(),
                "no view has reported a viewport yet");
    }

    @Test
    void layoutAttachesTheControllerAndPublishesDimensions() {
        PageController c = new PageController();
        c.viewportFraction(0.8);
        mountAndLayout(carousel(c, 6, null, null), 500, 200);

        assertTrue(c.hasClients(), "the mounted PageView must attach itself");
        assertTrue(c.position().haveDimensions(),
                "layout is the only chance an unscrolled PageView has to publish geometry");
        assertEquals(500, c.position().viewportDimension(), 0.0001);
        // five gaps of one page extent (0.8 * 500) between six pages
        assertEquals(5 * 400, c.position().maxScrollExtent(), 0.0001);
        assertEquals(0.0, c.page(), 0.0001);
    }

    @Test
    void scrollingMakesThePageFractional() {
        PageController c = new PageController();
        c.viewportFraction(0.8);
        mountAndLayout(carousel(c, 6, null, null), 500, 200);

        // half a page extent along: exactly the state a mid-drag frame is in
        c.applyMetrics(200, 400, 500, 0, 2000);
        assertEquals(0.5, c.page(), 0.0001);

        c.applyMetrics(400, 400, 500, 0, 2000);
        assertEquals(1.0, c.page(), 0.0001);
    }

    @Test
    void listenersFireOnlyWhenThePageActuallyMoves() {
        PageController c = new PageController();
        final int[] notifications = {0};
        c.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                notifications[0]++;
            }
        });

        c.applyMetrics(0, 400, 500, 0, 2000);
        assertEquals(1, notifications[0]);

        // a scroll event on a frame where nothing moved must not dirty the builders
        c.applyMetrics(0, 400, 500, 0, 2000);
        assertEquals(1, notifications[0], "an unchanged page must not notify");

        c.applyMetrics(40, 400, 500, 0, 2000);
        assertEquals(2, notifications[0]);
    }

    @Test
    void anAnimatedBuilderRebuildsAgainstTheController() {
        final PageController c = new PageController();
        final List<Double> pagesSeen = new ArrayList<Double>();

        AnimatedBuilder b = new AnimatedBuilder();
        b.animation(c);
        b.builder(new Funcs.Func2<com.codename1.flutter.BuildContext, Widget, Widget>() {
            @Override
            public Widget call(com.codename1.flutter.BuildContext ctx, Widget child) {
                pagesSeen.add(c.page());
                return new ProbeBox(10, 10);
            }
        });

        owner = new BuildOwner();
        host = new RenderHost();
        Element root = FlutterUI.mount(b, host, owner);
        assertEquals(1, pagesSeen.size());
        assertEquals(0.0, pagesSeen.get(0), 0.0001);

        c.applyMetrics(200, 400, 500, 0, 2000);
        owner.flushSync();

        assertEquals(2, pagesSeen.size(), "a controller notification must rebuild the builder");
        assertEquals(0.5, pagesSeen.get(1), 0.0001);
        assertTrue(root.isMounted());
    }

    @Test
    void pageSnappingDefaultsToOnAndIsOptOut() {
        assertTrue(new PageView().isPageSnapping(), "Flutter's default is to snap");

        PageView off = carousel(new PageController(), 3, Boolean.FALSE, null);
        assertFalse(off.isPageSnapping(),
                "the gallery carousel passes pageSnapping: false and scrolls freely");
    }

    @Test
    void onPageChangedFiresOncePerWholePage() {
        final List<Long> reported = new ArrayList<Long>();
        PageController c = new PageController();
        c.viewportFraction(0.8);
        mountAndLayout(carousel(c, 6, null, new Funcs.VoidFunc1<RefLong>() {
            @Override
            public void call(RefLong v) {
                reported.add(v.v);
            }
        }), 500, 200);

        // page 0 is the settled page at mount; it is not a change
        assertEquals(0, reported.size());
    }

    @Test
    void anInitialPageIsNotReportedAsAChange() {
        final List<Long> reported = new ArrayList<Long>();
        PageController c = new PageController();
        c.initialPage(3);
        c.viewportFraction(0.8);
        mountAndLayout(carousel(c, 6, null, new Funcs.VoidFunc1<RefLong>() {
            @Override
            public void call(RefLong v) {
                reported.add(v.v);
            }
        }), 500, 200);

        assertEquals(0, reported.size(),
                "starting on page 3 is not a page CHANGE");
    }

    /**
     * {@code animateToPage}/{@code jumpToPage} called before any {@link PageView}
     * attaches used to complete (or return) immediately and drop the requested page
     * entirely -- the view then always opened on {@code initialPage} regardless of
     * what was asked for. Flutter caches that request ({@code _pageToUseOnStartup})
     * and applies it as the starting page the moment the view's dimensions are
     * known, same as {@code initialPage} itself.
     */
    /// Flutter allows any positive viewportFraction; above 1 a page is wider than the
    /// viewport and rests centred (offset by half the overflow). It was clamped to 1.
    @Test
    void aViewportFractionAboveOneIsKeptAndPagesRestCentred() {
        PageController c = new PageController();
        c.viewportFraction(1.5);
        mountAndLayout(carousel(c, 3, null, null), 400, 200);
        assertEquals(0.0, c.page(), 0.0001, "the view opens on page 0");
        assertEquals(100.0, c.position().pixels(), 0.0001,
                "page 0 rests centred: (1.5 - 1) * 400 / 2 past the leading edge");
    }

    @Test
    void aReplacementControllerOpensOnItsOwnPendingOrInitialPage() {
        PageController first = new PageController();
        com.codename1.flutter.Element root = FlutterUI.mount(carousel(first, 6, null, null),
                host = new RenderHost(), owner = new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(500, 200));
        assertEquals(0.0, first.page(), 0.0001);

        // A controller that cached a jump before it had a view: Flutter applies it as
        // the starting page. Swapped in, it used to inherit the old pane offset instead.
        PageController pending = new PageController();
        pending.jumpToPage(4);
        root.update(carousel(pending, 6, null, null));
        r.layout(BoxConstraints.tight(500, 200));
        assertEquals(4.0, pending.page(), 0.0001, "the pending jump must be consumed");

        PageController initial = new PageController();
        initial.initialPage(2);
        root.update(carousel(initial, 6, null, null));
        r.layout(BoxConstraints.tight(500, 200));
        assertEquals(2.0, initial.page(), 0.0001, "and a fresh one opens on its initialPage");
    }

    @Test
    void anAnimateToPageBeforeAttachmentOpensOnThatPageInstead() {
        PageController c = new PageController();
        c.animateToPage(3, null, null);
        assertFalse(c.hasClients(), "nothing to attach to yet");

        final List<Long> reported = new ArrayList<Long>();
        mountAndLayout(carousel(c, 6, null, new Funcs.VoidFunc1<RefLong>() {
            @Override
            public void call(RefLong v) {
                reported.add(v.v);
            }
        }), 500, 200);

        assertEquals(3.0, c.page(), 0.0001,
                "the page requested before attachment must be where the view opens, "
                + "not dropped in favour of initialPage");
        assertEquals(0, reported.size(), "opening on the requested page is not a page CHANGE");
    }

    @Test
    void aJumpToPageBeforeAttachmentOpensOnThatPageInstead() {
        PageController c = new PageController();
        c.jumpToPage(4);
        assertFalse(c.hasClients(), "nothing to attach to yet");

        mountAndLayout(carousel(c, 6, null, null), 500, 200);

        assertEquals(4.0, c.page(), 0.0001,
                "the page requested before attachment must be where the view opens, "
                + "not dropped in favour of initialPage");
    }
}
