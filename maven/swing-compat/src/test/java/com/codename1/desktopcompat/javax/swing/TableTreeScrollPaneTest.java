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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseMotionAdapter;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import org.junit.Test;

/// What a table and a tree ask of the scroll pane around them: the
/// table's header becomes the pane's column header, the selection is
/// scrolled into view, and a finger drag over rows is handed to the pane.
public class TableTreeScrollPaneTest extends KernelTestBase {

    private static boolean shows(JScrollPane pane, Rectangle r) {
        Point at = pane.getViewport().getViewPosition();
        Dimension extent = pane.getViewport().getExtentSize();
        return r.y >= at.y && r.y + r.height <= at.y + extent.height;
    }

    @Test
    public void theTableHeaderBecomesTheColumnHeader() {
        JTable t = new JTable(new DefaultTableModel(5, 3));
        JScrollPane pane = new JScrollPane(t);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        show(f);
        assertNotNull(pane.getColumnHeader());
        assertSame(t.getTableHeader(), pane.getColumnHeader().getView());
        assertTrue(t.getScrollableTracksViewportWidth());
        assertEquals(t.getRowHeight(), t.getScrollableUnitIncrement(new Rectangle(0, 0, 100, 100),
                SwingConstants.VERTICAL, 1));
    }

    @Test
    public void changingTheSelectionScrollsItIntoView() {
        JTable t = new JTable(new DefaultTableModel(300, 3));
        JScrollPane pane = new JScrollPane(t);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        show(f);
        f.validate();
        assertTrue("the table is as high as its rows", t.getHeight() >= 300 * t.getRowHeight());
        t.changeSelection(250, 1, false, false);
        assertTrue(pane.getViewport().getViewPosition().y > 0);
        assertTrue(shows(pane, t.getCellRect(250, 1, true)));
        t.changeSelection(2, 1, false, false);
        assertTrue(shows(pane, t.getCellRect(2, 1, true)));
    }

    @Test
    public void aTreePathIsScrolledIntoView() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode last = null;
        for (int i = 0; i < 200; i++) {
            DefaultMutableTreeNode n = new DefaultMutableTreeNode("n" + i);
            n.add(new DefaultMutableTreeNode("leaf" + i));
            root.add(n);
            last = n;
        }
        JTree t = new JTree(root);
        JScrollPane pane = new JScrollPane(t);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        show(f);
        f.validate();
        TreePath deep = new TreePath(new Object[]{root, last, last.getChildAt(0)});
        t.scrollPathToVisible(deep);
        f.validate();
        assertTrue(t.isExpanded(deep.getParentPath()));
        assertTrue(pane.getViewport().getViewPosition().y > 0);
        assertTrue(shows(pane, t.getPathBounds(deep)));
    }

    @Test
    public void aFingerDragOverRowsIsHandedToTheScrollPane() {
        JTable t = new JTable(new DefaultTableModel(300, 3));
        JScrollPane pane = new JScrollPane(t);
        final int[] drags = new int[1];
        pane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                drags[0]++;
            }
        });
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        show(f);
        f.validate();
        press(f, t, 50, 100);
        drag(f, t, 50, 60);
        drag(f, t, 50, 20);
        release(f, t, 50, 20);
        assertTrue("the pane heard the drag", drags[0] >= 2);
        assertEquals("and nothing was selected", -1, t.getSelectedRow());
    }
}
