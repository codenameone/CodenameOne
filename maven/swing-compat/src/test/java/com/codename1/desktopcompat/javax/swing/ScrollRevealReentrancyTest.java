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
package com.codename1.desktopcompat.javax.swing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import org.junit.Test;

/// Showing a rectangle must never start the layout it is called from a
/// second time: a text area given its text inside a scroll pane that was
/// never laid out used to overflow the stack on the pane's first layout.
public class ScrollRevealReentrancyTest extends KernelTestBase {

    /// What is asserted here is the geometry of the pane's own layout, to
    /// the pixel, so the line a pane has around it by default is taken
    /// away, the way an application does.
    @org.junit.Before
    public void noPaneBorder() {
        com.codename1.desktopcompat.javax.swing.UIManager.put("ScrollPane.border",
                new com.codename1.desktopcompat.javax.swing.border.EmptyBorder(0, 0, 0, 0));
    }

    @org.junit.After
    public void themePaneBorder() {
        com.codename1.desktopcompat.javax.swing.UIManager.put("ScrollPane.border", null);
    }

    private static String lines(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("line ").append(i).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void textSetInAPaneThatWasNeverLaidOutLaysOutOnce() {
        JTextArea area = new JTextArea();
        JScrollPane pane = new JScrollPane(area);
        area.setText(lines(200));
        pane.setSize(200, 100);
        pane.validate();
        assertTrue(area.getHeight() > 100);
        assertEquals("the caret is at the end, and the end shows", area.getHeight() - 100,
                pane.getViewport().getViewPosition().y);
    }

    @Test
    public void textSetBeforeTheFrameIsShownFollowsTheCaret() {
        JTextArea area = new JTextArea();
        JScrollPane pane = new JScrollPane(area);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        area.setText(lines(200));
        f.setSize(200, 150);
        show(f);
        f.validate();
        int extent = pane.getViewport().getExtentSize().height;
        assertTrue(area.getHeight() > extent);
        assertEquals(area.getHeight() - extent, pane.getViewport().getViewPosition().y);
        area.setCaretPosition(0);
        assertEquals(0, pane.getViewport().getViewPosition().y);
    }

    @Test
    public void appendingToAShownAreaKeepsFollowing() {
        JTextArea area = new JTextArea();
        JScrollPane pane = new JScrollPane(area);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        f.setSize(200, 150);
        show(f);
        f.validate();
        for (int i = 0; i < 100; i++) {
            area.append("row " + i + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        }
        f.validate();
        int extent = pane.getViewport().getExtentSize().height;
        assertTrue(area.getHeight() > extent);
        assertEquals(area.getHeight() - extent, pane.getViewport().getViewPosition().y);
    }

    @Test
    public void revealingFromInsideALayoutDoesNotLayOutAgain() {
        final int[] layouts = {0};
        JPanel view = new JPanel() {
            @Override
            public void doLayout() {
                layouts[0]++;
                super.doLayout();
                scrollRectToVisible(new Rectangle(0, 900, 10, 10));
            }
        };
        view.setPreferredSize(new Dimension(100, 1000));
        JScrollPane pane = new JScrollPane(view);
        pane.setSize(100, 100);
        pane.validate();
        assertEquals(1, layouts[0]);
        assertEquals(810, pane.getViewport().getViewPosition().y);
    }

    @Test
    public void aListAndATableRevealInAPaneThatWasNeverLaidOut() {
        DefaultListModel<String> m = new DefaultListModel<String>();
        for (int i = 0; i < 300; i++) {
            m.addElement("item " + i);
        }
        JList<String> list = new JList<String>(m);
        JScrollPane pane = new JScrollPane(list);
        list.ensureIndexIsVisible(250);
        pane.setSize(100, 100);
        pane.validate();
        list.ensureIndexIsVisible(250);
        Rectangle r = list.getCellBounds(250, 250);
        int y = pane.getViewport().getViewPosition().y;
        assertTrue(r.y >= y && r.y + r.height <= y + 100);

        JTable t = new JTable(new DefaultTableModel(300, 2));
        JScrollPane tp = new JScrollPane(t);
        t.scrollRectToVisible(t.getCellRect(250, 0, true));
        tp.setSize(100, 200);
        tp.validate();
        t.scrollRectToVisible(t.getCellRect(250, 0, true));
        assertTrue(tp.getViewport().getViewPosition().y > 0);
    }
}
