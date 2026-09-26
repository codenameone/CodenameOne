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
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Flex layout semantics per Flutter's RenderFlex, driven headless (no CN1
 * Display; the render boxes have stubbed intrinsic sizes).
 */
class FlexLayoutTest {

    private RenderElement mountAndLayout(Widget root, double w, double h) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.tight(w, h));
        r.position(0, 0);
        return r;
    }

    @Test
    void columnWithFixedChildrenAndExpandedDistributesFreeSpace() {
        Column col = new Column();
        Expanded expanded = new Expanded();
        expanded.child(new ProbeBox(10, 10));
        col.children(DartList.of(new ProbeBox(100, 50), new ProbeBox(200, 60), expanded));

        RenderElement root = mountAndLayout(col, 400, 600);

        // Column fills the tight constraints.
        assertEquals(new Size(400, 600), root.size());

        List<RenderElement> kids = root.renderChildren();
        assertEquals(3, kids.size());

        RenderElement c1 = kids.get(0);
        RenderElement c2 = kids.get(1);
        RenderElement ex = kids.get(2);

        // Fixed children keep their intrinsic sizes.
        assertEquals(new Size(100, 50), c1.size());
        assertEquals(new Size(200, 60), c2.size());
        // Expanded gets all free main-axis space: 600 - (50 + 60) = 490,
        // tight; its child keeps its own width under the loose cross axis.
        assertEquals(new Size(10, 490), ex.size());

        // Main axis (vertical), MainAxisAlignment.start: stacked in order.
        assertEquals(0, c1.y());
        assertEquals(50, c2.y());
        assertEquals(110, ex.y());

        // Cross axis, CrossAxisAlignment.center (Flutter default).
        assertEquals(150, c1.x());
        assertEquals(100, c2.x());
        assertEquals(195, ex.x());

        // The Expanded is a pass-through: its child fills it at offset 0.
        List<RenderElement> exKids = ex.renderChildren();
        assertEquals(1, exKids.size());
        assertEquals(new Size(10, 490), exKids.get(0).size());
        assertEquals(195, exKids.get(0).x());
        assertEquals(110, exKids.get(0).y());
    }

    @Test
    void columnMainAxisAlignmentCenterCentersTheGroup() {
        Column col = new Column();
        col.mainAxisAlignment(MainAxisAlignment.center);
        col.children(DartList.of(new ProbeBox(100, 50), new ProbeBox(100, 50)));

        RenderElement root = mountAndLayout(col, 400, 600);
        List<RenderElement> kids = root.renderChildren();

        // 600 - 100 used = 500 free, half above the group.
        assertEquals(250, kids.get(0).y());
        assertEquals(300, kids.get(1).y());
    }

    @Test
    void columnSpaceBetweenPutsAllFreeSpaceBetweenChildren() {
        Column col = new Column();
        col.mainAxisAlignment(MainAxisAlignment.spaceBetween);
        col.children(DartList.of(new ProbeBox(10, 100), new ProbeBox(10, 100), new ProbeBox(10, 100)));

        RenderElement root = mountAndLayout(col, 400, 600);
        List<RenderElement> kids = root.renderChildren();

        // free = 600 - 300 = 300; between = 150
        assertEquals(0, kids.get(0).y());
        assertEquals(250, kids.get(1).y());
        assertEquals(500, kids.get(2).y());
    }

    @Test
    void rowLaysOutOnHorizontalMainAxis() {
        Row row = new Row();
        row.crossAxisAlignment(CrossAxisAlignment.start);
        Expanded expanded = new Expanded();
        expanded.flex(3);
        expanded.child(new ProbeBox(1, 20));
        Expanded expanded2 = new Expanded();
        expanded2.child(new ProbeBox(1, 20));
        row.children(DartList.of(new ProbeBox(100, 40), expanded, expanded2));

        RenderElement root = mountAndLayout(row, 500, 200);
        List<RenderElement> kids = root.renderChildren();

        // free = 500 - 100 = 400; flex 3:1 -> 300 and 100
        assertEquals(new Size(100, 40), kids.get(0).size());
        assertEquals(300.0, kids.get(1).size().width());
        assertEquals(100.0, kids.get(2).size().width());
        assertEquals(0, kids.get(0).x());
        assertEquals(100, kids.get(1).x());
        assertEquals(400, kids.get(2).x());
        // CrossAxisAlignment.start: all on the top edge.
        assertEquals(0, kids.get(0).y());
        assertEquals(0, kids.get(1).y());
    }

    @Test
    void mainAxisSizeMinShrinkWrapsUnderLooseConstraints() {
        Column col = new Column();
        col.mainAxisSize(MainAxisSize.min);
        col.crossAxisAlignment(CrossAxisAlignment.start);
        col.children(DartList.of(new ProbeBox(100, 50), new ProbeBox(200, 60)));

        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(col, host, owner);
        RenderElement r = host.rootRenderElement();
        Size s = r.layout(BoxConstraints.loose(400, 600));
        assertEquals(new Size(200, 110), s);
    }

    @Test
    void stretchTightensTheCrossAxis() {
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.children(DartList.of(new ProbeBox(100, 50)));

        RenderElement root = mountAndLayout(col, 400, 600);
        RenderElement kid = root.renderChildren().get(0);
        // stretch forces the child's cross axis to the full 400.
        assertEquals(new Size(400, 50), kid.size());
        assertEquals(0, kid.x());
    }
}
