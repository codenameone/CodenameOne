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
package com.codename1.desktopcompat.javax.swing.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.junit.Test;

/// The walks and relations of the general tree node, tree paths, the
/// default model's events and the selection model's modes. None of this
/// needs a display.
public class DefaultMutableTreeNodeTest {

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    private final DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
    private final DefaultMutableTreeNode a1 = new DefaultMutableTreeNode("a1");
    private final DefaultMutableTreeNode a2 = new DefaultMutableTreeNode("a2");
    private final DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
    private final DefaultMutableTreeNode b1 = new DefaultMutableTreeNode("b1");
    private final DefaultMutableTreeNode b1x = new DefaultMutableTreeNode("b1x");

    {
        root.add(a);
        root.add(b);
        a.add(a1);
        a.add(a2);
        b.add(b1);
        b1.add(b1x);
    }

    private static String walk(Enumeration<?> e) {
        StringBuilder sb = new StringBuilder();
        while (e.hasMoreElements()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.nextElement());
        }
        return sb.toString();
    }

    @Test
    public void theFourWalks() {
        assertEquals("root a a1 a2 b b1 b1x", walk(root.preorderEnumeration()));
        assertEquals("a1 a2 a b1x b1 b root", walk(root.postorderEnumeration()));
        assertEquals("a1 a2 a b1x b1 b root", walk(root.depthFirstEnumeration()));
        assertEquals("root a b a1 a2 b1 b1x", walk(root.breadthFirstEnumeration()));
        assertEquals("root b b1 b1x", walk(b1x.pathFromAncestorEnumeration(root)));
        assertEquals("b b1", walk(b1.pathFromAncestorEnumeration(b)));
        assertEquals("a1 a2", walk(a.children()));
        assertEquals("", walk(a1.children()));
        assertEquals("b b1 b1x", walk(b.preorderEnumeration()));
    }

    @Test
    public void levelsNeighboursAndLeaves() {
        assertEquals(3, root.getDepth());
        assertEquals(0, a1.getDepth());
        assertEquals(3, b1x.getLevel());
        assertSame(root, b1x.getRoot());
        assertTrue(root.isRoot());
        assertEquals(3, root.getLeafCount());
        assertSame(a1, root.getFirstLeaf());
        assertSame(b1x, root.getLastLeaf());
        assertSame(a2, a1.getNextLeaf());
        assertSame(b1x, a2.getNextLeaf());
        assertNull(b1x.getNextLeaf());
        assertSame(a2, b1x.getPreviousLeaf());
        assertSame(b, a2.getNextNode());
        assertSame(a2, b.getPreviousNode());
        assertSame(a, a1.getPreviousNode());
        assertSame(b, a.getNextSibling());
        assertNull(b.getNextSibling());
        assertSame(a, b.getPreviousSibling());
        assertEquals(2, a.getSiblingCount());
        assertSame(root, a1.getSharedAncestor(b1x));
        assertSame(a, a1.getSharedAncestor(a2));
        assertTrue(b1x.isNodeAncestor(b));
        assertTrue(b.isNodeDescendant(b1x));
        assertFalse(a.isNodeDescendant(b1x));
        assertTrue(a1.isNodeSibling(a2));
        assertSame(a2, a.getChildAfter(a1));
        assertNull(a.getChildBefore(a1));
        assertEquals(1, root.getIndex(b));
        assertEquals(-1, root.getIndex(b1));
        TreeNode[] path = b1x.getPath();
        assertEquals(4, path.length);
        assertSame(root, path[0]);
        assertSame(b1x, path[3]);
        assertEquals("b1", b1x.getUserObjectPath()[2]);
    }

    @Test
    public void movingAndRemovingNodes() {
        b.add(a2);
        assertSame(b, a2.getParent());
        assertEquals(1, a.getChildCount());
        assertEquals("b1 a2", walk(b.children()));
        a2.removeFromParent();
        assertNull(a2.getParent());
        b.insert(a2, 0);
        assertEquals("a2 b1", walk(b.children()));
        try {
            b1x.add(root);
            assertTrue("an ancestor cannot become a child", false);
        } catch (IllegalArgumentException expected) {
            assertEquals(2, root.getChildCount());
        }
        DefaultMutableTreeNode leafOnly = new DefaultMutableTreeNode("leaf", false);
        try {
            leafOnly.add(new DefaultMutableTreeNode("x"));
            assertTrue("a node that allows no children takes none", false);
        } catch (IllegalStateException expected) {
            assertEquals(0, leafOnly.getChildCount());
        }
        Object copy = a.clone();
        assertTrue(copy instanceof DefaultMutableTreeNode);
        assertEquals("a", ((DefaultMutableTreeNode) copy).getUserObject());
        assertEquals(0, ((DefaultMutableTreeNode) copy).getChildCount());
        b.removeAllChildren();
        assertTrue(b.isLeaf());
        assertNull(b1.getParent());
    }

    @Test
    public void pathsAreEqualNodeByNode() {
        TreePath one = new TreePath(new Object[]{"r", "x", "y"});
        TreePath two = new TreePath("r").pathByAddingChild("x").pathByAddingChild("y");
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        assertEquals(3, one.getPathCount());
        assertEquals("x", one.getPathComponent(1));
        assertEquals("y", one.getLastPathComponent());
        assertEquals(new TreePath(new Object[]{"r", "x"}), one.getParentPath());
        assertTrue(one.getParentPath().isDescendant(two));
        assertFalse(one.isDescendant(one.getParentPath()));
        assertFalse(one.equals(new TreePath(new Object[]{"r", "z", "y"})));
        assertEquals("[r, x, y]", one.toString());
        assertEquals(3, one.getPath().length);
    }

    @Test
    public void theDefaultModelTellsWhatChanged() {
        DefaultTreeModel model = new DefaultTreeModel(root);
        final List<String> log = new ArrayList<String>();
        model.addTreeModelListener(new TreeModelListener() {
            private String of(String what, TreeModelEvent e) {
                int[] at = e.getChildIndices();
                return what + " " + e.getTreePath() + (at != null && at.length > 0 ? " " + at[0] : "");
            }

            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                log.add(of("changed", e));
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                log.add(of("inserted", e));
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                log.add(of("removed", e));
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                log.add(of("structure", e));
            }
        });
        DefaultMutableTreeNode c = new DefaultMutableTreeNode("c");
        model.insertNodeInto(c, a, 1);
        model.valueForPathChanged(new TreePath(model.getPathToRoot(c)), "see");
        model.removeNodeFromParent(c);
        model.nodeStructureChanged(b);
        model.reload();
        assertEquals("inserted [root, a] 1", log.get(0));
        assertEquals("changed [root, a] 1", log.get(1));
        assertEquals("see", c.getUserObject());
        assertEquals("removed [root, a] 1", log.get(2));
        assertEquals("structure [root, b]", log.get(3));
        assertEquals("structure [root]", log.get(4));
        assertEquals(1, model.getIndexOfChild(root, b));
        assertSame(a2, model.getChild(a, 1));
        assertTrue(model.isLeaf(a1));
        model.setAsksAllowsChildren(true);
        assertFalse(model.isLeaf(a1));
    }

    @Test
    public void theSelectionModelKeepsToItsMode() {
        DefaultTreeSelectionModel sm = new DefaultTreeSelectionModel();
        final TreePath pa = new TreePath(new Object[]{"r", "a"});
        final TreePath pb = new TreePath(new Object[]{"r", "b"});
        final TreePath pc = new TreePath(new Object[]{"r", "c"});
        sm.setRowMapper(new RowMapper() {
            @Override
            public int[] getRowsForPaths(TreePath[] path) {
                int[] out = new int[path.length];
                for (int i = 0; i < path.length; i++) {
                    out[i] = path[i].equals(pa) ? 1 : path[i].equals(pb) ? 2 : path[i].equals(pc) ? 3 : -1;
                }
                return out;
            }
        });
        sm.setSelectionPaths(new TreePath[]{pa, pc});
        assertEquals(2, sm.getSelectionCount());
        assertEquals(1, sm.getMinSelectionRow());
        assertEquals(3, sm.getMaxSelectionRow());
        assertTrue(sm.isRowSelected(3));
        assertFalse(sm.isRowSelected(2));
        sm.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        sm.setSelectionPaths(new TreePath[]{pa, pc});
        assertEquals("rows 1 and 3 are not one run", 1, sm.getSelectionCount());
        sm.addSelectionPath(pb);
        assertEquals(2, sm.getSelectionCount());
        sm.addSelectionPath(pc);
        assertEquals(3, sm.getSelectionCount());
        sm.removeSelectionPath(pb);
        assertTrue("removing the middle of a run clears it", sm.isSelectionEmpty());
        sm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        sm.addSelectionPaths(new TreePath[]{pb, pc});
        assertEquals(1, sm.getSelectionCount());
        assertEquals(pb, sm.getSelectionPath());
        assertEquals(pb, sm.getLeadSelectionPath());
        assertEquals(2, sm.getLeadSelectionRow());
    }
}
