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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Canvas;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import java.util.List;
import org.junit.Test;

/// Who paints what: custom `paintComponent` code, the Codename One widget
/// behind a standard component, and neither when an override skips
/// `super.paintComponent`.
public class PaintRoutingTest extends KernelTestBase {

    @Test
    public void aCustomPaintComponentDrawsInLogicalCoordinates() {
        final int[] paints = new int[1];
        final boolean[] graphics2d = new boolean[1];
        final Color[] start = new Color[1];
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paints[0]++;
                graphics2d[0] = g instanceof Graphics2D;
                start[0] = g.getColor();
                g.drawString("custom", 10, 20);
            }
        };
        panel.setForeground(Color.RED);
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        assertEquals(1, paints[0]);
        assertTrue(graphics2d[0]);
        Object[] drawn = find(text, "custom");
        assertNotNull(drawn);
        assertEquals(onDisplay(panel, 10, 0)[0], ((Integer) drawn[1]).intValue());
    }

    @Test
    public void theDefaultPaintComponentDrawsTheWidget() {
        JButton b = new JButton("Go");
        JLabel l = new JLabel("Name");
        JFrame f = new JFrame();
        f.add(b, BorderLayout.SOUTH);
        f.add(l, BorderLayout.NORTH);
        show(f);
        List<Object[]> text = paint(f);
        assertNotNull(find(text, "Go"));
        Object[] name = find(text, "Name");
        assertNotNull(name);
        int x = ((Integer) name[1]).intValue();
        assertTrue(x >= l.cn1Peer().getAbsoluteX() && x < l.cn1Peer().getAbsoluteX() + l.cn1Peer().getWidth());
    }

    @Test
    public void skippingSuperPaintComponentLeavesTheWidgetOut() {
        JButton b = new JButton("Go") {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawString("mine", 2, 12);
            }
        };
        JFrame f = new JFrame();
        f.add(b, BorderLayout.SOUTH);
        show(f);
        List<Object[]> text = paint(f);
        assertNull(find(text, "Go"));
        assertNotNull(find(text, "mine"));
    }

    @Test
    public void callingSuperPaintComponentDrawsTheWidgetUnderCustomPainting() {
        JButton b = new JButton("Go") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawString("badge", 2, 12);
            }
        };
        JFrame f = new JFrame();
        f.add(b, BorderLayout.SOUTH);
        show(f);
        List<Object[]> text = paint(f);
        int go = text.indexOf(find(text, "Go"));
        int badge = text.indexOf(find(text, "badge"));
        assertTrue(go >= 0 && badge > go);
    }

    @Test
    public void childrenArePaintedAfterTheirParentAndCanvasPaintRuns() {
        final StringBuilder order = new StringBuilder();
        Canvas canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                order.append("canvas ");
                g.drawString("canvas", 1, 10);
            }
        };
        canvas.setPreferredSize(new Dimension(50, 20));
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                order.append("panel ");
            }
        };
        panel.add(canvas);
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        assertEquals("panel canvas ", order.toString());
        Object[] drawn = find(text, "canvas");
        assertNotNull(drawn);
        assertEquals(onDisplay(canvas, 1, 0)[0], ((Integer) drawn[1]).intValue());
    }

    @Test
    public void anInvisibleComponentIsNotPainted() {
        JButton b = new JButton("Hidden");
        b.setVisible(false);
        JFrame f = new JFrame();
        f.add(b, BorderLayout.SOUTH);
        show(f);
        assertNull(find(paint(f), "Hidden"));
    }
}
