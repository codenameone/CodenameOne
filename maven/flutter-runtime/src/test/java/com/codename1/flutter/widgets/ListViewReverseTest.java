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

import com.codename1.flutter.Axis;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import dart.core.DartList;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flutter's {@code ListView(reverse: true)}: the list runs from end to start -- a
 * vertical list bottom to top, horizontal right to left ({@code scroll_view.dart}'s
 * {@code reverse} doc: "scrolls from top to bottom when reverse is false and from
 * bottom to top when reverse is true"). Index 0 sits at that reversed leading edge,
 * and a {@link ScrollController}'s offset 0 is defined to be there too.
 *
 * <p>{@link ListViewRenderElement} used to store {@code reverse} and ignore it
 * entirely: indices were laid out ascending from the leading edge on every path --
 * eager children/builder mode, the windowed builder path, and a controller's own
 * offset -- so a reversed list looked identical to a non-reversed one.</p>
 */
class ListViewReverseTest {

    // ------------------------------------------------------------------
    // Eager layouts (children mode, and builder mode when not windowed):
    // headless, no CN1 pane needed -- see ScrollablesTest for the same pattern.
    // ------------------------------------------------------------------

    private static RenderElement mountAndLayout(Widget root, double w, double h) {
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(w, h));
        return r;
    }

    private static RenderElement contentOf(RenderElement scroll) {
        return RenderElement.findRenderElement(((ScrollRenderElement) scroll).contentElement());
    }

    @Test
    void reverseStacksChildrenModeBottomToTopVertically() {
        ListView lv = new ListView();
        lv.reverse(true);
        lv.children(DartList.of((Widget) new ProbeBox(10, 10), new ProbeBox(10, 20), new ProbeBox(10, 30)));

        RenderElement scroll = mountAndLayout(lv, 100, 200);
        List<RenderElement> items = contentOf(scroll).renderChildren();

        assertEquals(3, items.size());
        // Physically top-to-bottom: index 2 (h30), index 1 (h20), index 0 (h10) --
        // index 0 ends up LAST, at the bottom, which is the reversed leading edge.
        assertEquals(30.0, items.get(0).size().height(), 0.0001);
        assertEquals(20.0, items.get(1).size().height(), 0.0001);
        assertEquals(10.0, items.get(2).size().height(), 0.0001);
        assertEquals(0.0, items.get(0).relY(), 0.0001);
        assertEquals(30.0, items.get(1).relY(), 0.0001);
        assertEquals(50.0, items.get(2).relY(), 0.0001);
    }

    @Test
    void mirrorIfReverseIsIdentityUnreversedAndMirrorsAroundTheMaxWhenReversed() {
        assertEquals(40.0, ListViewRenderElement.mirrorIfReverse(40, 100, false), 0.0001);
        assertEquals(60.0, ListViewRenderElement.mirrorIfReverse(40, 100, true), 0.0001);
        assertEquals(0.0, ListViewRenderElement.mirrorIfReverse(100, 100, true), 0.0001,
                "px at the max mirrors to logical 0 -- the reversed leading edge");
        assertEquals(100.0, ListViewRenderElement.mirrorIfReverse(0, 100, true), 0.0001,
                "physical 0 mirrors to the far logical end");
        assertEquals(0.0, ListViewRenderElement.mirrorIfReverse(150, 100, true), 0.0001,
                "never negative, even past the max");
    }

    @Test
    void nonReverseChildrenModeIsUnaffectedAndStaysAscending() {
        ListView lv = new ListView();
        lv.children(DartList.of((Widget) new ProbeBox(10, 10), new ProbeBox(10, 20), new ProbeBox(10, 30)));

        RenderElement scroll = mountAndLayout(lv, 100, 200);
        List<RenderElement> items = contentOf(scroll).renderChildren();

        assertEquals(3, items.size());
        assertEquals(10.0, items.get(0).size().height(), 0.0001);
        assertEquals(20.0, items.get(1).size().height(), 0.0001);
        assertEquals(30.0, items.get(2).size().height(), 0.0001);
    }

    @Test
    void reverseStacksHorizontalEagerBuilderModeRightToLeft() {
        ListView lv = ListView.builder(null, 3L,
                new Funcs.Func2<BuildContext, Long, Widget>() {
                    @Override
                    public Widget call(BuildContext c, Long i) {
                        return new ProbeBox(10 + i, 50);
                    }
                }, null, null, null, Axis.horizontal, null, null, null, null, Boolean.TRUE);

        RenderElement scroll = mountAndLayout(lv, 200, 50);
        List<RenderElement> items = contentOf(scroll).renderChildren();

        assertEquals(3, items.size());
        // widths by index: 10, 11, 12 (index i -> 10+i). Reversed: index 2 (w12) first,
        // at the RIGHT-to-left reversed leading edge.
        assertEquals(12.0, items.get(0).size().width(), 0.0001);
        assertEquals(11.0, items.get(1).size().width(), 0.0001);
        assertEquals(10.0, items.get(2).size().width(), 0.0001);
        assertEquals(0.0, items.get(0).relX(), 0.0001);
        assertEquals(12.0, items.get(1).relX(), 0.0001);
        assertEquals(23.0, items.get(2).relX(), 0.0001);
    }

    // ------------------------------------------------------------------
    // Windowed (builder) layout: the spacer swap. Primed directly via reflection
    // on the private row-height bookkeeping, since real row measurement needs a
    // laid-out CN1 component (RasterDisplay) that this math does not otherwise
    // depend on -- buildContent() itself is package-visible and callable directly.
    // ------------------------------------------------------------------

    @Test
    void reverseSwapsTheWindowedSpacersAndOrdersTheWindowDescending() throws Exception {
        ListView lv = ListView.builder(null, 60L,
                new Funcs.Func2<BuildContext, Long, Widget>() {
                    @Override
                    public Widget call(BuildContext c, Long i) {
                        return new ProbeBox(10, 100 + i); // height encodes the index
                    }
                }, null, null, null, null, null, null, null, null, Boolean.TRUE);

        ListViewRenderElement e = new ListViewRenderElement(lv);
        primeWindow(e, /* winStart */ 20, /* winCount */ 24, /* rowHeight */ 20.0, /* count */ 60);

        Column col = (Column) e.buildContent();
        List<Widget> kids = col.getChildren();

        // start=20, end=44 (winStart + winCount). Below (indices 0..19) sums to
        // 20*20=400; above (indices 44..59) sums to 16*20=320. Ascending, pre-reverse,
        // the layout would be [spacer(400), item20..item43, spacer(320)]. Reversed end
        // to end: the trailing-index spacer sits PHYSICALLY FIRST, the window runs
        // descending, and the leading-index spacer sits PHYSICALLY LAST.
        assertEquals(26, kids.size(), "1 spacer + 24 items + 1 spacer");
        assertTrue(kids.get(0) instanceof SizedBox);
        assertEquals(320.0, ((SizedBox) kids.get(0)).getHeight(), 0.0001);
        for (int i = 0; i < 24; i++) {
            ProbeBox p = (ProbeBox) kids.get(1 + i);
            assertEquals(100.0 + (43 - i), p.height(), 0.0001,
                    "window items run descending end-1..start when reversed");
        }
        assertTrue(kids.get(25) instanceof SizedBox);
        assertEquals(400.0, ((SizedBox) kids.get(25)).getHeight(), 0.0001);
    }

    private static void primeWindow(ListViewRenderElement e, int winStart, int winCount,
            double rowHeight, int count) throws Exception {
        double[] heights = new double[count];
        double sum = 0;
        for (int i = 0; i < count; i++) {
            heights[i] = rowHeight;
            sum += rowHeight;
        }
        setField(e, "heights", heights);
        setField(e, "measuredSum", sum);
        setField(e, "measuredCount", count);
        setField(e, "winStart", winStart);
        setField(e, "winCount", winCount);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = ListViewRenderElement.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ------------------------------------------------------------------
    // Controller offsets and the initial scroll position: needs a real CN1 pane,
    // so RasterDisplay-backed -- same show()/paneOf() pattern as TransformHitTestTest.
    // ------------------------------------------------------------------

    private static Form show(Widget w, int width, int height) {
        Container root = FlutterUI.wrap(w);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, root);
        f.setX(0);
        f.setY(0);
        f.setWidth(width);
        f.setHeight(height);
        f.layoutContainer();
        f.getContentPane().layoutContainer();
        root.layoutContainer();
        return f;
    }

    private static Container paneOf(Form f) {
        Container root = (Container) f.getContentPane().getComponentAt(0);
        return (Container) root.getComponentAt(0);
    }

    private static DartList<Widget> rows(int count, double height) {
        DartList<Widget> children = new DartList<Widget>();
        for (int i = 0; i < count; i++) {
            children.add(new ProbeBox(10, height));
        }
        return children;
    }

    @Test
    void reversedListOpensOnIndexZeroAtTheReversedLeadingEdgeWithNoController() {
        RasterDisplay.install();
        try {
            ListView lv = new ListView();
            lv.reverse(true);
            lv.children(rows(20, 40));

            Form f = show(lv, 200, 300);
            Container pane = paneOf(f);
            pane.layoutContainer();

            int max = Math.max(0, pane.getScrollDimension().getHeight() - pane.getHeight());
            assertTrue(max > 0, "content must exceed the viewport for this to be meaningful");
            assertEquals(max, pane.getScrollY(),
                    "a reversed list opens on index 0 (the reversed leading edge), the FAR "
                    + "physical end, not CN1's own default of physical scroll 0");
        } finally {
            RasterDisplay.uninstall();
        }
    }

    @Test
    void nonReverseListStillOpensAtPhysicalZero() {
        RasterDisplay.install();
        try {
            ListView lv = new ListView();
            lv.children(rows(20, 40));

            Form f = show(lv, 200, 300);
            Container pane = paneOf(f);
            pane.layoutContainer();

            assertEquals(0, pane.getScrollY());
        } finally {
            RasterDisplay.uninstall();
        }
    }

    @Test
    void controllerOffsetZeroIsTheReversedLeadingEdge() {
        RasterDisplay.install();
        try {
            ScrollController controller = new ScrollController();
            ListView lv = new ListView();
            lv.reverse(true);
            lv.controller(controller);
            lv.children(rows(20, 40));

            Form f = show(lv, 200, 300);
            Container pane = paneOf(f);
            pane.layoutContainer();

            int max = Math.max(0, pane.getScrollDimension().getHeight() - pane.getHeight());
            assertTrue(max > 0, "content must exceed the viewport");
            assertEquals(max, pane.getScrollY(),
                    "the controller's default offset 0 is ALSO the reversed leading edge");

            controller.jumpTo(50);
            assertEquals(max - 50, pane.getScrollY(), 1,
                    "offset increasing moves away from the reversed leading edge, towards "
                    + "CN1 scroll 0 -- the mirror image of the ordinary mapping");
        } finally {
            RasterDisplay.uninstall();
        }
    }

    @Test
    void reportedScrollIsLogicalNotPhysicalWhenReversed() {
        RasterDisplay.install();
        try {
            ScrollController controller = new ScrollController();
            ListView lv = new ListView();
            lv.reverse(true);
            lv.controller(controller);
            lv.children(rows(20, 40));

            Form f = show(lv, 200, 300);
            Container pane = paneOf(f);
            pane.layoutContainer();

            int max = Math.max(0, pane.getScrollDimension().getHeight() - pane.getHeight());
            // Simulate a user drag moving the pane to physical max-60 (towards CN1
            // scroll 0, i.e. AWAY from the reversed leading edge -- a logical offset
            // increase). scrollToPosition is the pane's own public entry point and
            // fires the same scroll listener a real drag would; Component.setScrollY
            // itself is protected and not reachable from a different package.
            ((ScrollRenderElement.ScrollPane) pane).scrollToPosition(max - 60, false);

            assertEquals(60.0, controller.offset(), 1.0,
                    "the controller must see the LOGICAL offset (distance from index 0), "
                    + "not the raw physical scrollY");
        } finally {
            RasterDisplay.uninstall();
        }
    }
}
