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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.plaf.basic.BasicTreeUI;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.rt.EventBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// A scroll pane laid out by the application's own `ScrollPaneLayout`, a
/// tree whose rows are painted through the application's `BasicTreeUI`,
/// and a tool tip that depends on where the pointer is.
public class ScrollPaneLayoutAndTreeUiTest extends KernelTestBase {

    /// Bars that take room, as on a desktop.
    @org.junit.Before
    public void bars() {
        com.codename1.desktopcompat.rt.ScrollDelegate.setBarThickness(12);
    }

    @org.junit.After
    public void barsOfTheDevice() {
        com.codename1.desktopcompat.rt.ScrollDelegate.setBarThickness(-1);
    }

    /// The vertical bar on the left of the view, as an application that
    /// wants it there does it.
    private static final class BarOnTheLeft extends ScrollPaneLayout {

        int passes;

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            passes++;
            if (vsb != null && vsb.isVisible() && viewport != null) {
                Rectangle bar = vsb.getBounds();
                Rectangle view = viewport.getBounds();
                vsb.setBounds(view.x, bar.y, bar.width, bar.height);
                viewport.setBounds(view.x + bar.width, view.y, view.width, view.height);
            }
        }
    }

    @Test
    public void aLayoutOfTheApplicationPlacesTheParts() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        JPanel view = new JPanel();
        view.setPreferredSize(new Dimension(100, 5000));
        JScrollPane pane = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        assertTrue(pane.getLayout() instanceof ScrollPaneLayout.UIResource);
        BarOnTheLeft layout = new BarOnTheLeft();
        pane.setLayout(layout);
        assertSame(layout, pane.getLayout());
        f.add(pane, BorderLayout.CENTER);
        show(f);
        assertTrue(layout.passes > 0);
        // The layout knows the parts of the pane, and its policies.
        assertSame(pane.getViewport(), layout.getViewport());
        assertSame(pane.getVerticalScrollBar(), layout.getVerticalScrollBar());
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, layout.getVerticalScrollBarPolicy());
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, layout.getHorizontalScrollBarPolicy());
        JScrollBar bar = pane.getVerticalScrollBar();
        JViewport vp = pane.getViewport();
        assertTrue(bar.getWidth() > 0);
        assertTrue("the bar is left of the view: " + bar.getBounds() + " " + vp.getBounds(),
                bar.getX() + bar.getWidth() <= vp.getX());
        // A part set later reaches the layout too.
        JLabel head = new JLabel("head");
        pane.setColumnHeaderView(head);
        pane.doLayout();
        assertNotNull(layout.getColumnHeader());
        assertSame(head, layout.getColumnHeader().getView());
        assertNull(layout.getRowHeader());
    }

    @Test
    public void aScrollPaneRefusesAnyOtherLayout() {
        JScrollPane pane = new JScrollPane();
        try {
            pane.setLayout(new BorderLayout());
            fail("layout of JScrollPane must be a ScrollPaneLayout");
        } catch (ClassCastException expected) {
            assertTrue(pane.getLayout() instanceof ScrollPaneLayout);
        }
    }

    /// Counts the rows it is asked to paint, and paints them.
    private static final class Counting extends BasicTreeUI {

        final List<String> rows = new ArrayList<String>();
        JTree installedOn;

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            installedOn = tree;
        }

        @Override
        protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path,
                int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
            rows.add(row + ":" + path.getLastPathComponent() + (isLeaf ? " leaf" : "") + (isExpanded ? " open" : "")
                    + " " + bounds.y);
            super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }
    }

    @Test
    public void theRowsOfATreeArePaintedThroughItsUi() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        a.add(new DefaultMutableTreeNode("a1"));
        root.add(a);
        root.add(new DefaultMutableTreeNode("b"));
        JTree tree = new JTree(root);
        Counting ui = new Counting();
        tree.setUI(ui);
        assertSame(tree, ui.installedOn);
        assertEquals(3, ui.getRowCount(tree));
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        f.add(tree, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        assertEquals(ui.rows.toString(), 3, ui.rows.size());
        assertTrue(ui.rows.get(0), ui.rows.get(0).startsWith("0:root open"));
        assertTrue(ui.rows.get(1), ui.rows.get(1).startsWith("1:a "));
        assertTrue(ui.rows.get(2), ui.rows.get(2).startsWith("2:b leaf"));
        // What the UI passed on to the tree was drawn.
        assertNotNull(find(text, "root"));
        assertNotNull(find(text, "b"));
        assertEquals(ui.getPathBounds(tree, tree.getPathForRow(2)), tree.getRowBounds(2));
    }

    /// A tip that names the half of the label the pointer is in.
    private static final class Halves extends JLabel {

        Halves() {
            super("left and right");
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return event.getX() < getWidth() / 2 ? "left" : "right";
        }
    }

    @Test
    public void aToolTipFollowsThePlaceOfThePointer() {
        JFrame f = new JFrame();
        f.setLayout(new BorderLayout());
        Halves label = new Halves();
        f.add(label, BorderLayout.CENTER);
        show(f);
        // Registers the component with the tool tip manager, as on the
        // desktop; the text itself is never shown.
        label.setToolTipText("somewhere");
        int w = label.getWidth();
        int[] p = onDisplay(label, 3, 3);
        EventBridge.pointerEvent(f, MouseEvent.MOUSE_MOVED, p[0], p[1]);
        assertEquals("left", label.cn1Peer().getTooltip());
        p = onDisplay(label, w - 3, 3);
        EventBridge.pointerEvent(f, MouseEvent.MOUSE_MOVED, p[0], p[1]);
        assertEquals("right", label.cn1Peer().getTooltip());
        label.setToolTipText(null);
        assertNull(label.cn1Peer().getTooltip());
    }
}
