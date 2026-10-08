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
package com.codename1.desktopcompat;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.DefaultComboBoxModel;
import com.codename1.desktopcompat.javax.swing.DefaultListModel;
import com.codename1.desktopcompat.javax.swing.DefaultListSelectionModel;
import com.codename1.desktopcompat.javax.swing.JComboBox;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.ListCellRenderer;
import com.codename1.desktopcompat.javax.swing.ListSelectionModel;
import com.codename1.desktopcompat.javax.swing.event.ListDataEvent;
import com.codename1.desktopcompat.javax.swing.event.ListDataListener;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Lists and combo boxes: the models and their events against the desktop
/// ones, the rows a custom renderer paints, and selection by clicking.
public class ListAndComboTest extends KernelTestBase {

    /// The click count of the layer is kept for the whole process and by
    /// time alone, so the clicks made here would be counted into the first
    /// click of whichever test runs next.
    @org.junit.AfterClass
    public static void letTheClicksLapse() throws InterruptedException {
        Thread.sleep(600);
    }

    // ------------------------------------------------------------ selection model

    private static String state(ListSelectionModel m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append(m.isSelectedIndex(i) ? '1' : '0');
        }
        return sb + " a" + m.getAnchorSelectionIndex() + " l" + m.getLeadSelectionIndex() + " min"
                + m.getMinSelectionIndex() + " max" + m.getMaxSelectionIndex() + " e" + m.isSelectionEmpty();
    }

    private static String state(javax.swing.ListSelectionModel m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append(m.isSelectedIndex(i) ? '1' : '0');
        }
        return sb + " a" + m.getAnchorSelectionIndex() + " l" + m.getLeadSelectionIndex() + " min"
                + m.getMinSelectionIndex() + " max" + m.getMaxSelectionIndex() + " e" + m.isSelectionEmpty();
    }

    /// Thousands of random operations in each mode; after every one the
    /// selection, anchor, lead and the events fired are the desktop's.
    @Test
    public void theSelectionModelBehavesLikeTheDesktopOne() {
        for (int mode = 0; mode < 3; mode++) {
            final List<String> oursLog = new ArrayList<String>();
            final List<String> realLog = new ArrayList<String>();
            DefaultListSelectionModel ours = new DefaultListSelectionModel();
            javax.swing.DefaultListSelectionModel real = new javax.swing.DefaultListSelectionModel();
            ours.setSelectionMode(mode);
            real.setSelectionMode(mode);
            ours.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    oursLog.add(e.getFirstIndex() + "-" + e.getLastIndex() + (e.getValueIsAdjusting() ? "~" : ""));
                }
            });
            real.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
                @Override
                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    realLog.add(e.getFirstIndex() + "-" + e.getLastIndex() + (e.getValueIsAdjusting() ? "~" : ""));
                }
            });
            Random r = new Random(42 + mode);
            for (int step = 0; step < 3000; step++) {
                int a = r.nextInt(22) - 1;
                int b = r.nextInt(22) - 1;
                int op = r.nextInt(11);
                String what = "mode " + mode + " step " + step + " op " + op + " (" + a + "," + b + ")";
                switch (op) {
                    case 0:
                        ours.setSelectionInterval(a, b);
                        real.setSelectionInterval(a, b);
                        break;
                    case 1:
                        ours.addSelectionInterval(a, b);
                        real.addSelectionInterval(a, b);
                        break;
                    case 2:
                        ours.removeSelectionInterval(a, b);
                        real.removeSelectionInterval(a, b);
                        break;
                    case 3:
                        ours.setLeadSelectionIndex(a);
                        real.setLeadSelectionIndex(a);
                        break;
                    case 4:
                        ours.setAnchorSelectionIndex(a);
                        real.setAnchorSelectionIndex(a);
                        break;
                    case 5:
                        if (r.nextInt(8) == 0) {
                            ours.clearSelection();
                            real.clearSelection();
                        }
                        break;
                    case 6:
                        if (a >= 0) {
                            int len = 1 + r.nextInt(3);
                            boolean before = r.nextBoolean();
                            ours.insertIndexInterval(a, len, before);
                            real.insertIndexInterval(a, len, before);
                        }
                        break;
                    case 7:
                        if (a >= 0 && b >= 0) {
                            ours.removeIndexInterval(a, b);
                            real.removeIndexInterval(a, b);
                        }
                        break;
                    case 8:
                        boolean adjusting = r.nextBoolean();
                        ours.setValueIsAdjusting(adjusting);
                        real.setValueIsAdjusting(adjusting);
                        break;
                    case 9:
                        ours.moveLeadSelectionIndex(a);
                        real.moveLeadSelectionIndex(a);
                        break;
                    default:
                        if (r.nextInt(20) == 0) {
                            ours.clearSelection();
                            real.clearSelection();
                            ours.setAnchorSelectionIndex(-1);
                            real.setAnchorSelectionIndex(-1);
                        }
                        break;
                }
                assertEquals(what, state(real), state(ours));
                assertEquals(what, realLog.toString(), oursLog.toString());
                oursLog.clear();
                realLog.clear();
            }
        }
        try {
            new DefaultListSelectionModel().setSelectionMode(7);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    // ------------------------------------------------------------ list model

    private static final class DataLog implements ListDataListener, javax.swing.event.ListDataListener {
        final List<String> events = new ArrayList<String>();

        @Override
        public void intervalAdded(ListDataEvent e) {
            events.add("add " + e.getIndex0() + "-" + e.getIndex1());
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            events.add("remove " + e.getIndex0() + "-" + e.getIndex1());
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            events.add("change " + e.getIndex0() + "-" + e.getIndex1());
        }

        @Override
        public void intervalAdded(javax.swing.event.ListDataEvent e) {
            events.add("add " + e.getIndex0() + "-" + e.getIndex1());
        }

        @Override
        public void intervalRemoved(javax.swing.event.ListDataEvent e) {
            events.add("remove " + e.getIndex0() + "-" + e.getIndex1());
        }

        @Override
        public void contentsChanged(javax.swing.event.ListDataEvent e) {
            events.add("change " + e.getIndex0() + "-" + e.getIndex1());
        }
    }

    @Test
    public void theListModelTellsItsListenersWhatTheDesktopOneDoes() {
        DefaultListModel<String> ours = new DefaultListModel<String>();
        javax.swing.DefaultListModel<String> real = new javax.swing.DefaultListModel<String>();
        DataLog oursLog = new DataLog();
        DataLog realLog = new DataLog();
        ours.addListDataListener(oursLog);
        real.addListDataListener(realLog);
        ours.addElement("a");
        real.addElement("a");
        ours.add(0, "b");
        real.add(0, "b");
        ours.insertElementAt("c", 1);
        real.insertElementAt("c", 1);
        ours.set(2, "A");
        real.set(2, "A");
        ours.setElementAt("B", 0);
        real.setElementAt("B", 0);
        assertEquals(real.removeElement("zzz"), ours.removeElement("zzz"));
        assertEquals(real.removeElement("c"), ours.removeElement("c"));
        ours.addElement("d");
        real.addElement("d");
        ours.addElement("e");
        real.addElement("e");
        ours.removeRange(1, 2);
        real.removeRange(1, 2);
        assertEquals(real.remove(0), ours.remove(0));
        ours.setSize(3);
        real.setSize(3);
        ours.clear();
        real.clear();
        ours.clear();
        real.clear();
        assertEquals(realLog.events.toString(), oursLog.events.toString());
        assertEquals(real.toString(), ours.toString());
        assertEquals(real.getSize(), ours.getSize());
    }

    // ------------------------------------------------------------ list

    private static final int ROW = 20;

    /// A renderer that is not a label: it draws its row itself.
    private static final class Drawn extends JComponent implements ListCellRenderer<String> {
        String text;
        final List<String> asked = new ArrayList<String>();

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            asked.add(index + (isSelected ? "*" : ""));
            text = (isSelected ? "[" + value + "]" : value) + "#" + index;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(90, ROW);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawString(text + " " + getWidth() + "x" + getHeight(), 0, 12);
        }
    }

    private JList<String> list;
    private DefaultListModel<String> model;
    private Drawn drawn;

    private JFrame listFrame(int rows, boolean scrolled) {
        model = new DefaultListModel<String>();
        for (int i = 0; i < rows; i++) {
            model.addElement("row" + i);
        }
        list = new JList<String>(model);
        drawn = new Drawn();
        list.setCellRenderer(drawn);
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        f.add(scrolled ? new JScrollPane(list) : list, BorderLayout.CENTER);
        show(f);
        return f;
    }

    @Test
    public void aCustomRendererPaintsEveryRowWhereItBelongs() {
        JFrame f = listFrame(3, false);
        list.setSelectedIndex(1);
        drawn.asked.clear();
        List<Object[]> text = paint(f);
        int w = list.getWidth();
        Object[] first = find(text, "row0#0 " + w + "x" + ROW);
        Object[] second = find(text, "[row1]#1 " + w + "x" + ROW);
        Object[] third = find(text, "row2#2 " + w + "x" + ROW);
        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);
        int y0 = ((Number) first[2]).intValue();
        assertEquals(y0 + Units.toDevice(ROW), ((Number) second[2]).intValue());
        assertEquals(y0 + Units.toDevice(2 * ROW), ((Number) third[2]).intValue());
        assertEquals(first[1], third[1]);
        assertTrue(drawn.asked.containsAll(Arrays.asList("0", "1*", "2")));
    }

    @Test
    public void theDefaultRendererShowsTheValues() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JList<String> plain = new JList<String>(new String[]{"alpha", "beta"});
        f.add(plain, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        Object[] a = find(text, "alpha");
        Object[] b = find(text, "beta");
        assertNotNull(a);
        assertNotNull(b);
        assertTrue(((Number) b[2]).intValue() > ((Number) a[2]).intValue());
        assertEquals(plain.getCellBounds(0, 0).height, plain.getCellBounds(1, 1).height);
        assertTrue(plain.getCellBounds(0, 0).height > 0);
    }

    @Test
    public void rowGeometry() {
        listFrame(5, false);
        assertEquals(new Dimension(90, 5 * ROW), list.getPreferredSize());
        assertEquals(new Rectangle(0, 2 * ROW, list.getWidth(), ROW), list.getCellBounds(2, 2));
        assertEquals(new Rectangle(0, ROW, list.getWidth(), 3 * ROW), list.getCellBounds(3, 1));
        assertNull(list.getCellBounds(0, 5));
        assertEquals(new Point(0, 4 * ROW), list.indexToLocation(4));
        assertEquals(0, list.locationToIndex(new Point(5, 0)));
        assertEquals(1, list.locationToIndex(new Point(5, ROW)));
        assertEquals(2, list.locationToIndex(new Point(5, 3 * ROW - 1)));
        assertEquals(4, list.locationToIndex(new Point(5, 5000)));
        assertEquals(-1, new JList<String>().locationToIndex(new Point(0, 0)));
        list.setFixedCellHeight(30);
        list.setFixedCellWidth(120);
        assertEquals(new Dimension(120, 150), list.getPreferredSize());
        list.setVisibleRowCount(4);
        assertEquals(new Dimension(120, 120), list.getPreferredScrollableViewportSize());
        model.remove(0);
        assertEquals(new Dimension(120, 120), list.getPreferredSize());
    }

    @Test
    public void clickingSelects() {
        JFrame f = listFrame(5, false);
        final List<String> events = new ArrayList<String>();
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                assertEquals(list, e.getSource());
                events.add(e.getFirstIndex() + "-" + e.getLastIndex());
            }
        });
        press(f, list, 10, 2 * ROW + 5);
        release(f, list, 10, 2 * ROW + 5);
        assertEquals(2, list.getSelectedIndex());
        assertEquals("row2", list.getSelectedValue());
        assertEquals("[2-2]", events.toString());
        press(f, list, 10, 5);
        release(f, list, 10, 5);
        assertEquals(0, list.getSelectedIndex());
        assertEquals("[2-2, 0-2]", events.toString());
    }

    @Test
    public void selectionApi() {
        listFrame(6, false);
        list.setSelectedIndices(new int[]{1, 3, 4, 99});
        assertArrayEquals(new int[]{1, 3, 4}, list.getSelectedIndices());
        assertEquals(Arrays.asList("row1", "row3", "row4"), list.getSelectedValuesList());
        assertEquals(3, list.getSelectedValues().length);
        assertEquals(1, list.getMinSelectionIndex());
        assertEquals(4, list.getMaxSelectionIndex());
        list.setSelectedValue("row5", false);
        assertEquals(5, list.getSelectedIndex());
        list.setSelectedValue("nothing", false);
        assertEquals(5, list.getSelectedIndex());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionInterval(1, 3);
        assertArrayEquals(new int[]{3}, list.getSelectedIndices());
        list.clearSelection();
        assertTrue(list.isSelectionEmpty());
        assertNull(list.getSelectedValue());
    }

    @Test
    public void theSelectionFollowsRowsAddedAndRemoved() {
        listFrame(4, false);
        list.setSelectedIndex(2);
        model.add(0, "new");
        assertEquals(3, list.getSelectedIndex());
        assertEquals("row2", list.getSelectedValue());
        model.remove(1);
        assertEquals(2, list.getSelectedIndex());
        model.remove(2);
        assertTrue(list.isSelectionEmpty());
    }

    @Test
    public void aListInAScrollPaneScrollsToARow() {
        listFrame(400, true);
        JScrollPane pane = (JScrollPane) list.getParent().getParent();
        int extent = pane.getViewport().getExtentSize().height;
        assertEquals(400 * ROW, list.getHeight());
        assertEquals(pane.getViewport().getExtentSize().width, list.getWidth());
        assertEquals(0, list.getFirstVisibleIndex());
        list.ensureIndexIsVisible(300);
        assertEquals(301 * ROW - extent, pane.getViewport().getViewPosition().y);
        assertEquals(300, list.getLastVisibleIndex());
        list.ensureIndexIsVisible(300);
        assertEquals(301 * ROW - extent, pane.getViewport().getViewPosition().y);
        list.setSelectedValue("row2", true);
        assertEquals(2 * ROW, pane.getViewport().getViewPosition().y);
        assertEquals(2, list.getFirstVisibleIndex());
    }

    @Test
    public void onlyTheRowsInViewArePainted() {
        JFrame f = listFrame(400, true);
        drawn.asked.clear();
        paint(f);
        assertTrue(drawn.asked.size() > 0);
        assertTrue("asked for " + drawn.asked.size(), drawn.asked.size() < 100);
    }

    // ------------------------------------------------------------ combo box

    /// One log of item and action events, fed by either toolkit.
    private static final class ComboLog implements ItemListener, ActionListener, java.awt.event.ItemListener,
            java.awt.event.ActionListener {
        final List<String> events = new ArrayList<String>();

        @Override
        public void itemStateChanged(ItemEvent e) {
            events.add((e.getStateChange() == ItemEvent.SELECTED ? "+" : "-") + e.getItem());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            events.add("action:" + e.getActionCommand());
        }

        @Override
        public void itemStateChanged(java.awt.event.ItemEvent e) {
            events.add((e.getStateChange() == java.awt.event.ItemEvent.SELECTED ? "+" : "-") + e.getItem());
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            events.add("action:" + e.getActionCommand());
        }
    }

    @Test
    public void theComboBoxFiresWhatTheDesktopOneFires() {
        JComboBox<String> ours = new JComboBox<String>();
        javax.swing.JComboBox<String> real = new javax.swing.JComboBox<String>();
        ComboLog oursLog = new ComboLog();
        ComboLog realLog = new ComboLog();
        ours.addItemListener(oursLog);
        ours.addActionListener(oursLog);
        real.addItemListener(realLog);
        real.addActionListener(realLog);
        Random r = new Random(7);
        String[] names = {"a", "b", "c", "d", "e", "f"};
        for (int step = 0; step < 2000; step++) {
            String name = names[r.nextInt(names.length)];
            int op = r.nextInt(9);
            int count = real.getItemCount();
            String what = "step " + step + " op " + op + " " + name;
            switch (op) {
                case 0:
                    ours.addItem(name);
                    real.addItem(name);
                    break;
                case 1:
                    ours.setSelectedItem(name);
                    real.setSelectedItem(name);
                    break;
                case 2:
                    int index = r.nextInt(count + 1) - 1;
                    ours.setSelectedIndex(index);
                    real.setSelectedIndex(index);
                    break;
                case 3:
                    ours.removeItem(name);
                    real.removeItem(name);
                    break;
                case 4:
                    if (count > 0) {
                        int at = r.nextInt(count);
                        ours.removeItemAt(at);
                        real.removeItemAt(at);
                    }
                    break;
                case 5:
                    int where = r.nextInt(count + 1);
                    ours.insertItemAt(name, where);
                    real.insertItemAt(name, where);
                    break;
                case 6:
                    if (r.nextInt(10) == 0) {
                        ours.removeAllItems();
                        real.removeAllItems();
                    }
                    break;
                case 7:
                    ours.setSelectedItem(null);
                    real.setSelectedItem(null);
                    break;
                default:
                    if (count > 6) {
                        ours.removeItemAt(0);
                        real.removeItemAt(0);
                    }
                    break;
            }
            assertEquals(what, realLog.events.toString(), oursLog.events.toString());
            assertEquals(what, real.getSelectedItem(), ours.getSelectedItem());
            assertEquals(what, real.getSelectedIndex(), ours.getSelectedIndex());
            assertEquals(what, real.getItemCount(), ours.getItemCount());
            oursLog.events.clear();
            realLog.events.clear();
        }
        try {
            ours.setSelectedIndex(999);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void anEditableComboBoxTakesAnItemItDoesNotList() {
        JComboBox<String> ours = new JComboBox<String>(new String[]{"a", "b"});
        javax.swing.JComboBox<String> real = new javax.swing.JComboBox<String>(new String[]{"a", "b"});
        ours.setSelectedItem("typed");
        real.setSelectedItem("typed");
        assertEquals(real.getSelectedItem(), ours.getSelectedItem());
        ours.setEditable(true);
        real.setEditable(true);
        ours.setSelectedItem("typed");
        real.setSelectedItem("typed");
        assertEquals(real.getSelectedItem(), ours.getSelectedItem());
        assertEquals(real.getSelectedIndex(), ours.getSelectedIndex());
        assertEquals(real.getActionCommand(), ours.getActionCommand());
        assertEquals(real.getMaximumRowCount(), ours.getMaximumRowCount());
    }

    @Test
    public void theWidgetFollowsTheModelAndThePopupSelects() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<String>(new String[]{"one", "two", "three"});
        JComboBox<String> combo = new JComboBox<String>(m);
        f.add(combo, BorderLayout.NORTH);
        show(f);
        com.codename1.ui.ComboBox<?> peer = (com.codename1.ui.ComboBox<?>) combo.cn1Peer();
        assertEquals(3, peer.getModel().getSize());
        assertEquals(0, peer.getSelectedIndex());
        assertTrue(combo.getHeight() > 0);
        combo.setSelectedItem("three");
        assertEquals(2, peer.getSelectedIndex());
        assertEquals("three", peer.getSelectedItem());
        m.addElement("four");
        assertEquals(4, peer.getModel().getSize());
        ComboLog log = new ComboLog();
        combo.addItemListener(log);
        combo.addActionListener(log);
        // What the popup does when a row is picked.
        peer.getModel().setSelectedIndex(1);
        assertEquals("two", combo.getSelectedItem());
        assertEquals("[-three, +two, action:comboBoxChanged]", log.events.toString());
        log.events.clear();
        peer.getModel().setSelectedIndex(1);
        assertTrue(log.events.isEmpty());
    }

    @Test
    public void theComboBoxIsPaintedWithItsRenderer() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JComboBox<String> combo = new JComboBox<String>(new String[]{"one", "two"});
        final Drawn renderer = new Drawn();
        combo.setRenderer(renderer);
        f.add(combo, BorderLayout.NORTH);
        show(f);
        combo.setSelectedIndex(1);
        List<Object[]> text = paint(f);
        boolean found = false;
        for (int i = 0; i < text.size(); i++) {
            found |= String.valueOf(text.get(i)[0]).contains("two");
        }
        assertTrue(found);
    }
}
