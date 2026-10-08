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

import static org.junit.Assert.assertArrayEquals;
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
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import com.codename1.desktopcompat.javax.swing.event.TableModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableModelListener;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.javax.swing.table.JTableHeader;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.javax.swing.table.TableColumnModel;
import com.codename1.desktopcompat.javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;

/// The table: its model's events, indexes under sorting, filtering and
/// moved columns, selection, geometry in logical pixels, column sizing,
/// renderers, the header and the pointer and keys.
public class JTableTest extends KernelTestBase {

    /// The kernel counts clicks by time alone and keeps the count between
    /// tests, so the taps made here would reach the next test class as
    /// one long multiple click. Wait the count out.
    @AfterClass
    public static void letTheClickCountRunOut() throws InterruptedException {
        Thread.sleep(600);
    }

    /// name, age, member: `String`, `Integer`, `Boolean`.
    private static final class People extends DefaultTableModel {
        People() {
            super(new Object[][]{
                {"Carol", Integer.valueOf(30), Boolean.TRUE},
                {"Alice", Integer.valueOf(10), Boolean.FALSE},
                {"Bob", Integer.valueOf(20), Boolean.TRUE},
                {"Dave", Integer.valueOf(40), Boolean.FALSE}}, new Object[]{"Name", "Age", "Member"});
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : column == 1 ? Integer.class : Boolean.class;
        }
    }

    /// A renderer that draws text itself and remembers what it was asked.
    private static final class Stamp extends JComponent implements TableCellRenderer {
        final List<String> asked = new ArrayList<String>();
        private String text = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            asked.add(row + "," + column + "," + value + "," + isSelected);
            text = "cell:" + value;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.drawString(text, 2, 12);
        }
    }

    private JFrame placed(JTable t, int w, int h) {
        JPanel p = new JPanel(null);
        t.setBounds(0, 0, w, h);
        p.add(t);
        JFrame f = new JFrame();
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    private static int[] widths(JTable t) {
        TableColumnModel cm = t.getColumnModel();
        int[] out = new int[cm.getColumnCount()];
        for (int i = 0; i < out.length; i++) {
            out[i] = cm.getColumn(i).getWidth();
        }
        return out;
    }

    private static void tap(JTable t, int x, int y, int modifiers, int clicks) {
        long now = System.currentTimeMillis();
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_PRESSED, now, modifiers, x, y, clicks, false,
                MouseEvent.BUTTON1));
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_RELEASED, now, modifiers, x, y, clicks, false,
                MouseEvent.BUTTON1));
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_CLICKED, now, modifiers, x, y, clicks, false,
                MouseEvent.BUTTON1));
    }

    private static void key(Component c, int code) {
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code,
                KeyEvent.CHAR_UNDEFINED));
    }

    // ------------------------------------------------------------ construction

    @Test
    public void everyConstructionFormMakesColumnsFromTheModel() {
        assertEquals(0, new JTable().getColumnCount());
        JTable sized = new JTable(3, 4);
        assertEquals(3, sized.getRowCount());
        assertEquals(4, sized.getColumnCount());
        assertEquals("A", sized.getColumnName(0));
        final Object[][] data = {{"x", "y"}, {"z", "w"}};
        JTable arrays = new JTable(data, new Object[]{"One", "Two"});
        assertEquals("Two", arrays.getColumnModel().getColumn(1).getHeaderValue());
        assertTrue(arrays.isCellEditable(0, 0));
        arrays.setValueAt("changed", 1, 1);
        assertEquals("changed", data[1][1]);
        JTable people = new JTable(new People());
        assertEquals(Integer.class, people.getColumnClass(1));
        assertEquals("Bob", people.getValueAt(2, 0));
        assertSame(people, people.getTableHeader().getTable());
    }

    @Test
    public void defaultRenderersAndEditorsAreFoundByAssignability() {
        JTable t = new JTable(new People());
        TableCellRenderer forObject = t.getDefaultRenderer(Object.class);
        assertNotNull(forObject);
        assertSame(forObject, t.getDefaultRenderer(String.class));
        assertSame(t.getDefaultRenderer(Number.class), t.getDefaultRenderer(Integer.class));
        assertTrue(t.getDefaultRenderer(Boolean.class) instanceof JCheckBox);
        Stamp mine = new Stamp();
        t.setDefaultRenderer(Integer.class, mine);
        assertSame(mine, t.getCellRenderer(0, 1));
        assertSame(t.getDefaultRenderer(Number.class), t.getDefaultRenderer(Double.class));
        TableColumn c = t.getColumnModel().getColumn(0);
        c.setCellRenderer(mine);
        assertSame(mine, t.getCellRenderer(3, 0));
        assertSame(t.getDefaultEditor(Number.class), t.getDefaultEditor(Long.class));
        assertNotNull(t.getDefaultEditor(Boolean.class));
    }

    // ------------------------------------------------------------ model events

    @Test
    public void modelEventsMoveTheSelectionAndRemakeTheColumns() {
        People model = new People();
        JTable t = new JTable(model);
        t.setRowHeight(20);
        t.setRowSelectionInterval(1, 1);
        model.insertRow(0, new Object[]{"Eve", Integer.valueOf(5), Boolean.TRUE});
        assertEquals(5, t.getRowCount());
        assertEquals(2, t.getSelectedRow());
        assertEquals(100, t.getPreferredSize().height);
        model.removeRow(2);
        assertEquals(-1, t.getSelectedRow());
        assertEquals(80, t.getPreferredSize().height);
        model.addColumn("City");
        assertEquals(4, t.getColumnCount());
        assertEquals("City", t.getColumnName(3));
        t.setAutoCreateColumnsFromModel(false);
        model.addColumn("Zip");
        assertEquals(4, t.getColumnCount());
    }

    @Test
    public void aCellUpdateIsPaintedAgainWithTheNewValue() {
        People model = new People();
        JTable t = new JTable(model);
        Stamp stamp = new Stamp();
        t.setDefaultRenderer(String.class, stamp);
        JFrame f = placed(t, 300, 200);
        assertNotNull(find(paint(f), "cell:Alice"));
        final List<TableModelEvent> events = new ArrayList<TableModelEvent>();
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                events.add(e);
            }
        });
        model.setValueAt("Zoe", 1, 0);
        assertEquals(1, events.size());
        assertEquals(TableModelEvent.UPDATE, events.get(0).getType());
        assertEquals(1, events.get(0).getFirstRow());
        List<Object[]> text = paint(f);
        assertNull(find(text, "cell:Alice"));
        assertNotNull(find(text, "cell:Zoe"));
    }

    // ------------------------------------------------------------ indexes

    @Test
    public void sortingMapsViewRowsToModelRowsAndKeepsTheSelection() {
        JTable t = new JTable(new People());
        t.setAutoCreateRowSorter(true);
        assertNotNull(t.getRowSorter());
        t.setRowSelectionInterval(0, 0);
        t.getRowSorter().toggleSortOrder(0);
        assertEquals("Alice", t.getValueAt(0, 0));
        assertEquals("Dave", t.getValueAt(3, 0));
        assertEquals(1, t.convertRowIndexToModel(0));
        assertEquals(2, t.convertRowIndexToView(0));
        assertEquals("Carol stays selected", 2, t.getSelectedRow());
        t.getRowSorter().toggleSortOrder(0);
        assertEquals("Dave", t.getValueAt(0, 0));
        assertEquals(1, t.getSelectedRow());
        t.getRowSorter().toggleSortOrder(1);
        assertEquals(Integer.valueOf(10), t.getValueAt(0, 1));
        assertEquals(Integer.valueOf(40), t.getValueAt(3, 1));
    }

    @Test
    public void filteringHidesRowsAndAnEditGoesToTheRightModelRow() {
        People model = new People();
        JTable t = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
        t.setRowSorter(sorter);
        sorter.setRowFilter(new RowFilter<Object, Object>() {
            @Override
            public boolean include(Entry<? extends Object, ? extends Object> entry) {
                Object age = entry.getValue(1);
                return age instanceof Integer && ((Integer) age).intValue() >= 20;
            }
        });
        assertEquals(3, t.getRowCount());
        assertEquals(-1, t.convertRowIndexToView(1));
        assertEquals(2, t.convertRowIndexToModel(1));
        t.setValueAt("Robert", 1, 0);
        assertEquals("Robert", model.getValueAt(2, 0));
        sorter.toggleSortOrder(1);
        assertEquals("Robert", t.getValueAt(0, 0));
        sorter.setRowFilter(null);
        assertEquals(4, t.getRowCount());
        model.addRow(new Object[]{"Eve", Integer.valueOf(5), Boolean.TRUE});
        assertEquals("the new row is sorted in", "Eve", t.getValueAt(0, 0));
    }

    @Test
    public void movedColumnsKeepTheirModelIndex() {
        People model = new People();
        JTable t = new JTable(model);
        t.moveColumn(0, 2);
        assertEquals(0, t.convertColumnIndexToModel(2));
        assertEquals(2, t.convertColumnIndexToView(0));
        assertEquals(0, t.convertColumnIndexToView(1));
        assertEquals("Name", t.getColumnName(2));
        assertEquals(model.getValueAt(1, 0), t.getValueAt(1, 2));
        assertEquals(String.class, t.getColumnClass(2));
        t.removeColumn(t.getColumnModel().getColumn(0));
        assertEquals(-1, t.convertColumnIndexToView(1));
        assertEquals(2, t.getColumnCount());
    }

    // ------------------------------------------------------------ selection

    @Test
    public void selectionModesAndEvents() {
        JTable t = new JTable(new People());
        final List<ListSelectionEvent> events = new ArrayList<ListSelectionEvent>();
        t.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                events.add(e);
            }
        });
        t.setRowSelectionInterval(1, 2);
        assertFalse(events.isEmpty());
        assertArrayEquals(new int[]{1, 2}, t.getSelectedRows());
        assertEquals(2, t.getSelectedRowCount());
        t.addRowSelectionInterval(3, 3);
        t.removeRowSelectionInterval(2, 2);
        assertArrayEquals(new int[]{1, 3}, t.getSelectedRows());
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertEquals(0, t.getSelectedRowCount());
        t.setRowSelectionInterval(0, 2);
        assertArrayEquals(new int[]{2}, t.getSelectedRows());
        t.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        t.selectAll();
        assertEquals(4, t.getSelectedRowCount());
        t.clearSelection();
        assertEquals(-1, t.getSelectedRow());
    }

    @Test
    public void changeSelectionTogglesAndExtendsAndCellsCanBeSelected() {
        JTable t = new JTable(new People());
        t.changeSelection(1, 0, false, false);
        t.changeSelection(3, 0, false, true);
        assertArrayEquals(new int[]{1, 2, 3}, t.getSelectedRows());
        t.changeSelection(2, 0, true, false);
        assertArrayEquals(new int[]{1, 3}, t.getSelectedRows());
        assertTrue("a row is selected in every column", t.isCellSelected(1, 2));
        t.setCellSelectionEnabled(true);
        t.changeSelection(1, 1, false, false);
        assertTrue(t.isCellSelected(1, 1));
        assertFalse(t.isCellSelected(1, 0));
        assertFalse(t.isCellSelected(2, 1));
        assertEquals(1, t.getSelectedColumn());
        t.setCellSelectionEnabled(false);
        t.setRowSelectionAllowed(false);
        assertFalse(t.isCellSelected(1, 1));
    }

    // ------------------------------------------------------------ geometry

    @Test
    public void cellRectanglesAndHitTestsAreInLogicalPixels() {
        JTable t = new JTable(new People());
        t.setRowHeight(20);
        placed(t, 300, 200);
        assertArrayEquals(new int[]{100, 100, 100}, widths(t));
        assertEquals(new Rectangle(100, 40, 100, 20), t.getCellRect(2, 1, true));
        assertEquals(new Rectangle(100, 40, 99, 19), t.getCellRect(2, 1, false));
        assertEquals(2, t.rowAtPoint(new Point(5, 45)));
        assertEquals(1, t.columnAtPoint(new Point(150, 45)));
        assertEquals(3, t.rowAtPoint(new Point(5, 79)));
        assertEquals(-1, t.rowAtPoint(new Point(5, 80)));
        assertEquals(-1, t.columnAtPoint(new Point(300, 5)));
        t.setIntercellSpacing(new com.codename1.desktopcompat.java.awt.Dimension(4, 6));
        assertEquals(new Rectangle(102, 43, 96, 14), t.getCellRect(2, 1, false));
        t.setRowHeight(1, 50);
        assertEquals(new Rectangle(0, 70, 100, 20), t.getCellRect(2, 0, true));
        assertEquals(1, t.rowAtPoint(new Point(5, 69)));
        assertEquals(50, t.getRowHeight(1));
    }

    @Test
    public void rowsAreSizedForAFingerUntilAHeightIsSet() {
        JTable t = new JTable(new People());
        assertTrue("touch rows are at least 28 high, got " + t.getRowHeight(), t.getRowHeight() >= 28);
        assertEquals(4 * t.getRowHeight(), t.getPreferredSize().height);
        t.setRowHeight(17);
        assertEquals(17, t.getRowHeight());
        assertEquals(68, t.getPreferredSize().height);
    }

    // ------------------------------------------------------------ column sizing

    @Test
    public void columnsShareTheWidthFromTheirPreferredWidths() {
        JTable t = new JTable(new People());
        TableColumnModel cm = t.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(150);
        t.setSize(600, 100);
        t.doLayout();
        assertArrayEquals(new int[]{150, 200, 250}, widths(t));
        // Shrinking: each column gives up the share its room above its
        // minimum is of all the room. 300 down to 241 with minimums 90,
        // 15 and 15: 96.72, then 72 of the 144 left, and the rest.
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(100);
        cm.getColumn(0).setMinWidth(90);
        t.setSize(241, 100);
        t.doLayout();
        assertArrayEquals(new int[]{97, 72, 72}, widths(t));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.doLayout();
        assertArrayEquals(new int[]{100, 100, 100}, widths(t));
        assertFalse(t.getScrollableTracksViewportWidth());
        assertEquals(300, t.getPreferredSize().width);
    }

    private static int[] afterDragging(int mode) {
        JTable t = new JTable(new People());
        t.setAutoResizeMode(mode);
        t.setSize(300, 100);
        t.doLayout();
        JTableHeader h = t.getTableHeader();
        TableColumn first = t.getColumnModel().getColumn(0);
        h.setResizingColumn(first);
        first.setWidth(130);
        t.doLayout();
        h.setResizingColumn(null);
        t.doLayout();
        return widths(t);
    }

    @Test
    public void aDraggedColumnTakesItsWidthFromTheColumnsTheModeNames() {
        assertArrayEquals(new int[]{130, 70, 100}, afterDragging(JTable.AUTO_RESIZE_NEXT_COLUMN));
        assertArrayEquals(new int[]{130, 85, 85}, afterDragging(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS));
        assertArrayEquals(new int[]{130, 100, 70}, afterDragging(JTable.AUTO_RESIZE_LAST_COLUMN));
        assertArrayEquals(new int[]{130, 75, 75}, afterDragging(JTable.AUTO_RESIZE_OFF));
    }

    // ------------------------------------------------------------ painting

    @Test
    public void aCustomRendererIsAskedPerCellAndItsDrawingReachesTheScreen() {
        JTable t = new JTable(new People());
        t.setRowHeight(20);
        Stamp stamp = new Stamp();
        t.setDefaultRenderer(String.class, stamp);
        t.setDefaultRenderer(Integer.class, stamp);
        t.setRowSelectionInterval(2, 2);
        JFrame f = placed(t, 300, 200);
        stamp.asked.clear();
        List<Object[]> text = paint(f);
        assertTrue(stamp.asked.contains("0,0,Carol,false"));
        assertTrue(stamp.asked.contains("2,0,Bob,true"));
        assertTrue(stamp.asked.contains("3,1,40,false"));
        assertEquals(8, stamp.asked.size());
        Object[] bob = find(text, "cell:Bob");
        Object[] twenty = find(text, "cell:20");
        assertNotNull(bob);
        assertNotNull(twenty);
        int[] origin = onDisplay(t, 0, 0);
        // Column 0 starts at 0 and column 1 at 100, row 2 at 40; the
        // renderer draws 2 in from the cell's left edge.
        assertEquals(origin[0] + 2 * 2, ((Integer) bob[1]).intValue());
        assertEquals(origin[0] + 2 * 102, ((Integer) twenty[1]).intValue());
        assertEquals(bob[2], twenty[2]);
        Object[] carol = find(text, "cell:Carol");
        assertEquals(2 * 40, ((Integer) bob[2]).intValue() - ((Integer) carol[2]).intValue());
    }

    @Test
    public void theDefaultRenderersDrawTheValuesThroughTheNativeLabel() {
        JTable t = new JTable(new People());
        JFrame f = placed(t, 300, 200);
        List<Object[]> text = paint(f);
        assertNotNull(find(text, "Alice"));
        assertNotNull(find(text, "40"));
        Component c = t.prepareRenderer(t.getCellRenderer(1, 0), 1, 0);
        assertTrue(c instanceof DefaultTableCellRenderer);
        assertEquals("Alice", ((DefaultTableCellRenderer) c).getText());
        t.setRowSelectionInterval(1, 1);
        c = t.prepareRenderer(t.getCellRenderer(1, 0), 1, 0);
        assertEquals(t.getSelectionBackground(), c.getBackground());
        c = t.prepareRenderer(t.getCellRenderer(0, 0), 0, 0);
        assertEquals(t.getBackground(), c.getBackground());
    }

    @Test
    public void onlyTheCellsThatMeetTheClipArePainted() {
        JTable t = new JTable(new DefaultTableModel(1000, 3));
        t.setRowHeight(20);
        Stamp stamp = new Stamp();
        t.setDefaultRenderer(Object.class, stamp);
        t.setSize(300, 20000);
        t.doLayout();
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.createGraphics();
        g.setClip(0, 40, 300, 40);
        stamp.asked.clear();
        t.paint(g);
        g.dispose();
        assertEquals("two rows of three cells", 6, stamp.asked.size());
        assertTrue(stamp.asked.get(0).startsWith("2,0,"));
        assertTrue(stamp.asked.get(5).startsWith("3,2,"));
        g = image.createGraphics();
        g.setClip(110, 45, 20, 5);
        stamp.asked.clear();
        t.paint(g);
        g.dispose();
        assertEquals(1, stamp.asked.size());
        assertTrue(stamp.asked.get(0).startsWith("2,1,"));
    }

    // ------------------------------------------------------------ header

    @Test
    public void aClickOnAHeaderTitleSortsByThatColumn() {
        JTable t = new JTable(new People());
        t.setAutoCreateRowSorter(true);
        JTableHeader h = t.getTableHeader();
        JFrame f = new JFrame();
        f.add(h, BorderLayout.NORTH);
        f.add(t, BorderLayout.CENTER);
        show(f);
        assertTrue(h.getHeight() >= 20);
        assertEquals(t.getWidth(), h.getWidth());
        assertNotNull("titles are painted", find(paint(f), "Age"));
        int third = t.getWidth() / 3;
        press(f, h, third + 20, 10);
        release(f, h, third + 20, 10);
        assertEquals("sorted by age", "Alice", t.getValueAt(0, 0));
        assertEquals(1, t.getRowSorter().getSortKeys().get(0).getColumn());
        assertEquals(SortOrder.ASCENDING, t.getRowSorter().getSortKeys().get(0).getSortOrder());
        press(f, h, third + 20, 10);
        release(f, h, third + 20, 10);
        assertEquals("Dave", t.getValueAt(0, 0));
        press(f, h, 20, 10);
        release(f, h, 20, 10);
        assertEquals("sorted by name", "Alice", t.getValueAt(0, 0));
        assertEquals(1, h.columnAtPoint(new Point(third + 20, 5)));
        assertEquals(third, h.getHeaderRect(1).x);
    }

    @Test
    public void draggingTheLineBetweenTwoTitlesResizesTheColumn() {
        JTable t = new JTable(new People());
        JTableHeader h = t.getTableHeader();
        JFrame f = new JFrame();
        f.add(h, BorderLayout.NORTH);
        f.add(t, BorderLayout.CENTER);
        show(f);
        int w = t.getWidth();
        int first = t.getColumnModel().getColumn(0).getWidth();
        press(f, h, first, 10);
        assertSame(t.getColumnModel().getColumn(0), h.getResizingColumn());
        drag(f, h, first + 40, 10);
        release(f, h, first + 40, 10);
        assertNull(h.getResizingColumn());
        assertEquals(first + 40, t.getColumnModel().getColumn(0).getWidth());
        int[] now = widths(t);
        assertEquals("the others made room", w, now[0] + now[1] + now[2]);
        f.validate();
        assertArrayEquals("the widths stay after the next layout", now, widths(t));
    }

    // ------------------------------------------------------------ pointer and keys

    @Test
    public void aTapSelectsADragDoesNotAndModifiersToggleAndExtend() {
        JTable t = new JTable(new People());
        t.setRowHeight(20);
        JFrame f = placed(t, 300, 200);
        press(f, t, 150, 45);
        release(f, t, 150, 45);
        assertEquals(2, t.getSelectedRow());
        assertEquals(1, t.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        t.clearSelection();
        press(f, t, 150, 5);
        drag(f, t, 150, 30);
        drag(f, t, 150, 70);
        release(f, t, 150, 70);
        assertEquals("a finger drag scrolls, it does not select", -1, t.getSelectedRow());
        tap(t, 10, 5, 0, 1);
        tap(t, 10, 65, InputEvent.SHIFT_MASK, 1);
        assertArrayEquals(new int[]{0, 1, 2, 3}, t.getSelectedRows());
        tap(t, 10, 25, InputEvent.CTRL_MASK, 1);
        assertArrayEquals(new int[]{0, 2, 3}, t.getSelectedRows());
        tap(t, 10, 150, 0, 1);
        assertArrayEquals("below the rows nothing changes", new int[]{0, 2, 3}, t.getSelectedRows());
    }

    @Test
    public void arrowKeysMoveTheSelection() {
        JTable t = new JTable(new People());
        JFrame f = placed(t, 300, 200);
        assertNotNull(f);
        t.changeSelection(0, 0, false, false);
        key(t, KeyEvent.VK_DOWN);
        key(t, KeyEvent.VK_DOWN);
        assertEquals(2, t.getSelectedRow());
        key(t, KeyEvent.VK_UP);
        assertEquals(1, t.getSelectedRow());
        key(t, KeyEvent.VK_RIGHT);
        assertEquals(1, t.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        key(t, KeyEvent.VK_END);
        assertEquals(2, t.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        key(t, KeyEvent.VK_TAB);
        assertEquals("tab wraps to the next row", 2, t.getSelectedRow());
        assertEquals(0, t.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        key(t, KeyEvent.VK_PAGE_DOWN);
        assertEquals(3, t.getSelectedRow());
        key(t, KeyEvent.VK_DOWN);
        assertEquals(3, t.getSelectedRow());
    }
}
