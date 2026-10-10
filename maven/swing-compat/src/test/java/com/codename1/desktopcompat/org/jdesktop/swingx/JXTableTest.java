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
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.RowFilter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ColorHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.HighlightPredicate;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.Highlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.HighlighterFactory;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultTableRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.TableColumnExt;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// `JXTable`: highlighters through painting, hidden columns and the
/// indexes around them, packing, the sorting and filtering switches,
/// the viewport size in rows and rollover.
public class JXTableTest extends KernelTestBase {

    private static final Color RED = new Color(200, 0, 0);
    private static final Color BLUE = new Color(0, 0, 200);

    /// name, age, member: `String`, `Integer`, `Boolean`.
    private static final class People extends DefaultTableModel {
        People() {
            super(new Object[][]{
                {"Carol", Integer.valueOf(30), Boolean.TRUE},
                {"Alice", Integer.valueOf(10), Boolean.FALSE},
                {"Bob", Integer.valueOf(20), Boolean.TRUE},
                {"Dave the very long named one", Integer.valueOf(4), Boolean.FALSE}},
                    new Object[]{"Name", "Age", "Member"});
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : column == 1 ? Integer.class : Boolean.class;
        }
    }

    /// Draws its value and the background it was left with.
    private static final class Stamp extends JComponent implements TableCellRenderer {
        private String text = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
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

    private JFrame placed(JTable t, int w, int h) {
        JPanel p = new JPanel(null);
        t.setBounds(0, 0, w, h);
        p.add(t);
        JFrame f = new JFrame();
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    private static String name(JXTable t, int row) {
        return String.valueOf(t.getValueAt(row, 0));
    }

    @Test
    public void highlightersApplyInOrderWhenTheTablePaints() {
        JXTable t = new JXTable(new People());
        t.getColumn(0).setCellRenderer(new Stamp());
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.EVEN, BLUE, null));
        JFrame f = placed(t, 400, 300);
        List<Object[]> drawn = paint(f);
        assertNotNull("row 0 is even: the later highlighter wins", find(drawn, "Carol@0,0,200"));
        assertNotNull("row 1 only has the first", find(drawn, "Alice@200,0,0"));
        assertNotNull(find(drawn, "Bob@0,0,200"));

        t.setHighlighters(new ColorHighlighter(HighlightPredicate.EVEN, BLUE, null),
                new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        drawn = paint(f);
        assertNotNull("the order turned round: red is last everywhere", find(drawn, "Carol@200,0,0"));
        assertEquals(2, t.getHighlighters().length);

        t.setHighlighters();
        drawn = paint(f);
        assertNotNull(find(drawn, "Carol@255,255,255"));
    }

    @Test
    public void columnHighlightersComeAfterTheTables() {
        JXTable t = new JXTable(new People());
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS, RED, null));
        t.getColumnExt(1).addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS, BLUE, null));
        assertEquals(RED, t.prepareRenderer(0, 0).getBackground());
        assertEquals(BLUE, t.prepareRenderer(0, 1).getBackground());
        assertEquals(RED, t.prepareRenderer(0, 2).getBackground());
    }

    @Test
    public void aHighlightDoesNotStickToTheNextCell() {
        JXTable t = new JXTable(new People());
        Highlighter firstRow = new ColorHighlighter(new HighlightPredicate.RowGroupHighlightPredicate(100) {
            @Override
            public boolean isHighlighted(Component renderer,
                    com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
                return adapter.row == 0;
            }
        }, RED, BLUE);
        t.addHighlighter(firstRow);
        // The SwingX renderer resets itself for every cell.
        assertEquals(RED, t.prepareRenderer(0, 0).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 0).getBackground());
        assertEquals(t.getForeground(), t.prepareRenderer(1, 0).getForeground());
        // The JDK's remembers a color set on it: the table undoes that.
        DefaultTableCellRenderer plain = new DefaultTableCellRenderer();
        t.getColumn(0).setCellRenderer(plain);
        assertEquals(RED, t.prepareRenderer(0, 0).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 0).getBackground());
        assertEquals(t.getForeground(), t.prepareRenderer(1, 0).getForeground());
        assertEquals(RED, t.prepareRenderer(0, 0).getBackground());
        // A color the application set on the renderer survives.
        DefaultTableCellRenderer green = new DefaultTableCellRenderer();
        green.setBackground(Color.GREEN);
        t.getColumn(1).setCellRenderer(green);
        assertEquals(RED, t.prepareRenderer(0, 1).getBackground());
        assertEquals(Color.GREEN, t.prepareRenderer(1, 1).getBackground());
    }

    @Test
    public void predicatesSeeTheCell() {
        JXTable t = new JXTable(new People());
        t.setHighlighters(new ColorHighlighter(new HighlightPredicate.ColumnHighlightPredicate(1), RED, null));
        assertEquals(RED, t.prepareRenderer(0, 1).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(0, 0).getBackground());

        t.setHighlighters(new ColorHighlighter(new HighlightPredicate.EqualsHighlightPredicate("Bob"), RED, null));
        assertEquals(RED, t.prepareRenderer(2, 0).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 0).getBackground());

        t.setHighlighters(new ColorHighlighter(new HighlightPredicate.IdentifierHighlightPredicate("Age"), RED,
                null));
        assertEquals(RED, t.prepareRenderer(0, 1).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(0, 2).getBackground());

        t.setHighlighters(new ColorHighlighter(new HighlightPredicate.NotHighlightPredicate(
                HighlightPredicate.ODD), RED, null));
        assertEquals(RED, t.prepareRenderer(0, 0).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 0).getBackground());

        t.setHighlighters(new ColorHighlighter(new HighlightPredicate.AndHighlightPredicate(
                HighlightPredicate.EVEN, new HighlightPredicate.ColumnHighlightPredicate(2)), RED, null));
        assertEquals(RED, t.prepareRenderer(0, 2).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(0, 1).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 2).getBackground());

        t.setHighlighters(new ColorHighlighter(HighlightPredicate.IS_SELECTED, null, null, null, RED));
        t.setRowSelectionInterval(1, 1);
        assertEquals(RED, t.prepareRenderer(1, 0).getForeground());
        assertFalse(RED.equals(t.prepareRenderer(0, 0).getForeground()));
    }

    @Test
    public void stripingColorsEveryOtherRow() {
        JXTable t = new JXTable(new People());
        t.addHighlighter(HighlighterFactory.createSimpleStriping(RED));
        Color even = t.prepareRenderer(0, 0).getBackground();
        Color odd = t.prepareRenderer(1, 0).getBackground();
        assertFalse(even.equals(odd));
        assertTrue(RED.equals(even) || RED.equals(odd));
        assertEquals(even, t.prepareRenderer(2, 0).getBackground());
    }

    @Test
    public void hiddenColumnsLeaveTheViewAndComeBack() {
        JXTable t = new JXTable(new People());
        final int[] events = new int[2];
        TableColumnExt age = t.getColumnExt("Age");
        assertNotNull(age);
        assertSame(age, t.getColumnExt(1));
        age.setVisible(false);
        assertEquals(2, t.getColumnCount());
        assertEquals(3, t.getColumnCount(true));
        assertEquals(2, t.getColumns().size());
        assertEquals(3, t.getColumns(true).size());
        assertEquals(-1, t.convertColumnIndexToView(1));
        assertEquals(2, t.convertColumnIndexToModel(1));
        assertEquals(1, t.convertColumnIndexToView(2));
        assertEquals("Member", t.getColumnName(1));
        assertEquals(Boolean.TRUE, t.getValueAt(0, 1));
        assertSame("a hidden column is still found by identifier", age, t.getColumnExt("Age"));
        assertFalse(age.isVisible());
        assertEquals(events[0], events[1]);

        age.setVisible(true);
        assertEquals(3, t.getColumnCount());
        assertSame("back where it was", age, t.getColumn(1));
        assertEquals(1, t.convertColumnIndexToView(1));

        // Hide the first, move the remaining two, show it: it leads again.
        TableColumnExt name = t.getColumnExt("Name");
        name.setVisible(false);
        t.moveColumn(0, 1);
        assertEquals("Member", t.getColumnName(0));
        name.setVisible(true);
        assertEquals("Name", t.getColumnName(0));
        assertEquals("Member", t.getColumnName(1));
        assertEquals("Age", t.getColumnName(2));
    }

    @Test
    public void paintingSkipsAHiddenColumn() {
        JXTable t = new JXTable(new People());
        JFrame f = placed(t, 400, 300);
        assertNotNull(find(paint(f), "30"));
        t.getColumnExt("Age").setVisible(false);
        List<Object[]> drawn = paint(f);
        assertNull(find(drawn, "30"));
        assertNotNull(find(drawn, "Carol"));
    }

    @Test
    public void packAllSizesColumnsToTheirContent() {
        JXTable t = new JXTable(new People());
        t.setHorizontalScrollEnabled(true);
        assertTrue(t.isHorizontalScrollEnabled());
        assertEquals(JTable.AUTO_RESIZE_OFF, t.getAutoResizeMode());
        t.packAll();
        int margin = t.getColumnFactory().getDefaultPackMargin();
        for (int c = 0; c < t.getColumnCount(); c++) {
            int widest = t.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(t,
                    t.getColumn(c).getHeaderValue(), false, false, -1, -1).getPreferredSize().width;
            TableCellRenderer r = t.getCellRenderer(0, c);
            for (int row = 0; row < t.getRowCount(); row++) {
                widest = Math.max(widest, r.getTableCellRendererComponent(t, t.getValueAt(row, c), false, false,
                        row, c).getPreferredSize().width);
            }
            assertEquals("column " + c, widest + 2 * margin, t.getColumn(c).getPreferredWidth());
        }
        assertTrue(t.getColumn(0).getPreferredWidth() > t.getColumn(1).getPreferredWidth());

        t.packColumn(0, 10, 50);
        assertEquals("the maximum caps the width", 50, t.getColumn(0).getPreferredWidth());
        t.setHorizontalScrollEnabled(false);
        assertFalse(t.isHorizontalScrollEnabled());
        assertEquals(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, t.getAutoResizeMode());
    }

    @Test
    public void sortOrdersCycleAndReset() {
        JXTable t = new JXTable(new People());
        assertTrue(t.isSortable());
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(0));
        t.toggleSortOrder(0);
        assertEquals(SortOrder.ASCENDING, t.getSortOrder(0));
        assertEquals("Alice", name(t, 0));
        assertEquals(1, t.convertRowIndexToModel(0));
        assertSame(t.getColumn(0), t.getSortedColumn());
        t.toggleSortOrder(0);
        assertEquals(SortOrder.DESCENDING, t.getSortOrder(0));
        assertEquals("Dave the very long named one", name(t, 0));
        t.toggleSortOrder(0);
        assertEquals("two orders in the default cycle", SortOrder.ASCENDING, t.getSortOrder(0));
        t.resetSortOrder();
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(0));
        assertEquals("Carol", name(t, 0));
        assertNull(t.getSortedColumn());

        t.setSortOrderCycle(SortOrder.DESCENDING, SortOrder.UNSORTED);
        t.toggleSortOrder(1);
        assertEquals(SortOrder.DESCENDING, t.getSortOrder(1));
        assertEquals("numbers compare as numbers", "Carol", name(t, 0));
        t.toggleSortOrder(1);
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(1));

        t.setSortOrder("Age", SortOrder.ASCENDING);
        assertEquals(SortOrder.ASCENDING, t.getSortOrder("Age"));
        assertEquals("Dave the very long named one", name(t, 0));
        t.getColumnExt("Age").setVisible(false);
        assertEquals("sorted by a hidden column", SortOrder.ASCENDING, t.getSortOrder("Age"));
        assertEquals("Dave the very long named one", name(t, 0));
        t.toggleSortOrder("Age");
        assertEquals(SortOrder.DESCENDING, t.getSortOrder("Age"));
        assertEquals("Carol", name(t, 0));
    }

    @Test
    public void sortableSwitchesAndComparators() {
        JXTable t = new JXTable(new People());
        t.setSortable(false);
        t.toggleSortOrder(0);
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(0));
        assertEquals("Carol", name(t, 0));
        t.setSortable(true);
        t.getColumnExt(0).setSortable(false);
        t.toggleSortOrder(0);
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(0));
        t.getColumnExt(0).setSortable(true);
        t.getColumnExt(0).setComparator(new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                return a.toString().length() - b.toString().length();
            }
        });
        t.toggleSortOrder(0);
        assertEquals("by length", "Bob", name(t, 0));
        assertEquals("Dave the very long named one", name(t, 3));
    }

    @Test
    public void sortingByTheStringsTheRendererShows() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{{new int[]{1}}, {new int[]{2, 2, 2}},
            {new int[]{3, 3}}}, new Object[]{"Arrays"});
        JXTable t = new JXTable(m);
        // Shows an array by its length, upside down: 9 - length.
        t.getColumn(0).setCellRenderer(new DefaultTableRenderer(new StringValue() {
            @Override
            public String getString(Object value) {
                return value instanceof int[] ? String.valueOf(9 - ((int[]) value).length) : "";
            }
        }));
        assertEquals("8", t.getStringAt(0, 0));
        t.toggleSortOrder(0);
        assertEquals("6", t.getStringAt(0, 0));
        assertEquals("7", t.getStringAt(1, 0));
        assertEquals("8", t.getStringAt(2, 0));
    }

    @Test
    public void aRowFilterHidesRows() {
        JXTable t = new JXTable(new People());
        t.setRowFilter(new RowFilter<Object, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends Object, ? extends Integer> entry) {
                Object age = entry.getValue(1);
                return age instanceof Integer && ((Integer) age).intValue() >= 20;
            }
        });
        assertNotNull(t.getRowFilter());
        assertEquals(2, t.getRowCount());
        assertEquals("Carol", name(t, 0));
        assertEquals("Bob", name(t, 1));
        t.toggleSortOrder(0);
        assertEquals("Bob", name(t, 0));
        t.setRowFilter(null);
        assertEquals(4, t.getRowCount());
        assertEquals("the filter survives a new model", "Alice", name(t, 0));
    }

    @Test
    public void aNewModelGetsANewSorterWithTheTablesSettings() {
        JXTable t = new JXTable(new People());
        t.setSortable(false);
        t.setModel(new People());
        t.toggleSortOrder(0);
        assertEquals(SortOrder.UNSORTED, t.getSortOrder(0));
        t.setSortable(true);
        t.toggleSortOrder(0);
        assertEquals("Alice", name(t, 0));
        assertTrue(t.getColumn(0) instanceof TableColumnExt);
    }

    @Test
    public void theViewportSizeIsCountedInRowsAndColumns() {
        JXTable t = new JXTable(new People());
        assertEquals(20, t.getVisibleRowCount());
        t.setVisibleRowCount(5);
        assertEquals(5 * t.getRowHeight(), t.getPreferredScrollableViewportSize().height);
        int all = t.getPreferredScrollableViewportSize().width;
        assertEquals(t.getColumn(0).getPreferredWidth() + t.getColumn(1).getPreferredWidth()
                + t.getColumn(2).getPreferredWidth(), all);
        t.setVisibleColumnCount(2);
        assertEquals(t.getColumn(0).getPreferredWidth() + t.getColumn(1).getPreferredWidth(),
                t.getPreferredScrollableViewportSize().width);
    }

    @Test
    public void editableSwitches() {
        JXTable t = new JXTable(new People());
        assertTrue(t.isCellEditable(0, 0));
        t.getColumnExt(0).setEditable(false);
        assertFalse(t.isCellEditable(0, 0));
        assertTrue(t.isCellEditable(0, 1));
        t.setEditable(false);
        assertFalse(t.isCellEditable(0, 1));
    }

    @Test
    public void rolloverFollowsThePointer() {
        JXTable t = new JXTable(new People());
        placed(t, 300, 200);
        t.setRolloverEnabled(true);
        t.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, RED, null));
        int y = t.getCellRect(2, 1, true).y + 2;
        int x = t.getCellRect(2, 1, true).x + 2;
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false,
                MouseEvent.NOBUTTON));
        assertEquals(new Point(1, 2), t.getClientProperty("swingx.rollover"));
        assertEquals(RED, t.prepareRenderer(2, 0).getBackground());
        assertEquals(t.getBackground(), t.prepareRenderer(1, 0).getBackground());
        t.dispatchEvent(new MouseEvent(t, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, x, y, 0, false,
                MouseEvent.NOBUTTON));
        assertNull(t.getClientProperty("swingx.rollover"));
        assertEquals(t.getBackground(), t.prepareRenderer(2, 0).getBackground());
        t.setRolloverEnabled(false);
        assertFalse(t.isRolloverEnabled());
    }
}
