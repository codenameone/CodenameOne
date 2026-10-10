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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.GridLayout;
import com.codename1.desktopcompat.java.awt.event.WindowAdapter;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.rt.ContainerPeer;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Display;
import org.junit.Test;

/// Showing a frame, and the bounds the layout managers give components
/// and their peers once it is shown.
public class FrameAndLayoutTest extends KernelTestBase {

    @Test
    public void showingAFrameShowsItsFormAndSizesTheWindowToIt() {
        JFrame f = new JFrame("Title");
        final int[] opened = new int[1];
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                opened[0]++;
            }
        });
        assertFalse(f.isVisible());
        show(f);
        assertTrue(f.isVisible());
        assertTrue(f.isShowing());
        assertSame(f.cn1Form(), Display.getInstance().getCurrent());
        assertEquals("Title", f.cn1Form().getTitle());
        assertEquals(1, opened[0]);
        com.codename1.ui.Component peer = f.cn1Peer();
        assertTrue(peer.getWidth() > 0 && peer.getHeight() > 0);
        assertEquals(Units.toLogical(peer.getWidth()), f.getWidth());
        assertEquals(Units.toLogical(peer.getHeight()), f.getHeight());
        assertEquals(f.getWidth(), f.getContentPane().getWidth());
        assertEquals(f.getHeight(), f.getContentPane().getHeight());
    }

    @Test
    public void borderLayoutPlacesComponentsAndPeers() {
        JFrame f = new JFrame();
        JButton south = new JButton("South");
        JPanel center = new JPanel();
        JLabel north = new JLabel("North");
        f.add(south, BorderLayout.SOUTH);
        f.add(center, BorderLayout.CENTER);
        f.getContentPane().add(north, BorderLayout.NORTH);
        show(f);
        int w = f.getContentPane().getWidth();
        int h = f.getContentPane().getHeight();
        Dimension sp = south.getPreferredSize();
        Dimension np = north.getPreferredSize();
        assertTrue(sp.height > 0 && np.height > 0);
        assertEquals(0, north.getY());
        assertEquals(w, north.getWidth());
        assertEquals(np.height, north.getHeight());
        assertEquals(h - sp.height, south.getY());
        assertEquals(w, south.getWidth());
        assertEquals(np.height, center.getY());
        assertEquals(h - sp.height - np.height, center.getHeight());

        com.codename1.ui.Component sPeer = south.cn1Peer();
        assertEquals(Units.toDevice(south.getY()), sPeer.getY());
        assertEquals(Units.toDeviceSize(south.getY(), south.getHeight()), sPeer.getHeight());
        assertEquals(Units.toDevice(w), sPeer.getWidth());
        assertSame(f.getContentPane().cn1Peer(), sPeer.getParent());
    }

    @Test
    public void aWidgetsPreferredSizeIsItsPeersInLogicalPixels() {
        JButton b = new JButton("Preferred");
        com.codename1.ui.Component peer = b.cn1Peer();
        Dimension d = b.getPreferredSize();
        assertEquals(Units.toLogicalCeil(peer.getPreferredW()), d.width);
        assertEquals(Units.toLogicalCeil(peer.getPreferredH()), d.height);
        b.setPreferredSize(new Dimension(120, 30));
        assertEquals(new Dimension(120, 30), b.getPreferredSize());
    }

    @Test
    public void flowAndGridLayoutsWorkInLogicalPixels() {
        JFrame f = new JFrame();
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 7));
        JPanel a = sized(40, 20);
        JPanel b = sized(60, 30);
        flow.add(a);
        flow.add(b);
        JPanel grid = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel g1 = new JPanel();
        JPanel g2 = new JPanel();
        grid.add(g1);
        grid.add(g2);
        grid.setPreferredSize(new Dimension(100, 50));
        f.add(flow, BorderLayout.CENTER);
        f.add(grid, BorderLayout.SOUTH);
        show(f);
        assertEquals(5, a.getX());
        assertEquals(40, a.getWidth());
        assertEquals(50, b.getX());
        assertEquals(60, b.getWidth());
        assertEquals(7, b.getY());
        assertEquals(12, a.getY());
        assertEquals(50, grid.getHeight());
        int cell = (grid.getWidth() - 10) / 2;
        assertEquals(cell, g1.getWidth());
        assertEquals(cell, g2.getWidth());
        assertEquals(g1.getX() + cell + 10, g2.getX());
        assertEquals(Units.toDevice(a.getX()), a.cn1Peer().getX());
        assertEquals(Units.toDevice(40), a.cn1Peer().getWidth());
    }

    @Test
    public void peersAreStackedWithTheFirstChildOnTop() {
        JPanel p = new JPanel(null);
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        p.add(first);
        p.add(second);
        ContainerPeer peer = (ContainerPeer) p.cn1Peer();
        assertSame(second.cn1Peer(), peer.getComponentAt(0));
        assertSame(first.cn1Peer(), peer.getComponentAt(1));
        JPanel third = new JPanel();
        p.add(third, 0);
        assertSame(third.cn1Peer(), peer.getComponentAt(2));
        p.remove(first);
        assertEquals(2, peer.getComponentCount());
        assertNotNull(first.cn1Peer());
    }

    @Test
    public void revalidateLaysOutAComponentAddedLater() {
        JFrame f = new JFrame();
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        f.setContentPane(content);
        show(f);
        JPanel late = sized(30, 10);
        content.add(late);
        assertEquals(0, late.getWidth());
        content.revalidate();
        com.codename1.compat.testing.MainThreadRule.drain();
        f.cn1Form().revalidate();
        assertEquals(30, late.getWidth());
        assertEquals(Units.toDevice(30), late.cn1Peer().getWidth());
    }

    private static JPanel sized(int w, int h) {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }
}
