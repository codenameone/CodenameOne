/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.ui;

import com.codename1.components.StickyHeaderContainer;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Unit tests for [com.codename1.components.StickyHeaderContainer]. These cover
/// section bookkeeping, the activation/deactivation swap, push-out offsets, and
/// the pinned-component identity contract that lets event listeners on a header
/// continue to fire while it is pinned to the top.
public class StickyHeaderContainerTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        testSectionsRegistered();
        testInitialActivationIsInactive();
        testActivationOnScroll();
        testActivationFollowsScroll();
        testPushOutOffset();
        testHeaderIdentityPreserved();
        testDeactivateOnScrollBack();
        testClearSections();
        testHeaderOnlySection();
        return true;
    }

    private static StickyHeaderContainer build(int width, int height,
            int headerHeight, int contentHeight, int sectionCount) {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        zero(sticky);
        zero(sticky.getScrollContainer());
        zero(sticky.getStickyHost());
        for (int i = 0; i < sectionCount; i++) {
            Container header = sized("H" + i, width, headerHeight);
            Container content = sized("C" + i, width, contentHeight);
            sticky.addSection(header, content);
        }
        sticky.setWidth(width);
        sticky.setHeight(height);
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();
        return sticky;
    }

    private static void zero(Container c) {
        Style s = c.getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
    }

    private static Container sized(final String name, final int w, final int h) {
        Container c = new Container() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(w, h);
            }
        };
        c.setName(name);
        Style s = c.getAllStyles();
        s.setPadding(0, 0, 0, 0);
        s.setMargin(0, 0, 0, 0);
        return c;
    }

    private static int placeholderCount(StickyHeaderContainer sticky) {
        Container scroller = sticky.getScrollContainer();
        int count = 0;
        for (int i = 0; i < scroller.getComponentCount(); i++) {
            if (!scroller.getComponentAt(i).isVisible()) {
                count++;
            }
        }
        return count;
    }

    private void testSectionsRegistered() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);
        assertEqual(3, sticky.getStickyHeaders().size(),
                "expected 3 registered headers");
        // Each section adds 2 children (header + content).
        assertEqual(6, sticky.getScrollContainer().getComponentCount(),
                "scroller should hold one entry per header and content");
    }

    private void testInitialActivationIsInactive() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);
        assertEqual(-1, sticky.getActiveSectionIndex(),
                "no section should be active before scrolling");
        assertEqual(0, sticky.getStickyHost().getComponentCount(),
                "sticky host should start empty");
        assertEqual(0, placeholderCount(sticky),
                "no placeholder should be present initially");
    }

    private void testActivationOnScroll() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);
        Component firstHeader = sticky.getStickyHeaders().get(0);

        sticky.getScrollContainer().setScrollY(10);
        sticky.updateSticky();

        assertEqual(0, sticky.getActiveSectionIndex(),
                "first section should pin once it scrolls past the top");
        assertEqual(1, sticky.getStickyHost().getComponentCount(),
                "the pinned header should occupy the sticky host");
        assertTrue(sticky.getStickyHost().getComponentAt(0) == firstHeader,
                "the same header instance must be moved into the sticky host");
        assertEqual(1, placeholderCount(sticky),
                "scroller should have exactly one placeholder for the pinned section");
    }

    private void testActivationFollowsScroll() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);
        Component secondHeader = sticky.getStickyHeaders().get(1);

        // Scroll past the start of the second section.
        sticky.getScrollContainer().setScrollY(260);
        sticky.updateSticky();

        assertEqual(1, sticky.getActiveSectionIndex(),
                "second section should activate after scrolling past it");
        assertTrue(sticky.getStickyHost().getComponentAt(0) == secondHeader,
                "second section's header should be pinned");
    }

    private void testPushOutOffset() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);

        // Place the next header's relative top 30px below the viewport top.
        // With activeH=50, push-out offset should be 30 - 50 = -20.
        int sectionStride = 250; // headerHeight + contentHeight
        int targetScroll = sectionStride - 30;
        sticky.getScrollContainer().setScrollY(targetScroll);
        sticky.updateSticky();

        assertEqual(0, sticky.getActiveSectionIndex(),
                "first section should still be the pinned one during push-out");
        assertEqual(-20, sticky.getActiveSectionOffset(),
                "active offset should equal nextRelTop - activeHeaderHeight");
    }

    private void testHeaderIdentityPreserved() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 2);
        Component first = sticky.getStickyHeaders().get(0);
        Component second = sticky.getStickyHeaders().get(1);

        sticky.getScrollContainer().setScrollY(20);
        sticky.updateSticky();
        assertTrue(sticky.getStickyHost().getComponentAt(0) == first,
                "first header instance must be reused while pinned");

        sticky.getScrollContainer().setScrollY(280);
        sticky.updateSticky();
        assertTrue(sticky.getStickyHost().getComponentAt(0) == second,
                "second header instance must be reused after takeover");
    }

    private void testDeactivateOnScrollBack() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);

        sticky.getScrollContainer().setScrollY(50);
        sticky.updateSticky();
        assertEqual(0, sticky.getActiveSectionIndex(),
                "first section should be active after scrolling forward");

        sticky.getScrollContainer().setScrollY(0);
        sticky.updateSticky();
        assertEqual(-1, sticky.getActiveSectionIndex(),
                "no section should be active once scrolled back to the top");
        assertEqual(0, sticky.getStickyHost().getComponentCount(),
                "sticky host should be empty after deactivation");
        assertEqual(0, placeholderCount(sticky),
                "no placeholder should remain after deactivation");
    }

    private void testClearSections() {
        StickyHeaderContainer sticky = build(200, 600, 50, 200, 3);
        sticky.getScrollContainer().setScrollY(50);
        sticky.updateSticky();
        assertEqual(0, sticky.getActiveSectionIndex(), "section should be active before clearing");

        sticky.clearSections();

        assertEqual(0, sticky.getStickyHeaders().size(), "no headers should remain after clear");
        assertEqual(0, sticky.getScrollContainer().getComponentCount(),
                "scroller should be empty after clear");
        assertEqual(-1, sticky.getActiveSectionIndex(), "no section should be active after clear");
        assertEqual(0, sticky.getStickyHost().getComponentCount(),
                "sticky host should be empty after clear");
    }

    private void testHeaderOnlySection() {
        StickyHeaderContainer sticky = new StickyHeaderContainer();
        Container header = sized("solo", 200, 50);
        sticky.addSection(header);
        sticky.setWidth(200);
        sticky.setHeight(600);
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();

        assertEqual(1, sticky.getStickyHeaders().size(),
                "header-only section should still register the header");
        assertEqual(1, sticky.getScrollContainer().getComponentCount(),
                "header-only section adds a single child to the scroller");
    }
}
