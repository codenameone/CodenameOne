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
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code PageView.padEnds}'s centring gap is {@code SliverFillViewport}'s own
 * main-axis padding ({@code sliver_fill.dart}'s {@code _SliverFractionalPadding},
 * sized {@code clampDouble(1 - viewportFraction, 0, 1) / 2}) -- a sliver property
 * that applies identically whichever axis the sliver scrolls.
 *
 * <p>{@link PageViewRenderElement#buildContent} used to insert the leading/trailing
 * {@code PageGap} only inside its {@code horizontal()} branch, so a VERTICAL
 * {@link PageView} with a {@code viewportFraction} below 1 opened with page 0 flush
 * against the top instead of centred with its neighbour peeking in, and there was no
 * {@code padEnds} setter at all to opt out of the gap on either axis.</p>
 */
class PageViewPadEndsTest {

    private static PageView pageView(Axis axis, double viewportFraction, Boolean padEnds, int pages) {
        DartList<Widget> children = new DartList<Widget>();
        for (int i = 0; i < pages; i++) {
            children.add(new ProbeBox(10, 10));
        }
        PageController c = new PageController();
        c.viewportFraction(viewportFraction);
        PageView v = new PageView();
        v.controller(c);
        v.scrollDirection(axis);
        v.children(children);
        if (padEnds != null) {
            v.padEnds(padEnds.booleanValue());
        }
        return v;
    }

    private static RenderElement mountAndLayout(Widget root, double w, double h) {
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(w, h));
        return r;
    }

    private static List<RenderElement> columnChildren(RenderElement pageView) {
        RenderElement content = ((ScrollRenderElement) pageView).contentRender();
        return content.renderChildren();
    }

    @Test
    void verticalPageViewGetsLeadingAndTrailingGapsWhenFractionBelowOne() {
        RenderElement r = mountAndLayout(pageView(Axis.vertical, 0.8, null, 3), 500, 200);
        List<RenderElement> kids = columnChildren(r);

        assertEquals(5, kids.size(), "two PageGaps bracket the three pages");
        // slack = viewportH * (1 - fraction) / 2 = 200 * 0.2 / 2 = 20
        assertEquals(20.0, kids.get(0).size().height(), 0.0001, "leading gap height");
        assertEquals(500.0, kids.get(0).size().width(), 0.0001, "gap fills the cross axis");
        assertEquals(160.0, kids.get(1).size().height(), 0.0001, "page = viewportH * fraction");
        assertEquals(20.0, kids.get(4).size().height(), 0.0001, "trailing gap height");

        // Page 0 must NOT sit flush against the leading edge: this is the exact bug --
        // without the fix the leading gap is never inserted on the vertical axis, so
        // page 0's relY is 0 instead of 20.
        assertEquals(20.0, kids.get(1).relY(), 0.0001,
                "page 0 opens inset by the leading gap, not flush at y=0");
    }

    @Test
    void horizontalPageViewStillGetsGapsAfterTheVerticalFix() {
        RenderElement r = mountAndLayout(pageView(Axis.horizontal, 0.8, null, 3), 500, 200);
        List<RenderElement> kids = columnChildren(r);

        assertEquals(5, kids.size());
        assertEquals(50.0, kids.get(0).size().width(), 0.0001, "slack = 500 * 0.2 / 2");
        assertEquals(200.0, kids.get(0).size().height(), 0.0001, "gap fills the cross axis");
        assertEquals(400.0, kids.get(1).size().width(), 0.0001, "page = viewportW * fraction");
        assertEquals(50.0, kids.get(1).relX(), 0.0001, "page 0 inset by the leading gap");
    }

    @Test
    void padEndsFalseSuppressesTheGapOnTheVerticalAxis() {
        RenderElement r = mountAndLayout(pageView(Axis.vertical, 0.8, Boolean.FALSE, 3), 500, 200);
        List<RenderElement> kids = columnChildren(r);

        assertEquals(3, kids.size(), "no gaps: padEnds: false");
        assertEquals(0.0, kids.get(0).relY(), 0.0001, "page 0 flush against the leading edge");
    }

    @Test
    void padEndsFalseSuppressesTheGapOnTheHorizontalAxisToo() {
        RenderElement r = mountAndLayout(pageView(Axis.horizontal, 0.8, Boolean.FALSE, 3), 500, 200);
        List<RenderElement> kids = columnChildren(r);

        assertEquals(3, kids.size(), "no gaps: padEnds: false");
        assertEquals(0.0, kids.get(0).relX(), 0.0001);
    }

    @Test
    void fullViewportFractionNeedsNoGapsOnEitherAxis() {
        RenderElement r = mountAndLayout(pageView(Axis.vertical, 1.0, null, 2), 500, 200);
        assertEquals(2, columnChildren(r).size(), "viewportFraction 1 has no slack to centre");
    }
}
