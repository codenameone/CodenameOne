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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarouselTest extends UITestBase {

    private Carousel carousel(int pages) {
        Carousel c = new Carousel();
        for (int i = 0; i < pages; i++) {
            c.addPage(new Label("Page " + i));
        }
        return c;
    }

    @FormTest
    void pagesAndIndicatorStayInStep() {
        Form f = new Form(new BorderLayout());
        Carousel c = carousel(3);
        f.add(BorderLayout.CENTER, c);
        f.show();
        assertEquals(3, c.getPageCount());
        assertEquals(3, c.getIndicator().getPageCount());
        final List<Integer> changes = new ArrayList<Integer>();
        c.addSelectionListener((o, n) -> changes.add(n));
        c.setSelectedIndex(2, false);
        assertEquals(2, c.getSelectedIndex());
        assertEquals(2, c.getIndicator().getCurrentPage());
        assertEquals(1, changes.size());
        c.removePage(2);
        assertEquals(2, c.getIndicator().getPageCount());
    }

    @FormTest
    void autoPlayWrapsOnlyWhenLooping() throws Exception {
        Form f = new Form(new BorderLayout());
        Carousel c = carousel(2);
        f.add(BorderLayout.CENTER, c);
        f.show();
        c.setAutoPlay(60);
        Thread.sleep(80);
        c.tick();
        assertEquals(1, c.getIndicator().getCurrentPage());
        Thread.sleep(80);
        c.setLoop(false);
        c.tick();
        assertEquals(1, c.getIndicator().getCurrentPage(), "no wrap without loop");
        c.setLoop(true);
        Thread.sleep(80);
        c.tick();
        assertEquals(0, c.getIndicator().getCurrentPage());
    }

    @FormTest
    void indicatorCanMoveOrHide() {
        Carousel c = carousel(2);
        c.setIndicatorPosition(BorderLayout.NORTH);
        assertSame(c, c.getIndicator().getParent());
        c.setIndicatorPosition(null);
        assertNull(c.getIndicator().getParent());
    }

    @FormTest
    void finishedSlideDoesNotDelayTheNextTick() throws Exception {
        Form f = new Form(new BorderLayout());
        Carousel c = carousel(3);
        f.add(BorderLayout.CENTER, c);
        f.show();
        c.setAutoPlay(60);
        Thread.sleep(120);
        // What Tabs does when an auto play slide ends: commit the index, which
        // fires the selection listeners.
        c.getTabs().setSelectedIndex(1);
        c.tick();
        assertEquals(2, c.getIndicator().getCurrentPage(), "the tick after a finished slide advances");
    }

    @FormTest
    void removingALaterPageKeepsTheCurrentOne() {
        Carousel c = carousel(4);
        c.setSelectedIndex(1, false);
        c.removePage(3);
        assertEquals(1, c.getSelectedIndex());
        assertEquals(1, c.getIndicator().getCurrentPage());
    }
}
