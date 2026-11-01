/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A safe-area tab bar at the bottom of a screen must keep one height across
/// revalidates. It used to flip between two layouts: one sized for its content
/// plus the bottom inset, and one sized for the content alone with the inset
/// padded into it, clipping the labels. Which one a pass produced depended on
/// the bounds the bar happened to have when it was measured.
///
/// The fix has layout re-judge the inset and lay out again when the
/// measurement was stale, so the other tests here hold it to doing that only
/// when it must: a settled bar, a stretched safe-area pane and a scrollable one
/// are laid out exactly once per revalidate.
class SafeAreaBottomBarStabilityTest extends UITestBase {

    private static final int TOP_INSET = 47;
    private static final int INSET = 34;

    /// Counts its layouts, to tell a settled layout from one that keeps asking
    /// for another pass.
    private static final class Counting extends Container {
        int layouts;

        Counting(Layout l) {
            super(l);
        }

        @Override
        public void laidOut() {
            super.laidOut();
            layouts++;
        }
    }

    private Container body;
    private Counting tabs;
    private Button cart;
    private Container top;

    private Form notched() {
        Form f = Display.getInstance().getCurrent();
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        implementation.setDisplaySafeArea(new Rectangle(0, TOP_INSET, w, h - TOP_INSET - INSET));
        f.setLayout(new BorderLayout());
        return f;
    }

    private Form build() {
        Form f = notched();
        Container screen = new Container(new BorderLayout());
        top = new Container(BoxLayout.y());
        top.add(new Label("Deliver to"));
        top.setSafeArea(true);
        screen.add(BorderLayout.NORTH, top);
        body = new Container(new BorderLayout());
        screen.add(BorderLayout.CENTER, body);
        Container south = new Container(BoxLayout.y());
        cart = new Button("View cart");
        cart.setHidden(true);
        cart.setVisible(false);
        south.add(cart);
        tabs = new Counting(new GridLayout(1, 4));
        for (String s : new String[]{"Home", "Search", "Orders", "Favorites"}) {
            tabs.add(new Button(s));
        }
        tabs.setSafeArea(true);
        south.add(tabs);
        screen.add(BorderLayout.SOUTH, south);
        f.add(BorderLayout.CENTER, screen);
        f.show();
        f.revalidate();
        return f;
    }

    private void switchTo(Form f, int rows) {
        body.removeAll();
        Container list = new Container(BoxLayout.y());
        for (int i = 0; i < rows; i++) {
            list.add(new Label("Row " + i));
        }
        list.setScrollableY(true);
        body.add(BorderLayout.CENTER, list);
        f.revalidate();
    }

    private int contentHeight() {
        int max = 0;
        for (Component c : tabs) {
            max = Math.max(max, c.getOuterHeight());
        }
        return max + tabs.getStyle().getVerticalPadding();
    }

    @FormTest
    void bottomBarKeepsItsHeightAcrossTabSwitches() {
        Form f = build();
        int expected = contentHeight() + INSET;
        int[] rows = {3, 40, 0, 12, 1, 60, 5};
        for (int i = 0; i < rows.length; i++) {
            switchTo(f, rows[i]);
            assertEquals(expected, tabs.getHeight(), "tab bar height after switch " + i);
            assertEquals(f.getContentPane().getAbsoluteY() + f.getContentPane().getHeight(),
                    tabs.getAbsoluteY() + tabs.getHeight(), "tab bar bottom after switch " + i);
            // The header is nowhere near the bottom edge: no bottom inset.
            assertEquals(top.getComponentAt(0).getOuterHeight() + top.getStyle().getVerticalPadding()
                    + Math.max(0, TOP_INSET - top.getAbsoluteY()), top.getHeight(),
                    "header height after switch " + i);
        }
    }

    @FormTest
    void bottomBarKeepsItsHeightWhenTheCartAppearsAndGoes() {
        Form f = build();
        int expected = contentHeight() + INSET;
        for (int i = 0; i < 6; i++) {
            boolean show = i % 2 == 0;
            cart.setHidden(!show);
            cart.setVisible(show);
            f.revalidate();
            assertEquals(expected, tabs.getHeight(), "tab bar height, cart shown=" + show);
        }
    }

    @FormTest
    void aSettledBottomBarIsLaidOutOncePerRevalidate() {
        Form f = build();
        switchTo(f, 4);
        int height = tabs.getHeight();
        for (int i = 0; i < 3; i++) {
            tabs.layouts = 0;
            f.revalidate();
            assertEquals(1, tabs.layouts, "settled tab bar, revalidate " + i);
            assertEquals(height, tabs.getHeight());
        }
    }

    @FormTest
    void aStretchedSafeAreaPaneIsLaidOutOncePerRevalidate() {
        Form f = notched();
        Counting pane = new Counting(new BorderLayout());
        pane.setSafeArea(true);
        Label content = new Label("Short content");
        pane.add(BorderLayout.NORTH, content);
        f.add(BorderLayout.CENTER, pane);
        f.show();
        f.revalidate();
        for (int i = 0; i < 3; i++) {
            pane.layouts = 0;
            f.revalidate();
            assertEquals(1, pane.layouts, "stretched safe-area pane, revalidate " + i);
        }
        // Stretched to the bottom edge, its content still stays clear of it.
        int safeBottom = Display.getInstance().getDisplayHeight() - INSET;
        assertTrue(pane.getAbsoluteY() + pane.getHeight() > safeBottom, "pane reaches the unsafe strip");
    }

    @FormTest
    void aScrollableSafeAreaPaneKeepsTheInsetInItsScrollSize() {
        Form f = notched();
        Counting pane = new Counting(BoxLayout.y());
        pane.setScrollableY(true);
        pane.setSafeArea(true);
        for (int i = 0; i < 200; i++) {
            pane.add(new Label("Row " + i));
        }
        f.add(BorderLayout.CENTER, pane);
        f.show();
        f.revalidate();
        int content = 0;
        for (Component c : pane) {
            content += c.getOuterHeight();
        }
        int scroll = pane.getScrollDimension().getHeight();
        assertTrue(scroll >= content + INSET,
                "scroll size " + scroll + " must let the last row clear the inset (content " + content + ")");
        pane.layouts = 0;
        f.revalidate();
        assertEquals(1, pane.layouts, "scrollable safe-area pane, one layout per revalidate");
    }

    @FormTest
    void bottomBarDropsTheInsetWhenTheSafeAreaGoesAway() {
        Form f = build();
        int content = contentHeight();
        assertEquals(content + INSET, tabs.getHeight());
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        implementation.setDisplaySafeArea(new Rectangle(0, 0, w, h));
        for (int i = 0; i < 2; i++) {
            switchTo(f, 3 + i);
        }
        assertEquals(content, tabs.getHeight(), "no inset once the display has none");
    }

    @FormTest
    void aBarMovedToTheTopLosesTheBottomInset() {
        Form f = build();
        int content = contentHeight();
        Container south = tabs.getParent();
        south.removeComponent(tabs);
        top.add(tabs);
        f.revalidate();
        f.revalidate();
        assertEquals(content, tabs.getHeight(), "a bar at the top has no bottom inset");
    }

    @FormTest
    void aBarNoLongerAtTheEdgeDropsTheInsetInOneRevalidate() {
        // Measured at the edge with the inset, then pushed up by a footer: the
        // first layout applies none, so that same revalidate must re-measure.
        Form f = build();
        int content = contentHeight();
        assertEquals(content + INSET, tabs.getHeight());
        // Taller than the inset, so the bar clears the unsafe edge entirely.
        Label footer = new Label("Footer");
        footer.setPreferredH(INSET * 2);
        tabs.getParent().add(footer);
        f.revalidate();
        assertEquals(content, tabs.getHeight(), "the bar keeps an inset it no longer gets");
    }

    @FormTest
    void aBarPartlyOverTheEdgeGetsOnlyTheOverlap() {
        // A footer shorter than the inset leaves the bar over the unsafe edge
        // by the difference, and that is all the padding it should take.
        Form f = build();
        int content = contentHeight();
        Label footer = new Label("Footer");
        footer.setPreferredH(INSET - 8);
        tabs.getParent().add(footer);
        f.revalidate();
        int safeLine = Display.getInstance().getDisplayHeight() - INSET;
        int overlap = tabs.getAbsoluteY() + tabs.getHeight() - safeLine;
        assertTrue(overlap > 0 && overlap < INSET, "the bar ends inside the unsafe edge: " + overlap);
        assertEquals(content + overlap, tabs.getHeight(), "the bar's inset is the part of it over the edge");
        int settled = tabs.getHeight();
        f.revalidate();
        assertEquals(settled, tabs.getHeight(), "and it stays there");
    }

    @FormTest
    void aBarTakesANewInsetInOneRevalidate() {
        // Laid out with no inset, then the display gains one (a rotation, a
        // safe area reported late): the inset that last layout applied must not
        // hold the new measurement at zero.
        Form f = build();
        int content = contentHeight();
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        implementation.setDisplaySafeArea(new Rectangle(0, 0, w, h));
        f.revalidate();
        f.revalidate();
        assertEquals(content, tabs.getHeight());
        implementation.setDisplaySafeArea(new Rectangle(0, TOP_INSET, w, h - TOP_INSET - INSET));
        f.revalidate();
        assertEquals(content + INSET, tabs.getHeight(), "the bar gets the new inset in the same revalidate");
    }

    @FormTest
    void aBarMeasuredDuringItsOwnLayoutCountsTheInsetOnce() {
        // A bottom Tabs bar is measured again from inside its own layout, when
        // its style already holds the snapped padding; adding the inset to
        // that again made its preferred size an inset too tall.
        notched();
        Form f = Display.getInstance().getCurrent();
        Tabs t = new Tabs(Component.BOTTOM);
        t.addTab("A", new Label("first"));
        t.addTab("B", new Label("second"));
        f.add(BorderLayout.CENTER, t);
        f.revalidate();
        t.setSelectedIndex(1);
        f.revalidate();
        Container bar = t.getTabsContainer();
        int content = 0;
        for (Component c : bar) {
            content = Math.max(content, c.getOuterHeight());
        }
        int overlap = bar.getAbsoluteY() + bar.getHeight() - (Display.getInstance().getDisplayHeight() - INSET);
        assertTrue(overlap > 0, "the bar reaches into the unsafe edge: " + overlap);
        assertEquals(content + bar.getStyle().getVerticalPadding() + overlap, bar.getPreferredH(),
                "the bar is measured with one inset");
        assertEquals(bar.getPreferredH(), bar.getHeight());
    }

    @FormTest
    void aSafeAreaBarInsideASafeAreaPaneGetsOneInset() {
        Form f = notched();
        Container pane = new Container(new BorderLayout());
        pane.setSafeArea(true);
        pane.add(BorderLayout.CENTER, new Label("Body"));
        Counting bar = new Counting(new GridLayout(1, 2));
        bar.add(new Button("A"));
        bar.add(new Button("B"));
        bar.setSafeArea(true);
        pane.add(BorderLayout.SOUTH, bar);
        f.add(BorderLayout.CENTER, pane);
        f.show();
        f.revalidate();
        int content = 0;
        for (Component c : bar) {
            content = Math.max(content, c.getOuterHeight());
        }
        content += bar.getStyle().getVerticalPadding();
        for (int i = 0; i < 3; i++) {
            bar.layouts = 0;
            f.revalidate();
            assertEquals(1, bar.layouts, "nested bar, revalidate " + i);
            assertEquals(content, bar.getHeight(), "the pane already pads the inset; the bar adds none");
        }
    }
}
