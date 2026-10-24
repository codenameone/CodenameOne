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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeWillExpandListener;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeModel;
import com.codename1.desktopcompat.javax.swing.tree.ExpandVetoException;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreeModel;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.javax.swing.tree.TreeSelectionModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.junit.AfterClass;
import org.junit.Test;

/// The tree: rows under expanding and collapsing, a model that is only
/// asked about what shows, selection by path, model changes that keep the
/// expansion, and the pointer on arrows and on rows.
public class JTreeTest extends KernelTestBase {

    /// The kernel counts clicks by time alone and keeps the count between
    /// tests, so the taps made here would reach the next test class as
    /// one long multiple click. Wait the count out.
    @AfterClass
    public static void letTheClickCountRunOut() throws InterruptedException {
        Thread.sleep(600);
    }

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    private final DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
    private final DefaultMutableTreeNode a1 = new DefaultMutableTreeNode("a1");
    private final DefaultMutableTreeNode a2 = new DefaultMutableTreeNode("a2");
    private final DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
    private final DefaultMutableTreeNode b1 = new DefaultMutableTreeNode("b1");
    private final DefaultMutableTreeNode b1x = new DefaultMutableTreeNode("b1x");
    private final DefaultTreeModel model = new DefaultTreeModel(root);

    {
        root.add(a);
        root.add(b);
        a.add(a1);
        a.add(a2);
        b.add(b1);
        b1.add(b1x);
    }

    private TreePath path(DefaultMutableTreeNode n) {
        return new TreePath(model.getPathToRoot(n));
    }

    private static String rows(JTree t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.getRowCount(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(t.getPathForRow(i).getLastPathComponent());
        }
        return sb.toString();
    }

    /// A tree without end: every node has two children, made when asked.
    private static final class Endless implements TreeModel {
        final List<Object> asked = new ArrayList<Object>();

        @Override
        public Object getRoot() {
            return "r";
        }

        @Override
        public Object getChild(Object parent, int index) {
            asked.add(parent);
            return parent + "/" + index;
        }

        @Override
        public int getChildCount(Object parent) {
            asked.add(parent);
            return 2;
        }

        @Override
        public boolean isLeaf(Object node) {
            return false;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return String.valueOf(child).endsWith("/1") ? 1 : 0;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
        }
    }

    private static void key(Component c, int code) {
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code,
                KeyEvent.CHAR_UNDEFINED));
    }

    // ------------------------------------------------------------ rows

    @Test
    public void expandingAndCollapsingChangesTheRows() {
        JTree t = new JTree(model);
        assertEquals("the root starts expanded", "root a b", rows(t));
        assertTrue(t.isExpanded(0));
        assertFalse(t.isExpanded(path(a)));
        t.expandPath(path(a));
        assertEquals("root a a1 a2 b", rows(t));
        assertEquals(2, t.getRowForPath(path(a1)));
        assertEquals(-1, t.getRowForPath(path(b1)));
        assertEquals(path(b), t.getPathForRow(4));
        assertNull(t.getPathForRow(5));
        t.expandPath(path(b1));
        assertEquals("expanding expands what is above", "root a a1 a2 b b1 b1x", rows(t));
        t.collapsePath(path(b));
        assertEquals("root a a1 a2 b", rows(t));
        assertTrue(t.hasBeenExpanded(path(b)));
        t.expandRow(4);
        assertEquals("what was open below comes back", "root a a1 a2 b b1 b1x", rows(t));
        t.collapseRow(0);
        assertEquals("root", rows(t));
        t.expandPath(path(a1));
        assertEquals("a leaf does not expand", "root", rows(t));
        t.setRootVisible(false);
        assertEquals("a hidden root shows its children", "a a1 a2 b b1 b1x", rows(t));
        t.makeVisible(path(b1x));
        assertTrue(t.isVisible(path(b1x)));
    }

    @Test
    public void listenersHearAndCanStopAnExpansion() {
        JTree t = new JTree(model);
        final List<String> log = new ArrayList<String>();
        t.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                log.add("expanded " + event.getPath().getLastPathComponent());
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                log.add("collapsed " + event.getPath().getLastPathComponent());
            }
        });
        t.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (event.getPath().getLastPathComponent() == b) {
                    throw new ExpandVetoException(event);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                log.add("will collapse " + event.getPath().getLastPathComponent());
            }
        });
        t.expandPath(path(a));
        t.expandPath(path(b));
        t.expandPath(path(b1));
        t.collapsePath(path(a));
        assertEquals("root a b", rows(t));
        assertEquals("[expanded a, will collapse a, collapsed a]", log.toString());
    }

    @Test
    public void onlyExpandedNodesAreAskedForTheirChildren() {
        Endless endless = new Endless();
        JTree t = new JTree(endless);
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        assertEquals(3, t.getRowCount());
        assertNotNull(find(paint(f), "r/1"));
        assertNotNull(t.getPreferredSize());
        for (int i = 0; i < endless.asked.size(); i++) {
            assertEquals("r", endless.asked.get(i));
        }
        endless.asked.clear();
        t.expandRow(1);
        assertEquals(5, t.getRowCount());
        paint(f);
        assertTrue(endless.asked.contains("r/0"));
        assertFalse(endless.asked.contains("r/1"));
        assertFalse(endless.asked.contains("r/0/0"));
        assertEquals("r/0/1", t.getPathForRow(3).getLastPathComponent());
    }

    @Test
    public void arraysAndVectorsBecomeTrees() {
        Vector<Object> inner = new Vector<Object>();
        inner.add("x");
        inner.add("y");
        JTree t = new JTree(new Object[]{"one", inner, "three"});
        assertFalse(t.isRootVisible());
        assertTrue(t.getShowsRootHandles());
        assertEquals(3, t.getRowCount());
        t.expandRow(1);
        assertEquals(5, t.getRowCount());
        assertEquals("y", t.getPathForRow(3).getLastPathComponent().toString());
        assertEquals("colors", new JTree().getPathForRow(1).getLastPathComponent().toString());
    }

    // ------------------------------------------------------------ selection

    @Test
    public void selectionIsByPathAndTheTreeTellsItsListeners() {
        JTree t = new JTree(model);
        final List<TreeSelectionEvent> events = new ArrayList<TreeSelectionEvent>();
        t.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                events.add(e);
            }
        });
        t.setSelectionPath(path(a2));
        assertEquals("selecting a hidden node shows it", "root a a1 a2 b", rows(t));
        assertEquals(1, events.size());
        assertSame(t, events.get(0).getSource());
        assertTrue(events.get(0).isAddedPath());
        assertEquals(path(a2), events.get(0).getPath());
        assertEquals(3, t.getSelectionRows()[0]);
        assertSame(a2, t.getLastSelectedPathComponent());
        assertEquals(path(a2), t.getLeadSelectionPath());
        assertEquals(3, t.getLeadSelectionRow());
        t.addSelectionRow(4);
        assertEquals(2, t.getSelectionCount());
        assertTrue(t.isRowSelected(4));
        assertTrue(t.isPathSelected(path(b)));
        t.setSelectionInterval(1, 3);
        assertEquals(3, t.getSelectionCount());
        assertEquals(1, t.getMinSelectionRow());
        assertEquals(3, t.getMaxSelectionRow());
        t.collapsePath(path(a));
        assertEquals("a selection below a collapsed node moves onto it", 1, t.getSelectionCount());
        assertEquals(path(a), t.getSelectionPath());
        t.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        t.setSelectionInterval(0, 2);
        assertEquals(1, t.getSelectionCount());
        t.clearSelection();
        assertTrue(t.isSelectionEmpty());
        assertNull(t.getSelectionPaths());
    }

    // ------------------------------------------------------------ model events

    @Test
    public void modelChangesKeepWhatIsExpanded() {
        JTree t = new JTree(model);
        t.expandPath(path(a));
        t.expandPath(path(b1));
        t.setSelectionPath(path(a1));
        DefaultMutableTreeNode c = new DefaultMutableTreeNode("c");
        model.insertNodeInto(c, root, 0);
        assertEquals("root c a a1 a2 b b1 b1x", rows(t));
        assertTrue(t.isExpanded(path(a)));
        assertEquals("the selection follows its node", 3, t.getSelectionRows()[0]);
        a1.setUserObject("first");
        model.nodeChanged(a1);
        assertEquals("root c a first a2 b b1 b1x", rows(t));
        TreePath gone = path(a);
        model.removeNodeFromParent(a);
        assertEquals("root c b b1 b1x", rows(t));
        assertFalse(t.hasBeenExpanded(gone));
        assertTrue("the selection below the removed node is gone", t.isSelectionEmpty());
        b1.add(new DefaultMutableTreeNode("b1y"));
        model.nodeStructureChanged(b);
        assertEquals("b stays open, what was open below it is forgotten", "root c b b1", rows(t));
        t.expandPath(path(b1));
        assertEquals("root c b b1 b1x b1y", rows(t));
        model.reload();
        assertEquals("root c b", rows(t));
        DefaultMutableTreeNode other = new DefaultMutableTreeNode("other");
        other.add(new DefaultMutableTreeNode("leaf"));
        model.setRoot(other);
        assertEquals("other leaf", rows(t));
    }

    // ------------------------------------------------------------ geometry and pointer

    @Test
    public void rowsAreIndentedAndSizedInLogicalPixels() {
        JTree t = new JTree(model);
        t.setRowHeight(20);
        t.expandPath(path(a));
        Rectangle rootBounds = t.getRowBounds(0);
        Rectangle aBounds = t.getPathBounds(path(a));
        Rectangle a1Bounds = t.getPathBounds(path(a1));
        assertEquals(0, rootBounds.x);
        assertEquals(0, rootBounds.y);
        assertEquals(20, rootBounds.height);
        int step = aBounds.x;
        assertTrue("an indent a finger can hit, got " + step, step >= 20);
        assertEquals(20, aBounds.y);
        assertEquals(2 * step, a1Bounds.x);
        assertEquals(40, a1Bounds.y);
        assertTrue(a1Bounds.width > 0);
        assertNull(t.getPathBounds(path(b1)));
        assertEquals(5 * 20, t.getPreferredSize().height);
        assertTrue(t.getPreferredSize().width >= a1Bounds.x + a1Bounds.width);
        assertEquals(2, t.getRowForLocation(2 * step + 1, 45));
        assertEquals("before the text is not on the row", -1, t.getRowForLocation(step, 45));
        assertEquals(2, t.getClosestRowForLocation(0, 45));
        assertEquals(4, t.getClosestRowForLocation(0, 4000));
        assertNull(t.getPathForLocation(5, 400));
        t.setShowsRootHandles(true);
        assertEquals(step, t.getRowBounds(0).x);
        t.setRowHeight(0);
        assertTrue("touch rows are at least 32 high", t.getRowHeight() >= 32);
        assertEquals(20 * t.getRowHeight(), t.getPreferredScrollableViewportSize().height);
    }

    @Test
    public void aTapOnTheArrowTogglesAndATapOnTheRowSelects() {
        JTree t = new JTree(model);
        t.setRowHeight(30);
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        int step = t.getPathBounds(path(a)).x;
        press(f, t, step / 2, 45);
        release(f, t, step / 2, 45);
        assertTrue("the arrow expands", t.isExpanded(path(a)));
        assertTrue("and does not select", t.isSelectionEmpty());
        press(f, t, step + 5, 45);
        release(f, t, step + 5, 45);
        assertEquals(path(a), t.getSelectionPath());
        assertTrue("one tap on the row does not toggle", t.isExpanded(path(a)));
        press(f, t, step / 2, 45);
        release(f, t, step / 2, 45);
        assertFalse("the arrow collapses", t.isExpanded(path(a)));
        assertEquals("root a b", rows(t));
        press(f, t, step + 5, 75);
        release(f, t, step + 5, 75);
        assertEquals(path(b), t.getSelectionPath());
        assertFalse(t.isExpanded(path(b)));
        press(f, t, step + 5, 75);
        release(f, t, step + 5, 75);
        assertTrue("the second tap on a row toggles it", t.isExpanded(path(b)));
        press(f, t, step + 5, 15);
        drag(f, t, step + 5, 60);
        release(f, t, step + 5, 60);
        assertEquals("a finger drag does not select", path(b), t.getSelectionPath());
        press(f, t, 300, 15);
        release(f, t, 300, 15);
        assertEquals("the row reaches to the right edge", new TreePath(root), t.getSelectionPath());
    }

    @Test
    public void theRendererIsAskedPerVisibleRowAndPaints() {
        JTree t = new JTree(model);
        t.setRowHeight(20);
        final List<String> asked = new ArrayList<String>();
        final JComponent stamp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.drawString("node:" + getName(), 1, 12);
            }
        };
        t.setCellRenderer(new TreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                asked.add(value + "," + selected + "," + expanded + "," + leaf + "," + row);
                stamp.setName(String.valueOf(value));
                return stamp;
            }
        });
        t.expandPath(path(a));
        t.setSelectionPath(path(a1));
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        asked.clear();
        List<Object[]> text = paint(f);
        assertTrue(asked.contains("root,false,true,false,0"));
        assertTrue(asked.contains("a,false,true,false,1"));
        assertTrue(asked.contains("a1,true,false,true,2"));
        assertTrue(asked.contains("b,false,false,false,4"));
        Object[] rootText = find(text, "node:root");
        Object[] a1Text = find(text, "node:a1");
        assertNotNull(rootText);
        assertNotNull(a1Text);
        int step = t.getPathBounds(path(a)).x;
        assertEquals(2 * 2 * step, ((Integer) a1Text[1]).intValue() - ((Integer) rootText[1]).intValue());
        assertEquals(2 * 40, ((Integer) a1Text[2]).intValue() - ((Integer) rootText[2]).intValue());
        assertNull("what is collapsed is not painted", find(text, "node:b1"));
    }

    @Test
    public void theDefaultRendererDrawsTheNodeText() {
        JTree t = new JTree(model);
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        List<Object[]> text = paint(f);
        assertNotNull(find(text, "root"));
        assertNotNull(find(text, "b"));
        assertNull(find(text, "a1"));
    }

    // ------------------------------------------------------------ keys and editing

    @Test
    public void arrowKeysWalkAndOpenTheTree() {
        JTree t = new JTree(model);
        t.setSelectionRow(0);
        key(t, KeyEvent.VK_DOWN);
        assertEquals(path(a), t.getSelectionPath());
        key(t, KeyEvent.VK_RIGHT);
        assertTrue(t.isExpanded(path(a)));
        key(t, KeyEvent.VK_RIGHT);
        assertEquals(path(a1), t.getSelectionPath());
        key(t, KeyEvent.VK_LEFT);
        assertEquals(path(a), t.getSelectionPath());
        key(t, KeyEvent.VK_LEFT);
        assertFalse(t.isExpanded(path(a)));
        key(t, KeyEvent.VK_END);
        assertEquals(path(b), t.getSelectionPath());
        key(t, KeyEvent.VK_HOME);
        assertEquals(new TreePath(root), t.getSelectionPath());
    }

    @Test
    public void editingPutsATextFieldOverTheRowAndStoresItsValue() {
        JTree t = new JTree(model);
        t.setRowHeight(20);
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        t.startEditingAtPath(path(a));
        assertFalse("a tree is not editable until asked", t.isEditing());
        t.setEditable(true);
        t.startEditingAtPath(path(a1));
        assertTrue(t.isEditing());
        assertEquals(path(a1), t.getEditingPath());
        assertEquals(1, t.getComponentCount());
        Component c = t.getComponent(0);
        assertTrue(c instanceof JTextField);
        assertEquals("a1", ((JTextField) c).getText());
        assertEquals(t.getPathBounds(path(a1)).x, c.getX());
        assertEquals(40, c.getY());
        assertEquals(20, c.getHeight());
        ((JTextField) c).setText("renamed");
        assertTrue(t.stopEditing());
        assertFalse(t.isEditing());
        assertEquals(0, t.getComponentCount());
        assertEquals("renamed", a1.getUserObject());
        t.startEditingAtPath(path(a2));
        ((JTextField) t.getComponent(0)).setText("dropped");
        key(t.getComponent(0), KeyEvent.VK_ESCAPE);
        assertFalse(t.isEditing());
        assertEquals("a2", a2.getUserObject());
    }

    /// A node has a folder and a leaf a sheet before its text unless the
    /// application sets other icons, and the text starts after the icon.
    @Test
    public void nodesAndLeavesHaveStockIconsBeforeTheirText() {
        com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer r =
                new com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer();
        assertNotNull(r.getOpenIcon());
        assertNotNull(r.getClosedIcon());
        assertNotNull(r.getLeafIcon());
        assertSame(r.getDefaultLeafIcon(), r.getLeafIcon());
        assertEquals(16, r.getClosedIcon().getIconWidth());

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("top");
        DefaultMutableTreeNode folder = new DefaultMutableTreeNode("folder");
        folder.add(new DefaultMutableTreeNode("inner"));
        top.add(folder);
        top.add(new DefaultMutableTreeNode("leaf"));
        JTree t = new JTree(top);
        t.setBackground(com.codename1.desktopcompat.java.awt.Color.WHITE);
        t.setForeground(com.codename1.desktopcompat.java.awt.Color.BLACK);
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        assertEquals("only the root is expanded at first", 3, t.getRowCount());
        int[][] px = raster(f);
        for (int row = 0; row < 3; row++) {
            Rectangle b = t.getRowBounds(row);
            int painted = b.width * b.height * 4 - count(px, t, b.x, b.y, 16, b.height, 0xffffff)
                    - (b.width - 16) * b.height * 4;
            assertTrue("row " + row + " has an icon", painted > 40);
        }
        java.util.List<Object[]> text = paint(f);
        Object[] leaf = find(text, "leaf");
        assertNotNull(leaf);
        Rectangle lb = t.getRowBounds(2);
        assertTrue("the text follows the icon",
                ((Integer) leaf[1]).intValue() >= onDisplay(t, lb.x + 16, 0)[0]);

        // An application that wants bare text sets the icons to null.
        r.setLeafIcon(null);
        r.setOpenIcon(null);
        r.setClosedIcon(null);
        t.setCellRenderer(r);
        text = paint(f);
        leaf = find(text, "leaf");
        assertNotNull(leaf);
        assertEquals(onDisplay(t, t.getRowBounds(2).x, 0)[0], ((Integer) leaf[1]).intValue());
    }

    /// A renderer that colours one row is shared by all of them: the next
    /// row has its own colour again, as it does in the JDK.
    @Test
    public void aRowColouredByASubclassDoesNotColourTheNext() {
        JTree t = new JTree(model);
        com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer r =
                new com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (row == 1) {
                    setForeground(com.codename1.desktopcompat.java.awt.Color.RED);
                }
                return this;
            }
        };
        com.codename1.desktopcompat.java.awt.Color plain =
                r.getTreeCellRendererComponent(t, a, false, false, false, 0, false).getForeground();
        assertEquals(com.codename1.desktopcompat.java.awt.Color.RED,
                r.getTreeCellRendererComponent(t, a, false, false, false, 1, false).getForeground());
        assertEquals(plain, r.getTreeCellRendererComponent(t, b, false, false, false, 2, false).getForeground());
    }
}
