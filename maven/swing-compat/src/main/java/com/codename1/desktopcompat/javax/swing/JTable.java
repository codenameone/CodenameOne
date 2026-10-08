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

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.KeyAdapter;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.KeyListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.event.CellEditorListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import com.codename1.desktopcompat.javax.swing.event.RowSorterEvent;
import com.codename1.desktopcompat.javax.swing.event.RowSorterListener;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelListener;
import com.codename1.desktopcompat.javax.swing.event.TableModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableModelListener;
import com.codename1.desktopcompat.javax.swing.table.AbstractTableModel;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableColumnModel;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.javax.swing.table.JTableHeader;
import com.codename1.desktopcompat.javax.swing.table.TableCellEditor;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.javax.swing.table.TableColumnModel;
import com.codename1.desktopcompat.javax.swing.table.TableModel;
import com.codename1.desktopcompat.javax.swing.table.TableRowSorter;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.rt.CellPainter;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import com.codename1.desktopcompat.rt.Units;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Vector;

/// A grid of cells over a [TableModel], with the JDK's column model,
/// selection, renderers, editors and row sorter.
///
/// ## How it is drawn
///
/// The table paints itself. For every cell that meets the clip it asks
/// the cell's [TableCellRenderer] for a component, gives it the cell's
/// bounds and paints it there; the default renderers are a label and a
/// check box, so a cell is drawn by the Codename One widget in the theme's
/// colors. Nothing is created per cell, and a table of any size costs
/// what its visible part costs.
///
/// ## How it is edited
///
/// An editor's component is a real child of the table, placed over the
/// cell while it is edited: a Codename One text field, check box or combo
/// box. Enter in it, or its own "done", commits; Escape cancels.
///
/// ## What differs from a desktop
///
///  - Rows are sized for a finger on a touch screen unless
///    [#setRowHeight(int)] was called; [#getRowHeight()] answers the
///    height in use.
///  - On a touch screen a press selects when it is released without
///    having moved; a drag is handed to the enclosing scroll pane to
///    scroll. With a mouse a press selects and a drag extends.
///  - Default renderers and editors are found by assignability, because a
///    device class cannot be asked for its superclass: the most specific
///    registered class the column class can be assigned to wins.
///  - Numbers and dates are shown by `toString`. A number cell is edited
///    as text and parsed for the column class; text that does not parse
///    keeps the editor open.
///  - Keys are handled as plain key events: arrows, Home, End, Page Up
///    and Down, Enter, Tab, Escape and F2. Typing over an editable cell
///    starts editing with the typed character.
///  - Printing, drag and drop and tool tips per cell are absent.
public class JTable extends JComponent implements TableModelListener, Scrollable, TableColumnModelListener,
        ListSelectionListener, CellEditorListener, RowSorterListener {

    public static final int AUTO_RESIZE_OFF = 0;
    public static final int AUTO_RESIZE_NEXT_COLUMN = 1;
    public static final int AUTO_RESIZE_SUBSEQUENT_COLUMNS = 2;
    public static final int AUTO_RESIZE_LAST_COLUMN = 3;
    public static final int AUTO_RESIZE_ALL_COLUMNS = 4;

    protected TableModel dataModel;
    protected TableColumnModel columnModel;
    protected ListSelectionModel selectionModel;
    protected JTableHeader tableHeader;
    protected int rowHeight;
    protected int rowMargin;
    protected Color gridColor;
    protected boolean showHorizontalLines;
    protected boolean showVerticalLines;
    protected int autoResizeMode;
    protected boolean autoCreateColumnsFromModel;
    protected Dimension preferredViewportSize;
    protected boolean rowSelectionAllowed;
    protected boolean cellSelectionEnabled;
    protected transient Component editorComp;
    protected transient TableCellEditor cellEditor;
    protected transient int editingColumn;
    protected transient int editingRow;
    protected transient Hashtable defaultRenderersByColumnClass;
    protected transient Hashtable defaultEditorsByColumnClass;
    protected Color selectionForeground;
    protected Color selectionBackground;

    private RowSorter<? extends TableModel> sorter;
    private boolean autoCreateRowSorter;
    private boolean updateSelectionOnSort = true;
    private boolean fillsViewportHeight;
    private boolean surrendersFocusOnKeystroke;
    private boolean rowHeightSet;
    private int themeRowHeight;
    private Font themeRowHeightFont;
    private float themeRowHeightScale;
    private int[] rowHeights;
    private int[] rowTops;
    private boolean inLayout;
    private boolean ignoreSorter;
    private int pressRow = -1;
    private int pressColumn = -1;
    private int clickBase;
    private boolean pressOnCell;
    private boolean pressTouch;
    private KeyListener editorKeys;

    public JTable() {
        this(null, null, null);
    }

    public JTable(TableModel dm) {
        this(dm, null, null);
    }

    public JTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    public JTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        setLayout(null);
        editingColumn = -1;
        editingRow = -1;
        TableColumnModel columns = cm;
        if (columns == null) {
            columns = createDefaultColumnModel();
            autoCreateColumnsFromModel = true;
        }
        setColumnModel(columns);
        setSelectionModel(sm != null ? sm : createDefaultSelectionModel());
        setModel(dm != null ? dm : createDefaultDataModel());
        initializeLocalVars();
    }

    public JTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
    }

    public JTable(Vector rowData, Vector columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
    }

    /// Shows the arrays themselves: every cell is editable and an edit
    /// writes into `rowData`.
    public JTable(final Object[][] rowData, final Object[] columnNames) {
        this(new AbstractTableModel() {
            @Override
            public String getColumnName(int column) {
                return String.valueOf(columnNames[column]);
            }

            @Override
            public int getRowCount() {
                return rowData.length;
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public Object getValueAt(int row, int col) {
                return rowData[row][col];
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }

            @Override
            public void setValueAt(Object value, int row, int col) {
                rowData[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        });
    }

    // ------------------------------------------------------------ set up

    protected void initializeLocalVars() {
        setOpaque(true);
        createDefaultRenderers();
        createDefaultEditors();
        setTableHeader(createDefaultTableHeader());
        showHorizontalLines = true;
        showVerticalLines = true;
        autoResizeMode = AUTO_RESIZE_SUBSEQUENT_COLUMNS;
        rowMargin = 1;
        rowSelectionAllowed = true;
        cellEditor = null;
        editingColumn = -1;
        editingRow = -1;
        preferredViewportSize = new Dimension(450, 400);
        setBackground(CellTheme.background("Table.background"));
        setForeground(CellTheme.foreground("Table.foreground"));
        setFont(CellTheme.font());
        selectionBackground = CellTheme.selectionBackground("Table.selectionBackground");
        selectionForeground = CellTheme.selectionForeground("Table.selectionForeground");
        gridColor = CellTheme.grid("Table.gridColor");
        setFocusable(true);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    protected TableModel createDefaultDataModel() {
        return new DefaultTableModel();
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    protected ListSelectionModel createDefaultSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel);
    }

    @SuppressWarnings("unchecked")
    protected void createDefaultRenderers() {
        defaultRenderersByColumnClass = new Hashtable();
        defaultRenderersByColumnClass.put(Object.class, new DefaultTableCellRenderer());
        defaultRenderersByColumnClass.put(Number.class, new NumberRenderer());
        defaultRenderersByColumnClass.put(Icon.class, new IconRenderer());
        defaultRenderersByColumnClass.put(Boolean.class, new BooleanRenderer());
    }

    @SuppressWarnings("unchecked")
    protected void createDefaultEditors() {
        defaultEditorsByColumnClass = new Hashtable();
        defaultEditorsByColumnClass.put(Object.class, new ValueEditor(false));
        defaultEditorsByColumnClass.put(Number.class, new ValueEditor(true));
        defaultEditorsByColumnClass.put(Boolean.class, new DefaultCellEditor(new JCheckBox()));
    }

    /// The entry of the most specific registered class `c` can be
    /// assigned to. A device class has no `getSuperclass`, so the walk up
    /// the hierarchy the JDK does is replaced by this search; interfaces
    /// count too.
    private static Object bySuperclass(Hashtable table, Class<?> c) {
        Class<?> wanted = c == null ? Object.class : c;
        Object direct = table.get(wanted);
        if (direct != null) {
            return direct;
        }
        Class<?> best = null;
        for (Enumeration e = table.keys(); e.hasMoreElements();) {
            Object k = e.nextElement();
            if (k instanceof Class) {
                Class<?> kc = (Class<?>) k;
                if (kc.isAssignableFrom(wanted) && (best == null || best.isAssignableFrom(kc))) {
                    best = kc;
                }
            }
        }
        return best == null ? null : table.get(best);
    }

    @SuppressWarnings("unchecked")
    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        if (renderer != null) {
            defaultRenderersByColumnClass.put(columnClass, renderer);
        } else {
            defaultRenderersByColumnClass.remove(columnClass);
        }
        repaint();
    }

    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        Object r = bySuperclass(defaultRenderersByColumnClass, columnClass);
        return r instanceof TableCellRenderer ? (TableCellRenderer) r : null;
    }

    @SuppressWarnings("unchecked")
    public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        if (editor != null) {
            defaultEditorsByColumnClass.put(columnClass, editor);
        } else {
            defaultEditorsByColumnClass.remove(columnClass);
        }
    }

    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        Object r = bySuperclass(defaultEditorsByColumnClass, columnClass);
        return r instanceof TableCellEditor ? (TableCellEditor) r : null;
    }

    // ------------------------------------------------------------ scroll pane

    @Override
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    @Override
    public void removeNotify() {
        unconfigureEnclosingScrollPane();
        super.removeNotify();
    }

    private JScrollPane scrollPaneOfThis() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            Container gp = parent.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane pane = (JScrollPane) gp;
                JViewport viewport = pane.getViewport();
                if (viewport != null && viewport.getView() == this) {
                    return pane;
                }
            }
        }
        return null;
    }

    /// Makes the table header the column header of the scroll pane this
    /// table is the view of.
    protected void configureEnclosingScrollPane() {
        JScrollPane pane = scrollPaneOfThis();
        if (pane != null && tableHeader != null) {
            pane.setColumnHeaderView(tableHeader);
        }
    }

    protected void unconfigureEnclosingScrollPane() {
        JScrollPane pane = scrollPaneOfThis();
        if (pane != null) {
            JViewport header = pane.getColumnHeader();
            if (header != null && header.getView() == tableHeader && tableHeader != null) {
                pane.setColumnHeaderView(null);
            }
        }
    }

    /// Scrolls the viewport this table is in so that the rectangle shows.
    public void scrollRectToVisible(Rectangle aRect) {
        ScrollDelegate.reveal(this, aRect);
    }

    // ------------------------------------------------------------ properties

    public void setTableHeader(JTableHeader tableHeader) {
        JTableHeader old = this.tableHeader;
        if (old != tableHeader) {
            if (old != null && old.getTable() == this) {
                old.setTable(null);
            }
            this.tableHeader = tableHeader;
            if (tableHeader != null) {
                tableHeader.setTable(this);
            }
            firePropertyChange("tableHeader", old, tableHeader);
        }
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    /// Sets the height of every row, in logical pixels, and stops the
    /// table from sizing its rows for the device.
    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        int old = this.rowHeight;
        this.rowHeight = rowHeight;
        rowHeightSet = true;
        rowHeights = null;
        rowTops = null;
        resizeAndRepaint();
        firePropertyChange("rowHeight", old, rowHeight);
    }

    /// The height of a row: the one that was set, or else one that fits
    /// the font, and a finger on a touch screen.
    public int getRowHeight() {
        if (rowHeightSet) {
            return rowHeight;
        }
        Font f = getFont();
        float scale = Units.scale();
        if (themeRowHeight <= 0 || f != themeRowHeightFont || Float.compare(scale, themeRowHeightScale) != 0) {
            themeRowHeight = Math.max(1, CellTheme.rowHeight(f, 16, 28));
            themeRowHeightFont = f;
            themeRowHeightScale = scale;
        }
        return themeRowHeight;
    }

    /// Sets the height of one row. Heights of single rows are forgotten
    /// when rows are inserted, removed or sorted.
    public void setRowHeight(int row, int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        int n = getRowCount();
        if (row < 0 || row >= n) {
            return;
        }
        if (rowHeights == null || rowHeights.length != n) {
            rowHeights = new int[n];
            int h = getRowHeight();
            for (int i = 0; i < n; i++) {
                rowHeights[i] = h;
            }
        }
        rowHeights[row] = rowHeight;
        rowTops = null;
        resizeAndRepaint();
    }

    public int getRowHeight(int row) {
        int[] hs = heights();
        return hs != null && row >= 0 && row < hs.length ? hs[row] : getRowHeight();
    }

    /// The heights of single rows, or `null` when all rows are alike.
    private int[] heights() {
        if (rowHeights != null && rowHeights.length != getRowCount()) {
            rowHeights = null;
            rowTops = null;
        }
        return rowHeights;
    }

    /// Where each row of single heights starts, and where the last ends.
    private int[] tops(int[] hs) {
        if (rowTops == null) {
            rowTops = new int[hs.length + 1];
            for (int i = 0; i < hs.length; i++) {
                rowTops[i + 1] = rowTops[i] + hs[i];
            }
        }
        return rowTops;
    }

    private int rowY(int row) {
        int[] hs = heights();
        if (hs == null) {
            return row * getRowHeight();
        }
        int[] t = tops(hs);
        return t[Math.max(0, Math.min(row, t.length - 1))];
    }

    /// The row that covers `y`, which may be past the last row.
    private int rowAtY(int y) {
        int[] hs = heights();
        if (hs == null) {
            int h = getRowHeight();
            return h <= 0 ? 0 : y / h;
        }
        int[] t = tops(hs);
        int lo = 0;
        int hi = t.length - 1;
        if (y >= t[hi]) {
            return hi;
        }
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (t[mid] <= y) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private int totalRowHeight() {
        return rowY(getRowCount());
    }

    public void setRowMargin(int rowMargin) {
        int old = this.rowMargin;
        this.rowMargin = rowMargin;
        resizeAndRepaint();
        firePropertyChange("rowMargin", old, rowMargin);
    }

    public int getRowMargin() {
        return rowMargin;
    }

    public void setIntercellSpacing(Dimension intercellSpacing) {
        setRowMargin(intercellSpacing.height);
        getColumnModel().setColumnMargin(intercellSpacing.width);
        resizeAndRepaint();
    }

    public Dimension getIntercellSpacing() {
        return new Dimension(getColumnModel().getColumnMargin(), rowMargin);
    }

    public void setGridColor(Color gridColor) {
        if (gridColor == null) {
            throw new IllegalArgumentException("New color is null");
        }
        Color old = this.gridColor;
        this.gridColor = gridColor;
        firePropertyChange("gridColor", old, gridColor);
        repaint();
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setShowGrid(boolean showGrid) {
        setShowHorizontalLines(showGrid);
        setShowVerticalLines(showGrid);
        repaint();
    }

    public void setShowHorizontalLines(boolean showHorizontalLines) {
        boolean old = this.showHorizontalLines;
        this.showHorizontalLines = showHorizontalLines;
        firePropertyChange("showHorizontalLines", old, showHorizontalLines);
        repaint();
    }

    public void setShowVerticalLines(boolean showVerticalLines) {
        boolean old = this.showVerticalLines;
        this.showVerticalLines = showVerticalLines;
        firePropertyChange("showVerticalLines", old, showVerticalLines);
        repaint();
    }

    public boolean getShowHorizontalLines() {
        return showHorizontalLines;
    }

    public boolean getShowVerticalLines() {
        return showVerticalLines;
    }

    public void setAutoResizeMode(int mode) {
        if (mode < AUTO_RESIZE_OFF || mode > AUTO_RESIZE_ALL_COLUMNS) {
            return;
        }
        int old = autoResizeMode;
        autoResizeMode = mode;
        resizeAndRepaint();
        if (tableHeader != null) {
            tableHeader.resizeAndRepaint();
        }
        firePropertyChange("autoResizeMode", old, autoResizeMode);
    }

    public int getAutoResizeMode() {
        return autoResizeMode;
    }

    public void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) {
        if (this.autoCreateColumnsFromModel != autoCreateColumnsFromModel) {
            boolean old = this.autoCreateColumnsFromModel;
            this.autoCreateColumnsFromModel = autoCreateColumnsFromModel;
            if (autoCreateColumnsFromModel) {
                createDefaultColumnsFromModel();
            }
            firePropertyChange("autoCreateColumnsFromModel", old, autoCreateColumnsFromModel);
        }
    }

    public boolean getAutoCreateColumnsFromModel() {
        return autoCreateColumnsFromModel;
    }

    /// Replaces the columns by one per column of the model, in its order.
    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m == null) {
            return;
        }
        TableColumnModel cm = getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0));
        }
        for (int i = 0; i < m.getColumnCount(); i++) {
            addColumn(new TableColumn(i));
        }
    }

    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
        boolean old = this.autoCreateRowSorter;
        this.autoCreateRowSorter = autoCreateRowSorter;
        if (autoCreateRowSorter) {
            setRowSorter(new TableRowSorter<TableModel>(getModel()));
        }
        firePropertyChange("autoCreateRowSorter", old, autoCreateRowSorter);
    }

    public boolean getAutoCreateRowSorter() {
        return autoCreateRowSorter;
    }

    public void setUpdateSelectionOnSort(boolean update) {
        boolean old = updateSelectionOnSort;
        updateSelectionOnSort = update;
        firePropertyChange("updateSelectionOnSort", old, update);
    }

    public boolean getUpdateSelectionOnSort() {
        return updateSelectionOnSort;
    }

    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        RowSorter<? extends TableModel> old = this.sorter;
        if (old != null) {
            old.removeRowSorterListener(this);
        }
        this.sorter = sorter;
        if (sorter != null) {
            sorter.addRowSorterListener(this);
        }
        rowHeights = null;
        rowTops = null;
        if (selectionModel != null) {
            selectionModel.clearSelection();
        }
        firePropertyChange("rowSorter", old, sorter);
        resizeAndRepaint();
        if (tableHeader != null) {
            tableHeader.repaint();
        }
    }

    public RowSorter<? extends TableModel> getRowSorter() {
        return sorter;
    }

    public void setFillsViewportHeight(boolean fillsViewportHeight) {
        boolean old = this.fillsViewportHeight;
        this.fillsViewportHeight = fillsViewportHeight;
        resizeAndRepaint();
        firePropertyChange("fillsViewportHeight", old, fillsViewportHeight);
    }

    public boolean getFillsViewportHeight() {
        return fillsViewportHeight;
    }

    public void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
        this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
    }

    public boolean getSurrendersFocusOnKeystroke() {
        return surrendersFocusOnKeystroke;
    }

    // ------------------------------------------------------------ selection

    public void setSelectionMode(int selectionMode) {
        clearSelection();
        getSelectionModel().setSelectionMode(selectionMode);
        getColumnModel().getSelectionModel().setSelectionMode(selectionMode);
    }

    public void setRowSelectionAllowed(boolean rowSelectionAllowed) {
        boolean old = this.rowSelectionAllowed;
        this.rowSelectionAllowed = rowSelectionAllowed;
        if (old != rowSelectionAllowed) {
            repaint();
        }
        firePropertyChange("rowSelectionAllowed", old, rowSelectionAllowed);
    }

    public boolean getRowSelectionAllowed() {
        return rowSelectionAllowed;
    }

    public void setColumnSelectionAllowed(boolean columnSelectionAllowed) {
        boolean old = columnModel.getColumnSelectionAllowed();
        columnModel.setColumnSelectionAllowed(columnSelectionAllowed);
        if (old != columnSelectionAllowed) {
            repaint();
        }
        firePropertyChange("columnSelectionAllowed", old, columnSelectionAllowed);
    }

    public boolean getColumnSelectionAllowed() {
        return columnModel.getColumnSelectionAllowed();
    }

    public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
        setRowSelectionAllowed(cellSelectionEnabled);
        setColumnSelectionAllowed(cellSelectionEnabled);
        boolean old = this.cellSelectionEnabled;
        this.cellSelectionEnabled = cellSelectionEnabled;
        firePropertyChange("cellSelectionEnabled", old, cellSelectionEnabled);
    }

    public boolean getCellSelectionEnabled() {
        return getRowSelectionAllowed() && getColumnSelectionAllowed();
    }

    public void selectAll() {
        if (isEditing()) {
            removeEditor();
        }
        int rows = getRowCount();
        int columns = getColumnCount();
        if (rows > 0 && columns > 0) {
            selectionModel.setSelectionInterval(0, rows - 1);
            columnModel.getSelectionModel().setSelectionInterval(0, columns - 1);
        }
    }

    public void clearSelection() {
        selectionModel.clearSelection();
        columnModel.getSelectionModel().clearSelection();
    }

    private int boundRow(int row) {
        if (row < 0 || row >= getRowCount()) {
            throw new IllegalArgumentException("Row index out of range");
        }
        return row;
    }

    private int boundColumn(int col) {
        if (col < 0 || col >= getColumnCount()) {
            throw new IllegalArgumentException("Column index out of range");
        }
        return col;
    }

    public void setRowSelectionInterval(int index0, int index1) {
        selectionModel.setSelectionInterval(boundRow(index0), boundRow(index1));
    }

    public void setColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().setSelectionInterval(boundColumn(index0), boundColumn(index1));
    }

    public void addRowSelectionInterval(int index0, int index1) {
        selectionModel.addSelectionInterval(boundRow(index0), boundRow(index1));
    }

    public void addColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().addSelectionInterval(boundColumn(index0), boundColumn(index1));
    }

    public void removeRowSelectionInterval(int index0, int index1) {
        selectionModel.removeSelectionInterval(boundRow(index0), boundRow(index1));
    }

    public void removeColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().removeSelectionInterval(boundColumn(index0), boundColumn(index1));
    }

    public int getSelectedRow() {
        return selectionModel.getMinSelectionIndex();
    }

    public int getSelectedColumn() {
        return columnModel.getSelectionModel().getMinSelectionIndex();
    }

    public int[] getSelectedRows() {
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        if (min < 0 || max < 0) {
            return new int[0];
        }
        int[] tmp = new int[max - min + 1];
        int n = 0;
        for (int i = min; i <= max; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                tmp[n++] = i;
            }
        }
        int[] out = new int[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    public int[] getSelectedColumns() {
        return columnModel.getSelectedColumns();
    }

    public int getSelectedRowCount() {
        return getSelectedRows().length;
    }

    public int getSelectedColumnCount() {
        return columnModel.getSelectedColumnCount();
    }

    public boolean isRowSelected(int row) {
        return selectionModel.isSelectedIndex(row);
    }

    public boolean isColumnSelected(int column) {
        return columnModel.getSelectionModel().isSelectedIndex(column);
    }

    public boolean isCellSelected(int row, int column) {
        if (!getRowSelectionAllowed() && !getColumnSelectionAllowed()) {
            return false;
        }
        return (!getRowSelectionAllowed() || isRowSelected(row))
                && (!getColumnSelectionAllowed() || isColumnSelected(column));
    }

    private static void change(ListSelectionModel sm, int index, boolean toggle, boolean extend, boolean selected) {
        if (extend) {
            if (toggle) {
                sm.setAnchorSelectionIndex(index);
            } else {
                int anchor = sm.getAnchorSelectionIndex();
                sm.setSelectionInterval(anchor < 0 ? index : anchor, index);
            }
        } else if (toggle) {
            if (selected) {
                sm.removeSelectionInterval(index, index);
            } else {
                sm.addSelectionInterval(index, index);
            }
        } else {
            sm.setSelectionInterval(index, index);
        }
    }

    /// Moves the selection to a cell the way a click or a key does:
    /// `toggle` as with Control held, `extend` as with Shift held. The
    /// cell is scrolled into view.
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        boolean selected = isCellSelected(rowIndex, columnIndex);
        change(columnModel.getSelectionModel(), columnIndex, toggle, extend, selected);
        change(selectionModel, rowIndex, toggle, extend, selected);
        Rectangle cell = getCellRect(rowIndex, columnIndex, true);
        if (cell.width > 0 && cell.height > 0) {
            scrollRectToVisible(cell);
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        Color old = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", old, selectionForeground);
        repaint();
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color old = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", old, selectionBackground);
        repaint();
    }

    // ------------------------------------------------------------ indexes

    public TableColumn getColumn(Object identifier) {
        TableColumnModel cm = getColumnModel();
        return cm.getColumn(cm.getColumnIndex(identifier));
    }

    public int convertColumnIndexToModel(int viewColumnIndex) {
        if (viewColumnIndex < 0) {
            return viewColumnIndex;
        }
        return getColumnModel().getColumn(viewColumnIndex).getModelIndex();
    }

    public int convertColumnIndexToView(int modelColumnIndex) {
        if (modelColumnIndex < 0) {
            return modelColumnIndex;
        }
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            if (cm.getColumn(i).getModelIndex() == modelColumnIndex) {
                return i;
            }
        }
        return -1;
    }

    public int convertRowIndexToView(int modelRowIndex) {
        return sorter != null ? sorter.convertRowIndexToView(modelRowIndex) : modelRowIndex;
    }

    public int convertRowIndexToModel(int viewRowIndex) {
        return sorter != null ? sorter.convertRowIndexToModel(viewRowIndex) : viewRowIndex;
    }

    public int getRowCount() {
        if (sorter != null) {
            return sorter.getViewRowCount();
        }
        return dataModel == null ? 0 : dataModel.getRowCount();
    }

    public int getColumnCount() {
        return getColumnModel().getColumnCount();
    }

    public String getColumnName(int column) {
        return getModel().getColumnName(convertColumnIndexToModel(column));
    }

    public Class<?> getColumnClass(int column) {
        return getModel().getColumnClass(convertColumnIndexToModel(column));
    }

    public Object getValueAt(int row, int column) {
        return getModel().getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(column));
    }

    public void setValueAt(Object aValue, int row, int column) {
        getModel().setValueAt(aValue, convertRowIndexToModel(row), convertColumnIndexToModel(column));
    }

    public boolean isCellEditable(int row, int column) {
        return getModel().isCellEditable(convertRowIndexToModel(row), convertColumnIndexToModel(column));
    }

    public void addColumn(TableColumn aColumn) {
        if (aColumn.getHeaderValue() == null) {
            aColumn.setHeaderValue(getModel().getColumnName(aColumn.getModelIndex()));
        }
        getColumnModel().addColumn(aColumn);
    }

    public void removeColumn(TableColumn aColumn) {
        getColumnModel().removeColumn(aColumn);
    }

    public void moveColumn(int column, int targetColumn) {
        getColumnModel().moveColumn(column, targetColumn);
    }

    // ------------------------------------------------------------ geometry

    /// The view column at a point in logical pixels, or -1.
    public int columnAtPoint(Point point) {
        return getColumnModel().getColumnIndexAtX(point.x);
    }

    /// The view row at a point in logical pixels, or -1.
    public int rowAtPoint(Point point) {
        if (point.y < 0) {
            return -1;
        }
        int row = rowAtY(point.y);
        return row < getRowCount() ? row : -1;
    }

    private int columnX(int column) {
        TableColumnModel cm = getColumnModel();
        int x = 0;
        for (int i = 0; i < column; i++) {
            x += cm.getColumn(i).getWidth();
        }
        return x;
    }

    /// The rectangle of a cell in logical pixels: the whole cell with
    /// `includeSpacing`, else the part inside the gaps between cells,
    /// which is what a renderer or editor is given.
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle r = new Rectangle();
        boolean valid = true;
        if (row < 0) {
            valid = false;
        } else if (row >= getRowCount()) {
            r.y = getHeight();
            valid = false;
        } else {
            r.height = getRowHeight(row);
            r.y = rowY(row);
        }
        if (column < 0) {
            valid = false;
        } else if (column >= getColumnCount()) {
            r.x = getWidth();
            valid = false;
        } else {
            r.x = columnX(column);
            r.width = getColumnModel().getColumn(column).getWidth();
        }
        if (valid && !includeSpacing) {
            int rm = Math.min(getRowMargin(), r.height);
            int cm = Math.min(getColumnModel().getColumnMargin(), r.width);
            r.setBounds(r.x + cm / 2, r.y + rm / 2, r.width - cm, r.height - rm);
        }
        return r;
    }

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
        return new Dimension((int) Math.min(w, Integer.MAX_VALUE), totalRowHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        TableColumnModel cm = getColumnModel();
        long w = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            w += cm.getColumn(i).getMinWidth();
        }
        return new Dimension((int) Math.min(w, Integer.MAX_VALUE), totalRowHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        TableColumnModel cm = getColumnModel();
        long w = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            w += cm.getColumn(i).getMaxWidth();
        }
        return new Dimension((int) Math.min(w, Integer.MAX_VALUE), totalRowHeight());
    }

    /// Spreads `target` over the columns `from` up to `to`, starting from
    /// the preferred widths (or the current ones): each column takes the
    /// share of the difference that its own room between its limits is of
    /// all the room. The widths add up to `target` exactly whenever the
    /// limits allow.
    private void spread(long target, int from, int to, boolean fromPreferred) {
        TableColumnModel cm = getColumnModel();
        long total = 0;
        long totalMin = 0;
        long totalMax = 0;
        for (int i = from; i < to; i++) {
            TableColumn c = cm.getColumn(i);
            total += fromPreferred ? c.getPreferredWidth() : c.getWidth();
            totalMin += c.getMinWidth();
            totalMax += c.getMaxWidth();
        }
        boolean shrink = target < total;
        long lowerSum = shrink ? totalMin : total;
        long upperSum = shrink ? total : totalMax;
        long left = target;
        for (int i = from; i < to; i++) {
            TableColumn c = cm.getColumn(i);
            int size = fromPreferred ? c.getPreferredWidth() : c.getWidth();
            long lower = shrink ? c.getMinWidth() : size;
            long upper = shrink ? size : c.getMaxWidth();
            long span = upperSum - lowerSum;
            long now;
            if (span <= 0) {
                now = lower;
            } else {
                now = Math.round(lower + (double) (left - lowerSum) / (double) span * (upper - lower));
            }
            c.setWidth((int) Math.max(0, Math.min(now, Integer.MAX_VALUE)));
            left -= c.getWidth();
            lowerSum -= lower;
            upperSum -= upper;
        }
    }

    /// Gives the columns their widths. Without a column being dragged the
    /// table's width is shared out from the preferred widths (or, with
    /// [#AUTO_RESIZE_OFF], every column simply gets its preferred width).
    /// While the header drags a column, the columns the resize mode names
    /// take up the difference and the widths become the preferred ones.
    @Override
    public void doLayout() {
        inLayout = true;
        try {
            TableColumnModel cm = getColumnModel();
            int n = cm.getColumnCount();
            TableColumn resizing = tableHeader == null ? null : tableHeader.getResizingColumn();
            if (resizing == null) {
                if (autoResizeMode == AUTO_RESIZE_OFF) {
                    for (int i = 0; i < n; i++) {
                        TableColumn c = cm.getColumn(i);
                        c.setWidth(c.getPreferredWidth());
                    }
                } else if (getWidth() > 0 && n > 0) {
                    spread(getWidth(), 0, n, true);
                }
            } else {
                int at = -1;
                for (int i = 0; i < n; i++) {
                    if (cm.getColumn(i) == resizing) {
                        at = i;
                        break;
                    }
                }
                if (autoResizeMode != AUTO_RESIZE_OFF && at >= 0 && getWidth() > 0) {
                    int from = at + 1;
                    int to = n;
                    switch (autoResizeMode) {
                        case AUTO_RESIZE_NEXT_COLUMN:
                            to = Math.min(from + 1, n);
                            break;
                        case AUTO_RESIZE_LAST_COLUMN:
                            from = Math.max(from, n - 1);
                            break;
                        case AUTO_RESIZE_ALL_COLUMNS:
                            from = 0;
                            break;
                        default:
                            break;
                    }
                    long delta = (long) getWidth() - cm.getTotalColumnWidth();
                    if (from < to && delta != 0) {
                        long range = 0;
                        for (int i = from; i < to; i++) {
                            range += cm.getColumn(i).getWidth();
                        }
                        spread(range + delta, from, to, false);
                    }
                    delta = (long) getWidth() - cm.getTotalColumnWidth();
                    if (delta != 0) {
                        // The others could not take it all: the dragged
                        // column gives back what is left.
                        resizing.setWidth((int) (resizing.getWidth() + delta));
                    }
                }
                for (int i = 0; i < n; i++) {
                    TableColumn c = cm.getColumn(i);
                    c.setPreferredWidth(c.getWidth());
                }
            }
            if (isEditing() && editorComp != null) {
                editorComp.setBounds(getCellRect(editingRow, editingColumn, false));
            }
        } finally {
            inLayout = false;
        }
        super.doLayout();
    }

    /// Lays the columns out as [#doLayout()] does; the argument is ignored.
    public void sizeColumnsToFit(int resizingColumn) {
        doLayout();
        repaint();
    }

    protected void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ paint

    private boolean focusCell(int row, int column) {
        return selectionModel.getLeadSelectionIndex() == row
                && columnModel.getSelectionModel().getLeadSelectionIndex() == column && isFocusOwner();
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
        TableColumn c = getColumnModel().getColumn(column);
        TableCellRenderer r = c.getCellRenderer();
        return r != null ? r : getDefaultRenderer(getColumnClass(column));
    }

    /// Asks the renderer for the component that paints a cell, telling it
    /// the cell's value, whether the cell is selected and whether it is
    /// the focused cell of a table that has the focus.
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        return renderer.getTableCellRendererComponent(this, getValueAt(row, column), isCellSelected(row, column),
                focusCell(row, column), row, column);
    }

    /// Paints the cells that meet the clip, and the grid over them.
    @Override
    protected void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        if (isOpaque() && getBackground() != null) {
            g.setColor(getBackground());
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }
        int rows = getRowCount();
        TableColumnModel cm = getColumnModel();
        int columns = cm.getColumnCount();
        if (rows <= 0 || columns <= 0 || clip.width <= 0 || clip.height <= 0) {
            return;
        }
        int rMin = Math.max(0, rowAtY(Math.max(0, clip.y)));
        int rMax = Math.min(rows - 1, rowAtY(Math.max(0, clip.y + clip.height - 1)));
        int cMin = -1;
        int cMax = -1;
        int[] xs = new int[columns + 1];
        for (int i = 0; i < columns; i++) {
            int w = cm.getColumn(i).getWidth();
            xs[i + 1] = xs[i] + w;
            if (xs[i + 1] > clip.x && xs[i] < clip.x + clip.width) {
                if (cMin < 0) {
                    cMin = i;
                }
                cMax = i;
            }
        }
        if (cMin < 0 || rMin > rMax) {
            return;
        }
        int cmargin = cm.getColumnMargin();
        for (int row = rMin; row <= rMax; row++) {
            int y = rowY(row);
            int h = getRowHeight(row);
            int rm = Math.min(rowMargin, h);
            for (int col = cMin; col <= cMax; col++) {
                if (row == editingRow && col == editingColumn && editorComp != null) {
                    continue;
                }
                int w = xs[col + 1] - xs[col];
                int m = Math.min(cmargin, w);
                TableCellRenderer renderer = getCellRenderer(row, col);
                if (renderer == null) {
                    continue;
                }
                Component c = prepareRenderer(renderer, row, col);
                CellPainter.paint(g, c, xs[col] + m / 2, y + rm / 2, w - m, h - rm);
            }
        }
        if (gridColor != null && (showHorizontalLines || showVerticalLines)) {
            g.setColor(gridColor);
            int right = xs[cMax + 1] - 1;
            int bottom = rowY(rMax) + getRowHeight(rMax) - 1;
            if (showHorizontalLines) {
                for (int row = rMin; row <= rMax; row++) {
                    int y = rowY(row) + getRowHeight(row) - 1;
                    g.drawLine(xs[cMin], y, right, y);
                }
            }
            if (showVerticalLines) {
                for (int col = cMin; col <= cMax; col++) {
                    int x = xs[col + 1] - 1;
                    g.drawLine(x, rowY(rMin), x, bottom);
                }
            }
        }
    }

    private void repaintRows(int first, int last) {
        int rows = getRowCount();
        if (rows <= 0) {
            repaint();
            return;
        }
        int a = Math.max(0, Math.min(first, last));
        int b = Math.min(rows - 1, Math.max(first, last));
        if (a > b) {
            return;
        }
        int y = rowY(a);
        repaint(0, y, getWidth(), rowY(b) + getRowHeight(b) - y);
    }

    // ------------------------------------------------------------ editing

    public boolean editCellAt(int row, int column) {
        return editCellAt(row, column, null);
    }

    /// Starts editing a cell if the model and the editor allow it for the
    /// event: the editor's component is placed over the cell as a child
    /// of the table. An edit in progress is stopped first.
    public boolean editCellAt(int row, int column, EventObject e) {
        if (cellEditor != null && !cellEditor.stopCellEditing()) {
            return false;
        }
        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }
        if (!isCellEditable(row, column)) {
            return false;
        }
        TableCellEditor editor = getCellEditor(row, column);
        if (editor == null || !editor.isCellEditable(e)) {
            return false;
        }
        Component c = prepareEditor(editor, row, column);
        if (c == null) {
            removeEditor();
            return false;
        }
        editorComp = c;
        c.setBounds(getCellRect(row, column, false));
        add(c);
        setCellEditor(editor);
        setEditingRow(row);
        setEditingColumn(column);
        editor.addCellEditorListener(this);
        if (editorKeys == null) {
            editorKeys = new EditorKeys();
        }
        c.addKeyListener(editorKeys);
        revalidate();
        repaint();
        return true;
    }

    public boolean isEditing() {
        return cellEditor != null;
    }

    public Component getEditorComponent() {
        return editorComp;
    }

    public int getEditingColumn() {
        return editingColumn;
    }

    public int getEditingRow() {
        return editingRow;
    }

    public void setEditingColumn(int aColumn) {
        editingColumn = aColumn;
    }

    public void setEditingRow(int aRow) {
        editingRow = aRow;
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    public void setCellEditor(TableCellEditor anEditor) {
        TableCellEditor old = cellEditor;
        cellEditor = anEditor;
        firePropertyChange("tableCellEditor", old, anEditor);
    }

    public TableCellEditor getCellEditor(int row, int column) {
        TableColumn c = getColumnModel().getColumn(column);
        TableCellEditor editor = c.getCellEditor();
        return editor != null ? editor : getDefaultEditor(getColumnClass(column));
    }

    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        return editor.getTableCellEditorComponent(this, getValueAt(row, column), isCellSelected(row, column), row,
                column);
    }

    /// Takes the editor's component off the table and forgets the edit,
    /// without storing anything.
    public void removeEditor() {
        TableCellEditor editor = getCellEditor();
        if (editor == null) {
            return;
        }
        editor.removeCellEditorListener(this);
        Component c = editorComp;
        int row = editingRow;
        setCellEditor(null);
        setEditingColumn(-1);
        setEditingRow(-1);
        editorComp = null;
        if (c != null) {
            if (editorKeys != null) {
                c.removeKeyListener(editorKeys);
            }
            boolean focused = c.isFocusOwner();
            remove(c);
            if (focused) {
                requestFocusInWindow();
            }
        }
        if (row >= 0 && row < getRowCount()) {
            repaintRows(row, row);
        } else {
            repaint();
        }
    }

    /// The editor finished: its value goes to the model.
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            int row = editingRow;
            int column = editingColumn;
            removeEditor();
            if (row >= 0 && row < getRowCount() && column >= 0 && column < getColumnCount()) {
                setValueAt(value, row, column);
            }
        }
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        removeEditor();
    }

    // ------------------------------------------------------------ model events

    public void setModel(TableModel dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("Cannot set a null TableModel");
        }
        if (this.dataModel != dataModel) {
            TableModel old = this.dataModel;
            if (old != null) {
                old.removeTableModelListener(this);
            }
            this.dataModel = dataModel;
            dataModel.addTableModelListener(this);
            tableChanged(new TableModelEvent(dataModel, TableModelEvent.HEADER_ROW));
            firePropertyChange("model", old, dataModel);
            if (autoCreateRowSorter) {
                setRowSorter(new TableRowSorter<TableModel>(dataModel));
            }
        }
    }

    public TableModel getModel() {
        return dataModel;
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
            if (tableHeader != null) {
                tableHeader.setColumnModel(columnModel);
            }
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    public void setSelectionModel(ListSelectionModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Cannot set a null SelectionModel");
        }
        ListSelectionModel old = selectionModel;
        if (newModel != old) {
            if (old != null) {
                old.removeListSelectionListener(this);
            }
            selectionModel = newModel;
            newModel.addListSelectionListener(this);
            firePropertyChange("selectionModel", old, newModel);
            repaint();
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /// The model rows that are selected, read through the sorter as it is
    /// now.
    private int[] selectedModelRows() {
        int[] view = getSelectedRows();
        int[] out = new int[view.length];
        int n = 0;
        for (int i = 0; i < view.length; i++) {
            if (view[i] < getRowCount()) {
                out[n++] = convertRowIndexToModel(view[i]);
            }
        }
        if (n == out.length) {
            return out;
        }
        int[] cut = new int[n];
        System.arraycopy(out, 0, cut, 0, n);
        return cut;
    }

    /// Selects the view rows that show the given model rows; a negative
    /// entry is a row that is gone.
    private void selectModelRows(int[] modelRows) {
        selectionModel.setValueIsAdjusting(true);
        try {
            selectionModel.clearSelection();
            for (int i = 0; i < modelRows.length; i++) {
                int m = modelRows[i];
                if (m < 0 || m >= dataModel.getRowCount()) {
                    continue;
                }
                int v = convertRowIndexToView(m);
                if (v >= 0) {
                    selectionModel.addSelectionInterval(v, v);
                }
            }
        } finally {
            selectionModel.setValueIsAdjusting(false);
        }
    }

    /// The model changed while a sorter stands between it and the view:
    /// the sorter is told, and the selection follows the model rows it
    /// was on.
    private void sortedTableChanged(TableModelEvent e) {
        int first = e.getFirstRow();
        int last = e.getLastRow();
        int type = e.getType();
        boolean all = type == TableModelEvent.UPDATE && last == Integer.MAX_VALUE;
        int[] selected = all ? new int[0] : selectedModelRows();
        ignoreSorter = true;
        try {
            if (all) {
                sorter.allRowsChanged();
            } else if (type == TableModelEvent.INSERT) {
                int length = last - first + 1;
                for (int i = 0; i < selected.length; i++) {
                    if (selected[i] >= first) {
                        selected[i] += length;
                    }
                }
                sorter.rowsInserted(first, last);
            } else if (type == TableModelEvent.DELETE) {
                int length = last - first + 1;
                for (int i = 0; i < selected.length; i++) {
                    if (selected[i] > last) {
                        selected[i] -= length;
                    } else if (selected[i] >= first) {
                        selected[i] = -1;
                    }
                }
                sorter.rowsDeleted(first, last);
            } else if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                sorter.rowsUpdated(first, last);
            } else {
                sorter.rowsUpdated(first, last, e.getColumn());
            }
        } finally {
            ignoreSorter = false;
        }
        rowHeights = null;
        rowTops = null;
        selectModelRows(selected);
        resizeAndRepaint();
    }

    /// The table's model changed: rows are redrawn, the selection is
    /// shifted with inserted and removed rows, and after a change of
    /// structure the columns are made again if the table makes its own.
    @Override
    public void tableChanged(TableModelEvent e) {
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            if (isEditing()) {
                removeEditor();
            }
            if (selectionModel != null) {
                clearSelection();
            }
            rowHeights = null;
            rowTops = null;
            if (sorter != null) {
                if (autoCreateRowSorter) {
                    setRowSorter(new TableRowSorter<TableModel>(dataModel));
                } else {
                    ignoreSorter = true;
                    try {
                        sorter.modelStructureChanged();
                    } finally {
                        ignoreSorter = false;
                    }
                }
            }
            if (getAutoCreateColumnsFromModel()) {
                createDefaultColumnsFromModel();
            }
            resizeAndRepaint();
            if (tableHeader != null) {
                tableHeader.resizeAndRepaint();
            }
            return;
        }
        int first = e.getFirstRow();
        int last = e.getLastRow();
        int type = e.getType();
        if (isEditing() && (type != TableModelEvent.UPDATE || last == Integer.MAX_VALUE)) {
            removeEditor();
        }
        if (sorter != null) {
            sortedTableChanged(e);
            return;
        }
        if (type == TableModelEvent.INSERT) {
            int start = Math.max(0, first);
            int end = last < 0 ? getRowCount() - 1 : last;
            selectionModel.insertIndexInterval(start, end - start + 1, true);
            rowHeights = null;
            rowTops = null;
            resizeAndRepaint();
        } else if (type == TableModelEvent.DELETE) {
            int start = Math.max(0, first);
            int end = last < 0 ? getRowCount() - 1 : last;
            selectionModel.removeIndexInterval(start, end);
            rowHeights = null;
            rowTops = null;
            resizeAndRepaint();
        } else if (last == Integer.MAX_VALUE) {
            clearSelection();
            rowHeights = null;
            rowTops = null;
            resizeAndRepaint();
        } else {
            repaintRows(first, last);
        }
    }

    /// The sorter changed the order or the set of rows: the selection is
    /// moved to where its rows went, and the table is drawn again.
    @Override
    public void sorterChanged(RowSorterEvent e) {
        if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
            if (tableHeader != null) {
                tableHeader.repaint();
            }
            return;
        }
        if (ignoreSorter) {
            return;
        }
        if (isEditing()) {
            removeEditor();
        }
        int[] view = getSelectedRows();
        int[] model = new int[view.length];
        for (int i = 0; i < view.length; i++) {
            model[i] = updateSelectionOnSort ? e.convertPreviousRowIndexToModel(view[i]) : -1;
        }
        rowHeights = null;
        rowTops = null;
        selectModelRows(model);
        resizeAndRepaint();
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if (isEditing() && e.getFromIndex() != e.getToIndex()) {
            removeEditor();
        }
        repaint();
    }

    /// A column's width or the gap between columns changed. Outside the
    /// table's own layout this asks for a new layout; a column the header
    /// is dragging keeps its width as its preferred one when the table
    /// does not resize the others.
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        if (inLayout) {
            repaint();
            return;
        }
        TableColumn resizing = tableHeader == null ? null : tableHeader.getResizingColumn();
        if (resizing != null && autoResizeMode == AUTO_RESIZE_OFF) {
            resizing.setPreferredWidth(resizing.getWidth());
        }
        if (isEditing() && editorComp != null) {
            editorComp.setBounds(getCellRect(editingRow, editingColumn, false));
        }
        resizeAndRepaint();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        repaint();
    }

    /// The row selection changed: the rows concerned are drawn again.
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (getColumnCount() <= 0 || getRowCount() <= 0) {
            return;
        }
        repaintRows(e.getFirstIndex(), e.getLastIndex());
    }

    // ------------------------------------------------------------ pointer

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (isEnabled()) {
            pointer(e);
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (isEnabled() && e.getID() == MouseEvent.MOUSE_DRAGGED) {
            if (pressTouch) {
                ScrollDelegate.forward(this, e);
            } else if (pressOnCell && !isEditing()) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0 && !isCellSelectedAsLead(row, col)) {
                    changeSelection(row, col, false, true);
                }
            }
        }
        super.processMouseMotionEvent(e);
    }

    private boolean isCellSelectedAsLead(int row, int col) {
        return selectionModel.getLeadSelectionIndex() == row
                && columnModel.getSelectionModel().getLeadSelectionIndex() == col;
    }

    private boolean overEditor(MouseEvent e) {
        return editorComp != null && editorComp.getBounds().contains(e.getX(), e.getY());
    }

    private void pointer(MouseEvent e) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                pressTouch = CellTheme.touch();
                pressOnCell = false;
                if (overEditor(e)) {
                    return;
                }
                if (pressTouch) {
                    ScrollDelegate.forward(this, e);
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    select(e);
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (pressTouch) {
                    ScrollDelegate.forward(this, e);
                }
                break;
            case MouseEvent.MOUSE_CLICKED:
                if (pressTouch && !overEditor(e)) {
                    select(e);
                }
                break;
            default:
                break;
        }
    }

    /// What a press with a mouse, or a tap with a finger, does to the cell
    /// under it: starts editing if the editor wants to for this event,
    /// and moves the selection there.
    private void select(MouseEvent event) {
        MouseEvent e = event;
        int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());
        if (row < 0 || col < 0) {
            if (isEditing() && !getCellEditor().stopCellEditing()) {
                getCellEditor().cancelCellEditing();
            }
            requestFocusInWindow();
            return;
        }
        // Clicks are counted by time alone, so a quick press on another
        // cell arrives as a second click: count from the cell's own first.
        int clicks = e.getClickCount();
        if (row != pressRow || col != pressColumn || clicks <= 1) {
            clickBase = clicks - 1;
        }
        pressRow = row;
        pressColumn = col;
        pressOnCell = true;
        if (clickBase > 0) {
            e = new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY(),
                    clicks - clickBase, e.isPopupTrigger(), e.getButton());
        }
        boolean editing = editCellAt(row, col, e);
        TableCellEditor editor = getCellEditor();
        if (!editing) {
            requestFocusInWindow();
        }
        if (!editing || editor == null || editor.shouldSelectCell(e)) {
            changeSelection(row, col, e.isControlDown() || e.isMetaDown(), e.isShiftDown());
        }
        if (editing && editorComp != null) {
            startedBy(null);
        }
    }

    /// Hands a freshly placed editor the input that started it: a check
    /// box is clicked, a text field gets the typed character, and the
    /// component is focused so that the keyboard goes to it.
    private void startedBy(String typed) {
        Component c = editorComp;
        if (c instanceof JCheckBox) {
            ((JCheckBox) c).doClick();
            return;
        }
        if (typed != null && c instanceof JTextComponent) {
            ((JTextComponent) c).setText(typed);
        }
        c.requestFocus();
        com.codename1.ui.Component peer = c.cn1PeerOrNull();
        if (peer instanceof com.codename1.ui.TextArea && peer.getComponentForm() != null && CellTheme.touch()) {
            ((com.codename1.ui.TextArea) peer).startEditingAsync();
        }
    }

    // ------------------------------------------------------------ keys

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (isEnabled() && !e.isConsumed()) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (e.getID() == KeyEvent.KEY_TYPED) {
                keyTyped(e);
            }
        }
        super.processKeyEvent(e);
    }

    private int visibleRows() {
        Container p = getParent();
        int h = p instanceof JViewport ? p.getHeight() : getHeight();
        int rh = getRowHeight();
        return rh <= 0 ? 1 : Math.max(1, h / rh);
    }

    /// Moves the lead cell by a number of rows and columns; past the last
    /// column a move wraps to the next row when `wrap` is set.
    private void move(int dRow, int dCol, boolean wrap, boolean extend) {
        int rows = getRowCount();
        int columns = getColumnCount();
        if (rows <= 0 || columns <= 0) {
            return;
        }
        int row = selectionModel.getLeadSelectionIndex();
        int col = columnModel.getSelectionModel().getLeadSelectionIndex();
        if (row < 0 || col < 0) {
            row = Math.max(0, row);
            col = Math.max(0, col);
            dRow = 0;
            dCol = 0;
        }
        row += dRow;
        col += dCol;
        if (wrap) {
            if (col >= columns) {
                col = 0;
                row++;
            } else if (col < 0) {
                col = columns - 1;
                row--;
            }
            if (row >= rows) {
                row = 0;
            } else if (row < 0) {
                row = rows - 1;
            }
        }
        row = Math.max(0, Math.min(row, rows - 1));
        col = Math.max(0, Math.min(col, columns - 1));
        changeSelection(row, col, false, extend);
    }

    private void keyPressed(KeyEvent e) {
        boolean shift = e.isShiftDown();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                move(-1, 0, false, shift);
                break;
            case KeyEvent.VK_DOWN:
                move(1, 0, false, shift);
                break;
            case KeyEvent.VK_LEFT:
                move(0, -1, false, shift);
                break;
            case KeyEvent.VK_RIGHT:
                move(0, 1, false, shift);
                break;
            case KeyEvent.VK_HOME:
                move(0, -getColumnCount(), false, shift);
                break;
            case KeyEvent.VK_END:
                move(0, getColumnCount(), false, shift);
                break;
            case KeyEvent.VK_PAGE_UP:
                move(-visibleRows(), 0, false, shift);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                move(visibleRows(), 0, false, shift);
                break;
            case KeyEvent.VK_ENTER:
                if (isEditing() && !getCellEditor().stopCellEditing()) {
                    break;
                }
                move(shift ? -1 : 1, 0, false, false);
                break;
            case KeyEvent.VK_TAB:
                if (isEditing() && !getCellEditor().stopCellEditing()) {
                    break;
                }
                move(0, shift ? -1 : 1, true, false);
                break;
            case KeyEvent.VK_ESCAPE:
                if (isEditing()) {
                    getCellEditor().cancelCellEditing();
                }
                break;
            case KeyEvent.VK_F2:
                editLead(null);
                break;
            default:
                return;
        }
        e.consume();
    }

    private void editLead(String typed) {
        int row = selectionModel.getLeadSelectionIndex();
        int col = columnModel.getSelectionModel().getLeadSelectionIndex();
        if (row < 0 || col < 0 || isEditing()) {
            return;
        }
        if (editCellAt(row, col, null) && editorComp != null) {
            startedBy(typed);
        }
    }

    private void keyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        if (ch == KeyEvent.CHAR_UNDEFINED || ch < ' ' || ch == 127 || isEditing() || e.isControlDown()
                || e.isMetaDown() || e.isAltDown()) {
            return;
        }
        editLead(String.valueOf(ch));
        if (isEditing()) {
            e.consume();
        }
    }

    /// The keys that end an edit, heard on the editor's component: the
    /// component has the focus while it edits.
    private final class EditorKeys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!isEditing()) {
                return;
            }
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_ESCAPE) {
                getCellEditor().cancelCellEditing();
                e.consume();
            } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_TAB) {
                if (getCellEditor().stopCellEditing()) {
                    if (code == KeyEvent.VK_TAB) {
                        move(0, e.isShiftDown() ? -1 : 1, true, false);
                    }
                }
                e.consume();
            }
        }
    }

    // ------------------------------------------------------------ Scrollable

    public void setPreferredScrollableViewportSize(Dimension size) {
        if (size == null) {
            throw new IllegalArgumentException("size must be non-null");
        }
        preferredViewportSize = size;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return preferredViewportSize;
    }

    /// A row, or the width of the column at the edge.
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return getRowHeight();
        }
        int col = getColumnModel().getColumnIndexAtX(direction < 0 ? Math.max(0, visibleRect.x - 1) : visibleRect.x);
        if (col < 0) {
            return 100;
        }
        return Math.max(1, getColumnModel().getColumn(col).getWidth());
    }

    /// The visible height less a row, or the visible width.
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int rh = getRowHeight();
            return Math.max(rh, visibleRect.height - rh);
        }
        return Math.max(1, visibleRect.width);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return autoResizeMode != AUTO_RESIZE_OFF;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        return getFillsViewportHeight() && parent instanceof JViewport
                && parent.getHeight() > getPreferredSize().height;
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",autoResizeMode=" + autoResizeMode + ",rowHeight=" + getRowHeight();
    }

    // ------------------------------------------------------------ defaults

    /// Numbers: the value's string, at the trailing edge.
    private static final class NumberRenderer extends DefaultTableCellRenderer {
        NumberRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    /// Icons: the icon, centered, and no text.
    private static final class IconRenderer extends DefaultTableCellRenderer {
        IconRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void setValue(Object value) {
            setIcon(value instanceof Icon ? (Icon) value : null);
            setText("");
        }
    }

    /// Booleans: a check box.
    private static final class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        BooleanRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
            Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
            if (fg != null && !fg.equals(isForegroundSet() ? getForeground() : null)) {
                setForeground(fg);
            }
            if (bg != null && !bg.equals(isBackgroundSet() ? getBackground() : null)) {
                setBackground(bg);
            }
            boolean on = value instanceof Boolean && ((Boolean) value).booleanValue();
            if (on != isSelected()) {
                setSelected(on);
            }
            return this;
        }

        @Override
        public void revalidate() {
        }

        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
        }

        @Override
        public void repaint() {
        }
    }

    /// The text editor of the default editors. Its value is the text, or
    /// for a column of a number class the number the text parses to; text
    /// that is no such number keeps the editor open.
    private static final class ValueEditor extends DefaultCellEditor {

        private Class<?> type;
        private Object parsed;

        ValueEditor(boolean number) {
            super(new JTextField());
            if (number && editorComponent instanceof JTextField) {
                ((JTextField) editorComponent).setHorizontalAlignment(SwingConstants.RIGHT);
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            type = table.getColumnClass(column);
            parsed = null;
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        private static Object parse(Class<?> type, String s) {
            if (type == null || type == Object.class || type == String.class) {
                return s;
            }
            String t = s.trim();
            if (type == Integer.class) {
                return Integer.valueOf(t);
            }
            if (type == Long.class) {
                return Long.valueOf(t);
            }
            if (type == Double.class || type == Number.class) {
                return Double.valueOf(t);
            }
            if (type == Float.class) {
                return Float.valueOf(t);
            }
            if (type == Short.class) {
                return Short.valueOf(t);
            }
            if (type == Byte.class) {
                return Byte.valueOf(t);
            }
            return s;
        }

        @Override
        public boolean stopCellEditing() {
            Object text = super.getCellEditorValue();
            String s = text == null ? "" : text.toString();
            if (s.length() == 0 && type != null && type != String.class && type != Object.class) {
                parsed = null;
                return super.stopCellEditing();
            }
            try {
                parsed = parse(type, s);
            } catch (NumberFormatException e) {
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            return parsed;
        }
    }
}
