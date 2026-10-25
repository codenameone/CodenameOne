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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.JViewport;
import com.codename1.desktopcompat.javax.swing.ListSelectionModel;
import com.codename1.desktopcompat.javax.swing.RowFilter;
import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.ScrollPaneConstants;
import com.codename1.desktopcompat.javax.swing.SortOrder;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.TableColumnModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TableModelEvent;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.javax.swing.table.TableColumnModel;
import com.codename1.desktopcompat.javax.swing.table.TableModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.CompoundHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.Highlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.event.TableColumnModelExtListener;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.CheckBoxProvider;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultTableRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValues;
import com.codename1.desktopcompat.org.jdesktop.swingx.rollover.RolloverProducer;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.SortController;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.SortUtils;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.StringValueRegistry;
import com.codename1.desktopcompat.org.jdesktop.swingx.sort.TableSortController;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.ColumnControlButton;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.ColumnFactory;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.TableColumnExt;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.TableColumnModelExt;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/// A table with highlighters, columns that can be hidden, sorting and
/// filtering switches of its own, packing of columns to their content
/// and a size in rows for its scroll pane.
///
/// ## Highlighters
///
/// Every cell's renderer component passes through the table's
/// highlighters, in the order they were added, and then through those of
/// the cell's column. The table's default renderers are SwingX renderers,
/// which set every visual property for every cell; a
/// `DefaultTableCellRenderer` remembers a color set on it, so the table
/// puts the colors it had first back before each cell.
///
/// ## Sorting
///
/// The table makes its own row sorter, a
/// [com.codename1.desktopcompat.org.jdesktop.swingx.sort.TableSortController],
/// and drives it: [#setSortable(boolean)], the sort order of a column by
/// view index or by identifier, [#setRowFilter(RowFilter)]. A column
/// model index is what the sorter is given, so sorting by a hidden
/// column's identifier works.
///
/// ## Columns
///
/// Columns are [TableColumnExt]s in a [DefaultTableColumnModelExt].
/// Hiding one removes it from the view: view indexes then count the
/// visible columns only and `convertColumnIndexToView` answers -1 for
/// it.
///
/// ## Rollover
///
/// With rollover enabled the cell under the pointer is kept in the
/// client property `swingx.rollover` for the rollover predicates. On a
/// touch screen that is the cell last touched.
///
/// ## Not here
///
/// The column control is a button with a popup menu; see
/// [ColumnControlButton] for where it can be shown. Searching, the
/// focus-lost editor hack, the look and feel hooks and
/// column sequences are absent.
public class JXTable extends JTable implements TableColumnModelExtListener {

    public static final String FOCUS_PREVIOUS_COMPONENT = "focusPreviousComponent";
    public static final String FOCUS_NEXT_COMPONENT = "focusNextComponent";
    public static final String HORIZONTALSCROLL_ACTION_COMMAND = "column.horizontalScroll";
    public static final String PACKALL_ACTION_COMMAND = "column.packAll";
    public static final String PACKSELECTED_ACTION_COMMAND = "column.packSelected";
    public static final String UIPREFIX = "JXTable.";
    public static final String MATCH_HIGHLIGHTER = "match.highlighter";
    public static final String USE_DTCR_COLORMEMORY_HACK = "useDTCRColorMemoryHack";

    protected CompoundHighlighter compoundHighlighter;
    protected ComponentAdapter dataAdapter;

    // None of these has an initializer: the superclass constructor sets
    // the model and the columns, which reaches methods overridden here
    // before an initializer would run and undo what they did.
    private ChangeListener cn1HighlighterListener;
    private StringValueRegistry cn1Registry;
    private ColumnFactory cn1ColumnFactory;
    private boolean cn1Ready;
    private boolean cn1Sortable;
    private boolean cn1AutoSorter;
    private boolean cn1SortsOnUpdates;
    private SortOrder[] cn1SortCycle;
    private RowFilter<?, ?> cn1RowFilter;
    private boolean cn1Editable;
    private boolean cn1Rollover;
    private RolloverProducer cn1RolloverProducer;
    private boolean cn1ColumnControlVisible;
    private JComponent cn1ColumnControl;
    private boolean cn1HorizontalScroll;
    private int cn1OldResizeMode;
    private int cn1VisibleRows;
    private int cn1VisibleColumns;
    private boolean cn1ViewportSizeSet;
    private List<Memory> cn1Memories;

    public JXTable() {
        super();
        cn1Init();
    }

    public JXTable(TableModel dm) {
        super(dm);
        cn1Init();
    }

    public JXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        cn1Init();
    }

    public JXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        cn1Init();
    }

    public JXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        cn1Init();
    }

    public JXTable(Vector<?> rowData, Vector<?> columnNames) {
        super(rowData, columnNames);
        cn1Init();
    }

    public JXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        cn1Init();
    }

    private void cn1Init() {
        cn1Ready = true;
        cn1Editable = true;
        cn1Sortable = true;
        cn1SortsOnUpdates = true;
        cn1VisibleRows = 20;
        cn1VisibleColumns = -1;
        cn1OldResizeMode = getAutoResizeMode();
        cn1UpdateRegistry();
        setAutoCreateRowSorter(true);
    }

    // ------------------------------------------------------------ rollover

    /// Switches tracking of the cell under the pointer on or off.
    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = cn1Rollover;
        if (old == rolloverEnabled) {
            return;
        }
        cn1Rollover = rolloverEnabled;
        if (rolloverEnabled) {
            cn1RolloverProducer = createRolloverProducer();
            cn1RolloverProducer.install(this);
        } else {
            if (cn1RolloverProducer != null) {
                cn1RolloverProducer.release(this);
                cn1RolloverProducer = null;
            }
            putClientProperty(RolloverProducer.ROLLOVER_KEY, null);
            repaint();
        }
        firePropertyChange("rolloverEnabled", old, rolloverEnabled);
    }

    public boolean isRolloverEnabled() {
        return cn1Rollover;
    }

    protected RolloverProducer createRolloverProducer() {
        return new RolloverProducer() {
            @Override
            protected void updateRolloverPoint(JComponent component, Point mousePoint) {
                int col = columnAtPoint(mousePoint);
                int row = rowAtPoint(mousePoint);
                if (col < 0 || row < 0) {
                    row = -1;
                    col = -1;
                }
                rollover.setLocation(col, row);
            }

            @Override
            protected void updateClientProperty(JComponent component, String property, boolean fireAlways) {
                Object before = component.getClientProperty(property);
                super.updateClientProperty(component, property, fireAlways);
                Object after = component.getClientProperty(property);
                if (before == null ? after != null : !before.equals(after)) {
                    component.repaint();
                }
            }

            @Override
            public void mouseExited(com.codename1.desktopcompat.java.awt.event.MouseEvent e) {
                super.mouseExited(e);
                repaint();
            }
        };
    }

    // ------------------------------------------------------------ column control

    public boolean isColumnControlVisible() {
        return cn1ColumnControlVisible;
    }

    /// Makes the column control the upper trailing corner of the scroll
    /// pane the table is in, or takes it out again. The layer's scroll
    /// pane records its corners and shows none: see
    /// [ColumnControlButton].
    public void setColumnControlVisible(boolean visible) {
        boolean old = cn1ColumnControlVisible;
        if (old == visible) {
            return;
        }
        cn1ColumnControlVisible = visible;
        cn1ConfigureColumnControl();
        firePropertyChange("columnControlVisible", old, visible);
    }

    private JScrollPane cn1ScrollPane() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            Container pane = parent.getParent();
            if (pane instanceof JScrollPane && ((JScrollPane) pane).getViewport() == parent) {
                return (JScrollPane) pane;
            }
        }
        return null;
    }

    private void cn1ConfigureColumnControl() {
        JScrollPane pane = cn1ScrollPane();
        if (pane == null) {
            return;
        }
        if (cn1ColumnControlVisible) {
            pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, getColumnControl());
        } else if (cn1ColumnControl != null
                && pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER) == cn1ColumnControl) {
            pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, null);
        }
    }

    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        if (cn1Ready) {
            cn1ConfigureColumnControl();
        }
    }

    /// The column control, made on first use.
    public JComponent getColumnControl() {
        if (cn1ColumnControl == null) {
            cn1ColumnControl = createDefaultColumnControl();
        }
        return cn1ColumnControl;
    }

    public void setColumnControl(JComponent columnControl) {
        JComponent old = cn1ColumnControl;
        JScrollPane pane = cn1ScrollPane();
        if (pane != null && old != null && pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER) == old) {
            pane.setCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER, null);
        }
        cn1ColumnControl = columnControl;
        cn1ConfigureColumnControl();
        firePropertyChange("columnControl", old, columnControl);
    }

    protected JComponent createDefaultColumnControl() {
        return new ColumnControlButton(this);
    }

    // ------------------------------------------------------------ packing

    /// Packs every column with the default margin.
    public void packAll() {
        packTable(-1);
    }

    /// Packs the selected column, if there is one.
    public void packSelected() {
        int selected = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (selected >= 0 && selected < getColumnCount()) {
            packColumn(selected, -1);
        }
    }

    public void packTable(int margin) {
        for (int c = 0; c < getColumnCount(); c++) {
            packColumn(c, margin);
        }
    }

    public void packColumn(int column, int margin) {
        packColumn(column, margin, -1);
    }

    /// Gives a column, by view index, the preferred width its header and
    /// cells need plus `margin` on both sides, at most `max` (-1: no
    /// limit).
    public void packColumn(int column, int margin, int max) {
        TableColumnExt ext = getColumnExt(column);
        if (ext == null) {
            return;
        }
        getColumnFactory().packColumn(this, ext, margin, max);
        resizeAndRepaint();
    }

    // ------------------------------------------------------------ horizontal scrolling

    /// With `true` the columns keep their preferred widths and the
    /// scroll pane scrolls them; with `false` the resize mode the table
    /// had before comes back.
    public void setHorizontalScrollEnabled(boolean enabled) {
        boolean old = isHorizontalScrollEnabled();
        if (enabled == old) {
            return;
        }
        if (enabled) {
            int mode = getAutoResizeMode();
            cn1HorizontalScroll = true;
            super.setAutoResizeMode(AUTO_RESIZE_OFF);
            cn1OldResizeMode = mode;
        } else {
            cn1HorizontalScroll = false;
            super.setAutoResizeMode(cn1OldResizeMode == AUTO_RESIZE_OFF ? AUTO_RESIZE_SUBSEQUENT_COLUMNS
                    : cn1OldResizeMode);
        }
        firePropertyChange("horizontalScrollEnabled", old, isHorizontalScrollEnabled());
    }

    public boolean isHorizontalScrollEnabled() {
        return cn1HorizontalScroll && getAutoResizeMode() == AUTO_RESIZE_OFF;
    }

    @Override
    public void setAutoResizeMode(int mode) {
        if (mode != AUTO_RESIZE_OFF) {
            cn1OldResizeMode = mode;
            cn1HorizontalScroll = false;
        }
        super.setAutoResizeMode(mode);
    }

    // ------------------------------------------------------------ model

    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        if (!cn1Ready) {
            return;
        }
        cn1UpdateRegistry();
        if (cn1AutoSorter) {
            setRowSorter(createDefaultRowSorter());
        } else {
            configureSorterProperties();
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (cn1Ready && isStructureChanged(e)) {
            cn1UpdateRegistry();
            configureSorterProperties();
        }
    }

    protected boolean isStructureChanged(TableModelEvent e) {
        return e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW;
    }

    protected boolean isDataChanged(TableModelEvent e) {
        if (e == null) {
            return false;
        }
        return e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == 0
                && e.getLastRow() == Integer.MAX_VALUE;
    }

    protected boolean isUpdate(TableModelEvent e) {
        if (isStructureChanged(e)) {
            return false;
        }
        return e.getType() == TableModelEvent.UPDATE && e.getLastRow() < Integer.MAX_VALUE;
    }

    // ------------------------------------------------------------ sorting

    /// Whether the table makes a sort controller of its own for every
    /// model it is given. On by default.
    @Override
    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
        boolean old = cn1AutoSorter;
        cn1AutoSorter = autoCreateRowSorter;
        if (autoCreateRowSorter) {
            setRowSorter(createDefaultRowSorter());
        }
        firePropertyChange("autoCreateRowSorter", old, autoCreateRowSorter);
    }

    @Override
    public boolean getAutoCreateRowSorter() {
        return cn1AutoSorter;
    }

    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        super.setRowSorter(sorter);
        if (cn1Ready) {
            configureSorterProperties();
        }
    }

    protected RowSorter<? extends TableModel> createDefaultRowSorter() {
        return new TableSortController<TableModel>(getModel());
    }

    /// Hands the table's sorting properties, and those of its columns,
    /// to a sorter that is a sort controller.
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void configureSorterProperties() {
        if (!getControlsSorterProperties()) {
            return;
        }
        SortController controller = getSortController();
        controller.setStringValueProvider(getStringValueRegistry());
        controller.setSortable(cn1Sortable);
        controller.setSortsOnUpdates(cn1SortsOnUpdates);
        if (cn1SortCycle != null) {
            controller.setSortOrderCycle(cn1SortCycle);
        }
        int modelColumns = getModel().getColumnCount();
        List<TableColumn> columns = getColumns(true);
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            int index = c.getModelIndex();
            if (c instanceof TableColumnExt && index >= 0 && index < modelColumns) {
                TableColumnExt ext = (TableColumnExt) c;
                controller.setSortable(index, ext.isSortable());
                controller.setComparator(index, ext.getComparator());
            }
        }
        controller.setRowFilter(cn1RowFilter);
    }

    /// Switches sorting by the user, and through the table's sort
    /// methods, on or off.
    public void setSortable(boolean sortable) {
        boolean old = cn1Sortable;
        cn1Sortable = sortable;
        if (getControlsSorterProperties()) {
            getSortController().setSortable(sortable);
        }
        firePropertyChange("sortable", old, sortable);
    }

    public boolean isSortable() {
        return cn1Sortable;
    }

    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        boolean old = cn1SortsOnUpdates;
        cn1SortsOnUpdates = sortsOnUpdates;
        if (getControlsSorterProperties()) {
            getSortController().setSortsOnUpdates(sortsOnUpdates);
        }
        firePropertyChange("sortsOnUpdates", old, sortsOnUpdates);
    }

    public boolean getSortsOnUpdates() {
        return cn1SortsOnUpdates;
    }

    /// Sets the orders a click on a header steps a column through.
    public void setSortOrderCycle(SortOrder... cycle) {
        SortOrder[] copy = new SortOrder[cycle == null ? 0 : cycle.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = cycle[i];
        }
        cn1SortCycle = copy;
        if (getControlsSorterProperties()) {
            getSortController().setSortOrderCycle(copy);
        }
    }

    public SortOrder[] getSortOrderCycle() {
        SortOrder[] from = cn1SortCycle;
        if (from == null) {
            return TableSortController.getDefaultSortOrderCycle();
        }
        SortOrder[] copy = new SortOrder[from.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = from[i];
        }
        return copy;
    }

    /// Shows only the rows the filter includes; `null` shows all. Needs
    /// a sort controller, which the table has unless it was taken away.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends TableModel> void setRowFilter(RowFilter<? super R, ? super Integer> filter) {
        cn1RowFilter = filter;
        if (hasSortController()) {
            SortController controller = getSortController();
            controller.setRowFilter(filter);
        }
    }

    public RowFilter<?, ?> getRowFilter() {
        return hasSortController() ? getSortController().getRowFilter() : cn1RowFilter;
    }

    /// Takes the sorting off every sortable column.
    public void resetSortOrder() {
        if (hasSortController()) {
            getSortController().resetSortOrders();
        }
    }

    /// Moves a column, by view index, to its next sort order.
    public void toggleSortOrder(int columnIndex) {
        if (hasSortController()) {
            getSortController().toggleSortOrder(convertColumnIndexToModel(columnIndex));
        }
    }

    public void setSortOrder(int columnIndex, SortOrder sortOrder) {
        if (hasSortController()) {
            getSortController().setSortOrder(convertColumnIndexToModel(columnIndex), sortOrder);
        }
    }

    public SortOrder getSortOrder(int columnIndex) {
        if (hasSortController()) {
            return getSortController().getSortOrder(convertColumnIndexToModel(columnIndex));
        }
        return SortOrder.UNSORTED;
    }

    /// Moves the column with the identifier, visible or hidden, to its
    /// next sort order.
    public void toggleSortOrder(Object identifier) {
        TableColumnExt column = getColumnExt(identifier);
        if (column != null && hasSortController()) {
            getSortController().toggleSortOrder(column.getModelIndex());
        }
    }

    public void setSortOrder(Object identifier, SortOrder sortOrder) {
        TableColumnExt column = getColumnExt(identifier);
        if (column != null && hasSortController()) {
            getSortController().setSortOrder(column.getModelIndex(), sortOrder);
        }
    }

    public SortOrder getSortOrder(Object identifier) {
        TableColumnExt column = getColumnExt(identifier);
        if (column != null && hasSortController()) {
            return getSortController().getSortOrder(column.getModelIndex());
        }
        return SortOrder.UNSORTED;
    }

    /// The row sorter when it is a sort controller, else `null`.
    @SuppressWarnings("unchecked")
    protected SortController<? extends TableModel> getSortController() {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter instanceof SortController) {
            return (SortController<? extends TableModel>) sorter;
        }
        return null;
    }

    protected boolean hasSortController() {
        return getRowSorter() instanceof SortController;
    }

    /// Whether the table writes its sorting properties into the sorter:
    /// when the sorter is a sort controller the table made itself.
    protected boolean getControlsSorterProperties() {
        return hasSortController() && getAutoCreateRowSorter();
    }

    /// The column the rows are sorted by first, visible or hidden, or
    /// `null`.
    public TableColumn getSortedColumn() {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter == null) {
            return null;
        }
        RowSorter.SortKey key = SortUtils.getFirstSortingKey(sorter.getSortKeys());
        if (key == null) {
            return null;
        }
        List<TableColumn> columns = getColumns(true);
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getModelIndex() == key.getColumn()) {
                return columns.get(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------ columns

    @Override
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModelExt();
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        super.columnAdded(e);
        if (cn1Ready) {
            cn1UpdateRegistry();
            configureSorterProperties();
        }
    }

    /// The visible column at a view index.
    public TableColumn getColumn(int viewColumnIndex) {
        return getColumnModel().getColumn(viewColumnIndex);
    }

    /// The visible columns in view order.
    public List<TableColumn> getColumns() {
        return getColumns(false);
    }

    public int getColumnMargin() {
        return getColumnModel().getColumnMargin();
    }

    public void setColumnMargin(int value) {
        getColumnModel().setColumnMargin(value);
    }

    /// The number of columns, hidden ones included when asked for.
    public int getColumnCount(boolean includeHidden) {
        TableColumnModel cm = getColumnModel();
        if (cm instanceof TableColumnModelExt) {
            return ((TableColumnModelExt) cm).getColumnCount(includeHidden);
        }
        return cm.getColumnCount();
    }

    /// The columns in order, hidden ones included when asked for.
    public List<TableColumn> getColumns(boolean includeHidden) {
        TableColumnModel cm = getColumnModel();
        if (cm instanceof TableColumnModelExt) {
            return ((TableColumnModelExt) cm).getColumns(includeHidden);
        }
        List<TableColumn> out = new ArrayList<TableColumn>();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            out.add(cm.getColumn(i));
        }
        return out;
    }

    /// The column with the identifier, visible or hidden, when it is a
    /// [TableColumnExt]; else `null`.
    public TableColumnExt getColumnExt(Object identifier) {
        if (identifier == null) {
            return null;
        }
        List<TableColumn> columns = getColumns(true);
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            if (c instanceof TableColumnExt && identifier.equals(c.getIdentifier())) {
                return (TableColumnExt) c;
            }
        }
        return null;
    }

    /// The visible column at a view index when it is a [TableColumnExt];
    /// else `null`.
    public TableColumnExt getColumnExt(int viewColumnIndex) {
        TableColumnModel cm = getColumnModel();
        if (viewColumnIndex < 0 || viewColumnIndex >= cm.getColumnCount()) {
            return null;
        }
        TableColumn c = cm.getColumn(viewColumnIndex);
        return c instanceof TableColumnExt ? (TableColumnExt) c : null;
    }

    /// A property of a column changed.
    @Override
    public void columnPropertyChange(PropertyChangeEvent event) {
        String name = event.getPropertyName();
        if ("editable".equals(name)) {
            if (isEditing()) {
                removeEditor();
            }
        } else if ("sortable".equals(name) || "comparator".equals(name)) {
            configureSorterProperties();
        } else if (TableColumn.CELL_RENDERER_PROPERTY.equals(name)) {
            cn1UpdateRegistry();
        } else if (TableColumn.HEADER_VALUE_PROPERTY.equals(name) && getTableHeader() != null) {
            getTableHeader().repaint();
        }
        repaint();
    }

    /// Replaces the columns, hidden ones too, by one per column of the
    /// model, made by the column factory.
    @Override
    public final void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m == null) {
            return;
        }
        TableColumnModel cm = getColumnModel();
        List<TableColumn> all = getColumns(true);
        for (int i = 0; i < all.size(); i++) {
            cm.removeColumn(all.get(i));
        }
        ColumnFactory factory = getColumnFactory();
        for (int i = 0; i < m.getColumnCount(); i++) {
            TableColumnExt column = factory.createAndConfigureTableColumn(m, i);
            if (column != null) {
                cm.addColumn(column);
            }
        }
    }

    /// The table's own column factory, or the shared one.
    public ColumnFactory getColumnFactory() {
        return cn1ColumnFactory != null ? cn1ColumnFactory : ColumnFactory.getInstance();
    }

    /// Sets the factory that makes this table's columns; `null` goes
    /// back to the shared one.
    public void setColumnFactory(ColumnFactory columnFactory) {
        ColumnFactory old = getColumnFactory();
        cn1ColumnFactory = columnFactory;
        firePropertyChange("columnFactory", old, getColumnFactory());
    }

    // ------------------------------------------------------------ viewport size

    public int getVisibleRowCount() {
        return cn1VisibleRows;
    }

    /// Sets how many rows the table asks its scroll pane to show.
    public void setVisibleRowCount(int visibleRowCount) {
        if (visibleRowCount < 0) {
            throw new IllegalArgumentException("visible row count must not be negative " + visibleRowCount);
        }
        int old = cn1VisibleRows;
        if (old == visibleRowCount) {
            return;
        }
        cn1VisibleRows = visibleRowCount;
        revalidate();
        firePropertyChange("visibleRowCount", old, visibleRowCount);
    }

    public int getVisibleColumnCount() {
        return cn1VisibleColumns;
    }

    /// Sets how many columns the table asks its scroll pane to show; a
    /// negative count is all of them.
    public void setVisibleColumnCount(int visibleColumnCount) {
        int old = cn1VisibleColumns;
        if (old == visibleColumnCount) {
            return;
        }
        cn1VisibleColumns = visibleColumnCount;
        revalidate();
        firePropertyChange("visibleColumnCount", old, visibleColumnCount);
    }

    @Override
    public void setPreferredScrollableViewportSize(Dimension size) {
        cn1ViewportSizeSet = size != null;
        if (size != null) {
            super.setPreferredScrollableViewportSize(size);
        }
    }

    /// The size set, or else the visible rows by the visible columns.
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        if (cn1ViewportSizeSet || !cn1Ready) {
            return super.getPreferredScrollableViewportSize();
        }
        int w = getColumnFactory().getPreferredScrollableViewportWidth(this);
        return new Dimension(w, cn1VisibleRows * getRowHeight());
    }

    public void scrollRowToVisible(int row) {
        Rectangle cell = getCellRect(row, 0, false);
        Rectangle visible = getVisibleRect();
        cell.x = visible.x;
        cell.width = visible.width;
        scrollRectToVisible(cell);
    }

    public void scrollColumnToVisible(int column) {
        Rectangle cell = getCellRect(0, column, false);
        Rectangle visible = getVisibleRect();
        cell.y = visible.y;
        cell.height = visible.height;
        scrollRectToVisible(cell);
    }

    public void scrollCellToVisible(int row, int column) {
        scrollRectToVisible(getCellRect(row, column, false));
    }

    // ------------------------------------------------------------ highlighters

    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new TableAdapter(this);
        }
        return dataAdapter;
    }

    /// The adapter, pointed at a cell in view coordinates.
    protected ComponentAdapter getComponentAdapter(int row, int column) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.row = row;
        adapter.column = column;
        return adapter;
    }

    public void setHighlighters(Highlighter... highlighters) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().setHighlighters(highlighters);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter().getHighlighters();
    }

    public void addHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().addHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    public void removeHighlighter(Highlighter highlighter) {
        Highlighter[] old = getHighlighters();
        getCompoundHighlighter().removeHighlighter(highlighter);
        firePropertyChange("highlighters", old, getHighlighters());
    }

    protected CompoundHighlighter getCompoundHighlighter() {
        if (compoundHighlighter == null) {
            compoundHighlighter = new CompoundHighlighter();
            compoundHighlighter.addChangeListener(getHighlighterChangeListener());
        }
        return compoundHighlighter;
    }

    protected ChangeListener getHighlighterChangeListener() {
        if (cn1HighlighterListener == null) {
            cn1HighlighterListener = createHighlighterChangeListener();
        }
        return cn1HighlighterListener;
    }

    /// A listener that repaints the table when a highlighter changes.
    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        };
    }

    // ------------------------------------------------------------ strings

    protected StringValueRegistry getStringValueRegistry() {
        if (cn1Registry == null) {
            cn1Registry = createDefaultStringValueRegistry();
        }
        return cn1Registry;
    }

    protected StringValueRegistry createDefaultStringValueRegistry() {
        return new StringValueRegistry();
    }

    /// Tells the registry the classes of the model's columns and the
    /// converters of the columns whose renderer is one.
    private void cn1UpdateRegistry() {
        StringValueRegistry registry = getStringValueRegistry();
        registry.clearColumnStringValues();
        registry.setColumnClasses(null);
        TableModel m = getModel();
        if (m == null) {
            return;
        }
        int n = m.getColumnCount();
        for (int i = 0; i < n; i++) {
            registry.setColumnClass(m.getColumnClass(i), i);
        }
        List<TableColumn> columns = getColumns(true);
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            TableCellRenderer r = c.getCellRenderer();
            if (r instanceof StringValue && c.getModelIndex() >= 0 && c.getModelIndex() < n) {
                registry.setStringValue((StringValue) r, c.getModelIndex());
            }
        }
    }

    @Override
    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        super.setDefaultRenderer(columnClass, renderer);
        getStringValueRegistry().setStringValue(renderer instanceof StringValue ? (StringValue) renderer : null,
                columnClass);
    }

    /// The text the table shows for a cell, in view coordinates: what
    /// the cell's renderer says when it is a `StringValue`, else the
    /// value's `toString`.
    public String getStringAt(int row, int column) {
        TableCellRenderer renderer = getCellRenderer(row, column);
        Object value = getValueAt(row, column);
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(value);
        }
        return StringValues.TO_STRING.getString(value);
    }

    // ------------------------------------------------------------ rendering

    private static final class Memory {
        DefaultTableCellRenderer renderer;
        Color background;
        Color foreground;
        boolean dirty;
    }

    private static boolean cn1Same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private Memory cn1Memory(DefaultTableCellRenderer renderer, int row, int column, boolean make) {
        if (cn1Memories == null) {
            cn1Memories = new ArrayList<Memory>();
        }
        for (int i = 0; i < cn1Memories.size(); i++) {
            if (cn1Memories.get(i).renderer == renderer) {
                return cn1Memories.get(i);
            }
        }
        if (!make) {
            return null;
        }
        // First sight: ask for an unselected cell, which shows the
        // colors set on the renderer, or the table's when none was.
        renderer.getTableCellRendererComponent(this, getValueAt(row, column), false, false, row, column);
        Memory m = new Memory();
        m.renderer = renderer;
        Color bg = renderer.getBackground();
        Color fg = renderer.getForeground();
        m.background = cn1Same(bg, getBackground()) ? null : bg;
        m.foreground = cn1Same(fg, getForeground()) ? null : fg;
        if (cn1Memories.size() >= 32) {
            cn1Memories.remove(0);
        }
        cn1Memories.add(m);
        return m;
    }

    /// Asks the renderer for the cell's component and passes that
    /// through the table's highlighters and then the column's.
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Memory memory = null;
        if (renderer instanceof DefaultTableCellRenderer) {
            DefaultTableCellRenderer dtcr = (DefaultTableCellRenderer) renderer;
            memory = cn1Memory(dtcr, row, column, true);
            if (memory.dirty) {
                resetDefaultTableCellRendererColors(dtcr, row, column);
            }
        }
        Component stamp = super.prepareRenderer(renderer, row, column);
        if (stamp == null) {
            return null;
        }
        Color bg = stamp.getBackground();
        Color fg = stamp.getForeground();
        Component before = stamp;
        ComponentAdapter adapter = getComponentAdapter(row, column);
        if (compoundHighlighter != null) {
            stamp = compoundHighlighter.highlight(stamp, adapter);
        }
        TableColumnExt ext = getColumnExt(column);
        if (ext != null) {
            stamp = ext.cn1Highlight(stamp, adapter);
        }
        if (memory != null && before == memory.renderer
                && (!cn1Same(bg, before.getBackground()) || !cn1Same(fg, before.getForeground()))) {
            memory.dirty = true;
        }
        return stamp;
    }

    /// The highlighted component of a cell with the cell's own renderer.
    public Component prepareRenderer(int row, int column) {
        return prepareRenderer(getCellRenderer(row, column), row, column);
    }

    /// Gives a `DefaultTableCellRenderer` back the colors it had before
    /// a highlighter changed them, which it would otherwise use for
    /// every unselected cell from then on.
    protected void resetDefaultTableCellRendererColors(Component renderer, int row, int column) {
        if (!(renderer instanceof DefaultTableCellRenderer)) {
            return;
        }
        Memory memory = cn1Memory((DefaultTableCellRenderer) renderer, row, column, false);
        if (memory != null) {
            renderer.setBackground(memory.background);
            renderer.setForeground(memory.foreground);
            memory.dirty = false;
        }
    }

    /// The SwingX renderers: text for objects, numbers at the trailing
    /// edge, dates in the device's format and a check box for booleans.
    /// Icons keep the renderer the plain table has.
    @Override
    protected void createDefaultRenderers() {
        super.createDefaultRenderers();
        setDefaultRenderer(Object.class, new DefaultTableRenderer());
        setDefaultRenderer(Number.class, new DefaultTableRenderer(StringValues.NUMBER_TO_STRING,
                SwingConstants.RIGHT));
        setDefaultRenderer(Date.class, new DefaultTableRenderer(StringValues.DATE_TO_STRING));
        setDefaultRenderer(Boolean.class, new DefaultTableRenderer(new CheckBoxProvider()));
    }

    // ------------------------------------------------------------ editing

    public boolean isEditable() {
        return cn1Editable;
    }

    /// Switches editing of the whole table on or off; a cell is editable
    /// when the table, its column and the model all say so.
    public void setEditable(boolean editable) {
        boolean old = cn1Editable;
        cn1Editable = editable;
        if (!editable && isEditing()) {
            removeEditor();
        }
        firePropertyChange("editable", old, editable);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (!cn1Editable) {
            return false;
        }
        TableColumnExt ext = getColumnExt(column);
        if (ext != null && !ext.isEditable()) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    /// Shows or hides the horizontal and the vertical grid lines.
    public void setShowGrid(boolean showHorizontalLines, boolean showVerticalLines) {
        setShowHorizontalLines(showHorizontalLines);
        setShowVerticalLines(showVerticalLines);
    }

    // ------------------------------------------------------------ adapter

    private static final class TableAdapter extends ComponentAdapter {

        private final JXTable table;

        TableAdapter(JXTable table) {
            super(table);
            this.table = table;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return table.getModel().getColumnName(columnIndex);
        }

        private TableColumn byModelIndex(int columnIndex) {
            List<TableColumn> columns = table.getColumns(true);
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).getModelIndex() == columnIndex) {
                    return columns.get(i);
                }
            }
            return null;
        }

        @Override
        public Object getColumnIdentifierAt(int columnIndex) {
            if (columnIndex < 0 || columnIndex >= getColumnCount()) {
                throw new ArrayIndexOutOfBoundsException("invalid column index: " + columnIndex);
            }
            TableColumn c = byModelIndex(columnIndex);
            return c != null ? c.getIdentifier() : null;
        }

        @Override
        public int getColumnIndex(Object identifier) {
            TableColumnExt c = table.getColumnExt(identifier);
            return c != null ? c.getModelIndex() : -1;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return table.getModel().getColumnClass(column);
        }

        @Override
        public int getColumnCount() {
            return table.getModel().getColumnCount();
        }

        @Override
        public int getRowCount() {
            return table.getModel().getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return table.getModel().getValueAt(row, column);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            int view = table.convertColumnIndexToView(column);
            if (view >= 0) {
                return table.isCellEditable(row, view);
            }
            return table.getModel().isCellEditable(table.convertRowIndexToModel(row), column);
        }

        @Override
        public String getStringAt(int row, int column) {
            return table.getStringValueRegistry().getStringValue(row, column).getString(getValueAt(row, column));
        }

        @Override
        public Rectangle getCellBounds() {
            return table.getCellRect(row, column, false);
        }

        @Override
        public boolean hasFocus() {
            return table.isFocusOwner() && table.getSelectionModel().getLeadSelectionIndex() == row
                    && table.getColumnModel().getSelectionModel().getLeadSelectionIndex() == column;
        }

        @Override
        public boolean isSelected() {
            return table.isCellSelected(row, column);
        }

        @Override
        public boolean isEditable() {
            return table.isCellEditable(row, column);
        }

        @Override
        public int convertColumnIndexToView(int columnModelIndex) {
            return table.convertColumnIndexToView(columnModelIndex);
        }

        @Override
        public int convertColumnIndexToModel(int columnViewIndex) {
            return table.convertColumnIndexToModel(columnViewIndex);
        }

        @Override
        public int convertRowIndexToView(int rowModelIndex) {
            return table.convertRowIndexToView(rowModelIndex);
        }

        @Override
        public int convertRowIndexToModel(int rowViewIndex) {
            return table.convertRowIndexToModel(rowViewIndex);
        }
    }
}
