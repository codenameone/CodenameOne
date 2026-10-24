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
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Point;
import org.junit.Test;

/// A text area wraps its lines only when it was asked to.
public class TextAreaLineWrapTest extends KernelTestBase {

    private static String longLine() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            sb.append("word").append(i).append(' ');
        }
        return sb.toString();
    }

    private JScrollPane shown(JTextArea area) {
        JScrollPane pane = new JScrollPane(area);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        return pane;
    }

    private static int shownLines(JTextArea area) {
        return ((com.codename1.ui.TextArea) area.cn1Peer()).getLines();
    }

    @Test
    public void aLongLineIsNotWrappedByDefault() {
        JTextArea area = new JTextArea("first\n" + longLine() + "\nlast");
        assertFalse(area.getLineWrap());
        JScrollPane pane = shown(area);
        int extent = pane.getViewport().getExtentSize().width;
        assertFalse(area.getScrollableTracksViewportWidth());
        assertTrue("wider than the viewport: " + area.getWidth(), area.getWidth() > extent);
        assertEquals("three lines, none broken", 3, shownLines(area));
        // The pane scrolls it sideways.
        assertEquals(area.getWidth(), pane.getHorizontalScrollBar().getMaximum());
        pane.getHorizontalScrollBar().setValue(50);
        assertEquals(new Point(50, 0), pane.getViewport().getViewPosition());
        assertEquals(-50, area.getX());
    }

    @Test
    public void shortLinesFillTheViewport() {
        JTextArea area = new JTextArea("a\nb");
        JScrollPane pane = shown(area);
        assertTrue(area.getScrollableTracksViewportWidth());
        assertEquals(pane.getViewport().getExtentSize().width, area.getWidth());
        assertEquals(2, shownLines(area));
    }

    @Test
    public void lineWrapBreaksAtTheViewportsWidth() {
        JTextArea area = new JTextArea("first\n" + longLine() + "\nlast");
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        assertTrue(area.getWrapStyleWord());
        JScrollPane pane = shown(area);
        assertTrue(area.getScrollableTracksViewportWidth());
        assertEquals(pane.getViewport().getExtentSize().width, area.getWidth());
        assertTrue("the long line is broken", shownLines(area) > 3);
        assertEquals(0, pane.getHorizontalScrollBar().getMaximum() - pane.getHorizontalScrollBar().getVisibleAmount());
    }

    @Test
    public void turningWrapOnAndOffLaysOutAgain() {
        JTextArea area = new JTextArea(longLine());
        JScrollPane pane = shown(area);
        int extent = pane.getViewport().getExtentSize().width;
        assertTrue(area.getWidth() > extent);
        area.setLineWrap(true);
        pane.validate();
        assertEquals(extent, area.getWidth());
        assertTrue(shownLines(area) > 1);
        area.setLineWrap(false);
        pane.validate();
        assertTrue(area.getWidth() > extent);
        assertEquals(1, shownLines(area));
    }

    @Test
    public void theWidthFollowsTheText() {
        JTextArea area = new JTextArea("short");
        JScrollPane pane = shown(area);
        int extent = pane.getViewport().getExtentSize().width;
        assertEquals(extent, area.getWidth());
        area.append(longLine());
        pane.validate();
        assertTrue(area.getWidth() > extent);
        area.setText("short again");
        pane.validate();
        assertEquals(extent, area.getWidth());
    }
}
