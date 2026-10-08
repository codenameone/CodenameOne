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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import com.codename1.ui.Display;
import org.junit.After;
import org.junit.Test;

/// The scroll bars, corners and headers of a scroll pane, and what the
/// mouse wheel does over one.
public class ScrollPaneBarsTest extends KernelTestBase {

    private static final int BAR = 14;

    @After
    public void deviceDecidesAgain() {
        ScrollDelegate.setBarThickness(-1);
    }

    private static JPanel panel(int w, int h) {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    private JFrame frame(JScrollPane pane) {
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        return f;
    }

    @Test
    public void aTouchDeviceGivesTheBarsNoRoom() {
        JScrollPane pane = new JScrollPane(panel(100, 2000));
        frame(pane);
        assertEquals(0, pane.getVerticalScrollBar().getWidth());
        assertEquals(pane.getWidth(), pane.getViewport().getWidth());
        assertSame(pane, pane.getVerticalScrollBar().getParent());
    }

    @Test
    public void aDesktopShowsTheBarTheViewNeeds() {
        ScrollDelegate.setBarThickness(BAR);
        JScrollPane pane = new JScrollPane(panel(100, 2000));
        frame(pane);
        JScrollBar v = pane.getVerticalScrollBar();
        JScrollBar h = pane.getHorizontalScrollBar();
        assertEquals(new Rectangle(pane.getWidth() - BAR, 0, BAR, pane.getHeight()), v.getBounds());
        assertEquals(0, h.getHeight());
        assertEquals(pane.getWidth() - BAR, pane.getViewport().getWidth());
        assertEquals(pane.getHeight(), pane.getViewport().getHeight());
        assertEquals(2000, v.getMaximum());
        assertEquals(pane.getHeight(), v.getVisibleAmount());
    }

    @Test
    public void aViewThatFitsShowsNoBarUnlessThePolicyIsAlways() {
        ScrollDelegate.setBarThickness(BAR);
        JScrollPane pane = new JScrollPane(panel(50, 50));
        frame(pane);
        assertEquals(0, pane.getVerticalScrollBar().getWidth());
        assertEquals(0, pane.getHorizontalScrollBar().getHeight());
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.validate();
        assertEquals(BAR, pane.getVerticalScrollBar().getWidth());
        assertEquals(BAR, pane.getHorizontalScrollBar().getHeight());
        assertEquals(pane.getHeight() - BAR, pane.getVerticalScrollBar().getHeight());
        assertEquals(pane.getWidth() - BAR, pane.getViewport().getWidth());
        assertEquals(pane.getHeight() - BAR, pane.getViewport().getHeight());
    }

    @Test
    public void oneBarCanMakeTheOtherNecessary() {
        ScrollDelegate.setBarThickness(BAR);
        JScrollPane pane = new JScrollPane();
        JFrame f = frame(pane);
        // As high as the pane: fits until the horizontal bar takes its room.
        pane.setViewportView(panel(2000, pane.getHeight() - 4));
        f.validate();
        assertEquals(BAR, pane.getHorizontalScrollBar().getHeight());
        assertEquals(BAR, pane.getVerticalScrollBar().getWidth());
    }

    @Test
    public void aWrappedTextAreaGetsItsBarFromTheHeightItReallyHas() {
        ScrollDelegate.setBarThickness(BAR);
        JTextArea area = new JTextArea();
        area.setLineWrap(true);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("line ").append(i).append('\n');
        }
        area.setText(sb.toString());
        JScrollPane pane = new JScrollPane(area);
        frame(pane);
        assertEquals(BAR, pane.getVerticalScrollBar().getWidth());
        assertEquals(pane.getWidth() - BAR, area.getWidth());
        assertEquals(0, pane.getHorizontalScrollBar().getHeight());
    }

    @Test
    public void draggingTheThumbScrollsTheView() {
        ScrollDelegate.setBarThickness(BAR);
        JScrollPane pane = new JScrollPane(panel(100, 2000));
        JFrame f = frame(pane);
        JScrollBar v = pane.getVerticalScrollBar();
        press(f, v, 5, 5);
        drag(f, v, 5, 45);
        drag(f, v, 5, 85);
        release(f, v, 5, 85);
        int y = pane.getViewport().getViewPosition().y;
        // 80 pixels of a track as high as the pane, over a view of 2000.
        int expected = 80 * (2000 - v.getHeight()) / (v.getHeight() - Math.max(16, v.getHeight() * v.getHeight() / 2000));
        assertTrue("scrolled by the drag: " + y + " for " + expected, Math.abs(y - expected) <= 4);
        assertEquals(y, v.getValue());
        assertFalse(v.getValueIsAdjusting());
        // A press on the track below the thumb moves a block.
        press(f, v, 5, v.getHeight() - 2);
        release(f, v, 5, v.getHeight() - 2);
        assertEquals(Math.min(y + pane.getViewport().getExtentSize().height, 2000 - pane.getViewport().getExtentSize().height),
                pane.getViewport().getViewPosition().y);
    }

    @Test
    public void theUpperTrailingCornerSitsBesideTheColumnHeader() {
        JTable t = new JTable(new DefaultTableModel(50, 3));
        JScrollPane pane = new JScrollPane(t);
        JButton control = new JButton("+");
        control.setPreferredSize(new Dimension(20, 20));
        pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, control);
        JFrame f = frame(pane);
        int headH = pane.getColumnHeader().getHeight();
        assertTrue(headH > 0);
        assertSame(pane, control.getParent());
        assertSame(control, pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER));
        // No bar on a touch device: the corner has its own width.
        assertEquals(new Rectangle(pane.getWidth() - 20, 0, 20, headH), control.getBounds());
        assertEquals(pane.getWidth() - 20, pane.getColumnHeader().getWidth());
        assertEquals(pane.getWidth(), pane.getViewport().getWidth());

        ScrollDelegate.setBarThickness(BAR);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        f.validate();
        assertEquals(new Rectangle(pane.getWidth() - BAR, 0, BAR, headH), control.getBounds());
        assertEquals(pane.getViewport().getWidth(), pane.getColumnHeader().getWidth());
        assertEquals(headH, pane.getVerticalScrollBar().getY());

        pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, null);
        assertEquals(null, control.getParent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void anUnknownCornerIsRefused() {
        new JScrollPane().setCorner("MIDDLE", new JPanel());
    }

    @Test
    public void theHeadersFollowTheView() {
        JScrollPane pane = new JScrollPane(panel(2000, 2000));
        pane.setRowHeaderView(panel(30, 2000));
        pane.setColumnHeaderView(panel(2000, 20));
        JPanel corner = new JPanel();
        pane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner);
        frame(pane);
        assertEquals(new Rectangle(0, 0, 30, 20), corner.getBounds());
        pane.getViewport().setViewPosition(new Point(120, 340));
        assertEquals(new Point(0, 340), pane.getRowHeader().getViewPosition());
        assertEquals(new Point(120, 0), pane.getColumnHeader().getViewPosition());
        assertEquals(-340, pane.getRowHeader().getView().getY());
        assertEquals(-120, pane.getColumnHeader().getView().getX());
        pane.getVerticalScrollBar().setValue(700);
        assertEquals(new Point(0, 700), pane.getRowHeader().getViewPosition());
        assertEquals(700, pane.getViewport().getViewPosition().y);
    }

    // ------------------------------------------------------------ wheel

    private static void turnWheel(JFrame f, com.codename1.desktopcompat.java.awt.Component over, int deltaY) {
        int[] at = onDisplay(over, 10, 10);
        Display.getInstance().fireMouseWheelEvent(at[0], at[1], 0, deltaY, false, 0);
    }

    private static int[] changes(JScrollPane pane) {
        final int[] n = {0};
        pane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                n[0]++;
            }
        });
        return n;
    }

    @Test
    public void theWheelScrollsAPaneOnce() {
        JPanel view = panel(100, 2000);
        JScrollPane pane = new JScrollPane(view);
        JFrame f = frame(pane);
        int[] changes = changes(pane);
        turnWheel(f, pane, -40);
        int y = pane.getViewport().getViewPosition().y;
        assertTrue("the wheel scrolled down: " + y, y > 0);
        assertEquals("one movement for one turn of the wheel", 1, changes[0]);
        assertEquals(y, pane.getVerticalScrollBar().getValue());
        turnWheel(f, pane, 40);
        assertEquals("and back up", 0, pane.getViewport().getViewPosition().y);
        assertEquals(2, changes[0]);
    }

    @Test
    public void aWheelListenerGetsTheEventInsteadOfTheScrolling() {
        JPanel view = panel(100, 2000);
        JScrollPane pane = new JScrollPane(view);
        JFrame f = frame(pane);
        final int[] heard = {0, 0};
        view.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                heard[0]++;
                heard[1] = e.getWheelRotation();
            }
        });
        turnWheel(f, pane, -40);
        assertEquals(1, heard[0]);
        assertEquals(1, heard[1]);
        assertEquals("zooming with the wheel must not scroll as well", 0, pane.getViewport().getViewPosition().y);
        turnWheel(f, pane, 40);
        assertEquals(2, heard[0]);
        assertEquals(-1, heard[1]);
    }

    @Test
    public void aListenerThatPassesTheEventOnScrollsThePaneOnce() {
        final JPanel view = panel(100, 2000);
        JScrollPane pane = new JScrollPane(view);
        JFrame f = frame(pane);
        view.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!e.isControlDown()) {
                    for (com.codename1.desktopcompat.java.awt.Container p = view.getParent(); p != null;
                            p = p.getParent()) {
                        if (p instanceof JScrollPane) {
                            p.dispatchEvent(e);
                        }
                    }
                }
            }
        });
        int[] changes = changes(pane);
        turnWheel(f, pane, -40);
        int unit = pane.getVerticalScrollBar().getUnitIncrement(1);
        assertEquals(3 * unit, pane.getViewport().getViewPosition().y);
        assertEquals(1, changes[0]);
    }

    @Test
    public void wheelScrollingCanBeTurnedOff() {
        JScrollPane pane = new JScrollPane(panel(100, 2000));
        JFrame f = frame(pane);
        pane.setWheelScrollingEnabled(false);
        turnWheel(f, pane, -40);
        assertEquals(0, pane.getViewport().getViewPosition().y);
        pane.setWheelScrollingEnabled(true);
        turnWheel(f, pane, -40);
        assertTrue(pane.getViewport().getViewPosition().y > 0);
    }
}
