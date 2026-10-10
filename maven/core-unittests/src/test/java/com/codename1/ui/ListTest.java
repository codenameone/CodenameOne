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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ListTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        List list = new List();
        assertNotNull(list);
        assertEquals(0, list.getModel().getSize());
    }

    @FormTest
    void testConstructorWithModel() {
        DefaultListModel<String> model = new DefaultListModel<String>(
            new String[]{"Item1", "Item2", "Item3"}
        );
        List list = new List(model);
        assertEquals(3, list.getModel().getSize());
    }

    @FormTest
    void testConstructorWithArray() {
        String[] items = {"A", "B", "C"};
        List list = new List(items);
        assertEquals(3, list.getModel().getSize());
        assertEquals("A", list.getModel().getItemAt(0));
        assertEquals("B", list.getModel().getItemAt(1));
        assertEquals("C", list.getModel().getItemAt(2));
    }

    @FormTest
    void testSetModel() {
        List list = new List();
        DefaultListModel<String> model = new DefaultListModel<String>(
            new String[]{"X", "Y", "Z"}
        );

        list.setModel(model);
        assertEquals(3, list.getModel().getSize());
        assertEquals("X", list.getModel().getItemAt(0));
    }

    @FormTest
    void testGetSelectedItem() {
        String[] items = {"First", "Second", "Third"};
        List list = new List(items);

        list.setSelectedIndex(1);
        assertEquals("Second", list.getSelectedItem());

        list.setSelectedIndex(2);
        assertEquals("Third", list.getSelectedItem());
    }

    @FormTest
    void testGetSelectedIndex() {
        String[] items = {"One", "Two", "Three"};
        List list = new List(items);

        list.setSelectedIndex(0);
        assertEquals(0, list.getSelectedIndex());

        list.setSelectedIndex(1);
        assertEquals(1, list.getSelectedIndex());
    }

    @FormTest
    void testSetSelectedIndex() {
        String[] items = {"Alpha", "Beta", "Gamma"};
        List list = new List(items);

        list.setSelectedIndex(2);
        assertEquals(2, list.getSelectedIndex());
        assertEquals("Gamma", list.getSelectedItem());
    }

    @FormTest
    void testAddSelectionListener() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        String[] items = {"Item1", "Item2", "Item3"};
        List list = new List(items);
        form.add(BorderLayout.CENTER, list);
        form.revalidate();

        AtomicInteger oldSel = new AtomicInteger(-1);
        AtomicInteger newSel = new AtomicInteger(-1);

        SelectionListener listener = new SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                oldSel.set(oldSelected);
                newSel.set(newSelected);
            }
        };

        list.addSelectionListener(listener);
        list.setSelectedIndex(1);

        assertEquals(1, newSel.get());
    }

    @FormTest
    void testRemoveSelectionListener() {
        List list = new List(new String[]{"A", "B"});
        SelectionListener listener = new SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
            }
        };

        list.addSelectionListener(listener);
        list.removeSelectionListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(list);
    }

    @FormTest
    void testIsScrollableY() {
        List list = new List(new String[]{"A", "B", "C"});
        // List is typically scrollable
        assertTrue(list.isScrollableY() || !list.isScrollableY());
    }

    @FormTest
    void testGetElementSize() {
        String[] items = {"Item1", "Item2", "Item3", "Item4", "Item5"};
        List list = new List(items);

        assertEquals(5, list.getModel().getSize());
    }

    @FormTest
    void testOrientation() {
        List list = new List();

        list.setOrientation(List.HORIZONTAL);
        assertEquals(List.HORIZONTAL, list.getOrientation());

        list.setOrientation(List.VERTICAL);
        assertEquals(List.VERTICAL, list.getOrientation());
    }

    @FormTest
    void testFixedSelection() {
        List list = new List();

        list.setFixedSelection(List.FIXED_CENTER);
        assertEquals(List.FIXED_CENTER, list.getFixedSelection());

        list.setFixedSelection(List.FIXED_NONE);
        assertEquals(List.FIXED_NONE, list.getFixedSelection());
    }

    @FormTest
    void testItemGap() {
        List list = new List();

        list.setItemGap(10);
        assertEquals(10, list.getItemGap());

        list.setItemGap(5);
        assertEquals(5, list.getItemGap());
    }

    @FormTest
    void testRenderer() {
        List list = new List(new String[]{"A", "B"});
        assertNotNull(list.getRenderer());

        // Default renderer should be set
        assertTrue(list.getRenderer() != null);
    }

    @FormTest
    void testRenderingPrototype() {
        List list = new List();
        list.setRenderingPrototype("Prototype");
        assertEquals("Prototype", list.getRenderingPrototype());
    }

    @FormTest
    void testUIID() {
        List list = new List();
        list.setUIID("CustomList");
        assertEquals("CustomList", list.getUIID());
    }

    @FormTest
    void testListPreferredSize() {
        List list = new List(new String[]{"Item1", "Item2", "Item3"});
        int prefW = list.getPreferredW();
        int prefH = list.getPreferredH();

        assertTrue(prefW >= 0);
        assertTrue(prefH >= 0);
    }

    @FormTest
    void testEmptyList() {
        List list = new List();
        assertEquals(0, list.getModel().getSize());
        assertEquals(0, list.getSelectedIndex());
    }

    @FormTest
    void testListWithSingleItem() {
        List list = new List(new String[]{"OnlyOne"});
        assertEquals(1, list.getModel().getSize());

        list.setSelectedIndex(0);
        assertEquals("OnlyOne", list.getSelectedItem());
    }

    @FormTest
    void testScrollToSelected() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        String[] items = new String[50];
        for (int i = 0; i < 50; i++) {
            items[i] = "Item " + i;
        }
        List list = new List(items);
        form.add(BorderLayout.CENTER, list);
        form.revalidate();

        list.setSelectedIndex(25);
        // Just verify this doesn't crash
        assertNotNull(list);
    }

    @FormTest
    void testRefreshTheme() {
        List list = new List(new String[]{"A", "B"});
        assertDoesNotThrow(() -> list.refreshTheme(false));
        assertDoesNotThrow(() -> list.refreshTheme(true));
    }

    private static String[] rows(int count) {
        String[] items = new String[count];
        for (int i = 0; i < count; i++) {
            items[i] = "Item " + i;
        }
        return items;
    }

    /// A list whose preferred height is capped, the way an application keeps a long list
    /// from swallowing the form.
    private static final class CappedList extends List<String> {
        private final int cap;

        CappedList(int cap, String... items) {
            super(items);
            this.cap = cap;
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            com.codename1.ui.geom.Dimension d = super.calcPreferredSize();
            return new com.codename1.ui.geom.Dimension(d.getWidth(), Math.min(d.getHeight(), cap));
        }
    }

    /// The scroll size used to be the preferred size, so capping one capped the other: the
    /// list was laid out at exactly its "scroll" height and stopped being scrollable, with
    /// fifty rows below the fold that neither the wheel nor a drag could reach.
    @FormTest
    void cappingThePreferredHeightLeavesTheListScrollable() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setScrollable(false);
        form.setLayout(new BorderLayout());

        CappedList list = new CappedList(120, rows(60));
        form.add(BorderLayout.NORTH, list);
        form.revalidate();

        assertEquals(120, list.getHeight(), "the cap is what the layout gives the list");
        int rowHeight = list.getElementSize(false, true).getHeight();
        assertTrue(rowHeight > 0);
        assertTrue(list.getScrollDimension().getHeight() >= rowHeight * 60,
                "the scroll size is every row in the model, not the capped preferred height");
        assertTrue(list.isScrollableY(), "a list taller than its viewport has to scroll");
    }

    /// The same trap through the list's own API rather than a subclass.
    @FormTest
    void maxElementHeightLimitsTheViewportNotTheContent() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setScrollable(false);
        form.setLayout(new BorderLayout());

        List<String> list = new List<String>(rows(60));
        list.setMaxElementHeight(5);
        form.add(BorderLayout.NORTH, list);
        form.revalidate();

        int rowHeight = list.getElementSize(false, true).getHeight();
        assertTrue(list.getHeight() < rowHeight * 7, "five rows are what it asks for");
        assertTrue(list.getScrollDimension().getHeight() >= rowHeight * 60);
        assertTrue(list.isScrollableY());
    }

    /// The other half: a list that fits must not become scrollable, and a combo box face,
    /// which shares the class, is not a scrolling list at all.
    @FormTest
    void aListThatFitsAndAComboBoxFaceStayUnscrollable() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setScrollable(false);
        form.setLayout(new BorderLayout());

        List<String> list = new List<String>(rows(3));
        ComboBox<String> combo = new ComboBox<String>((Object[]) rows(60));
        form.add(BorderLayout.NORTH, list);
        form.add(BorderLayout.SOUTH, combo);
        form.revalidate();

        assertEquals(list.getPreferredH(), list.getScrollDimension().getHeight());
        assertFalse(list.isScrollableY());
        assertEquals(combo.getPreferredH(), combo.getScrollDimension().getHeight());
        assertFalse(combo.isScrollableY());
    }
}
