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
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeWillExpandListener;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.javax.swing.tree.ExpandVetoException;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ColorHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.HighlightPredicate;
import com.codename1.desktopcompat.org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import com.codename1.desktopcompat.org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import com.codename1.desktopcompat.org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// `JXTreeTable`: rows that follow the expanded nodes, values per
/// column, the indent of the hierarchical column, the arrow, highlighters
/// that know the node, and a table that follows its model's events.
public class JXTreeTableTest extends KernelTestBase {

    private static final Color RED = new Color(200, 0, 0);
    private static final Color BLUE = new Color(0, 0, 200);

    /// A node with a name and a size: two columns, neither editable
    /// unless asked for.
    private static final class Entry extends AbstractMutableTreeTableNode {
        private Object size;
        private final boolean editable;

        Entry(String name, Object size) {
            this(name, size, false);
        }

        Entry(String name, Object size, boolean editable) {
            super(name);
            this.size = size;
            this.editable = editable;
        }

        @Override
        public Object getValueAt(int column) {
            return column == 0 ? getUserObject() : size;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public boolean isEditable(int column) {
            return editable;
        }

        @Override
        public void setValueAt(Object value, int column) {
            if (column == 0) {
                setUserObject(value);
            } else {
                size = value;
            }
        }
    }

    /// Draws the node and the background it was left with.
    private static final class Stamp extends JComponent implements TreeCellRenderer {
        private String text = "";

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            text = String.valueOf(value);
            setBackground(Color.WHITE);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color bg = getBackground();
            g.drawString(text + "@" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue(), 2, 12);
        }
    }

    private Entry root;
    private Entry docs;
    private Entry notes;
    private Entry todo;
    private Entry readme;
    private DefaultTreeTableModel model;

    /// root > docs > (notes, todo), readme; the root is hidden.
    private JXTreeTable sample() {
        root = new Entry("root", "-");
        docs = new Entry("docs", "2 items");
        notes = new Entry("notes", Integer.valueOf(12));
        todo = new Entry("todo", Integer.valueOf(3));
        readme = new Entry("readme", Integer.valueOf(40));
        root.add(docs);
        docs.add(notes);
        docs.add(todo);
        root.add(readme);
        model = new DefaultTreeTableModel(root, Arrays.asList("Name", "Size"));
        return new JXTreeTable(model);
    }

    private TreePath path(Object... nodes) {
        return new TreePath(nodes);
    }

    private JFrame placed(JXTreeTable t, int w, int h) {
        JPanel p = new JPanel(null);
        t.setBounds(0, 0, w, h);
        p.add(t);
        JFrame f = new JFrame();
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    private static int x(Object[] drawn) {
        return ((Number) drawn[1]).intValue();
    }

    @Test
    public void theRowsAreTheNodesThatShow() {
        JXTreeTable t = sample();
        assertFalse(t.isRootVisible());
        assertTrue(t.getShowsRootHandles());
        assertEquals(2, t.getRowCount());
        assertEquals("docs", t.getValueAt(0, 0));
        assertEquals("readme", t.getValueAt(1, 0));
        assertEquals(path(root, docs), t.getPathForRow(0));
        assertNull(t.getPathForRow(-1));
        assertNull(t.getPathForRow(7));
        assertEquals(-1, t.getRowForPath(path(root)));
        assertEquals(-1, t.getRowForPath(path(root, docs, notes)));
        assertTrue(t.isExpanded(path(root)));
        assertFalse(t.isExpanded(0));
        assertTrue(t.isCollapsed(0));
        assertTrue(t.isCollapsed(-1));
        assertFalse(t.isVisible(path(root, docs, notes)));

        t.expandRow(0);
        assertEquals(4, t.getRowCount());
        assertEquals("notes", t.getValueAt(1, 0));
        assertEquals("todo", t.getValueAt(2, 0));
        assertEquals("readme", t.getValueAt(3, 0));
        assertEquals(2, t.getRowForPath(path(root, docs, todo)));
        assertTrue(t.isExpanded(0));
        assertTrue(t.isVisible(path(root, docs, notes)));

        t.expandRow(1);
        assertEquals("a leaf does not expand", 4, t.getRowCount());

        t.collapsePath(path(root, docs));
        assertEquals(2, t.getRowCount());

        t.expandAll();
        assertEquals(4, t.getRowCount());
        t.collapseAll();
        assertEquals("the hidden root stays expanded", 2, t.getRowCount());

        t.setRootVisible(true);
        assertEquals(3, t.getRowCount());
        assertEquals("root", t.getValueAt(0, 0));
        assertEquals(0, t.getRowForPath(path(root)));
        t.setRootVisible(false);
        assertEquals(2, t.getRowCount());

        t.scrollPathToVisible(path(root, docs, todo));
        assertEquals("the nodes above were expanded", 2, t.getRowForPath(path(root, docs, todo)));
    }

    @Test
    public void everyColumnShowsTheNodesValue() {
        JXTreeTable t = sample();
        t.expandAll();
        assertEquals(2, t.getColumnCount());
        assertEquals("Name", t.getColumnName(0));
        assertEquals("Size", t.getColumnName(1));
        assertEquals(0, t.getHierarchicalColumn());
        assertTrue(t.isHierarchical(0));
        assertFalse(t.isHierarchical(1));
        assertFalse(t.isHierarchical(5));
        assertEquals("2 items", t.getValueAt(0, 1));
        assertEquals(Integer.valueOf(12), t.getValueAt(1, 1));
        assertEquals(Integer.valueOf(40), t.getValueAt(3, 1));
        assertEquals("notes", t.getStringAt(1, 0));
        assertEquals("12", t.getStringAt(1, 1));
        assertFalse(t.isCellEditable(1, 0));

        t.setValueAt(Integer.valueOf(99), 1, 1);
        assertEquals("the node took the value", Integer.valueOf(99), notes.getValueAt(1));

        t.moveColumn(0, 1);
        assertEquals("the hierarchical column moved with its column", 1, t.getHierarchicalColumn());
        assertTrue(t.isHierarchical(1));
        assertEquals("notes", t.getValueAt(1, 1));
        t.getColumnExt(1).setVisible(false);
        assertEquals(-1, t.getHierarchicalColumn());
    }

    @Test
    public void aTreeTableDoesNotSortAndTakesOnlyItsOwnModel() {
        JXTreeTable t = sample();
        assertFalse(t.isSortable());
        assertNull(t.getRowSorter());
        t.setSortable(true);
        t.setAutoCreateRowSorter(true);
        assertFalse(t.isSortable());
        assertFalse(t.getAutoCreateRowSorter());
        assertNull(t.getRowSorter());
        try {
            t.setModel(new DefaultTableModel(2, 2));
            fail("a plain table model was taken");
        } catch (IllegalArgumentException expected) {
            assertEquals(2, t.getColumnCount());
        }
        assertSame(model, t.getTreeTableModel());
    }

    @Test
    public void theHierarchicalColumnIndentsByDepth() {
        JXTreeTable t = sample();
        t.expandAll();
        JFrame f = placed(t, 400, 300);
        List<Object[]> drawn = paint(f);
        Object[] folder = find(drawn, "docs");
        Object[] child = find(drawn, "notes");
        Object[] sibling = find(drawn, "readme");
        assertNotNull("the default renderer shows the model's value", folder);
        assertNotNull(child);
        assertNotNull(sibling);
        assertNotNull("the other column is a plain table column", find(drawn, "2 items"));
        assertEquals("same depth, same indent", x(folder), x(sibling));
        int step = x(child) - x(folder);
        assertTrue("a child is indented one step: " + step, step >= 2 * 20);

        t.setShowsRootHandles(false);
        List<Object[]> flat = paint(f);
        assertEquals("without root handles the first level loses its step", x(folder) - step,
                x(find(flat, "docs")));
        t.setShowsRootHandles(true);
        t.setRootVisible(true);
        List<Object[]> rooted = paint(f);
        assertNotNull(find(rooted, "root"));
        assertEquals("with a root row every node is one step further in", x(child) + step,
                x(find(rooted, "notes")));
    }

    @Test
    public void aPressOnTheArrowTogglesTheNode() {
        JXTreeTable t = sample();
        JFrame f = placed(t, 400, 300);
        int rh = t.getRowHeight();
        assertEquals(2, t.getRowCount());
        // First level, root handles shown: the arrow is in the first step.
        press(f, t, 10, rh / 2);
        release(f, t, 10, rh / 2);
        assertEquals("the press on the arrow expanded docs", 4, t.getRowCount());
        assertFalse(t.isEditing());
        assertEquals("and selected its row", 0, t.getSelectedRow());

        // readme is a leaf: nothing to toggle where its arrow would be.
        press(f, t, 10, 3 * rh + rh / 2);
        release(f, t, 10, 3 * rh + rh / 2);
        assertEquals(4, t.getRowCount());
        assertEquals(3, t.getSelectedRow());

        press(f, t, 10, rh / 2);
        release(f, t, 10, rh / 2);
        assertEquals("a second press collapsed it", 2, t.getRowCount());

        // One press on the text selects and leaves the node alone.
        t.setToggleClickCount(1);
        t.setToggleClickCount(2);
        press(f, t, 200, rh + rh / 2);
        release(f, t, 200, rh + rh / 2);
        assertEquals(2, t.getRowCount());
        assertEquals(1, t.getSelectedRow());
    }

    @Test
    public void theToggleClickCountOnTheCellTogglesTheNode() {
        JXTreeTable t = sample();
        JFrame f = placed(t, 400, 300);
        int rh = t.getRowHeight();
        assertEquals(2, t.getToggleClickCount());
        press(f, t, 100, rh / 2);
        release(f, t, 100, rh / 2);
        assertEquals("one click does nothing", 2, t.getRowCount());
        press(f, t, 100, rh / 2);
        release(f, t, 100, rh / 2);
        assertEquals("the second click expanded docs", 4, t.getRowCount());
    }

    @Test
    public void highlightersKnowTheNode() {
        JXTreeTable t = sample();
        t.expandAll();
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.IS_LEAF, RED, null));
        t.addHighlighter(new ColorHighlighter(new HighlightPredicate.DepthHighlightPredicate(2), null, BLUE));
        // docs: a folder at depth 1.
        Component c = t.prepareRenderer(0, 1);
        assertFalse(RED.equals(c.getBackground()));
        assertFalse(BLUE.equals(c.getForeground()));
        // notes: a leaf at depth 2.
        c = t.prepareRenderer(1, 1);
        assertEquals(RED, c.getBackground());
        assertEquals(BLUE, c.getForeground());
        // readme: a leaf at depth 1.
        c = t.prepareRenderer(3, 1);
        assertEquals(RED, c.getBackground());
        assertFalse(BLUE.equals(c.getForeground()));

        // In the hierarchical column the tree cell renderer's component
        // is what the highlighters decorate.
        t.setTreeCellRenderer(new Stamp());
        JFrame f = placed(t, 400, 300);
        List<Object[]> drawn = paint(f);
        assertNotNull(find(drawn, "docs@255,255,255"));
        assertNotNull(find(drawn, "notes@200,0,0"));
        assertNotNull(find(drawn, "readme@200,0,0"));

        t.setHighlighters(new ColorHighlighter(HighlightPredicate.IS_FOLDER, BLUE, null));
        drawn = paint(f);
        assertNotNull(find(drawn, "docs@0,0,200"));
        assertNotNull(find(drawn, "notes@255,255,255"));

        t.setHighlighters(new ColorHighlighter(new HighlightPredicate() {
            @Override
            public boolean isHighlighted(Component renderer,
                    com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
                return adapter.isHierarchical() && adapter.isExpanded() && "docs".equals(adapter.getString());
            }
        }, RED, null));
        drawn = paint(f);
        assertNotNull(find(drawn, "docs@200,0,0"));
        assertNotNull(find(drawn, "notes@255,255,255"));
        assertFalse(RED.equals(t.prepareRenderer(0, 1).getBackground()));
    }

    @Test
    public void theTableFollowsTheModelsEvents() {
        JXTreeTable t = sample();
        t.expandAll();
        t.setRowSelectionInterval(3, 3);
        Entry draft = new Entry("draft", Integer.valueOf(1));
        model.insertNodeInto(draft, docs, 0);
        assertEquals(5, t.getRowCount());
        assertEquals("draft", t.getValueAt(1, 0));
        assertEquals("the selection stayed with readme", 4, t.getSelectedRow());

        model.removeNodeFromParent(draft);
        assertEquals(4, t.getRowCount());
        assertEquals("notes", t.getValueAt(1, 0));
        assertEquals(3, t.getSelectedRow());

        t.collapseRow(0);
        model.insertNodeInto(draft, docs, 0);
        assertEquals("a node below a collapsed one has no row", 2, t.getRowCount());
        t.expandRow(0);
        assertEquals(5, t.getRowCount());

        model.setValueAt("renamed", todo, 0);
        assertEquals("renamed", t.getValueAt(3, 0));
        model.setValueAt(Integer.valueOf(7), todo, 1);
        assertEquals(Integer.valueOf(7), t.getValueAt(3, 1));

        model.setColumnIdentifiers(Arrays.asList("Name", "Size", "Kind"));
        assertEquals("the columns were made again", 3, t.getColumnCount());
        assertEquals("Kind", t.getColumnName(2));
        assertNull("a node with two columns has nothing in the third", t.getValueAt(0, 2));

        Entry other = new Entry("other", "-");
        other.add(new Entry("only", Integer.valueOf(5)));
        model.setRoot(other);
        assertEquals(1, t.getRowCount());
        assertEquals("only", t.getValueAt(0, 0));
    }

    @Test
    public void theFirstChildrenOfAHiddenRootShow() {
        Entry empty = new Entry("empty", "-");
        DefaultTreeTableModel m = new DefaultTreeTableModel(empty, Arrays.asList("Name", "Size"));
        JXTreeTable t = new JXTreeTable(m);
        assertEquals(0, t.getRowCount());
        m.insertNodeInto(new Entry("first", Integer.valueOf(1)), empty, 0);
        assertEquals(1, t.getRowCount());
        assertEquals("first", t.getValueAt(0, 0));
    }

    @Test
    public void anotherModelReplacesColumnsAndRows() {
        JXTreeTable t = sample();
        final List<String> names = new ArrayList<String>();
        t.addPropertyChangeListener(new com.codename1.desktopcompat.java.beans.PropertyChangeListener() {
            @Override
            public void propertyChange(com.codename1.desktopcompat.java.beans.PropertyChangeEvent e) {
                names.add(e.getPropertyName());
            }
        });
        DefaultMutableTreeTableNode r = new DefaultMutableTreeTableNode("r");
        r.add(new DefaultMutableTreeTableNode("one"));
        DefaultTreeTableModel next = new DefaultTreeTableModel(r, Arrays.asList("Only"));
        t.setTreeTableModel(next);
        assertTrue(names.contains("treeTableModel"));
        assertSame(next, t.getTreeTableModel());
        assertEquals(1, t.getColumnCount());
        assertEquals(1, t.getRowCount());
        assertEquals("one", t.getValueAt(0, 0));
        assertTrue("the user object of a default node can be edited", t.isCellEditable(0, 0));
        // The old model is let go.
        model.insertNodeInto(new Entry("late", "-"), root, 0);
        assertEquals(1, t.getRowCount());
        try {
            t.setTreeTableModel(null);
            fail("null was taken for a model");
        } catch (NullPointerException expected) {
            assertSame(next, t.getTreeTableModel());
        }

        JXTreeTable none = new JXTreeTable();
        assertEquals(0, none.getRowCount());
        assertEquals(0, none.getColumnCount());
        assertNull(none.getTreeTableModel().getRoot());
    }

    @Test
    public void expansionListenersHearTheTable() {
        final JXTreeTable t = sample();
        final List<String> heard = new ArrayList<String>();
        TreeExpansionListener l = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                heard.add("expanded " + e.getPath().getLastPathComponent() + " " + (e.getSource() == t) + " "
                        + t.getRowCount());
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
                heard.add("collapsed " + e.getPath().getLastPathComponent() + " " + (e.getSource() == t) + " "
                        + t.getRowCount());
            }
        };
        t.addTreeExpansionListener(l);
        t.expandRow(0);
        t.setRowSelectionInterval(2, 2);
        t.collapseRow(0);
        assertEquals(Arrays.asList("expanded docs true 4", "collapsed docs true 2"), heard);
        assertEquals("the selection moved up to the collapsed node", 0, t.getSelectedRow());
        t.removeTreeExpansionListener(l);
        t.expandRow(0);
        assertEquals(2, heard.size());
        t.collapseRow(0);

        t.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
                throw new ExpandVetoException(e);
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent e) {
            }
        });
        t.expandRow(0);
        assertEquals("the listener refused", 2, t.getRowCount());
    }

    @Test
    public void anEditorInTheHierarchicalColumnStartsAfterTheIndent() {
        DefaultMutableTreeTableNode r = new DefaultMutableTreeTableNode("r");
        DefaultMutableTreeTableNode a = new DefaultMutableTreeTableNode("a");
        r.add(a);
        a.add(new DefaultMutableTreeTableNode("b"));
        JXTreeTable t = new JXTreeTable(new DefaultTreeTableModel(r, Arrays.asList("Name")));
        t.expandAll();
        placed(t, 400, 300);
        assertTrue(t.editCellAt(0, 0));
        int step = t.getEditorComponent().getX() - t.getCellRect(0, 0, false).x;
        assertTrue("the first level is one step in, for its arrow: " + step, step >= 20);
        t.removeEditor();
        assertTrue(t.editCellAt(1, 0));
        Component editor = t.getEditorComponent();
        assertNotNull(editor);
        assertEquals("one level down is one step more", 2 * step, editor.getX() - t.getCellRect(1, 0, false).x);
        t.doLayout();
        assertEquals(2 * step, t.getEditorComponent().getX() - t.getCellRect(1, 0, false).x);
        t.removeEditor();
    }
}
