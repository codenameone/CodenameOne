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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.DefaultListModel;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JList;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.ListCellRenderer;
import com.codename1.desktopcompat.javax.swing.RowFilter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ColorHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.HighlightPredicate;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultListRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultTreeRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// `JXList` and `JXTree`: highlighters through painting, the list's
/// sorting and filtering, the tree's expand-all, and rollover.
public class JXListAndTreeTest extends KernelTestBase {

    private static final Color RED = new Color(200, 0, 0);
    private static final Color BLUE = new Color(0, 0, 200);

    /// Draws its value and the background it was left with.
    private static final class Stamp extends JComponent implements ListCellRenderer<Object>, TreeCellRenderer {
        private String text = "";

        @Override
        public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            text = String.valueOf(value);
            setBackground(Color.WHITE);
            return this;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            text = String.valueOf(value);
            setBackground(Color.WHITE);
            return this;
        }

        @Override
        public com.codename1.desktopcompat.java.awt.Dimension getPreferredSize() {
            return new com.codename1.desktopcompat.java.awt.Dimension(150, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color bg = getBackground();
            g.drawString(text + "@" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue(), 2, 12);
        }
    }

    private JFrame placed(JComponent c, int w, int h) {
        JPanel p = new JPanel(null);
        c.setBounds(0, 0, w, h);
        p.add(c);
        JFrame f = new JFrame();
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    private static DefaultListModel<Object> fruit() {
        DefaultListModel<Object> m = new DefaultListModel<Object>();
        m.addElement("pear");
        m.addElement("apple");
        m.addElement("fig");
        m.addElement("banana");
        return m;
    }

    @Test
    public void listHighlightersApplyInOrderWhenTheListPaints() {
        JXList l = new JXList(fruit());
        l.setCellRenderer(new Stamp());
        l.addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        l.addHighlighter(new ColorHighlighter(HighlightPredicate.ODD, BLUE, null));
        JFrame f = placed(l, 300, 300);
        List<Object[]> drawn = paint(f);
        assertNotNull(find(drawn, "pear@200,0,0"));
        assertNotNull(find(drawn, "apple@0,0,200"));
        assertNotNull(find(drawn, "fig@200,0,0"));
        l.setHighlighters(new ColorHighlighter(HighlightPredicate.ODD, BLUE, null),
                new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        assertNotNull(find(paint(f), "apple@200,0,0"));
        assertTrue(l.getWrappedCellRenderer() instanceof Stamp);
        assertFalse(l.getCellRenderer() instanceof Stamp);
    }

    @Test
    public void theDefaultListRendererTakesAHighlight() {
        JXList l = new JXList(fruit());
        assertTrue(l.getWrappedCellRenderer() instanceof DefaultListRenderer);
        l.addHighlighter(new ColorHighlighter(new HighlightPredicate.EqualsHighlightPredicate("fig"), RED, null));
        Component c = l.getCellRenderer().getListCellRendererComponent(l, "fig", 2, false, false);
        assertEquals(RED, c.getBackground());
        c = l.getCellRenderer().getListCellRendererComponent(l, "banana", 3, false, false);
        assertEquals(l.getBackground(), c.getBackground());
        JFrame f = placed(l, 300, 300);
        assertNotNull(find(paint(f), "banana"));
    }

    @Test
    public void aListSortsAndFiltersItsView() {
        DefaultListModel<Object> m = fruit();
        JXList l = new JXList(m, true);
        assertSame(m, l.getModel());
        assertEquals(4, l.getElementCount());
        assertEquals("pear", l.getElementAt(0));
        assertEquals(SortOrder.UNSORTED, l.getSortOrder());

        l.setSelectedIndex(0);
        l.toggleSortOrder();
        assertEquals(SortOrder.ASCENDING, l.getSortOrder());
        assertEquals("apple", l.getElementAt(0));
        assertEquals("pear", l.getElementAt(3));
        assertEquals(1, l.convertIndexToModel(0));
        assertEquals(3, l.convertIndexToView(0));
        assertEquals("the selection followed its element", 3, l.getSelectedIndex());
        assertEquals("pear", l.getSelectedValue());

        l.toggleSortOrder();
        assertEquals(SortOrder.DESCENDING, l.getSortOrder());
        assertEquals("pear", l.getElementAt(0));

        l.setRowFilter(new RowFilter<Object, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends Object, ? extends Integer> entry) {
                return entry.getStringValue(0).length() > 3;
            }
        });
        assertEquals(3, l.getElementCount());
        assertEquals(4, l.getModel().getSize());
        assertEquals(-1, l.convertIndexToView(2));
        assertEquals("pear", l.getElementAt(0));
        assertEquals("apple", l.getElementAt(2));

        m.addElement("cherry");
        assertEquals(4, l.getElementCount());
        assertEquals("cherry", l.getElementAt(1));

        l.setRowFilter(null);
        l.resetSortOrder();
        assertEquals(5, l.getElementCount());
        assertEquals("pear", l.getElementAt(0));

        l.setComparator(new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                return a.toString().length() - b.toString().length();
            }
        });
        l.setSortOrder(SortOrder.ASCENDING);
        assertEquals("fig", l.getElementAt(0));

        l.setSortable(false);
        l.toggleSortOrder();
        assertEquals(SortOrder.ASCENDING, l.getSortOrder());
    }

    @Test
    public void aSortedListPaintsInViewOrder() {
        JXList l = new JXList(fruit(), true);
        l.setCellRenderer(new DefaultListRenderer(new StringValue() {
            @Override
            public String getString(Object value) {
                return "<" + value + ">";
            }
        }));
        assertEquals("<pear>", l.getStringAt(0));
        l.toggleSortOrder();
        JFrame f = placed(l, 300, 300);
        List<Object[]> drawn = paint(f);
        Object[] apple = find(drawn, "<apple>");
        Object[] pear = find(drawn, "<pear>");
        assertNotNull(apple);
        assertNotNull(pear);
        assertTrue(((Integer) apple[2]).intValue() < ((Integer) pear[2]).intValue());
    }

    @Test
    public void aListWithoutASorterShowsItsModel() {
        DefaultListModel<Object> m = fruit();
        JXList l = new JXList(m);
        assertNull(l.getRowSorter());
        l.toggleSortOrder();
        assertEquals("pear", l.getElementAt(0));
        l.setSelectedIndex(2);
        m.add(0, "kiwi");
        assertEquals(5, l.getElementCount());
        assertEquals("kiwi", l.getElementAt(0));
        JXList fromArray = new JXList(new Object[]{"b", "a"}, true);
        fromArray.toggleSortOrder();
        assertEquals("a", fromArray.getElementAt(0));
    }

    @Test
    public void listRolloverFollowsThePointer() {
        JXList l = new JXList(fruit());
        placed(l, 300, 300);
        l.setRolloverEnabled(true);
        l.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, RED, null));
        com.codename1.desktopcompat.java.awt.Rectangle cell = l.getCellBounds(1, 1);
        l.dispatchEvent(new MouseEvent(l, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, cell.x + 2,
                cell.y + 2, 0, false, MouseEvent.NOBUTTON));
        assertEquals(new Point(0, 1), l.getClientProperty("swingx.rollover"));
        assertEquals(RED, l.getCellRenderer().getListCellRendererComponent(l, "apple", 1, false, false)
                .getBackground());
        assertEquals(l.getBackground(), l.getCellRenderer().getListCellRendererComponent(l, "pear", 0, false, false)
                .getBackground());
    }

    private static JXTree tree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
        a.add(new DefaultMutableTreeNode("a1"));
        DefaultMutableTreeNode a2 = new DefaultMutableTreeNode("a2");
        a2.add(new DefaultMutableTreeNode("deep"));
        a.add(a2);
        b.add(new DefaultMutableTreeNode("b1"));
        root.add(a);
        root.add(b);
        return new JXTree(root);
    }

    @Test
    public void expandAllAndCollapseAll() {
        JXTree t = tree();
        assertEquals(3, t.getRowCount());
        t.expandAll();
        assertEquals(7, t.getRowCount());
        assertEquals("deep", t.getStringAt(4));
        t.collapseAll();
        assertEquals(1, t.getRowCount());
    }

    @Test
    public void treeHighlightersSeeDepthAndLeaves() {
        JXTree t = tree();
        t.expandAll();
        assertTrue(t.getWrappedCellRenderer() instanceof DefaultTreeRenderer);
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.IS_LEAF, RED, null));
        t.addHighlighter(new ColorHighlighter(new HighlightPredicate.DepthHighlightPredicate(1), BLUE, null));
        Object a1 = t.getPathForRow(2).getLastPathComponent();
        Object a = t.getPathForRow(1).getLastPathComponent();
        Object root = t.getPathForRow(0).getLastPathComponent();
        assertEquals(RED, t.getCellRenderer().getTreeCellRendererComponent(t, a1, false, false, true, 2, false)
                .getBackground());
        assertEquals(BLUE, t.getCellRenderer().getTreeCellRendererComponent(t, a, false, true, false, 1, false)
                .getBackground());
        Color plain = t.getCellRenderer().getTreeCellRendererComponent(t, root, false, true, false, 0, false)
                .getBackground();
        assertFalse(RED.equals(plain));
        assertFalse(BLUE.equals(plain));
    }

    @Test
    public void treeHighlightersApplyInOrderWhenTheTreePaints() {
        JXTree t = tree();
        t.setCellRenderer(new Stamp());
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.IS_FOLDER, BLUE, null));
        t.expandAll();
        JFrame f = placed(t, 300, 400);
        List<Object[]> drawn = paint(f);
        assertNotNull(find(drawn, "a1@200,0,0"));
        assertNotNull(find(drawn, "a@0,0,200"));
        t.setHighlighters(new ColorHighlighter(HighlightPredicate.IS_FOLDER, BLUE, null),
                new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        assertNotNull(find(paint(f), "a@200,0,0"));
    }

    @Test
    public void treeRolloverFollowsThePointer() {
        JXTree t = tree();
        placed(t, 300, 300);
        t.setRolloverEnabled(true);
        com.codename1.desktopcompat.java.awt.Rectangle row = t.getRowBounds(1);
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, row.x + 2,
                row.y + 2, 0, false, MouseEvent.NOBUTTON));
        assertEquals(new Point(0, 1), t.getClientProperty("swingx.rollover"));
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, row.x + 2,
                row.y + 2, 0, false, MouseEvent.NOBUTTON));
        assertNull(t.getClientProperty("swingx.rollover"));
    }
}
