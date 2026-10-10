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
package com.codename1.desktopcompat.javax.swing.table;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import com.codename1.desktopcompat.rt.CellPainter;
import com.codename1.desktopcompat.rt.CellTheme;
import java.util.List;

/// The row of column titles above a table.
///
/// A click on a title sorts by that column when the table has a row
/// sorter, and an arrow shows the direction. A drag that starts on the
/// line between two titles resizes the column before it; a drag that
/// starts on a title moves the column. The header paints itself the way
/// the table does, with a renderer per column.
public class JTableHeader extends JComponent implements Accessible, TableColumnModelListener {

    protected JTable table;
    protected TableColumnModel columnModel;
    protected boolean reorderingAllowed;
    protected boolean resizingAllowed;
    protected boolean updateTableInRealTime;
    protected transient TableColumn resizingColumn;
    protected transient TableColumn draggedColumn;
    protected transient int draggedDistance;

    private TableCellRenderer defaultRenderer;
    private int pressX;
    private int pressWidth;
    private int pressIndex = -1;
    private boolean pressOnLine;
    private boolean moved;

    public JTableHeader() {
        this(null);
    }

    public JTableHeader(TableColumnModel cm) {
        setLayout(null);
        setColumnModel(cm != null ? cm : createDefaultColumnModel());
        initializeLocalVars();
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    protected TableCellRenderer createDefaultRenderer() {
        return new TitleRenderer();
    }

    protected void initializeLocalVars() {
        setOpaque(true);
        table = null;
        reorderingAllowed = true;
        resizingAllowed = true;
        draggedColumn = null;
        draggedDistance = 0;
        resizingColumn = null;
        updateTableInRealTime = true;
        setBackground(CellTheme.headerBackground("TableHeader.background"));
        setForeground(CellTheme.headerForeground("TableHeader.foreground"));
        setFont(CellTheme.headerFont());
        setDefaultRenderer(createDefaultRenderer());
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void setTable(JTable table) {
        JTable old = this.table;
        this.table = table;
        firePropertyChange("table", old, table);
    }

    public JTable getTable() {
        return table;
    }

    public void setReorderingAllowed(boolean reorderingAllowed) {
        boolean old = this.reorderingAllowed;
        this.reorderingAllowed = reorderingAllowed;
        firePropertyChange("reorderingAllowed", old, reorderingAllowed);
    }

    public boolean getReorderingAllowed() {
        return reorderingAllowed;
    }

    public void setResizingAllowed(boolean resizingAllowed) {
        boolean old = this.resizingAllowed;
        this.resizingAllowed = resizingAllowed;
        firePropertyChange("resizingAllowed", old, resizingAllowed);
    }

    public boolean getResizingAllowed() {
        return resizingAllowed;
    }

    public TableColumn getDraggedColumn() {
        return draggedColumn;
    }

    public int getDraggedDistance() {
        return draggedDistance;
    }

    public TableColumn getResizingColumn() {
        return resizingColumn;
    }

    public void setUpdateTableInRealTime(boolean flag) {
        updateTableInRealTime = flag;
    }

    public boolean getUpdateTableInRealTime() {
        return updateTableInRealTime;
    }

    public void setDefaultRenderer(TableCellRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    public TableCellRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    public int columnAtPoint(Point point) {
        if (point.y < 0 || point.y >= getHeight()) {
            return -1;
        }
        return getColumnModel().getColumnIndexAtX(point.x);
    }

    public Rectangle getHeaderRect(int column) {
        Rectangle r = new Rectangle();
        TableColumnModel cm = getColumnModel();
        r.height = getHeight();
        if (column < 0) {
            return r;
        }
        if (column >= cm.getColumnCount()) {
            r.x = getWidth();
            return r;
        }
        for (int i = 0; i < column; i++) {
            r.x += cm.getColumn(i).getWidth();
        }
        r.width = cm.getColumn(column).getWidth();
        return r;
    }

    public void setColumnModel(TableColumnModel columnModel) {
        if (columnModel == null) {
            throw new IllegalArgumentException("Cannot set a null ColumnModel");
        }
        TableColumnModel old = this.columnModel;
        if (columnModel != old) {
            if (old != null) {
                old.removeColumnModelListener(this);
            }
            this.columnModel = columnModel;
            columnModel.addColumnModelListener(this);
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        repaint();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        resizeAndRepaint();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    public void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    public void setDraggedColumn(TableColumn aColumn) {
        draggedColumn = aColumn;
    }

    public void setDraggedDistance(int distance) {
        draggedDistance = distance;
    }

    public void setResizingColumn(TableColumn aColumn) {
        resizingColumn = aColumn;
    }

    /// As wide as the columns, and one row of title text high: sized for
    /// a finger on a touch screen.
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        TableColumnModel cm = getColumnModel();
        long w = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            w += cm.getColumn(i).getPreferredWidth();
        }
        return new Dimension((int) Math.min(w, Integer.MAX_VALUE), CellTheme.rowHeight(getFont(), 20, 32));
    }

    // ------------------------------------------------------------ paint

    private SortOrder sortOrderOf(int viewColumn) {
        if (table == null || table.getRowSorter() == null) {
            return SortOrder.UNSORTED;
        }
        List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
        if (keys == null || keys.isEmpty()) {
            return SortOrder.UNSORTED;
        }
        RowSorter.SortKey first = keys.get(0);
        return first.getColumn() == getColumnModel().getColumn(viewColumn).getModelIndex()
                ? first.getSortOrder() : SortOrder.UNSORTED;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        Color bg = getBackground();
        if (isOpaque() && bg != null) {
            g.setColor(bg);
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }
        TableColumnModel cm = getColumnModel();
        int h = getHeight();
        Color fg = getForeground();
        Color line = bg != null && fg != null ? CellTheme.mix(bg, fg, 0.25f) : Color.GRAY;
        int x = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn column = cm.getColumn(i);
            int w = column.getWidth();
            if (x + w > clip.x && x < clip.x + clip.width && w > 0) {
                TableCellRenderer r = column.getHeaderRenderer();
                if (r == null) {
                    r = defaultRenderer;
                }
                if (r != null) {
                    Component c = r.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, i);
                    CellPainter.paint(g, c, x, 0, w - 1, h - 1);
                }
                SortOrder order = sortOrderOf(i);
                if (order != SortOrder.UNSORTED && w > 16) {
                    g.setColor(fg != null ? fg : Color.BLACK);
                    int ax = x + w - 12;
                    int ay = h / 2;
                    int[] px = {ax, ax + 8, ax + 4};
                    int[] py = order == SortOrder.ASCENDING ? new int[]{ay + 2, ay + 2, ay - 3}
                            : new int[]{ay - 2, ay - 2, ay + 3};
                    g.fillPolygon(px, py, 3);
                }
                g.setColor(line);
                g.drawLine(x + w - 1, 0, x + w - 1, h - 1);
            }
            x += w;
        }
        g.setColor(line);
        g.drawLine(clip.x, h - 1, clip.x + clip.width - 1, h - 1);
    }

    // ------------------------------------------------------------ pointer

    /// The column whose trailing line is within reach of `x`, or -1.
    private int lineAt(int x) {
        int reach = CellTheme.touch() ? 10 : 4;
        TableColumnModel cm = getColumnModel();
        int right = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            right += cm.getColumn(i).getWidth();
            if (Math.abs(x - right) <= reach) {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(TableColumn c) {
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            if (cm.getColumn(i) == c) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (isEnabled()) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    pressed(e);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    released();
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    clicked(e);
                    break;
                default:
                    break;
            }
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (isEnabled() && e.getID() == MouseEvent.MOUSE_DRAGGED) {
            dragged(e);
        }
        super.processMouseMotionEvent(e);
    }

    private void pressed(MouseEvent e) {
        pressX = e.getX();
        moved = false;
        pressOnLine = false;
        pressIndex = -1;
        resizingColumn = null;
        draggedColumn = null;
        draggedDistance = 0;
        int line = resizingAllowed ? lineAt(e.getX()) : -1;
        if (line >= 0 && getColumnModel().getColumn(line).getResizable()) {
            pressOnLine = true;
            resizingColumn = getColumnModel().getColumn(line);
            pressWidth = resizingColumn.getWidth();
            return;
        }
        pressIndex = columnAtPoint(new Point(e.getX(), Math.max(0, Math.min(e.getY(), getHeight() - 1))));
    }

    private void dragged(MouseEvent e) {
        int dx = e.getX() - pressX;
        if (resizingColumn != null) {
            moved = true;
            resizingColumn.setWidth(pressWidth + dx);
            if (table != null) {
                // Now, not on the next layout pass: the other columns
                // must follow the one under the finger.
                table.doLayout();
                table.repaint();
            }
            repaint();
            return;
        }
        if (!reorderingAllowed || pressIndex < 0) {
            return;
        }
        if (draggedColumn == null) {
            if (Math.abs(dx) < 6) {
                return;
            }
            draggedColumn = getColumnModel().getColumn(pressIndex);
        }
        moved = true;
        draggedDistance = dx;
        int from = indexOf(draggedColumn);
        int to = getColumnModel().getColumnIndexAtX(Math.max(0, e.getX()));
        if (from >= 0 && to >= 0 && to != from) {
            getColumnModel().moveColumn(from, to);
        }
        repaint();
    }

    private void released() {
        boolean was = resizingColumn != null || draggedColumn != null;
        resizingColumn = null;
        draggedColumn = null;
        draggedDistance = 0;
        if (was) {
            resizeAndRepaint();
            if (table != null) {
                table.repaint();
            }
        }
    }

    private void clicked(MouseEvent e) {
        if (moved || pressOnLine || table == null) {
            return;
        }
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        int column = columnAtPoint(e.getPoint());
        if (sorter != null && column >= 0) {
            sorter.toggleSortOrder(table.convertColumnIndexToModel(column));
            repaint();
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",reorderingAllowed=" + reorderingAllowed + ",resizingAllowed=" + resizingAllowed;
    }

    /// The renderer of titles: a centered label in the header's colors
    /// and font.
    private final class TitleRenderer extends DefaultTableCellRenderer {

        TitleRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Color fg = JTableHeader.this.getForeground();
            Color bg = JTableHeader.this.getBackground();
            Font f = JTableHeader.this.getFont();
            if (fg != null && !fg.equals(isForegroundSet() ? getForeground() : null)) {
                super.setForeground(fg);
            }
            if (bg != null && !bg.equals(isBackgroundSet() ? getBackground() : null)) {
                super.setBackground(bg);
            }
            if (!f.equals(isFontSet() ? getFont() : null)) {
                setFont(f);
            }
            setValue(value);
            return this;
        }
    }
}
