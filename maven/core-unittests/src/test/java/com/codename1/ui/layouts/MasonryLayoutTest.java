/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui.layouts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

class MasonryLayoutTest extends UITestBase {

    private static Label tile(final int h) {
        // Not setPreferredSize: a component with an explicit preferred size ignores setHidden.
        Label l = new Label() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(10, h);
            }
        };
        l.getAllStyles().setMargin(0, 0, 0, 0);
        l.getAllStyles().setPadding(0, 0, 0, 0);
        return l;
    }

    @FormTest
    void eachTileGoesToTheShortestColumn() {
        MasonryLayout m = new MasonryLayout(2);
        m.setGap(0);
        Container c = new Container(m);
        c.getAllStyles().setPadding(0, 0, 0, 0);
        Component a = tile(100);
        Component b = tile(30);
        Component d = tile(30);
        Component e = tile(30);
        c.add(a).add(b).add(d).add(e);
        c.setWidth(200);
        c.setHeight(1000);
        c.layoutContainer();
        assertEquals(0, a.getX());
        assertEquals(b.getX(), d.getX());
        assertEquals(30, d.getY());
        assertEquals(e.getX(), b.getX());
        assertEquals(60, e.getY());
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(100, m.getPreferredSize(c).getHeight());
    }

    @FormTest
    void columnsFollowWidthWhenAsked() {
        MasonryLayout m = new MasonryLayout(1);
        assertEquals(1, m.columnsFor(10000));
        m.setMinimumColumnWidth(20);
        assertTrue(m.columnsFor(10000) > 1);
        assertEquals(1, m.columnsFor(1));
    }

    @FormTest
    void hiddenTilesTakeNoSpace() {
        MasonryLayout m = new MasonryLayout(1);
        m.setGap(0);
        Container c = new Container(m);
        c.getAllStyles().setPadding(0, 0, 0, 0);
        Component a = tile(40);
        a.setHidden(true);
        c.add(a).add(tile(25));
        c.setWidth(100);
        assertEquals(25, m.getPreferredSize(c).getHeight());
    }

    @FormTest
    void tilesAreMeasuredAtTheirColumnWidth() {
        MasonryLayout m = new MasonryLayout(2);
        m.setGap(0);
        Container c = new Container(m);
        c.getAllStyles().setPadding(0, 0, 0, 0);
        final int[] seenWidth = {-1};
        Label wraps = new Label() {
            @Override
            protected Dimension calcPreferredSize() {
                seenWidth[0] = getWidth();
                return new Dimension(1000, getWidth() > 0 ? 10000 / getWidth() : 10);
            }
        };
        wraps.getAllStyles().setMargin(0, 0, 0, 0);
        c.add(wraps).add(tile(10));
        c.setWidth(200);
        c.layoutContainer();
        assertEquals(100, seenWidth[0]);
        assertEquals(100, wraps.getHeight());
    }

    @FormTest
    void preferredWidthCanShrink() {
        MasonryLayout m = new MasonryLayout(2);
        m.setGap(0);
        Container c = new Container(m);
        c.getAllStyles().setPadding(0, 0, 0, 0);
        c.add(tile(10)).add(tile(10));
        c.setWidth(5000);
        assertEquals(20, m.getPreferredSize(c).getWidth());
    }
}
