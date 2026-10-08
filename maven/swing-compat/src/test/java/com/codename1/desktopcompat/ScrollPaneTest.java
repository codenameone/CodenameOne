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
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.BoundedRangeModel;
import com.codename1.desktopcompat.javax.swing.DefaultBoundedRangeModel;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollBar;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.JViewport;
import com.codename1.desktopcompat.javax.swing.ScrollPaneConstants;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.Units;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The viewport arithmetic of a scroll pane, in logical units, and the
/// way it follows the Codename One container that really scrolls.
public class ScrollPaneTest extends KernelTestBase {

    private JPanel view;
    private JScrollPane pane;

    private JViewport shown(int viewW, int viewH) {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        view = new JPanel();
        view.setPreferredSize(new Dimension(viewW, viewH));
        pane = new JScrollPane(view);
        f.add(pane, BorderLayout.CENTER);
        show(f);
        return pane.getViewport();
    }

    @Test
    public void theViewKeepsItsPreferredSizeWhenItIsLarger() {
        JViewport vp = shown(2000, 3000);
        assertSame(view, vp.getView());
        assertEquals(new Dimension(2000, 3000), vp.getViewSize());
        Dimension extent = vp.getExtentSize();
        assertTrue(extent.width > 0 && extent.width < 2000);
        assertTrue(extent.height > 0 && extent.height < 3000);
        assertEquals(new Point(0, 0), vp.getViewPosition());
        assertEquals(new Rectangle(0, 0, extent.width, extent.height), vp.getViewRect());
    }

    @Test
    public void aSmallViewIsStretchedToTheViewport() {
        JViewport vp = shown(10, 10);
        assertEquals(vp.getExtentSize(), vp.getViewSize());
        assertEquals(vp.getExtentSize(), view.getSize());
    }

    @Test
    public void movingTheViewMovesTheComponentAndTheNativeScroll() {
        JViewport vp = shown(2000, 3000);
        final int[] changes = {0};
        vp.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                changes[0]++;
            }
        });
        vp.setViewPosition(new Point(100, 250));
        assertEquals(new Point(100, 250), vp.getViewPosition());
        assertEquals(new Point(-100, -250), view.getLocation());
        assertEquals(1, changes[0]);
        com.codename1.ui.Component peer = vp.cn1Peer();
        assertEquals(Units.toDevice(100), peer.getScrollX());
        assertEquals(Units.toDevice(250), peer.getScrollY());
        vp.setViewPosition(new Point(100, 250));
        assertEquals(1, changes[0]);
    }

    @Test
    public void theBarsDescribeTheViewport() {
        JViewport vp = shown(2000, 3000);
        Dimension extent = vp.getExtentSize();
        JScrollBar v = pane.getVerticalScrollBar();
        JScrollBar h = pane.getHorizontalScrollBar();
        assertEquals(0, v.getMinimum());
        assertEquals(3000, v.getMaximum());
        assertEquals(extent.height, v.getVisibleAmount());
        assertEquals(2000, h.getMaximum());
        assertEquals(extent.width, h.getVisibleAmount());
        vp.setViewPosition(new Point(40, 70));
        assertEquals(70, v.getValue());
        assertEquals(40, h.getValue());
    }

    @Test
    public void movingABarMovesTheView() {
        JViewport vp = shown(2000, 3000);
        pane.getVerticalScrollBar().setValue(500);
        assertEquals(new Point(0, 500), vp.getViewPosition());
        pane.getHorizontalScrollBar().setValue(30);
        assertEquals(new Point(30, 500), vp.getViewPosition());
        pane.getVerticalScrollBar().setValue(999999);
        assertEquals(3000 - vp.getExtentSize().height, vp.getViewPosition().y);
    }

    @Test
    public void aNativeScrollMovesTheView() throws Exception {
        JViewport vp = shown(2000, 3000);
        // What a finger does: Codename One moves the scroll of the container
        // itself, and nothing on the Swing side asked for it.
        java.lang.reflect.Method x = com.codename1.ui.Component.class.getDeclaredMethod("setScrollX", int.class);
        java.lang.reflect.Method y = com.codename1.ui.Component.class.getDeclaredMethod("setScrollY", int.class);
        x.setAccessible(true);
        y.setAccessible(true);
        x.invoke(vp.cn1Peer(), Integer.valueOf(Units.toDevice(60)));
        y.invoke(vp.cn1Peer(), Integer.valueOf(Units.toDevice(120)));
        assertEquals(new Point(60, 120), vp.getViewPosition());
        assertEquals(120, pane.getVerticalScrollBar().getValue());
        assertEquals(new Point(-60, -120), view.getLocation());
    }

    @Test
    public void scrollRectToVisibleMovesOnlyAsFarAsNeeded() {
        JViewport vp = shown(2000, 3000);
        Dimension extent = vp.getExtentSize();
        vp.scrollRectToVisible(new Rectangle(0, 0, 10, 10));
        assertEquals(new Point(0, 0), vp.getViewPosition());
        vp.scrollRectToVisible(new Rectangle(0, extent.height + 90, 10, 10));
        assertEquals(new Point(0, 100), vp.getViewPosition());
        JViewport.cn1ScrollRectToVisible(view, new Rectangle(0, 0, 10, 10));
        assertEquals(new Point(0, 0), vp.getViewPosition());
        JViewport.cn1ScrollRectToVisible(view, new Rectangle(extent.width + 5, 2000, 10, 10));
        assertEquals(new Point(15, 2010 - extent.height), vp.getViewPosition());
    }

    @Test
    public void aPolicyOfNeverSwitchesTheAxisOff() {
        JViewport vp = shown(2000, 3000);
        com.codename1.ui.Component peer = vp.cn1Peer();
        assertTrue(peer.isScrollableX());
        assertTrue(peer.isScrollableY());
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertFalse(peer.isScrollableX());
        assertTrue(peer.isScrollableY());
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, pane.getHorizontalScrollBarPolicy());
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, pane.getVerticalScrollBarPolicy());
    }

    @Test
    public void headersTakeTheirPreferredStripAndFollowTheView() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        view = new JPanel();
        view.setPreferredSize(new Dimension(2000, 3000));
        pane = new JScrollPane(view);
        JPanel column = new JPanel();
        column.setPreferredSize(new Dimension(2000, 20));
        JPanel row = new JPanel();
        row.setPreferredSize(new Dimension(30, 3000));
        pane.setColumnHeaderView(column);
        pane.setRowHeaderView(row);
        f.add(pane, BorderLayout.CENTER);
        show(f);
        JViewport vp = pane.getViewport();
        assertEquals(20, pane.getColumnHeader().getHeight());
        assertEquals(30, pane.getRowHeader().getWidth());
        assertEquals(30, vp.getX());
        assertEquals(20, vp.getY());
        assertEquals(pane.getWidth() - 30, vp.getWidth());
        assertEquals(pane.getHeight() - 20, vp.getHeight());
        vp.setViewPosition(new Point(50, 80));
        assertEquals(new Point(50, 0), pane.getColumnHeader().getViewPosition());
        assertEquals(new Point(0, 80), pane.getRowHeader().getViewPosition());
    }

    @Test
    public void aTextAreaTracksTheWidthOfItsViewport() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JTextArea area = new JTextArea("x", 5, 20);
        pane = new JScrollPane(area);
        f.add(pane, BorderLayout.CENTER);
        show(f);
        assertEquals(pane.getViewport().getExtentSize().width, area.getWidth());
    }

    @Test
    public void theViewCanBeReplaced() {
        JViewport vp = shown(2000, 3000);
        vp.setViewPosition(new Point(0, 100));
        JLabel other = new JLabel("other");
        pane.setViewportView(other);
        assertSame(other, vp.getView());
        assertEquals(1, vp.getComponentCount());
        assertEquals(null, view.getParent());
    }

    @Test
    public void theRangeModelClampsLikeTheDesktopOne() {
        BoundedRangeModel ours = new DefaultBoundedRangeModel(5, 10, 0, 100);
        javax.swing.BoundedRangeModel real = new javax.swing.DefaultBoundedRangeModel(5, 10, 0, 100);
        int[][] steps = {{95, 10, 0, 100}, {50, 80, 0, 100}, {-5, 10, 0, 100}, {20, 10, 30, 25}, {7, -1, 0, 5}};
        for (int[] s : steps) {
            ours.setRangeProperties(s[0], s[1], s[2], s[3], false);
            real.setRangeProperties(s[0], s[1], s[2], s[3], false);
            assertEquals(real.getValue(), ours.getValue());
            assertEquals(real.getExtent(), ours.getExtent());
            assertEquals(real.getMinimum(), ours.getMinimum());
            assertEquals(real.getMaximum(), ours.getMaximum());
        }
        ours.setValue(1000);
        real.setValue(1000);
        assertEquals(real.getValue(), ours.getValue());
        ours.setExtent(1000);
        real.setExtent(1000);
        assertEquals(real.getExtent(), ours.getExtent());
        ours.setMinimum(500);
        real.setMinimum(500);
        assertEquals(real.getValue(), ours.getValue());
        assertEquals(real.getMaximum(), ours.getMaximum());
    }

    @Test
    public void aScrollBarStepsByItsIncrements() {
        JScrollBar bar = new JScrollBar(JScrollBar.VERTICAL, 0, 10, 0, 100);
        javax.swing.JScrollBar real = new javax.swing.JScrollBar(javax.swing.JScrollBar.VERTICAL, 0, 10, 0, 100);
        assertEquals(real.getUnitIncrement(), bar.getUnitIncrement());
        assertEquals(real.getBlockIncrement(), bar.getBlockIncrement());
        bar.setValue(95);
        real.setValue(95);
        assertEquals(real.getValue(), bar.getValue());
        assertEquals(real.getOrientation(), bar.getOrientation());
    }
}
