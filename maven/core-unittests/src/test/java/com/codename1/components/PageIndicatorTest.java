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
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class PageIndicatorTest extends UITestBase {

    @FormTest
    void countAndCurrentPageStayInRange() {
        PageIndicator p = new PageIndicator(3);
        assertEquals(3, p.getPageCount());
        p.setCurrentPage(2);
        assertEquals(2, p.getCurrentPage());
        p.setCurrentPage(5);
        assertEquals(2, p.getCurrentPage());
        p.setPageCount(2);
        assertEquals(1, p.getCurrentPage());
        int wide = new PageIndicator(5).getPreferredW();
        assertTrue(wide > new PageIndicator(2).getPreferredW());
    }

    @FormTest
    void bindFollowsTabsAndTapSelectsTab() {
        Form f = new Form(new BorderLayout());
        Tabs tabs = new Tabs();
        tabs.hideTabs();
        tabs.addTab("", new Label("1"));
        tabs.addTab("", new Label("2"));
        tabs.addTab("", new Label("3"));
        PageIndicator dots = new PageIndicator();
        dots.bind(tabs);
        f.add(BorderLayout.CENTER, tabs).add(BorderLayout.SOUTH, dots);
        f.show();
        assertEquals(3, dots.getPageCount());
        tabs.setSelectedIndex(1, false);
        assertEquals(1, dots.getCurrentPage());

        int last = -1;
        for (int x = dots.getAbsoluteX(); x < dots.getAbsoluteX() + dots.getWidth(); x++) {
            if (dots.pageAt(x) == 2) {
                last = x;
                break;
            }
        }
        assertTrue(last >= 0, "the last dot is somewhere in the row");
        final int[] taps = {0};
        dots.addActionListener(e -> taps[0]++);
        dots.pointerReleased(last, dots.getAbsoluteY() + dots.getHeight() / 2);
        assertEquals(2, dots.getCurrentPage());
        assertEquals(1, taps[0]);
    }
}
