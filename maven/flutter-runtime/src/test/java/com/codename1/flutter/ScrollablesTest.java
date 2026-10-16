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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.GridView;
import com.codename1.flutter.widgets.ListView;
import com.codename1.flutter.widgets.ScrollRenderElement;
import com.codename1.flutter.widgets.SingleChildScrollView;

import dart.core.DartList;
import dart.core.UnsupportedError;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Scrollable boundary layout (SingleChildScrollView/ListView/GridView),
 * headless: the content subtree is laid out with a tight viewport width and
 * an unbounded main axis; ListView.builder materializes its items eagerly.
 */
class ScrollablesTest {

    private RenderHost host;

    private ScrollRenderElement mountAndLayout(Widget root, BoxConstraints constraints) {
        BuildOwner owner = new BuildOwner();
        host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        ScrollRenderElement r = (ScrollRenderElement) host.rootRenderElement();
        r.layout(constraints);
        r.position(0, 0);
        return r;
    }

    private static RenderElement contentOf(ScrollRenderElement scroll) {
        return RenderElement.findRenderElement(scroll.contentElement());
    }

    @Test
    void builderMaterializesItemCountChildrenEagerly() {
        final List<Long> builtIndexes = new ArrayList<Long>();
        final List<BuildContext> contexts = new ArrayList<BuildContext>();
        ListView lv = ListView.builder(null, 5L, (c, i) -> {
            builtIndexes.add(i);
            contexts.add(c);
            return new ProbeBox(10, 20);
        }, null, null, null, null, null, null, null, null, null);

        ScrollRenderElement scroll = mountAndLayout(lv, BoxConstraints.tight(100, 50));

        assertEquals(List.of(0L, 1L, 2L, 3L, 4L), builtIndexes);
        assertSame(scroll, contexts.get(0), "the scroll element is the builder's BuildContext");

        RenderElement content = contentOf(scroll);
        assertNotNull(content);
        assertEquals(5, content.renderChildren().size());
        // stretched to the viewport width, stacked vertically, taller than
        // the 50px viewport (that's what scrolls)
        assertEquals(new Size(100, 100), content.size());
        assertEquals(new Size(100, 50), scroll.size());
    }

    @Test
    void builderWithoutItemCountThrowsUnsupportedError() {
        assertThrows(UnsupportedError.class,
                () -> ListView.builder(null, null, (c, i) -> new ProbeBox(1, 1),
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    void childrenModeStacksChildrenWithPadding() {
        ListView lv = new ListView();
        lv.padding(EdgeInsets.all(5));
        lv.children(DartList.of((Widget) new ProbeBox(10, 10), new ProbeBox(10, 10), new ProbeBox(10, 10)));

        ScrollRenderElement scroll = mountAndLayout(lv, BoxConstraints.tight(100, 200));
        assertEquals(new Size(100, 200), scroll.size());

        RenderElement content = contentOf(scroll);
        // padding wrapper: 3 * 10 high + 10 padding, viewport-wide
        assertEquals(new Size(100, 40), content.size());

        // headless positioning goes through the scroll origin
        List<RenderElement> items = itemsOf(content);
        assertEquals(3, items.size());
        assertEquals(5, items.get(0).x());
        assertEquals(5, items.get(0).y());
        assertEquals(15, items.get(1).y());
        assertEquals(25, items.get(2).y());
        // stretched to the padded viewport width
        assertEquals(90.0, items.get(0).size().width());
    }

    @Test
    void shrinkWrapSizesTheMainAxisToTheContent() {
        ListView lv = new ListView();
        lv.shrinkWrap(true);
        lv.children(DartList.of((Widget) new ProbeBox(10, 10), new ProbeBox(10, 30)));

        ScrollRenderElement scroll = mountAndLayout(lv, BoxConstraints.loose(100, 500));
        assertEquals(new Size(100, 40), scroll.size());
    }

    @Test
    void singleChildScrollViewGivesTheChildAnUnboundedMainAxis() {
        SingleChildScrollView sv = new SingleChildScrollView();
        sv.child(new ProbeBox(50, 1000));

        ScrollRenderElement scroll = mountAndLayout(sv, BoxConstraints.tight(100, 200));
        assertEquals(new Size(100, 200), scroll.size());
        RenderElement content = contentOf(scroll);
        // tight viewport width, free height
        assertEquals(new Size(100, 1000), content.size());
    }

    @Test
    void gridViewCountLaysOutRowsOfTightCells() {
        DartList<Widget> cells = new DartList<Widget>();
        for (int i = 0; i < 6; i++) {
            cells.add(new ProbeBox(1, 1));
        }
        GridView gv = GridView.count(null, null, null, null, 2L, null, 10.0, 20.0, null, cells);

        ScrollRenderElement scroll = mountAndLayout(gv, BoxConstraints.tight(220, 500));
        RenderElement content = contentOf(scroll);

        // cellW = (220 - 20) / 2 = 100, ratio 1 -> cellH = 100
        List<RenderElement> kids = content.renderChildren();
        assertEquals(6, kids.size());
        assertEquals(new Size(100, 100), kids.get(0).size());
        assertEquals(0, kids.get(0).x());
        assertEquals(0, kids.get(0).y());
        assertEquals(120, kids.get(1).x());
        assertEquals(0, kids.get(1).y());
        assertEquals(0, kids.get(2).x());
        assertEquals(110, kids.get(2).y());
        assertEquals(120, kids.get(3).x());
        assertEquals(110, kids.get(3).y());
        // 3 rows: 3*100 + 2*10 spacing
        assertEquals(new Size(220, 320), content.size());
    }

    @Test
    void gridViewAspectRatioShrinksTheCellHeight() {
        DartList<Widget> cells = new DartList<Widget>();
        cells.add(new ProbeBox(1, 1));
        cells.add(new ProbeBox(1, 1));
        GridView gv = GridView.count(null, null, null, null, 2L, 2.0, null, null, null, cells);

        ScrollRenderElement scroll = mountAndLayout(gv, BoxConstraints.tight(200, 500));
        RenderElement content = contentOf(scroll);
        // cellW = 100, ratio 2 -> cellH = 50
        assertEquals(new Size(100, 50), content.renderChildren().get(0).size());
        assertEquals(new Size(200, 50), content.size());
    }

    /**
     * The probe items inside the ListView content (descending through the
     * padding wrapper and the synthesized column).
     */
    private static List<RenderElement> itemsOf(RenderElement content) {
        // content is the Padding render element; its child is the Column
        RenderElement column = content.renderChildren().get(0);
        return column.renderChildren();
    }
}
