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

import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.JToolBar;
import com.codename1.desktopcompat.rt.RootPan;
import com.codename1.desktopcompat.rt.Units;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A window on a display that is smaller than a desktop: content that can
/// shrink gets the size of the display, content that cannot is panned
/// instead of cut off, and a tool bar goes on in a further row.
///
/// The test display is 1080 by 1920 device pixels at a scale of two, so
/// 540 by 960 logical pixels.
public class SmallDisplayTest extends KernelTestBase {

    private static RootPan pan(JFrame f) {
        com.codename1.ui.Component p = f.cn1Peer().getParent();
        assertTrue(p instanceof RootPan);
        return (RootPan) p;
    }

    /// A text area in a scroll pane can be any size: the window is as
    /// wide as the display and nothing pans.
    @Test
    public void contentThatCanShrinkGetsTheSizeOfTheDisplay() {
        JFrame f = new JFrame();
        JTextArea area = new JTextArea(40, 200);
        f.getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        f.setSize(1280, 800);
        show(f);
        RootPan pan = pan(f);
        assertEquals(Units.toLogical(pan.getWidth()), f.getWidth());
        assertEquals(Units.toLogical(pan.getHeight()), f.getHeight());
        assertTrue(f.getWidth() <= 540);
        assertFalse(pan.isScrollableX());
        assertFalse(pan.isScrollableY());
    }

    /// Content with a minimum width beyond the display keeps that width
    /// and the root pans across it; its height still follows the display.
    /// A press at the far end, after panning there, reaches the component
    /// that is there.
    @Test
    public void contentThatCannotShrinkIsPannedNotCutOff() {
        JFrame f = new JFrame();
        JPanel fixed = new JPanel(new BorderLayout());
        fixed.setMinimumSize(new Dimension(900, 100));
        final int[] presses = {0};
        JButton far = new JButton("far");
        far.addMouseListener(new com.codename1.desktopcompat.java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(com.codename1.desktopcompat.java.awt.event.MouseEvent e) {
                presses[0]++;
            }
        });
        fixed.add(far, BorderLayout.EAST);
        f.getContentPane().add(fixed, BorderLayout.CENTER);
        show(f);
        RootPan pan = pan(f);
        assertEquals(900, f.getWidth());
        assertEquals(900, fixed.getWidth());
        assertEquals(Units.toLogical(pan.getHeight()), f.getHeight());
        assertTrue(pan.isScrollableX());
        assertFalse(pan.isScrollableY());
        assertEquals(Units.toDevice(900), pan.getScrollDimension().getWidth());
        assertEquals(900, far.getX() + far.getWidth());

        // Panned to the end, the button is under the right edge of the display.
        int right = pan.getAbsoluteX() + pan.getWidth();
        pan.panTo(Units.toDevice(900) - pan.getWidth(), 0);
        int[] at = onDisplay(far, far.getWidth() - 2, 5);
        assertTrue(at[0] < right);
        assertTrue(at[0] > right - Units.toDevice(8));
        press(f, far, far.getWidth() - 2, 5);
        release(f, far, far.getWidth() - 2, 5);
        assertEquals(1, presses[0]);

        // Once the content can shrink again the window is the display's size.
        fixed.setMinimumSize(new Dimension(10, 10));
        fixed.revalidate();
        MainThreadRule.drain();
        assertEquals(Units.toLogical(pan.getWidth()), f.getWidth());
        assertFalse(pan.isScrollableX());
        assertEquals(0, pan.getScrollX());
    }

    /// A tool bar with more buttons than fit side by side does not hold
    /// the window wide: it goes on in a second row, takes the height of
    /// both, and every button is inside the window.
    @Test
    public void aToolBarGoesOnInAFurtherRow() {
        JFrame f = new JFrame();
        JToolBar bar = new JToolBar();
        JButton[] buttons = new JButton[12];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JButton("Button " + i);
            bar.add(buttons[i]);
        }
        int one = buttons[0].getPreferredSize().height;
        int wanted = bar.getPreferredSize().width;
        assertTrue("the test needs a bar wider than the display", wanted > 540);
        assertTrue(bar.getMinimumSize().width < 200);
        f.getContentPane().add(bar, BorderLayout.NORTH);
        f.getContentPane().add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);
        show(f);
        // The bar learns its width in the first pass and asks for a second.
        MainThreadRule.drain();
        f.cn1Form().revalidate();
        assertFalse(pan(f).isScrollableX());
        assertEquals(f.getWidth(), bar.getWidth());
        assertTrue(bar.getHeight() >= 2 * one);
        assertEquals(bar.getPreferredSize().height, bar.getHeight());
        // The width it would like in one row is still what it prefers.
        assertEquals(wanted, bar.getPreferredSize().width);
        int rows = 1;
        for (int i = 0; i < buttons.length; i++) {
            JButton b = buttons[i];
            assertTrue(b.getX() >= 0 && b.getX() + b.getWidth() <= bar.getWidth());
            assertTrue(b.getY() >= 0 && b.getY() + b.getHeight() <= bar.getHeight());
            assertEquals(b.getPreferredSize().width, b.getWidth());
            if (i > 0 && b.getY() > buttons[i - 1].getY()) {
                rows++;
                assertEquals(bar.getInsets().left, b.getX());
            } else if (i > 0) {
                assertEquals(buttons[i - 1].getX() + buttons[i - 1].getWidth(), b.getX());
            }
        }
        assertTrue(rows >= 2);

        // With room for all of it the bar is one row again.
        JToolBar few = new JToolBar();
        JButton a = new JButton("A");
        JButton b = new JButton("B");
        few.add(a);
        few.add(b);
        JFrame g = new JFrame();
        g.getContentPane().add(few, BorderLayout.NORTH);
        show(g);
        MainThreadRule.drain();
        g.cn1Form().revalidate();
        assertEquals(a.getY(), b.getY());
        assertEquals(few.getPreferredSize().height, few.getHeight());
    }
}
