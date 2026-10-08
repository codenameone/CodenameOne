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
package com.codename1.desktopcompat.org.jdesktop.swingx.treetable;

import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.org.jdesktop.swingx.tree.TreeModelSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The tree table nodes and models, held to what SwingX does with the
/// same calls.
public class TreeTableModelTest {

    private static final class Log implements TreeModelListener {
        final List<String> heard = new ArrayList<String>();

        private void add(String kind, TreeModelEvent e) {
            heard.add(kind + " " + e.getTreePath() + " " + Arrays.toString(e.getChildIndices()) + " "
                    + Arrays.toString(e.getChildren()));
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            add("changed", e);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            add("inserted", e);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            add("removed", e);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            add("structure", e);
        }

        String last() {
            return heard.get(heard.size() - 1);
        }
    }

    private static String kids(DefaultMutableTreeTableNode n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.getChildCount(); i++) {
            sb.append(n.getChildAt(i)).append(' ');
        }
        return sb.toString().trim();
    }

    private static DefaultMutableTreeTableNode three(DefaultMutableTreeTableNode[] c) {
        DefaultMutableTreeTableNode p = new DefaultMutableTreeTableNode("p");
        for (int i = 0; i < 3; i++) {
            c[i] = new DefaultMutableTreeTableNode("c" + i);
            p.add(c[i]);
        }
        return p;
    }

    @Test
    public void aNodeKeepsBothSidesOfAMove() {
        DefaultMutableTreeTableNode p1 = new DefaultMutableTreeTableNode("p1");
        DefaultMutableTreeTableNode p2 = new DefaultMutableTreeTableNode("p2");
        DefaultMutableTreeTableNode c = new DefaultMutableTreeTableNode("c");
        p1.add(c);
        assertSame(p1, c.getParent());
        p2.add(c);
        assertEquals(0, p1.getChildCount());
        assertEquals(1, p2.getChildCount());
        assertSame(p2, c.getParent());
        c.setParent(p1);
        assertEquals("c", kids(p1));
        assertEquals("", kids(p2));
        c.setParent(null);
        assertEquals(0, p1.getChildCount());
        assertNull(c.getParent());
        p1.add(c);
        c.removeFromParent();
        assertEquals(0, p1.getChildCount());
        assertNull(c.getParent());
        p1.add(c);
        p2.remove(c);
        assertEquals("removing takes the node from the parent it has", 0, p1.getChildCount());
        assertNull(c.getParent());
        assertEquals(-1, p1.getIndex(c));
        assertEquals(-1, p1.getIndex(null));
    }

    @Test
    public void insertingAChildAgainMovesItAndCountsWithoutIt() {
        DefaultMutableTreeTableNode[] c = new DefaultMutableTreeTableNode[3];
        DefaultMutableTreeTableNode p = three(c);
        p.insert(c[0], 2);
        assertEquals("c1 c0 c2", kids(p));
        p = three(c);
        p.insert(c[0], 3);
        assertEquals("c1 c2 c0", kids(p));
        p = three(c);
        p.insert(c[2], 1);
        assertEquals("c2 c0 c1", kids(p));
        p = three(c);
        p.add(c[0]);
        assertEquals("c1 c2 c0", kids(p));
        p = three(c);
        try {
            p.insert(c[1], 0);
            fail("index 0 counts as -1 once the child is out");
        } catch (IndexOutOfBoundsException expected) {
            assertEquals(2, p.getChildCount());
        }
    }

    @Test
    public void aNodeThatTakesNoChildrenRefusesThem() {
        DefaultMutableTreeTableNode closed = new DefaultMutableTreeTableNode("x", false);
        DefaultMutableTreeTableNode c = new DefaultMutableTreeTableNode("c");
        try {
            closed.add(c);
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("this node cannot accept children", expected.getMessage());
        }
        try {
            c.setParent(closed);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("newParent does not allow children", expected.getMessage());
        }
        assertNull(c.getParent());
        DefaultMutableTreeTableNode p = new DefaultMutableTreeTableNode("p");
        p.add(c);
        p.setAllowsChildren(false);
        assertEquals(0, p.getChildCount());
        assertTrue(p.isLeaf());
        assertFalse(p.getAllowsChildren());
    }

    @Test
    public void theDefaultNodeIsItsUserObjectInOneEditableColumn() {
        DefaultMutableTreeTableNode n = new DefaultMutableTreeTableNode("u");
        assertEquals(1, n.getColumnCount());
        assertEquals("u", n.getValueAt(0));
        assertEquals("u", n.getValueAt(3));
        assertTrue(n.isEditable(0));
        n.setValueAt("v", 0);
        assertEquals("v", n.getUserObject());
        assertEquals("v", n.toString());
        assertEquals("", new DefaultMutableTreeTableNode().toString());
        assertTrue(n.isLeaf());
        assertTrue(n.getAllowsChildren());
        AbstractMutableTreeTableNode plain = new AbstractMutableTreeTableNode("plain") {
            @Override
            public Object getValueAt(int column) {
                return "v" + column;
            }

            @Override
            public int getColumnCount() {
                return 2;
            }
        };
        assertFalse(plain.isEditable(0));
        plain.setValueAt("z", 0);
        assertEquals("plain", plain.getUserObject());
        assertTrue(plain.children().hasMoreElements() == false);
    }

    @Test
    public void theDefaultModelAnswersForItsNodesOnly() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("root");
        DefaultMutableTreeTableNode f1 = new DefaultMutableTreeTableNode("f1");
        DefaultMutableTreeTableNode l1 = new DefaultMutableTreeTableNode("l1");
        root.add(f1);
        f1.add(l1);
        DefaultTreeTableModel m = new DefaultTreeTableModel(root, Arrays.asList("Name", "Size"));
        assertSame(root, m.getRoot());
        assertEquals(2, m.getColumnCount());
        assertEquals("Name", m.getColumnName(0));
        assertEquals("F", m.getColumnName(5));
        assertEquals(Object.class, m.getColumnClass(0));
        assertEquals(0, m.getHierarchicalColumn());
        assertEquals("f1", m.getValueAt(f1, 0));
        assertNull("the node has one column", m.getValueAt(f1, 1));
        assertTrue(m.isCellEditable(f1, 0));
        assertFalse(m.isCellEditable(f1, 1));
        assertFalse(m.isLeaf(f1));
        assertTrue(m.isLeaf(l1));
        assertSame(f1, m.getChild(root, 0));
        assertEquals(1, m.getChildCount(f1));
        assertEquals(0, m.getIndexOfChild(f1, l1));
        assertEquals(-1, m.getIndexOfChild("zz", l1));
        assertEquals(Arrays.asList(root, f1, l1), Arrays.asList(m.getPathToRoot(l1)));
        DefaultMutableTreeTableNode orphan = new DefaultMutableTreeTableNode("o");
        try {
            m.getValueAt(orphan, 0);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("node must be a valid node managed by this model", expected.getMessage());
        }
        try {
            m.getValueAt(f1, 9);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("column must be a valid index", expected.getMessage());
        }
        try {
            m.isLeaf("zz");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("node must be a TreeTableNode managed by this model", expected.getMessage());
        }
        try {
            m.getChildCount(orphan);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("parent must be a TreeTableNode managed by this model", expected.getMessage());
        }
        try {
            m.removeNodeFromParent(orphan);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("node does not have a parent.", expected.getMessage());
        }
        try {
            m.valueForPathChanged(new TreePath(orphan), "x");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("invalid path", expected.getMessage());
        }
        assertEquals(0, new DefaultTreeTableModel().getColumnCount());
        assertNull(new DefaultTreeTableModel().getRoot());
        assertEquals(-1, new DefaultTreeTableModel().getHierarchicalColumn());
        DefaultTreeTableModel unnamed = new DefaultTreeTableModel(root);
        assertEquals(1, unnamed.getColumnCount());
        assertEquals("A", unnamed.getColumnName(0));
        assertEquals("AB", unnamed.getColumnName(27));
    }

    @Test
    public void theDefaultModelTellsItsListeners() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("root");
        DefaultMutableTreeTableNode f1 = new DefaultMutableTreeTableNode("f1");
        root.add(f1);
        DefaultTreeTableModel m = new DefaultTreeTableModel(root, Arrays.asList("Name", "Size"));
        Log log = new Log();
        m.addTreeModelListener(log);
        assertEquals(1, m.getTreeModelListeners().length);
        DefaultMutableTreeTableNode x = new DefaultMutableTreeTableNode("x");
        m.insertNodeInto(x, f1, 0);
        assertEquals("inserted [root, f1] [0] [x]", log.last());
        m.setValueAt("x2", x, 0);
        assertEquals("changed [root, f1] [0] [x2]", log.last());
        int before = log.heard.size();
        m.setValueAt("q", x, 1);
        assertEquals("nothing in a column the node does not have", before, log.heard.size());
        m.setUserObject(x, "x3");
        assertEquals("changed [root, f1] [0] [x3]", log.last());
        m.valueForPathChanged(new TreePath(new Object[]{root, f1, x}), "x4");
        assertEquals("changed [root, f1] [0] [x4]", log.last());
        m.setValueAt("r2", root, 0);
        assertEquals("changed [r2] null null", log.last());
        m.removeNodeFromParent(x);
        assertEquals("removed [r2, f1] [0] [x4]", log.last());
        m.setColumnIdentifiers(Arrays.asList("A", "B", "C"));
        assertEquals("structure [r2] [] null", log.last());
        assertEquals(3, m.getColumnCount());
        m.setRoot(null);
        assertEquals("structure null [] null", log.last());
        m.removeTreeModelListener(log);
        assertEquals(0, m.getTreeModelListeners().length);
    }

    @Test
    public void theSupportNamesTheParentOfAChangedNodeAndTellsTheLastListenerFirst() {
        final List<String> order = new ArrayList<String>();
        AbstractTreeTableModel m = new AbstractTreeTableModel("R") {
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public Object getValueAt(Object node, int column) {
                return node + ":" + column;
            }

            @Override
            public Object getChild(Object parent, int index) {
                return "R".equals(parent) ? "c" + index : null;
            }

            @Override
            public int getChildCount(Object parent) {
                return "R".equals(parent) ? 2 : 0;
            }

            @Override
            public int getIndexOfChild(Object parent, Object child) {
                return "c0".equals(child) ? 0 : "c1".equals(child) ? 1 : -1;
            }
        };
        assertFalse(m.isLeaf("R"));
        assertTrue(m.isLeaf("c0"));
        assertFalse(m.isCellEditable("R", 0));
        assertEquals("B", m.getColumnName(1));
        TreeModelSupport s = new TreeModelSupport(m);
        Log log = new Log();
        s.addTreeModelListener(log);
        TreePath rp = new TreePath("R");
        s.firePathChanged(rp.pathByAddingChild("c1"));
        assertEquals("changed [R] [1] [c1]", log.last());
        s.firePathChanged(rp);
        assertEquals("changed [R] null null", log.last());
        s.fireNewRoot();
        assertEquals("structure [R] [] null", log.last());
        s.firePathLeafStateChanged(rp.pathByAddingChild("c0"));
        assertEquals("structure [R, c0] [] null", log.last());
        s.fireChildrenAdded(rp, new int[]{0, 1}, new Object[]{"c0", "c1"});
        assertEquals("inserted [R] [0, 1] [c0, c1]", log.last());
        s.fireChildRemoved(rp, 1, "c1");
        assertEquals("removed [R] [1] [c1]", log.last());
        s.removeTreeModelListener(log);
        for (int i = 0; i < 2; i++) {
            final int id = i;
            s.addTreeModelListener(new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent e) {
                    order.add("L" + id);
                    assertSame("the model is the source", m, e.getSource());
                }

                @Override
                public void treeNodesInserted(TreeModelEvent e) {
                }

                @Override
                public void treeNodesRemoved(TreeModelEvent e) {
                }

                @Override
                public void treeStructureChanged(TreeModelEvent e) {
                }
            });
        }
        s.fireChildChanged(rp, 0, "c0");
        assertEquals(Arrays.asList("L1", "L0"), order);
        try {
            new TreeModelSupport(null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("model must not be null", expected.getMessage());
        }
    }
}
