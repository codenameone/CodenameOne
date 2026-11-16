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
package com.codename1.ui.table;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The grid through its surface: what a tap, a drag and a key do, what a
/// paint reads, and how an edit reaches the model.
class DataGridTest extends UITestBase {

    private DataGrid show(DataGrid g) {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, g);
        form.revalidate();
        g.refreshLayout();
        return g;
    }

    private static DefaultTableModel people() {
        return new DefaultTableModel(new String[]{"Name", "Team", "Score", "Active"}, new Object[][]{
                {"dana", "red", Integer.valueOf(7), Boolean.TRUE},
                {"Ann", "blue", Integer.valueOf(3), Boolean.FALSE},
                {"carl", "red", Integer.valueOf(3), Boolean.TRUE},
                {"Bea", "blue", Integer.valueOf(9), Boolean.FALSE},
        }, true);
    }

    // The middle of a cell, in the grid's coordinates.
    private static int[] at(DataGrid g, int row, int column) {
        int[] b = new int[4];
        g.getCellBounds(row, column, b);
        return new int[]{b[0] + b[2] / 2, b[1] + b[3] / 2};
    }

    private static int[] header(DataGrid g, int column) {
        int[] b = new int[4];
        g.getCellBounds(0, column, b);
        return new int[]{b[0] + b[2] / 2, g.getHeaderHeight() / 2};
    }

    private static long tapClock;

    private static void tap(DataGrid g, int[] p) {
        // Taps of a test follow each other in microseconds; spread them so
        // two in a row are not taken for a double tap.
        long now = System.currentTimeMillis();
        while (now - tapClock < 400) {
            Thread.yield();
            now = System.currentTimeMillis();
        }
        g.pointerPressed(g.getAbsoluteX() + p[0], g.getAbsoluteY() + p[1]);
        g.pointerReleased(g.getAbsoluteX() + p[0], g.getAbsoluteY() + p[1]);
        tapClock = System.currentTimeMillis();
    }

    private static void doubleTap(DataGrid g, int[] p) {
        long now = System.currentTimeMillis();
        while (now - tapClock < 400) {
            Thread.yield();
            now = System.currentTimeMillis();
        }
        for (int i = 0; i < 2; i++) {
            g.pointerPressed(g.getAbsoluteX() + p[0], g.getAbsoluteY() + p[1]);
            g.pointerReleased(g.getAbsoluteX() + p[0], g.getAbsoluteY() + p[1]);
        }
        tapClock = System.currentTimeMillis();
    }

    private static void drag(DataGrid g, int fx, int fy, int tx, int ty) {
        int ax = g.getAbsoluteX();
        int ay = g.getAbsoluteY();
        g.pointerPressed(ax + fx, ay + fy);
        g.pointerDragged(ax + (fx + tx) / 2, ay + (fy + ty) / 2);
        g.pointerDragged(ax + tx, ay + ty);
        g.pointerReleased(ax + tx, ay + ty);
    }

    @FormTest
    void tappingAHeaderSortsAscendingThenDescendingThenNot() {
        DataGrid g = show(new DataGrid(people()));
        tap(g, header(g, 0));
        assertEquals(1, g.getSortDirection(g.getColumn(0)));
        assertEquals("Ann", g.getValueAt(0, g.getColumn(0)));
        tap(g, header(g, 0));
        assertEquals(-1, g.getSortDirection(g.getColumn(0)));
        assertEquals("dana", g.getValueAt(0, g.getColumn(0)));
        tap(g, header(g, 0));
        assertEquals(0, g.getSortDirection(g.getColumn(0)));
        assertEquals(0, g.getModelRow(0));
    }

    @FormTest
    void shiftOnAHeaderAddsASecondSortKey() {
        DataGrid g = show(new DataGrid(people()));
        tap(g, header(g, 2));
        implementation.setShiftKeyDown(true);
        try {
            tap(g, header(g, 0));
        } finally {
            implementation.setShiftKeyDown(false);
        }
        assertEquals(2, g.getSortColumns().size());
        // the 3s ordered by name: Ann, carl
        assertEquals("Ann", g.getValueAt(0, g.getColumn(0)));
        assertEquals("carl", g.getValueAt(1, g.getColumn(0)));
    }

    @FormTest
    void theSelectionStaysOnItsRowsWhenTheyAreSorted() {
        DataGrid g = show(new DataGrid(people()));
        tap(g, at(g, 3, 0));
        assertEquals(3, g.getSelectedRow());
        assertEquals("Bea", g.getValueAt(g.getSelectedRow(), g.getColumn(0)));
        g.sortBy(g.getColumn(0), true);
        assertEquals(1, g.getSelectedRow());
        assertEquals("Bea", g.getValueAt(g.getSelectedRow(), g.getColumn(0)));
        assertTrue(g.isRowSelected(1));
    }

    @FormTest
    void shiftAndControlTapsExtendAndToggle() {
        DataGrid g = show(new DataGrid(people()));
        final int[] events = new int[1];
        g.addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(int oldSelected, int newSelected) {
                events[0]++;
            }
        });
        tap(g, at(g, 0, 0));
        implementation.setShiftKeyDown(true);
        try {
            tap(g, at(g, 2, 0));
        } finally {
            implementation.setShiftKeyDown(false);
        }
        assertEquals(3, g.getSelectedRowCount());
        assertTrue(events[0] >= 2);
        g.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
        assertEquals(1, g.getSelectedRowCount());
    }

    @FormTest
    void aTapOnABooleanCellTogglesItInTheModel() {
        DefaultTableModel m = people();
        DataGrid g = show(new DataGrid(m));
        tap(g, at(g, 1, 3));
        assertEquals(Boolean.TRUE, m.getValueAt(1, 3));
        assertNull(findEditor(), "a check box needs no editor");
    }

    @FormTest
    void aCellIsEditedInAFieldOverItAndThePaintedCellComesBack() {
        DefaultTableModel m = people();
        DataGrid g = show(new DataGrid(m));
        assertTrue(g.editCellAt(0, 2));
        assertTrue(g.isEditing());
        TextField tf = findEditor();
        assertNotNull(tf, "the editor is in the layered pane");
        assertEquals("7", tf.getText());
        g.getComponentForm().revalidate();
        int[] cellBounds = new int[4];
        g.getCellBounds(0, 2, cellBounds);
        assertEquals(cellBounds[2], tf.getWidth(), "the editor is as wide as the cell");
        assertEquals(cellBounds[3], tf.getHeight(), "the editor is as tall as the cell");
        assertEquals(g.getAbsoluteX() + cellBounds[0], tf.getAbsoluteX(), "the editor lies over the cell");
        assertEquals(g.getAbsoluteY() + cellBounds[1], tf.getAbsoluteY(), "the editor lies over the cell");
        tf.setText("42");
        g.commitEdit();
        assertFalse(g.isEditing());
        assertEquals(Integer.valueOf(42), m.getValueAt(0, 2), "a number is read back as the type the cell held");
        assertNull(findEditor(), "nothing stays in the hierarchy");

        // Text that is not a number leaves the cell as it was.
        g.editCellAt(0, 2);
        findEditor().setText("lots");
        g.commitEdit();
        assertEquals(Integer.valueOf(42), m.getValueAt(0, 2));

        // A cancelled edit writes nothing.
        g.editCellAt(0, 0);
        findEditor().setText("changed");
        g.cancelEdit();
        assertEquals("dana", m.getValueAt(0, 0));
    }

    @FormTest
    void groupingAfterLayoutWidensTheIndentedColumn() {
        DataGrid g = show(new DataGrid(people()));
        int[] b = new int[4];
        g.getCellBounds(0, 0, b);
        int plain = b[2];
        g.setGroupColumn(g.getColumn(1));
        g.getCellBounds(1, 0, b);
        assertTrue(b[2] > plain, "the first column makes room for the indent and the arrow, "
                + plain + " -> " + b[2]);
    }

    @FormTest
    void scrollingCommitsTheEdit() {
        DefaultTableModel m = big(200);
        DataGrid g = show(new DataGrid(m));
        g.editCellAt(0, 0);
        findEditor().setText("edited");
        g.scrollTo(0, g.getRowHeight() * 20);
        assertFalse(g.isEditing());
        assertEquals("edited", m.getValueAt(0, 0));
    }

    @FormTest
    void aColumnTheModelOrTheColumnForbidsIsNotEdited() {
        DefaultTableModel m = new DefaultTableModel(new String[]{"a"}, new Object[][]{{"x"}}, false);
        DataGrid g = show(new DataGrid(m));
        assertFalse(g.editCellAt(0, 0));
        DefaultTableModel e = people();
        DataGrid g2 = show(new DataGrid(e));
        g2.getColumn(0).setEditable(false);
        assertFalse(g2.editCellAt(0, 0));
    }

    private TextField findEditor() {
        Form f = Display.getInstance().getCurrent();
        Container layer = f.getFormLayeredPane(DataGrid.class, true);
        return find(layer);
    }

    private static TextField find(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof TextField) {
                return (TextField) k;
            }
            if (k instanceof Container) {
                TextField t = find((Container) k);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    @FormTest
    void theKeyboardMovesTheLeadAndScrollsToIt() {
        DataGrid g = show(new DataGrid(big(500)));
        g.setSelectedRow(0);
        int down = keyFor(Display.GAME_DOWN);
        for (int i = 0; i < 100; i++) {
            g.keyPressed(down);
        }
        assertEquals(100, g.getSelectedRow());
        int top = g.getOffsetY() / g.getRowHeight();
        int bottom = (g.getOffsetY() + g.getBodyHeight()) / g.getRowHeight();
        assertTrue(top <= 100 && 100 <= bottom, "row 100 is on screen: " + top + ".." + bottom);
    }

    private int keyFor(int gameAction) {
        for (int k = -200; k < 300; k++) {
            if (Display.getInstance().getGameAction(k) == gameAction) {
                return k;
            }
        }
        throw new AssertionError("no key for game action " + gameAction);
    }

    @FormTest
    void frozenColumnsStayInTheLeftHeader() {
        DataGrid g = show(new DataGrid(people()));
        g.setFrozenColumns(1);
        g.refreshLayout();
        int[] b = new int[4];
        g.getCellBounds(0, 0, b);
        assertEquals(b[2], g.getHeaderWidth(), "the frozen column is the left header");
        g.getCellBounds(0, 1, b);
        int before = b[0];
        // wider than any screen, at any density
        g.getColumn(1).setWidthMM(Display.getInstance().getDisplayWidth() * 2);
        g.refreshColumns();
        g.refreshLayout();
        g.scrollTo(50, 0);
        int[] f = new int[4];
        g.getCellBounds(0, 0, f);
        assertEquals(0, f[0], "the frozen column does not move");
        g.getCellBounds(0, 1, b);
        assertEquals(before - g.getOffsetX(), b[0], "the others scroll");
        assertTrue(g.getOffsetX() > 0);
    }

    @FormTest
    void draggingAHeaderEdgeResizesAndTheHeaderMovesAColumn() {
        DataGrid g = show(new DataGrid(people()));
        g.setDragEdits(true);
        int[] b = new int[4];
        g.getCellBounds(0, 0, b);
        int edge = b[0] + b[2] - 1;
        int y = g.getHeaderHeight() / 2;
        int w0 = b[2];
        drag(g, edge, y, edge + 60, y);
        g.refreshLayout();
        g.getCellBounds(0, 0, b);
        assertEquals(w0 + 60, b[2], 2, "the column is as wide as the drag made it");
        assertTrue(g.getColumn(0).getWidthMM() > 0, "and keeps the width");

        GridColumn first = g.getColumn(0);
        int[] h0 = header(g, 0);
        int[] h2 = header(g, 2);
        drag(g, h0[0], h0[1], h2[0], h2[1]);
        assertSame(first, g.getColumn(2), "the header was dropped on the third column");
    }

    @FormTest
    void groupRowsOpenAndCloseWithATap() {
        DataGrid g = show(new DataGrid(people()));
        g.getColumn(2).setAggregate(GridAggregate.SUM);
        g.setGroupColumn(g.getColumn(1));
        assertEquals(6, g.getRowCount());
        assertTrue(g.isGroupRow(0));
        assertEquals("blue", g.getValueAt(0, g.getColumn(1)));
        assertEquals(Long.valueOf(12), g.getValueAt(0, g.getColumn(2)));
        tap(g, at(g, 0, 2));
        assertEquals(4, g.getRowCount());
        assertFalse(g.isExpanded(0));
        g.expandAll();
        assertEquals(6, g.getRowCount());
    }

    @FormTest
    void aTreeOpensWithItsArrowAndTheKeyboard() {
        GridRowsTest.PathTree m = new GridRowsTest.PathTree();
        DataGrid g = show(new DataGrid(m));
        assertTrue(g.isTree());
        assertEquals(3, g.getRowCount());
        int[] b = new int[4];
        g.getCellBounds(0, 0, b);
        // The arrow is at the start of a root's cell.
        tap(g, new int[]{b[0] + Display.getInstance().convertToPixels(2), b[1] + b[3] / 2});
        assertEquals(5, g.getRowCount());
        assertEquals("a/2", g.getNode(1));
        assertEquals(1, g.getDepth(1));

        g.setSelectedRow(2);
        g.keyPressed(keyFor(Display.GAME_RIGHT));
        assertTrue(g.isExpanded(2), "right opens a/1");
        assertEquals(6, g.getRowCount());
        g.keyPressed(keyFor(Display.GAME_LEFT));
        assertFalse(g.isExpanded(2), "left closes it");
        g.keyPressed(keyFor(Display.GAME_LEFT));
        assertEquals(0, g.getSelectedRow(), "left on a closed child goes to its parent");

        // The selection follows its rows when a node above them closes.
        g.setSelectedRow(3);
        assertEquals("b", g.getNode(3));
        g.setExpanded(0, false);
        assertEquals(1, g.getSelectedRow());
        assertEquals("b", g.getNode(g.getSelectedRow()));
    }

    @FormTest
    void aTreeCellIsEditedThroughTheTreeModel() {
        GridRowsTest.PathTree m = new GridRowsTest.PathTree();
        DataGrid g = show(new DataGrid(m));
        assertTrue(g.editCellAt(1, 1));
        findEditor().setText("11");
        g.commitEdit();
        assertEquals(Integer.valueOf(11), m.getValueAt("b", 1));
    }

    /// A million rows cost a paint what the rows on screen cost: the model is
    /// asked for the values of those rows and no others.
    @FormTest
    void aPaintReadsOnlyTheRowsOnScreen() {
        final int n = 1000000;
        final int[] reads = new int[1];
        final int[] maxRow = new int[1];
        AbstractTableModel m = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return n;
            }

            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int i) {
                return "c" + i;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Object getValueAt(int row, int column) {
                reads[0]++;
                maxRow[0] = Math.max(maxRow[0], row);
                return "r" + row;
            }

            @Override
            public void setValueAt(int row, int column, Object o) {
            }

            @Override
            public void addDataChangeListener(com.codename1.ui.events.DataChangedListener d) {
            }

            @Override
            public void removeDataChangeListener(com.codename1.ui.events.DataChangedListener d) {
            }
        };
        DataGrid g = show(new DataGrid(m));
        for (int i = 0; i < 3; i++) {
            g.getColumn(i).setWidthMM(20);
        }
        g.refreshColumns();
        g.refreshLayout();
        g.scrollTo(0, g.getRowHeight() * 500000);
        int onScreen = g.getBodyHeight() / g.getRowHeight() + 2;
        reads[0] = 0;
        maxRow[0] = 0;
        Image img = Image.createImage(Math.max(1, g.getWidth()), Math.max(1, g.getHeight()));
        Graphics gr = img.getGraphics();
        gr.translate(-g.getX(), -g.getY());
        g.paint(gr);
        assertTrue(reads[0] > 0, "the visible cells were read");
        assertTrue(reads[0] <= onScreen * 3, "only the rows on screen were read: " + reads[0]);
        assertTrue(maxRow[0] < 500000 + onScreen && maxRow[0] >= 500000);
    }

    @FormTest
    void aModelChangeIsShownAndTheSelectionKept() {
        DefaultTableModel m = people();
        DataGrid g = show(new DataGrid(m));
        g.sortBy(g.getColumn(0), true);
        g.setSelectedRow(0);
        assertEquals("Ann", g.getValueAt(0, g.getColumn(0)));
        m.addRow("Aaron", "green", Integer.valueOf(1), Boolean.TRUE);
        assertEquals(5, g.getRowCount());
        assertEquals("Aaron", g.getValueAt(0, g.getColumn(0)), "the new row sorts first");
        assertEquals("Ann", g.getValueAt(g.getSelectedRow(), g.getColumn(0)), "the selection stayed on Ann");
    }

    @FormTest
    void aFilterShowsOnlyMatchingRows() {
        DataGrid g = show(new DataGrid(people()));
        g.setFilter(new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                Object v = row.getValue(2);
                return v instanceof Integer && ((Integer) v).intValue() > 5;
            }
        });
        assertEquals(2, g.getRowCount());
        assertEquals(3, g.getModelRow(1));
        assertEquals(-1, g.getViewRow(1));
        g.setFilter(null);
        assertEquals(4, g.getRowCount());
    }

    @FormTest
    void selectingTheOnlyShownRowDoesNotSelectEveryRowWhenTheFilterClears() {
        DataGrid g = show(new DataGrid(people()));
        g.setSelectionMode(DataGrid.SelectionMode.MULTIPLE_ROWS);
        g.setFilter(new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                return "Bea".equals(row.getValue(0));
            }
        });
        g.setSelectedRow(0);
        g.setFilter(null);
        assertEquals(1, g.getSelectedRows().length);
        assertEquals("Bea", g.getValueAt(g.getSelectedRow(), g.getColumn(0)));

        // Select all is remembered as select all.
        g.selectAll();
        g.setFilter(new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                return "Bea".equals(row.getValue(0));
            }
        });
        g.setFilter(null);
        assertEquals(4, g.getSelectedRows().length);
    }

    @FormTest
    void insertingAndRemovingModelRowsKeepsTheSelectionOnItsRecord() {
        DefaultTableModel m = people();
        DataGrid g = show(new DataGrid(m));
        g.setSelectedRow(2);
        m.insertRow(0, "zed", "green", Integer.valueOf(1), Boolean.TRUE);
        assertEquals("carl", g.getValueAt(g.getSelectedRow(), g.getColumn(0)));
        m.addRow("yan", "green", Integer.valueOf(2), Boolean.TRUE);
        assertEquals("carl", g.getValueAt(g.getSelectedRow(), g.getColumn(0)), "an appended row moves nothing");
        // A removal does not say which row went, so the selection is let go
        // rather than left on the record that slid into its place.
        m.removeRow(0);
        assertEquals(-1, g.getSelectedRow());
    }

    @FormTest
    void aModelChangeWhileEditingClosesTheEditorWithoutWritingAnotherRecord() {
        DefaultTableModel m = people();
        DataGrid g = show(new DataGrid(m));
        assertTrue(g.editCellAt(1, 0));
        findEditor().setText("changed");
        m.insertRow(0, "zed", "green", Integer.valueOf(1), Boolean.TRUE);
        assertFalse(g.isEditing());
        assertEquals("zed", m.getValueAt(0, 0));
        assertEquals("Ann", m.getValueAt(2, 0));
        assertFalse(g.editCellAt(99, 0), "a row past the end is not editable");
    }

    private static DefaultTableModel big(int n) {
        Object[][] rows = new Object[n][];
        for (int i = 0; i < n; i++) {
            rows[i] = new Object[]{"row " + i, Integer.valueOf(i)};
        }
        return new DefaultTableModel(new String[]{"Name", "N"}, rows, true);
    }
}
