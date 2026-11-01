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
import com.codename1.flutter.TextScaler;
import com.codename1.flutter.TextSelection;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;
import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Scroll-driven persistent headers, the draggable sheet, text scaling, horizontal
/// grids and SelectableText's selection callback.
class ScrollDrivenWidgetsTest {

    private static final class Recording extends SliverPersistentHeaderDelegate {
        final List<Double> shrinks = new ArrayList<Double>();

        @Override
        public Widget build(BuildContext context, double shrinkOffset, boolean overlapsContent) {
            shrinks.add(shrinkOffset);
            return new SizedBox();
        }

        @Override
        public double minExtent() {
            return 40;
        }

        @Override
        public double maxExtent() {
            return 120;
        }

        @Override
        public boolean shouldRebuild(SliverPersistentHeaderDelegate oldDelegate) {
            return true;
        }
    }

    @Test
    void pinnedHeaderShrinksToItsMinimumAndStays() {
        double[] g = CustomScrollView.headerGeometry(40, 120, 0, 30, true, false, 0);
        assertEquals(90, g[0], 1e-9);
        assertEquals(30, g[1], 1e-9);
        g = CustomScrollView.headerGeometry(40, 120, 0, 500, true, false, 0);
        assertEquals(40, g[0], 1e-9);
        assertEquals(80, g[1], 1e-9);
    }

    @Test
    void plainHeaderScrollsAwayAndFloatingOneReturns() {
        double[] g = CustomScrollView.headerGeometry(40, 120, 0, 500, false, false, 0);
        assertEquals(0, g[0], 1e-9);
        assertEquals(120, g[1], 1e-9);
        g = CustomScrollView.headerGeometry(40, 120, 0, 500, false, true, 50);
        assertEquals(50, g[0], 1e-9);
        assertEquals(70, g[1], 1e-9);
    }

    @Test
    void customScrollViewHandsTheScrollOffsetToItsLeadingHeader() {
        Recording d = new Recording();
        SliverPersistentHeader h = new SliverPersistentHeader();
        h.delegate(d);
        h.pinned(true);
        ScrollController c = new ScrollController();
        CustomScrollView v = new CustomScrollView();
        v.controller(c);
        v.slivers(DartList.of((Widget) h, new ProbeBox(100, 2000)));
        RenderHost host = new RenderHost();
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(v, host, owner);
        host.rootRenderElement().layout(BoxConstraints.tight(300, 400));
        assertEquals(0.0, d.shrinks.get(d.shrinks.size() - 1), 1e-9);
        // What the list reports when the user scrolls it (headless, the pane has no
        // geometry for a controller jump to land on).
        c.userScrolled(60, 2000, 400);
        owner.flushSync();
        assertEquals(60.0, d.shrinks.get(d.shrinks.size() - 1), 1e-9);
        c.userScrolled(300, 2000, 400);
        owner.flushSync();
        assertEquals(80.0, d.shrinks.get(d.shrinks.size() - 1), 1e-9, "pinned at minExtent");
    }

    @Test
    void sheetSnapsToTheNearestStopOrTheNextOneAFlingReaches() {
        double[] stops = {0.25, 0.5, 1.0};
        assertEquals(0.5, DraggableScrollableSheet.snapTarget(0.6, 0, stops), 1e-9);
        assertEquals(1.0, DraggableScrollableSheet.snapTarget(0.6, 2, stops), 1e-9);
        assertEquals(0.5, DraggableScrollableSheet.snapTarget(0.6, -2, stops), 1e-9);
        assertEquals(0.25, DraggableScrollableSheet.snapTarget(0.3, -2, stops), 1e-9);
    }

    @Test
    void controllerMovesTheSheetWithinItsBounds() {
        DraggableScrollableController c = new DraggableScrollableController();
        DraggableScrollableSheet s = new DraggableScrollableSheet();
        s.controller(c);
        s.minChildSize(0.2);
        s.maxChildSize(0.9);
        s.builder(new Funcs.Func2<BuildContext, ScrollController, Widget>() {
            @Override
            public Widget call(BuildContext ctx, ScrollController sc) {
                return new ProbeBox(10, 10);
            }
        });
        RenderHost host = new RenderHost();
        BuildOwner owner = new BuildOwner();
        FlutterUI.mount(s, host, owner);
        host.rootRenderElement().layout(BoxConstraints.tight(300, 400));
        assertTrue(c.isAttached());
        assertEquals(0.5, c.size(), 1e-9);
        final int[] heard = {0};
        c.addListener(() -> heard[0]++);
        c.jumpTo(2.0);
        assertEquals(0.9, c.size(), 1e-9);
        assertEquals(360.0, c.pixels(), 1e-6);
        assertEquals(1, heard[0]);
        c.jumpTo(0.0);
        assertEquals(0.2, c.size(), 1e-9);
    }

    @Test
    void textScalerScalesTheFontSize() {
        TextStyle s = new TextStyle();
        s.fontSize(10);
        assertEquals(20.0, TextScaler.applyTo(TextScaler.linear(2), s).getFontSize(), 1e-9);
        assertEquals(21.0, TextScaler.applyTo(TextScaler.linear(1.5), null).getFontSize(), 1e-9);
        assertTrue(TextScaler.applyTo(TextScaler.noScaling, s) == s);
        assertTrue(TextScaler.applyTo("not a scaler", s) == s);
    }

    @Test
    void horizontalGridFillsColumnsTopToBottom() {
        GridView g = GridView.count(null, null, null, null, 2, 1.0, null, null, null,
                DartList.of((Widget) new ProbeBox(1, 1), new ProbeBox(1, 1), new ProbeBox(1, 1)),
                Axis.horizontal);
        RenderHost host = new RenderHost();
        FlutterUI.mount(g, host, new BuildOwner());
        RenderElement scroll = host.rootRenderElement();
        scroll.layout(BoxConstraints.tight(500, 200));
        RenderElement content = RenderElement.findRenderElement(((ScrollRenderElement) scroll).contentElement());
        List<RenderElement> items = content.renderChildren();
        assertEquals(0.0, items.get(1).relX(), 1e-6);
        assertTrue(items.get(1).relY() > 0, "second cell under the first");
        assertTrue(items.get(2).relX() > 0, "third cell starts the next column");
        assertEquals(0.0, items.get(2).relY(), 1e-6);
        assertTrue(content.size().width() < 500 || content.size().width() == 500);
    }

    @Test
    void selectableTextReportsSelectionChanges() {
        final List<TextSelection> seen = new ArrayList<TextSelection>();
        SelectableText t = new SelectableText("hello");
        t.onSelectionChanged(new Funcs.VoidFunc2<Object, Object>() {
            @Override
            public void call(Object sel, Object cause) {
                seen.add((TextSelection) sel);
            }
        });
        t.select(0, 5);
        assertEquals(1, seen.size());
        assertEquals(5, seen.get(0).extentOffset());
        assertFalse(seen.get(0).baseOffset() != 0);
    }
}
