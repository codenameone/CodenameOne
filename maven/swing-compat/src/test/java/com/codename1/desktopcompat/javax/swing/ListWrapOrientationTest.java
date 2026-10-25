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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import java.util.List;
import org.junit.Test;

/// The wrapping layout orientations of a list are a grid, the desktop's.
public class ListWrapOrientationTest extends KernelTestBase {

    private static String[] items(int n) {
        String[] all = new String[n];
        for (int i = 0; i < n; i++) {
            all[i] = "item" + i;
        }
        return all;
    }

    private static String r(Rectangle b) {
        return b == null ? "null" : b.x + "," + b.y + " " + b.width + "x" + b.height;
    }

    private static String r(java.awt.Rectangle b) {
        return b == null ? "null" : b.x + "," + b.y + " " + b.width + "x" + b.height;
    }

    /// Cell bounds, hit testing and sizes against the JDK's own list, for
    /// both orientations, with the row count fixed and left to the size.
    @Test
    public void theGridIsTheDesktopOne() {
        int[] orientations = {JList.HORIZONTAL_WRAP, JList.VERTICAL_WRAP};
        int[] rowCounts = {3, 4, 0, -1, 8};
        int[] sizes = {10, 7, 1, 12};
        for (int o = 0; o < orientations.length; o++) {
            for (int v = 0; v < rowCounts.length; v++) {
                for (int z = 0; z < sizes.length; z++) {
                    String what = "orientation " + orientations[o] + " rows " + rowCounts[v] + " n " + sizes[z];
                    JList<String> ours = new JList<String>(items(sizes[z]));
                    javax.swing.JList<String> real = new javax.swing.JList<String>(items(sizes[z]));
                    ours.setFixedCellWidth(50);
                    ours.setFixedCellHeight(20);
                    real.setFixedCellWidth(50);
                    real.setFixedCellHeight(20);
                    ours.setLayoutOrientation(orientations[o]);
                    real.setLayoutOrientation(orientations[o]);
                    ours.setVisibleRowCount(rowCounts[v]);
                    real.setVisibleRowCount(rowCounts[v]);
                    ours.setSize(170, 90);
                    real.setSize(170, 90);
                    assertEquals(what, real.getPreferredSize().width, ours.getPreferredSize().width);
                    assertEquals(what, real.getPreferredSize().height, ours.getPreferredSize().height);
                    for (int i = 0; i < sizes[z]; i++) {
                        assertEquals(what + " cell " + i, r(real.getCellBounds(i, i)), r(ours.getCellBounds(i, i)));
                    }
                    for (int x = 5; x < 300; x += 35) {
                        for (int y = 5; y < 200; y += 15) {
                            assertEquals(what + " at " + x + "," + y, real.locationToIndex(new java.awt.Point(x, y)),
                                    ours.locationToIndex(new Point(x, y)));
                        }
                    }
                    assertEquals(what, real.getScrollableTracksViewportWidth(),
                            ours.getScrollableTracksViewportWidth());
                    assertEquals(what, real.getScrollableTracksViewportHeight(),
                            ours.getScrollableTracksViewportHeight());
                }
            }
        }
    }

    @Test
    public void theCellsAreDrawnSideBySide() {
        JList<String> list = new JList<String>(items(6));
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(2);
        JFrame f = new JFrame();
        f.add(list, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        List<Object[]> text = paint(f);
        int[] x = new int[6];
        int[] y = new int[6];
        for (int i = 0; i < 6; i++) {
            Object[] at = find(text, "item" + i);
            assertNotNull("item" + i, at);
            x[i] = ((Integer) at[1]).intValue();
            y[i] = ((Integer) at[2]).intValue();
        }
        // Three columns of two rows, filled row by row.
        assertTrue(x[0] < x[1] && x[1] < x[2]);
        assertEquals(y[0], y[1]);
        assertEquals(y[0], y[2]);
        assertEquals(x[0], x[3]);
        assertEquals(x[2], x[5]);
        assertTrue(y[3] > y[0]);

        // The same cells column by column.
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        f.validate();
        text = paint(f);
        int x0 = ((Integer) find(text, "item0")[1]).intValue();
        int y0 = ((Integer) find(text, "item0")[2]).intValue();
        assertEquals(x0, ((Integer) find(text, "item1")[1]).intValue());
        assertTrue(((Integer) find(text, "item1")[2]).intValue() > y0);
        assertTrue(((Integer) find(text, "item2")[1]).intValue() > x0);
        assertEquals(y0, ((Integer) find(text, "item2")[2]).intValue());
    }

    @Test
    public void aClickSelectsTheCellUnderIt() {
        JList<String> list = new JList<String>(items(9));
        list.setFixedCellWidth(60);
        list.setFixedCellHeight(24);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(3);
        JFrame f = new JFrame();
        f.add(list, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        // Row 1, column 2 of a grid three wide.
        press(f, list, 2 * 60 + 10, 24 + 5);
        release(f, list, 2 * 60 + 10, 24 + 5);
        assertEquals(5, list.getSelectedIndex());
    }

    @Test
    public void theArrowKeysMoveThroughTheGrid() {
        JList<String> list = new JList<String>(items(9));
        list.setFixedCellWidth(60);
        list.setFixedCellHeight(24);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(3);
        list.setSelectedIndex(4);
        list.getActionMap().get("selectNextColumn").actionPerformed(null);
        assertEquals(5, list.getSelectedIndex());
        list.getActionMap().get("selectNextRow").actionPerformed(null);
        assertEquals(8, list.getSelectedIndex());
        list.getActionMap().get("selectNextRow").actionPerformed(null);
        assertEquals("no row below", 8, list.getSelectedIndex());
        list.getActionMap().get("selectPreviousColumn").actionPerformed(null);
        assertEquals(7, list.getSelectedIndex());
        list.getActionMap().get("selectPreviousRow").actionPerformed(null);
        assertEquals(4, list.getSelectedIndex());

        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setSelectedIndex(4);
        list.getActionMap().get("selectNextColumn").actionPerformed(null);
        assertEquals(7, list.getSelectedIndex());
        list.getActionMap().get("selectNextRow").actionPerformed(null);
        assertEquals(8, list.getSelectedIndex());

        // One column has no columns to step to.
        list.setLayoutOrientation(JList.VERTICAL);
        list.setSelectedIndex(4);
        list.getActionMap().get("selectNextColumn").actionPerformed(null);
        assertEquals(4, list.getSelectedIndex());
        list.getActionMap().get("selectNextRow").actionPerformed(null);
        assertEquals(5, list.getSelectedIndex());
    }

    @Test
    public void aGridSizedByItsWidthScrollsDownOnly() {
        JList<String> list = new JList<String>(items(400));
        list.setFixedCellWidth(60);
        list.setFixedCellHeight(24);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(0);
        JScrollPane pane = new JScrollPane(list);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        f.setSize(300, 200);
        show(f);
        f.validate();
        f.validate();
        int extent = pane.getViewport().getExtentSize().width;
        assertEquals(extent, list.getWidth());
        int cols = extent / 60;
        assertTrue(cols > 1);
        int rows = (400 + cols - 1) / cols;
        assertEquals(rows * 24, list.getPreferredSize().height);
        assertEquals(rows * 24, list.getHeight());
        assertFalse(list.getScrollableTracksViewportHeight());
        assertEquals(new Rectangle(60, 24, 60, 24), list.getCellBounds(cols + 1, cols + 1));
    }
}
