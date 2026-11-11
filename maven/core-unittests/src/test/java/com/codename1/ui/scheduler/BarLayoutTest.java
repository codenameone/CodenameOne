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
package com.codename1.ui.scheduler;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The bar over a schedule on a screen it fits and on one it does not.
class BarLayoutTest extends UITestBase {
    private static Label part(int w, int h) {
        Label l = new Label();
        l.getAllStyles().setMargin(0, 0, 0, 0);
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.setPreferredSize(new Dimension(w, h));
        return l;
    }

    private static Container bar(BarLayout layout, Label... parts) {
        Container c = new Container(layout);
        c.getAllStyles().setMargin(0, 0, 0, 0);
        c.getAllStyles().setPadding(0, 0, 0, 0);
        for (Label l : parts) {
            c.add(l);
        }
        return c;
    }

    private static void lay(Container c, int w) {
        c.setShouldCalcPreferredSize(true);
        c.setWidth(w);
        c.setHeight(c.getPreferredH());
        c.getLayout().layoutContainer(c);
    }

    @FormTest
    void aBarThatFitsIsOneRowWithTheMiddleTakingWhatIsLeft() {
        BarLayout layout = new BarLayout();
        Label lead = part(100, 30);
        Label middle = part(50, 20);
        Label trail = part(120, 40);
        Container c = bar(layout, lead, middle, trail);

        assertFalse(layout.fit(c, 400), "it was a row already");
        assertFalse(layout.isStacked());
        lay(c, 400);

        assertEquals(40, c.getHeight(), "as tall as its tallest part");
        assertEquals(0, lead.getX());
        assertEquals(100, lead.getWidth());
        assertEquals(100, middle.getX());
        assertEquals(180, middle.getWidth(), "the middle fills");
        assertEquals(280, trail.getX());
        assertEquals(120, trail.getWidth());
        assertEquals(0, trail.getY());
    }

    @FormTest
    void aBarTooNarrowForARowPutsWhatTrailsUnderTheRest() {
        BarLayout layout = new BarLayout();
        Label lead = part(100, 30);
        Label middle = part(50, 20);
        Label trail = part(120, 40);
        Container c = bar(layout, lead, middle, trail);

        assertTrue(layout.fit(c, 200), "270 across does not fit in 200");
        assertTrue(layout.isStacked());
        lay(c, 200);

        assertEquals(70, c.getHeight(), "two rows");
        assertEquals(0, lead.getY());
        assertEquals(100, middle.getX());
        assertEquals(100, middle.getWidth());
        assertEquals(30, trail.getY(), "under the first row");
        assertEquals(40, trail.getX(), "and in the middle of the bar");
        assertEquals(120, trail.getWidth());
        // Nothing lies over anything else.
        assertTrue(lead.getX() + lead.getWidth() <= middle.getX());
        assertTrue(lead.getY() + lead.getHeight() <= trail.getY());

        assertTrue(layout.fit(c, 400), "and goes back to a row when there is room");
        assertFalse(layout.isStacked());
    }

    @FormTest
    void aTrailingPartWiderThanTheBarStartsAtItsStart() {
        BarLayout layout = new BarLayout();
        Label lead = part(60, 30);
        Label trail = part(300, 40);
        Container c = bar(layout, lead, trail);

        layout.fit(c, 200);
        lay(c, 200);

        assertEquals(0, trail.getX());
        assertEquals(200, trail.getWidth(), "no wider than the bar: it scrolls inside it");
    }

    @FormTest
    void aBarWithNoWidthYetIsNotStacked() {
        BarLayout layout = new BarLayout();
        Container c = bar(layout, part(100, 30), part(120, 40));
        assertFalse(layout.fit(c, 0));
        assertFalse(layout.isStacked());
    }
}
