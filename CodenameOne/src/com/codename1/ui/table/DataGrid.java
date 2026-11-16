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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.ScrollSurface;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

/// A grid of data for large sets: a `TableModel` or a tree of rows, with
/// sorting, filtering, grouping, frozen columns, resizable and movable
/// columns, selection, keyboard navigation and editing in place.
///
/// The grid paints its cells itself, like the `com.codename1.ui.scheduler`
/// views. There is no component per cell or per row: only the rows on screen
/// are painted, sorting and filtering move row numbers in an array and never
/// the data, and opening a node of a tree costs what it shows. A million rows
/// scroll as one hundred do. `Table`, which lays out a component per cell,
/// is the class for a small form laid out as a table.
///
/// ```java
/// DataGrid grid = new DataGrid(model);
/// grid.getColumn(0).setFlex(1);
/// grid.getColumn(2).setKind(GridColumn.Kind.NUMBER).setAggregate(GridAggregate.SUM);
/// grid.setFrozenColumns(1);
/// grid.sortBy(grid.getColumn(2), false);
/// form.add(BorderLayout.CENTER, grid);
/// ```
///
/// #### Rows
///
/// A row number of the grid is a *view row*: an index into what is shown
/// after filtering, sorting, grouping and the opening of nodes. It is not a
/// row of the model; `getModelRow(int)` and `getNode(int)` translate it.
///
/// #### Trees
///
/// Built over a `TreeTableModel`, the grid is a tree table: the tree column
/// (`setTreeColumn`, the first visible one by default) shows each node
/// indented under its parent with an arrow that opens it. Children are asked
/// of the model when a node is first opened. Sorting orders siblings.
///
/// #### Editing
///
/// A cell is edited when its column and its model both allow it. A boolean
/// toggles with a tap. Any other value is edited by its column's
/// `GridCellEditor`, which supplies a component the grid lays over the cell
/// while it is edited and removes afterwards: a text field, or a picker for
/// a date or a choice.
///
/// #### Styles
///
/// `DataGrid` (the background), `DataGridHeader`, `DataGridRow` and its
/// selected state, `DataGridRowAlt` (every other row), `DataGridGroupRow`,
/// `DataGridLine` (the foreground is the colour of the grid lines),
/// `DataGridCheck` and `DataGridEditor`. Only colours, fonts and paddings are
/// read; nothing has a border.
public class DataGrid extends ScrollSurface {

    /// What a tap selects.
    public enum SelectionMode {
        /// Nothing is selected; the keyboard still moves between cells.
        NONE,
        /// One row.
        SINGLE_ROW,
        /// Any rows: Shift extends from the last row chosen, Control or
        /// Command adds and removes one, and a mouse drag selects a range.
        MULTIPLE_ROWS,
        /// One cell, outlined, in a row that is shown selected.
        SINGLE_CELL
    }

    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_RESIZE = 1;
    private static final int GESTURE_MOVE = 2;
    private static final int GESTURE_SELECT = 3;

    private GridRows rows;
    private FlatRows flat;
    private TreeRows tree;
    private boolean listening;
    private final DataChangedListener modelListener = new DataChangedListener() {
        @Override
        public void dataChanged(int row, int column) {
            modelChanged(row, column);
        }
    };

    private final ArrayList<GridColumn> columns = new ArrayList<GridColumn>();
    private GridColumn treeColumn;
    private int frozen;
    private final ArrayList<GridColumn> sortColumns = new ArrayList<GridColumn>();
    private final ArrayList<Boolean> sortAscending = new ArrayList<Boolean>();

    private SelectionMode selectionMode = SelectionMode.MULTIPLE_ROWS;
    private final GridSelection selection = new GridSelection();
    private int lead = -1;
    private int leadColumn;
    private int anchor = -1;

    private float rowHeightMM;
    private boolean headerVisible = true;
    private boolean striped = true;
    private boolean horizontalLines = true;
    private boolean verticalLines = true;
    private String emptyText = "";
    private GridCellRenderer defaultRenderer = new DefaultGridCellRenderer();
    private GridCellEditor defaultEditor = new DefaultGridCellEditor();

    // layout
    private boolean dirty = true;
    private int laidW = -1;
    private int laidH = -1;
    private int laidGen = -1;
    private GridColumn[] vis = new GridColumn[0];
    private int[] colX = new int[0];
    private int[] colW = new int[0];
    private int frozenCount;
    private int frozenW;
    private int scrollW;
    private int rowH = 1;
    private int headerH;
    private int indentW;
    private int arrowW;

    // gestures
    private int gesture;
    private int dragColumn = -1;
    private int dragStartX;
    private int dragStartW;
    private int dragX;
    private int moveTarget = -1;

    // editing
    private int editRow = -1;
    private GridColumn editColumn;
    private Component editor;
    private GridCellEditor editorSource;
    private Container editorHolder;
    private Object editKey;
    private boolean committing;
    // True while the selection is what selectAll() made it: only then does a
    // rebuild select every row again, however many there are now.
    private boolean selectedAll;

    private final GridCell cell = new GridCell();
    private final EventDispatcher selectionListeners = new EventDispatcher();
    private final EventDispatcher actionListeners = new EventDispatcher();

    /// A grid with no data.
    public DataGrid() {
        this(new DefaultTableModel(new String[0], new Object[0][]));
    }

    /// A grid over the rows of a table model, with a column for each of its
    /// columns.
    public DataGrid(TableModel model) {
        setUIID("DataGrid");
        setModel(model);
    }

    /// A tree grid over a tree table model, with a column for each of its
    /// columns.
    public DataGrid(TreeTableModel model) {
        setUIID("DataGrid");
        setModel(model);
    }

    // ------------------------------------------------------------- models

    /// Shows the rows of a table model, with a column for each of its
    /// columns. Sorting, filtering and grouping are kept when their columns
    /// still exist.
    public final void setModel(TableModel model) {
        cancelEdit();
        unlisten();
        flat = new FlatRows(model);
        tree = null;
        rows = flat;
        resetColumns(model.getColumnCount());
        listen();
        rebuildKeepingNothing();
    }

    /// Shows the nodes of a tree table model, with a column for each of its
    /// columns. The roots are shown closed.
    public final void setModel(TreeTableModel model) {
        cancelEdit();
        unlisten();
        flat = null;
        tree = new TreeRows(model);
        rows = tree;
        resetColumns(model.getColumnCount());
        rebuildKeepingNothing();
    }

    /// The table model shown, or null for a tree grid.
    public TableModel getTableModel() {
        return flat == null ? null : flat.model();
    }

    /// The tree model shown, or null for a flat grid.
    public TreeTableModel getTreeModel() {
        return tree == null ? null : tree.model();
    }

    /// Whether this is a tree grid.
    public boolean isTree() {
        return tree != null;
    }

    private void resetColumns(int count) {
        columns.clear();
        sortColumns.clear();
        sortAscending.clear();
        treeColumn = null;
        for (int i = 0; i < count; i++) {
            String name = rows.columnName(i);
            columns.add(new GridColumn(i, name == null ? "" : name));
        }
        if (flat != null) {
            flat.setGroupColumn(null);
        }
        pushColumns();
    }

    private void pushColumns() {
        GridColumn[] all = new GridColumn[columns.size()];
        columns.toArray(all);
        if (flat != null) {
            flat.setColumns(all);
        }
        GridColumn[] sc = new GridColumn[sortColumns.size()];
        boolean[] sa = new boolean[sc.length];
        for (int i = 0; i < sc.length; i++) {
            sc[i] = sortColumns.get(i);
            sa[i] = sortAscending.get(i).booleanValue();
        }
        rows.setSort(sc, sa);
        dirty = true;
    }

    private void listen() {
        if (!listening && flat != null && isInitialized()) {
            flat.model().addDataChangeListener(modelListener);
            listening = true;
        }
    }

    private void unlisten() {
        if (listening && flat != null) {
            flat.model().removeDataChangeListener(modelListener);
        }
        listening = false;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        boolean was = listening;
        listen();
        if (!was && listening) {
            // It may have changed while nobody listened.
            refresh();
        }
    }

    @Override
    protected void deinitialize() {
        commitEdit();
        unlisten();
        super.deinitialize();
    }

    // A TableModel fires (row, column) for a changed cell. It has no event
    // for an inserted or removed row: DefaultTableModel fires the cells of an
    // inserted row, and Integer.MIN_VALUE for a removed one. Selection and
    // edits remember rows by model index, so a change in the row count moves
    // them: a cell event that comes with more rows is taken as rows inserted
    // at that row, and a row count that shrank with no position drops the
    // selection rather than leave it on whatever record slid into its rows.
    private void modelChanged(int row, int column) {
        if (flat == null) {
            refresh();
            return;
        }
        if (row == Integer.MIN_VALUE) {
            // A removal, or "everything changed": no position to follow.
            // Events posted from another thread may also arrive after an
            // insert has evened out the row count.
            rebuildKeepingNothing();
            return;
        }
        int delta = flat.model().getRowCount() - flat.modelRowsBuilt();
        if (delta == 0) {
            if (row >= 0 && !dependsOnValues(column)) {
                // A value changed where nothing depends on its order.
                repaint();
                return;
            }
            refresh();
            return;
        }
        dropEdit();
        if (delta < 0 && (row < 0 || row >= flat.modelRowsBuilt())) {
            rebuildKeepingNothing();
            return;
        }
        Object[] keys = selectionKeys();
        Object leadKey = lead >= 0 && lead < rows.size() ? rows.key(lead) : null;
        boolean all = selectedAll && !selection.isEmpty() && selection.size() == rows.size();
        int at = Math.max(0, row);
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                keys[i] = shiftKey(keys[i], at, delta);
            }
        }
        leadKey = shiftKey(leadKey, at, delta);
        Object anchorKey = shiftKey(anchor >= 0 && anchor < rows.size() ? rows.key(anchor) : null, at, delta);
        rows.rebuild();
        restoreSelection(keys, leadKey, all);
        anchor = anchorKey == null ? lead : rows.find(anchorKey);
    }

    // Whether a change to a value of the model column can move rows: it is
    // sorted or grouped by, or a filter may read it.
    private boolean dependsOnValues(int modelColumn) {
        if (rows.filter != null) {
            return true;
        }
        GridColumn g = flat.getGroupColumn();
        if (g != null && g.getModelColumn() == modelColumn) {
            return true;
        }
        for (GridColumn c : sortColumns) {
            if (c.getModelColumn() == modelColumn) {
                return true;
            }
        }
        return false;
    }

    // A model row index key after `delta` rows were inserted at `at`, or
    // removed from `at` on when negative; null for a removed row. Group keys
    // follow their values and are left alone.
    private static Object shiftKey(Object key, int at, int delta) {
        if (!(key instanceof Integer)) {
            return key;
        }
        int v = ((Integer) key).intValue();
        if (v < at) {
            return key;
        }
        if (delta < 0 && v < at - delta) {
            return null;
        }
        return Integer.valueOf(v + delta);
    }

    // Closes an open editor without writing it: the row it edits may no
    // longer be the row it is over, or exist at all.
    private void dropEdit() {
        if (editor != null && !committing) {
            committing = true;
            try {
                closeEditor();
            } finally {
                committing = false;
            }
        }
    }

    /// Reads the model again: filters, sorts and groups its rows anew and
    /// keeps the selection on the same rows where they are still shown.
    public void refresh() {
        Object[] keys = selectionKeys();
        Object leadKey = lead >= 0 && lead < rows.size() ? rows.key(lead) : null;
        if (flat != null && flat.model().getRowCount() != flat.modelRowsBuilt()) {
            // Rows came or went while no one was listening: where is unknown.
            rebuildKeepingNothing();
            return;
        }
        boolean all = selectedAll && !selection.isEmpty() && selection.size() == rows.size();
        dropEdit();
        if (tree != null) {
            tree.forget(null);
        } else {
            rows.rebuild();
        }
        restoreSelection(keys, leadKey, all);
    }

    /// Asks the tree model again for the children of one node, and shows
    /// them. Use it after the children of a node changed; `refresh()` reads
    /// the whole tree again.
    public void refreshNode(Object node) {
        if (tree == null) {
            return;
        }
        Object[] keys = selectionKeys();
        Object leadKey = lead >= 0 && lead < rows.size() ? rows.key(lead) : null;
        dropEdit();
        tree.forget(node);
        restoreSelection(keys, leadKey, false);
    }

    private void rebuildKeepingNothing() {
        dropEdit();
        selectedAll = false;
        selection.clear();
        lead = -1;
        anchor = -1;
        rows.rebuild();
        dirty = true;
        repaint();
    }

    // Keys of the selected rows, or null when there are too many to be worth
    // remembering one by one.
    private Object[] selectionKeys() {
        int n = selection.size();
        if (n == 0 || n > 100000 || (selectedAll && n == rows.size())) {
            return null;
        }
        int[] sel = selection.toArray();
        Object[] keys = new Object[sel.length];
        for (int i = 0; i < sel.length; i++) {
            keys[i] = sel[i] < rows.size() ? rows.key(sel[i]) : null;
        }
        return keys;
    }

    private void restoreSelection(Object[] keys, Object leadKey, boolean all) {
        int oldLead = lead;
        selection.clear();
        selectedAll = all;
        if (all && rows.size() > 0) {
            selection.set(0, rows.size() - 1);
        } else if (keys != null) {
            for (Object key : keys) {
                if (key != null) {
                    int r = rows.find(key);
                    if (r >= 0) {
                        selection.add(r, r);
                    }
                }
            }
        }
        lead = leadKey == null ? -1 : rows.find(leadKey);
        if (anchor >= rows.size()) {
            anchor = lead;
        }
        dirty = true;
        repaint();
        if (oldLead != lead) {
            selectionListeners.fireSelectionEvent(oldLead, lead);
        }
    }

    // ------------------------------------------------------------ columns

    /// The columns, in the order they are shown, hidden ones included.
    public List<GridColumn> getColumns() {
        return new ArrayList<GridColumn>(columns);
    }

    /// How many columns there are, hidden ones included.
    public int getColumnCount() {
        return columns.size();
    }

    /// The column at a place in the order, hidden ones counted.
    public GridColumn getColumn(int index) {
        return columns.get(index);
    }

    /// The column showing a column of the model, or null.
    public GridColumn getColumnForModel(int modelColumn) {
        for (GridColumn col : columns) {
            if (col.getModelColumn() == modelColumn) {
                return col;
            }
        }
        return null;
    }

    /// Replaces the columns. A model column with no `GridColumn` is not
    /// shown, and one may be shown twice.
    public void setColumns(GridColumn... cols) {
        cancelEdit();
        columns.clear();
        for (GridColumn gc : cols) {
            columns.add(gc);
        }
        dropMissing();
        pushColumns();
        rows.rebuild();
        repaint();
    }

    /// Adds a column at the end.
    public void addColumn(GridColumn c) {
        columns.add(c);
        pushColumns();
        repaint();
    }

    /// Removes a column.
    public void removeColumn(GridColumn c) {
        cancelEdit();
        columns.remove(c);
        dropMissing();
        pushColumns();
        rows.rebuild();
        repaint();
    }

    /// Moves the column at `from` in the order to `to`, hidden ones counted.
    public void moveColumn(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= columns.size() || to >= columns.size()) {
            return;
        }
        cancelEdit();
        GridColumn c = columns.remove(from);
        columns.add(to, c);
        pushColumns();
        repaint();
    }

    private void dropMissing() {
        for (int i = sortColumns.size() - 1; i >= 0; i--) {
            if (!columns.contains(sortColumns.get(i))) {
                sortColumns.remove(i);
                sortAscending.remove(i);
            }
        }
        if (flat != null && flat.getGroupColumn() != null && !columns.contains(flat.getGroupColumn())) {
            flat.setGroupColumn(null);
        }
        if (treeColumn != null && !columns.contains(treeColumn)) {
            treeColumn = null;
        }
    }

    /// Asks the grid to lay its columns out again, after a column's width,
    /// title or visibility changed.
    public void refreshColumns() {
        for (GridColumn col : columns) {
            col.width(-1);
        }
        pushColumns();
        repaint();
    }

    /// How many of the first visible columns stay in place while the rest
    /// scroll sideways.
    public int getFrozenColumns() {
        return frozen;
    }

    /// Sets how many of the first visible columns stay in place while the
    /// rest scroll sideways; 0 by default.
    public void setFrozenColumns(int count) {
        frozen = Math.max(0, count);
        dirty = true;
        repaint();
    }

    /// The column a tree grid draws the tree in, or null for the first
    /// visible one.
    public GridColumn getTreeColumn() {
        return treeColumn;
    }

    /// Sets the column a tree grid draws the tree in; null, the default, is
    /// the first visible one.
    public void setTreeColumn(GridColumn c) {
        treeColumn = c;
        dirty = true;
        repaint();
    }

    // ---------------------------------------------------- sort and filter

    /// Sorts by one column alone.
    public void sortBy(GridColumn c, boolean ascending) {
        sortColumns.clear();
        sortAscending.clear();
        if (c != null) {
            sortColumns.add(c);
            sortAscending.add(Boolean.valueOf(ascending));
        }
        resort();
    }

    /// Adds a column to sort by after the ones already sorted by, or changes
    /// its direction when it is one of them.
    public void addSort(GridColumn c, boolean ascending) {
        int i = sortColumns.indexOf(c);
        if (i >= 0) {
            sortAscending.set(i, Boolean.valueOf(ascending));
        } else {
            sortColumns.add(c);
            sortAscending.add(Boolean.valueOf(ascending));
        }
        resort();
    }

    /// Stops sorting: rows are shown in the model's order.
    public void clearSort() {
        sortColumns.clear();
        sortAscending.clear();
        resort();
    }

    /// The columns sorted by, most significant first.
    public List<GridColumn> getSortColumns() {
        return new ArrayList<GridColumn>(sortColumns);
    }

    /// 1 when sorted by `c` ascending, -1 descending, 0 when not sorted by it.
    public int getSortDirection(GridColumn c) {
        int i = sortColumns.indexOf(c);
        if (i < 0) {
            return 0;
        }
        return sortAscending.get(i).booleanValue() ? 1 : -1;
    }

    private void resort() {
        pushColumns();
        refreshRows();
    }

    // Rebuilds the view rows from what is already loaded.
    private void refreshRows() {
        Object[] keys = selectionKeys();
        Object leadKey = lead >= 0 && lead < rows.size() ? rows.key(lead) : null;
        boolean all = selectedAll && !selection.isEmpty() && selection.size() == rows.size();
        dropEdit();
        rows.rebuild();
        restoreSelection(keys, leadKey, all);
    }

    /// The filter of the rows, or null.
    public GridFilter getFilter() {
        return rows.filter;
    }

    /// Shows only the rows a filter accepts; null shows them all.
    public void setFilter(GridFilter filter) {
        cancelEdit();
        rows.filter = filter;
        refreshRows();
    }

    /// The column the rows are grouped by, or null.
    public GridColumn getGroupColumn() {
        return flat == null ? null : flat.getGroupColumn();
    }

    /// Groups the rows by the values of a column, each group under a row of
    /// its own that opens and closes it; null stops grouping. A tree grid is
    /// never grouped.
    ///
    /// Groups are ordered by their value, in the column's sort direction
    /// when it is sorted by, and the group rows show the column's
    /// `GridAggregate`s.
    public void setGroupColumn(GridColumn c) {
        if (flat == null) {
            return;
        }
        cancelEdit();
        flat.setGroupColumn(c);
        refreshRows();
    }

    // --------------------------------------------------------------- rows

    /// How many rows are shown, group rows included.
    public int getRowCount() {
        return rows.size();
    }

    /// The value a view row shows in a column.
    public Object getValueAt(int row, GridColumn c) {
        return rows.value(row, c.getModelColumn());
    }

    /// Sets the value of a view row in a column, through the model.
    public void setValueAt(int row, GridColumn c, Object value) {
        rows.setValue(row, c.getModelColumn(), value);
        if (tree != null) {
            repaint();
        }
    }

    /// The model row a view row shows, or -1 for a group row or in a tree
    /// grid.
    public int getModelRow(int row) {
        return rows.modelRow(row);
    }

    /// The view row showing a model row, or -1 when it is filtered out or in
    /// a closed group.
    public int getViewRow(int modelRow) {
        return flat == null ? -1 : flat.viewRowOf(modelRow);
    }

    /// The node a view row of a tree grid shows, or null.
    public Object getNode(int row) {
        return rows.node(row);
    }

    /// The view row showing a node, or -1 when it is not shown.
    public int getRowOfNode(Object node) {
        return tree == null ? -1 : tree.find(node);
    }

    /// Whether a view row is a group row.
    public boolean isGroupRow(int row) {
        return rows.isGroup(row);
    }

    /// How deep a view row is in the tree: 0 for a root.
    public int getDepth(int row) {
        return tree == null ? 0 : tree.depth(row);
    }

    /// Whether a view row can be opened: a node that is not a leaf, or a
    /// group.
    public boolean isExpandable(int row) {
        return rows.isExpandable(row);
    }

    /// Whether a view row is open.
    public boolean isExpanded(int row) {
        return rows.isExpandable(row) && rows.isExpanded(row);
    }

    /// Opens or closes a view row: a node of a tree, or a group.
    public void setExpanded(int row, boolean open) {
        if (row < 0 || row >= rows.size()) {
            return;
        }
        cancelEdit();
        int delta = rows.setExpanded(row, open);
        if (delta != 0) {
            int oldLead = lead;
            selection.shift(row + 1, delta);
            if (lead > row) {
                lead = delta < 0 && lead <= row - delta ? row : lead + delta;
            }
            if (anchor > row) {
                anchor = delta < 0 && anchor <= row - delta ? row : anchor + delta;
            }
            if (oldLead != lead) {
                selectionListeners.fireSelectionEvent(oldLead, lead);
            }
        } else if (tree != null) {
            // Opened under a filter, where every path is shown open, or a
            // leaf: nothing moved.
            refreshRows();
        }
        dirty = true;
        repaint();
    }

    /// Opens or closes a node of a tree grid, whether it is shown or not; a
    /// node that is not shown opens the next time it is.
    public void setNodeExpanded(Object node, boolean open) {
        if (tree == null) {
            return;
        }
        int row = tree.find(node);
        if (row >= 0) {
            setExpanded(row, open);
        } else {
            tree.setNodeExpanded(node, open);
        }
    }

    /// Whether a node of a tree grid is open.
    public boolean isNodeExpanded(Object node) {
        return tree != null && tree.isExpandedNode(node);
    }

    /// Opens every row. In a tree grid this loads the whole tree.
    public void expandAll() {
        for (int r = 0; r < rows.size(); r++) {
            if (rows.isExpandable(r) && !rows.isExpanded(r)) {
                setExpanded(r, true);
            }
        }
    }

    /// Closes every row.
    public void collapseAll() {
        for (int r = rows.size() - 1; r >= 0; r--) {
            if (rows.isExpandable(r) && rows.isExpanded(r)) {
                setExpanded(r, false);
            }
        }
    }

    // ---------------------------------------------------------- selection

    /// What a tap selects.
    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    /// Sets what a tap selects; `MULTIPLE_ROWS` by default.
    public void setSelectionMode(SelectionMode mode) {
        selectionMode = mode == null ? SelectionMode.NONE : mode;
        if (selectionMode == SelectionMode.NONE) {
            selectedAll = false;
            selection.clear();
        } else if (selectionMode != SelectionMode.MULTIPLE_ROWS && selection.size() > 1) {
            selectedAll = false;
            selection.set(lead, lead);
        }
        repaint();
    }

    /// The row the keyboard is on, the last one chosen, or -1.
    public int getSelectedRow() {
        return lead;
    }

    /// The visible column index of the cell the keyboard is on.
    public int getSelectedColumn() {
        return leadColumn;
    }

    /// The selected view rows, in order.
    public int[] getSelectedRows() {
        return selection.toArray();
    }

    /// How many rows are selected.
    public int getSelectedRowCount() {
        return selection.size();
    }

    /// Whether a view row is selected.
    public boolean isRowSelected(int row) {
        return selection.contains(row);
    }

    /// Selects one view row and moves the keyboard to it; -1 clears the
    /// selection.
    public void setSelectedRow(int row) {
        int old = lead;
        if (row < 0 || row >= rows.size()) {
            selectedAll = false;
            selection.clear();
            lead = -1;
            anchor = -1;
        } else {
            if (selectionMode != SelectionMode.NONE) {
                selectedAll = false;
                selection.set(row, row);
            }
            lead = row;
            anchor = row;
        }
        repaint();
        selectionListeners.fireSelectionEvent(old, lead);
    }

    /// Selects a range of view rows, adding to the selection when `add`.
    public void selectRows(int from, int to, boolean add) {
        if (selectionMode != SelectionMode.MULTIPLE_ROWS) {
            setSelectedRow(to);
            return;
        }
        int n = rows.size();
        if (n == 0) {
            return;
        }
        from = Math.max(0, Math.min(n - 1, from));
        to = Math.max(0, Math.min(n - 1, to));
        if (add) {
            selection.add(from, to);
        } else {
            selection.set(from, to);
            anchor = from;
        }
        selectedAll = selection.size() == n && from == 0 && to == n - 1;
        int old = lead;
        lead = to;
        repaint();
        selectionListeners.fireSelectionEvent(old, lead);
    }

    /// Selects every row.
    public void selectAll() {
        if (rows.size() > 0) {
            selectRows(0, rows.size() - 1, false);
        }
    }

    /// Selects nothing.
    public void clearSelection() {
        int old = lead;
        selectedAll = false;
        selection.clear();
        repaint();
        selectionListeners.fireSelectionEvent(old, lead);
    }

    /// Listens to changes of the selection. The values given are the row the
    /// keyboard was on and is on now; read the rest from the grid.
    public void addSelectionListener(SelectionListener l) {
        selectionListeners.addListener(l);
    }

    /// Stops listening to changes of the selection.
    public void removeSelectionListener(SelectionListener l) {
        selectionListeners.removeListener(l);
    }

    /// Listens to rows being opened: a double tap, or Enter on a cell that is
    /// not edited. The selected row is the one opened.
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }

    /// Stops listening to rows being opened.
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }

    // ---------------------------------------------------------- appearance

    /// The height of a row in millimetres, 0 for the height of the font.
    public float getRowHeightMM() {
        return rowHeightMM;
    }

    /// Sets the height of a row in millimetres; 0, the default, fits the
    /// row style's font and padding.
    public void setRowHeightMM(float mm) {
        rowHeightMM = Math.max(0, mm);
        dirty = true;
        repaint();
    }

    /// The height of a row, in pixels, as last laid out.
    public int getRowHeight() {
        refreshLayout();
        return rowH;
    }

    /// Whether the header row is shown.
    public boolean isHeaderVisible() {
        return headerVisible;
    }

    /// Shows or hides the header row.
    public void setHeaderVisible(boolean v) {
        headerVisible = v;
        dirty = true;
        repaint();
    }

    /// Whether every other row is painted in `DataGridRowAlt`.
    public boolean isStriped() {
        return striped;
    }

    /// Sets whether every other row is painted in `DataGridRowAlt`.
    public void setStriped(boolean striped) {
        this.striped = striped;
        repaint();
    }

    /// Sets which grid lines are drawn.
    public void setGridLines(boolean horizontal, boolean vertical) {
        horizontalLines = horizontal;
        verticalLines = vertical;
        repaint();
    }

    /// The text shown when there are no rows.
    public String getEmptyText() {
        return emptyText;
    }

    /// Sets the text shown when there are no rows; empty by default.
    public void setEmptyText(String text) {
        emptyText = text == null ? "" : text;
        repaint();
    }

    /// The renderer of a column that has none.
    public GridCellRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    /// Sets the renderer of a column that has none.
    public void setDefaultRenderer(GridCellRenderer r) {
        defaultRenderer = r == null ? new DefaultGridCellRenderer() : r;
        refreshColumns();
    }

    /// The editor of a column that has none.
    public GridCellEditor getDefaultEditor() {
        return defaultEditor;
    }

    /// Sets the editor of a column that has none.
    public void setDefaultEditor(GridCellEditor e) {
        defaultEditor = e == null ? new DefaultGridCellEditor() : e;
    }

    // ------------------------------------------------------------- layout

    @Override
    protected void layoutSurface(int width, int height) {
        int gen = GridLook.generation();
        if (!dirty && width == laidW && height == laidH && gen == laidGen) {
            return;
        }
        laidW = width;
        laidH = height;
        laidGen = gen;
        dirty = false;
        ArrayList<GridColumn> v = new ArrayList<GridColumn>();
        for (GridColumn col : columns) {
            GridColumn c = col;
            if (c.isVisible() && c.getModelColumn() >= 0 && c.getModelColumn() < rows.columnCount()) {
                v.add(c);
            }
        }
        vis = new GridColumn[v.size()];
        v.toArray(vis);
        Style rowSt = GridLook.style("DataGridRow");
        Style headSt = GridLook.style("DataGridHeader");
        int fontH = rowSt.getFont().getHeight();
        if (rowHeightMM > 0) {
            rowH = GridLook.mm(rowHeightMM);
        } else {
            rowH = Math.max(GridLook.mm(6), fontH + Math.max(GridLook.mm(2.4f),
                    rowSt.getPaddingTop() + rowSt.getPaddingBottom()));
        }
        headerH = headerVisible ? Math.max(GridLook.mm(6.5f), headSt.getFont().getHeight()
                + Math.max(GridLook.mm(2.8f), headSt.getPaddingTop() + headSt.getPaddingBottom())) : 0;
        indentW = GridLook.mm(4);
        arrowW = hasArrows() ? GridLook.mm(4) : 0;
        int n = vis.length;
        colW = new int[n];
        colX = new int[n];
        int total = 0;
        int flexSum = 0;
        GridColumn tc = effectiveTreeColumn();
        for (int i = 0; i < n; i++) {
            GridColumn c = vis[i];
            int w = c.width();
            if (w < 0) {
                w = c.getWidthMM() > 0 ? GridLook.mm(c.getWidthMM()) : measure(c, c == tc); //NOPMD CompareObjectsWithEquals - a column is an identity
                c.width(w);
            }
            colW[i] = w;
            total += w;
            flexSum += c.getFlex();
        }
        if (flexSum > 0 && total < width) {
            int extra = width - total;
            int given = 0;
            int last = -1;
            for (int i = 0; i < n; i++) {
                int f = vis[i].getFlex();
                if (f > 0) {
                    int add = extra * f / flexSum;
                    colW[i] += add;
                    given += add;
                    last = i;
                }
            }
            if (last >= 0) {
                colW[last] += extra - given;
            }
        }
        frozenCount = Math.min(frozen, n);
        int x = 0;
        for (int i = 0; i < frozenCount; i++) {
            colX[i] = x;
            x += colW[i];
        }
        frozenW = x;
        x = 0;
        for (int i = frozenCount; i < n; i++) {
            colX[i] = x;
            x += colW[i];
        }
        scrollW = x;
        setHeaderSize(frozenW, headerH);
    }

    private boolean hasArrows() {
        return tree != null || (flat != null && flat.getGroupColumn() != null);
    }

    private GridColumn effectiveTreeColumn() {
        if (!hasArrows()) {
            return null;
        }
        if (treeColumn != null && treeColumn.isVisible() && columns.contains(treeColumn)) {
            return treeColumn;
        }
        for (GridColumn col : columns) {
            if (col.isVisible()) {
                return col;
            }
        }
        return null;
    }

    // The width a column needs for its title and the first rows.
    private int measure(GridColumn c, boolean isTree) {
        Style head = GridLook.style("DataGridHeader");
        int w = head.getFont().stringWidth(c.getTitle()) + GridLook.mm(3) + GridLook.mm(4);
        Style rowSt = GridLook.style("DataGridRow");
        GridCellRenderer r = c.getRenderer() != null ? c.getRenderer() : defaultRenderer;
        int n = Math.min(rows.size(), 60);
        for (int i = 0; i < n; i++) {
            fillCell(i, c, rowSt);
            int cw = r.getPreferredWidth(cell);
            if (isTree) {
                cw += rows.depth(i) * GridLook.mm(4) + GridLook.mm(4);
            }
            w = Math.max(w, cw);
        }
        int min = GridLook.mm(c.getMinWidthMM());
        int max = GridLook.mm(c.getMaxWidthMM());
        return Math.max(min, Math.min(max, w));
    }

    @Override
    protected int getContentWidth() {
        return scrollW;
    }

    @Override
    protected int getContentHeight() {
        // Clamped: 17 million rows of a tall row would pass an int.
        return (int) Math.min(Integer.MAX_VALUE, (long) rows.size() * rowH);
    }

    @Override
    protected Dimension calcPreferredSize() {
        layoutSurface(Math.max(1, getWidth()), Math.max(1, getHeight()));
        Style s = getStyle();
        int w = frozenW + scrollW + s.getHorizontalPadding();
        int h = headerH + Math.min(Math.max(rows.size(), 3), 12) * rowH + s.getVerticalPadding();
        return new Dimension(Math.min(w, Display.getInstance().getDisplayWidth()), h);
    }

    @Override
    protected int getPageStep(int bodyHeight) {
        return Math.max(rowH, bodyHeight / rowH * rowH);
    }

    // -------------------------------------------------------------- paint

    @Override
    protected void paintContent(Graphics g, int x, int y) {
        Style bg = GridLook.style("DataGrid");
        GridLook.fill(g, bg, g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        paintRows(g, x, y, frozenCount, vis.length);
        if (rows.size() == 0 && emptyText.length() > 0) {
            Style st = GridLook.style("DataGridRow");
            Font f = st.getFont();
            g.setFont(f);
            g.setColor(GridLook.mix(GridLook.under(st), st.getFgColor(), 140));
            int bw = getBodyWidth();
            String t = GridLook.fit(emptyText, f, bw - GridLook.mm(4));
            g.drawString(t, getX() + getHeaderWidth() + (bw - f.stringWidth(t)) / 2, y + GridLook.mm(4));
        }
    }

    @Override
    protected void paintLeftHeader(Graphics g, int x, int y) {
        Style bg = GridLook.style("DataGrid");
        GridLook.fill(g, bg, g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        paintRows(g, x, y, 0, frozenCount);
        g.setColor(lineColor(150));
        g.drawLine(x + frozenW - 1, g.getClipY(), x + frozenW - 1, g.getClipY() + g.getClipHeight());
    }

    @Override
    protected void paintTopHeader(Graphics g, int x, int y) {
        paintHeaders(g, x, y, frozenCount, vis.length);
    }

    @Override
    protected void paintCorner(Graphics g, int x, int y) {
        paintHeaders(g, x, y, 0, frozenCount);
        g.setColor(lineColor(150));
        g.drawLine(x + frozenW - 1, y, x + frozenW - 1, y + headerH);
    }

    private int lineColor(int amount) {
        Style line = GridLook.style("DataGridLine");
        return GridLook.mix(GridLook.under(GridLook.style("DataGridRow")), line.getFgColor(), amount);
    }

    private void paintRows(Graphics g, int x, int y, int from, int to) {
        int n = rows.size();
        if (n == 0 || from >= to) {
            return;
        }
        int cx = g.getClipX();
        int cy = g.getClipY();
        int cw = g.getClipWidth();
        int ch = g.getClipHeight();
        int first = Math.max(0, (cy - y) / rowH);
        int last = Math.min(n - 1, (cy + ch - 1 - y) / rowH);
        Style rowSt = GridLook.style("DataGridRow");
        Style rowSel = GridLook.selected("DataGridRow");
        Style alt = GridLook.style("DataGridRowAlt");
        Style groupSt = GridLook.style("DataGridGroupRow");
        int hline = lineColor(60);
        int left = x + colX[from];
        int right = x + colX[to - 1] + colW[to - 1];
        boolean focused = hasFocus();
        for (int r = first; r <= last; r++) {
            int ry = (int) (y + (long) r * rowH);
            boolean sel = selectionMode != SelectionMode.NONE && selection.contains(r);
            boolean group = rows.isGroup(r);
            Style st;
            if (sel) {
                st = rowSel;
            } else if (group) {
                st = groupSt;
            } else if (striped && (r & 1) == 1) {
                st = alt;
            } else {
                st = rowSt;
            }
            GridLook.fill(g, st, Math.max(cx, left), ry, Math.min(cx + cw, right) - Math.max(cx, left), rowH);
            for (int i = from; i < to; i++) {
                int x0 = x + colX[i];
                if (x0 + colW[i] < cx || x0 > cx + cw) {
                    continue;
                }
                paintCell(g, r, i, x0, ry, colW[i], st, sel);
                if (verticalLines && !group) {
                    g.setColor(hline);
                    g.drawLine(x0 + colW[i] - 1, ry, x0 + colW[i] - 1, ry + rowH - 1);
                }
            }
            if (horizontalLines || group) {
                g.setColor(hline);
                g.drawLine(Math.max(cx, left), ry + rowH - 1, Math.min(cx + cw, right), ry + rowH - 1);
            }
            if (r == lead && focused && leadColumn >= from && leadColumn < to
                    && (selectionMode == SelectionMode.SINGLE_CELL || selectionMode == SelectionMode.NONE
                    || editRow < 0)) {
                if (selectionMode == SelectionMode.SINGLE_CELL || selectionMode == SelectionMode.NONE) {
                    g.setColor(GridLook.style("DataGridCheck").getBgColor());
                    int lx = x + colX[leadColumn];
                    g.drawRect(lx, ry, colW[leadColumn] - 1, rowH - 1);
                    g.drawRect(lx + 1, ry + 1, colW[leadColumn] - 3, rowH - 3);
                }
            }
        }
    }

    private void fillCell(int r, GridColumn c, Style st) {
        cell.grid = this;
        cell.row = r;
        cell.column = c;
        cell.value = rows.value(r, c.getModelColumn());
        cell.group = rows.isGroup(r);
        cell.rowSelected = selection.contains(r);
        cell.lead = r == lead && c == (leadColumn < vis.length ? vis[leadColumn] : null); //NOPMD CompareObjectsWithEquals - a column is an identity
        cell.editable = !cell.group && c.isEditable() && rows.isEditable(r, c.getModelColumn());
        cell.depth = rows.depth(r);
        cell.style = st;
    }

    private void paintCell(Graphics g, int r, int i, int x0, int ry, int w, Style st, boolean sel) {
        GridColumn c = vis[i];
        int ox = g.getClipX();
        int oy = g.getClipY();
        int ow = g.getClipWidth();
        int oh = g.getClipHeight();
        g.clipRect(x0, ry, w, rowH);
        fillCell(r, c, st);
        int cx = x0;
        if (arrowW > 0 && c == effectiveTreeColumn()) { //NOPMD CompareObjectsWithEquals - a column is an identity
            cx += rows.depth(r) * indentW;
            if (rows.isExpandable(r)) {
                g.setColor(GridLook.mix(GridLook.under(st), st.getFgColor(), sel ? 230 : 170));
                GridLook.arrow(g, cx, ry + (rowH - arrowW) / 2, arrowW, rows.isExpanded(r));
            }
            cx += arrowW;
        }
        if (cell.group && c == effectiveTreeColumn()) { //NOPMD CompareObjectsWithEquals - a column is an identity
            Font f = st.getFont();
            g.setFont(f);
            g.setColor(st.getFgColor());
            String label = rows.groupLabel(r) + "  (" + rows.groupCount(r) + ")";
            // A group's label may run over the columns after it that show
            // nothing for the group.
            int span = w - (cx - x0);
            for (int k = i + 1; k < vis.length && vis[k].getAggregate() == null
                    && (i >= frozenCount || k < frozenCount); k++) {
                span += colW[k];
            }
            g.setClip(ox, oy, ow, oh);
            g.clipRect(cx, ry, span, rowH);
            g.drawString(GridLook.fit(label, f, span - GridLook.mm(1.5f)), cx + GridLook.mm(1), ry + (rowH - f.getHeight()) / 2);
        } else if (!cell.group || c.getAggregate() != null) {
            GridCellRenderer rend = c.getRenderer() != null ? c.getRenderer() : defaultRenderer;
            if (!(editRow == r && editColumn == c && editor != null)) { //NOPMD CompareObjectsWithEquals - a column is an identity
                rend.paint(g, cell, cx, ry, x0 + w - cx, rowH);
            }
        }
        g.setClip(ox, oy, ow, oh);
    }

    private void paintHeaders(Graphics g, int x, int y, int from, int to) {
        if (headerH == 0 || from >= to) {
            return;
        }
        Style head = GridLook.style("DataGridHeader");
        int cx = g.getClipX();
        int cw = g.getClipWidth();
        GridLook.fill(g, head, cx, y, cw, headerH);
        Font f = head.getFont();
        g.setFont(f);
        int pad = Math.max(GridLook.mm(1.5f), head.getPaddingLeftNoRTL());
        int arrow = GridLook.mm(2.6f);
        int line = lineColor(110);
        for (int i = from; i < to; i++) {
            int x0 = x + colX[i];
            int w = colW[i];
            if (x0 + w < cx || x0 > cx + cw) {
                continue;
            }
            GridColumn c = vis[i];
            if (gesture == GESTURE_MOVE && i == dragColumn) {
                GridLook.fill(g, GridLook.selected("DataGridRow"), x0, y, w, headerH);
            }
            int dir = getSortDirection(c);
            int textW = w - pad * 2 - (dir != 0 ? arrow + GridLook.mm(1) : 0);
            String t = GridLook.fit(c.getTitle(), f, textW);
            g.setColor(head.getFgColor());
            int align = c.resolveAlignment(null);
            if (c.getKind() == GridColumn.Kind.NUMBER) {
                align = Component.RIGHT;
            }
            int tw = f.stringWidth(t);
            int tx = DefaultGridCellRenderer.place(align, x0 + pad, textW, tw);
            g.drawString(t, tx, y + (headerH - f.getHeight()) / 2);
            if (dir != 0) {
                int ax = x0 + w - pad - arrow;
                int ay = y + (headerH - arrow) / 2;
                if (dir > 0) {
                    g.fillTriangle(ax, ay + arrow * 3 / 4, ax + arrow, ay + arrow * 3 / 4, ax + arrow / 2, ay + arrow / 4);
                } else {
                    g.fillTriangle(ax, ay + arrow / 4, ax + arrow, ay + arrow / 4, ax + arrow / 2, ay + arrow * 3 / 4);
                }
                if (sortColumns.size() > 1) {
                    Font small = f;
                    String num = String.valueOf(sortColumns.indexOf(c) + 1);
                    g.drawString(num, ax - small.stringWidth(num) - 1, y + (headerH - small.getHeight()) / 2);
                }
            }
            g.setColor(line);
            g.drawLine(x0 + w - 1, y + headerH / 4, x0 + w - 1, y + headerH * 3 / 4);
        }
        g.setColor(line);
        g.drawLine(cx, y + headerH - 1, cx + cw, y + headerH - 1);
    }

    @Override
    protected void paintOverlay(Graphics g, int x, int y) {
        if (gesture == GESTURE_MOVE && moveTarget >= 0 && dragColumn >= 0) {
            int tx = columnScreenX(moveTarget, moveTarget > dragColumn);
            g.setColor(GridLook.style("DataGridCheck").getBgColor());
            g.fillRect(x + tx - 1, y, 3, headerH);
            Style head = GridLook.selected("DataGridRow");
            int w = colW[dragColumn];
            int a = g.getAlpha();
            g.setAlpha(a * 3 / 4);
            GridLook.fill(g, head, x + dragX - w / 2, y, w, headerH);
            g.setAlpha(a);
            g.setColor(head.getFgColor());
            Font f = GridLook.style("DataGridHeader").getFont();
            g.setFont(f);
            g.drawString(GridLook.fit(vis[dragColumn].getTitle(), f, w - GridLook.mm(3)),
                    x + dragX - w / 2 + GridLook.mm(1.5f), y + (headerH - f.getHeight()) / 2);
        }
        if (gesture == GESTURE_RESIZE && dragColumn >= 0) {
            int ex = columnScreenX(dragColumn, true);
            g.setColor(GridLook.style("DataGridCheck").getBgColor());
            g.drawLine(x + ex - 1, y, x + ex - 1, y + getHeight());
        }
    }

    // Where a visible column's left edge, or its right edge, is in the
    // component's coordinates.
    private int columnScreenX(int i, boolean rightEdge) {
        int e = colX[i] + (rightEdge ? colW[i] : 0);
        return i < frozenCount ? e : getHeaderWidth() + e - getOffsetX();
    }

    /// The bounds of a cell in the grid's own coordinates: x, y, width,
    /// height. `column` is an index among the visible columns.
    public void getCellBounds(int row, int column, int[] out) {
        refreshLayout();
        if (column < 0 || column >= vis.length) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            out[3] = 0;
            return;
        }
        out[0] = columnScreenX(column, false);
        out[1] = (int) (headerH + (long) row * rowH - getOffsetY());
        out[2] = colW[column];
        out[3] = rowH;
    }

    /// The columns shown, in order: the ones `getSelectedColumn()` and
    /// `getCellBounds` count.
    public GridColumn[] getVisibleColumns() {
        refreshLayout();
        GridColumn[] out = new GridColumn[vis.length];
        System.arraycopy(vis, 0, out, 0, vis.length);
        return out;
    }

    /// Scrolls so a view row is shown, and a visible column too when it is
    /// 0 or more.
    public void scrollToCell(int row, int column) {
        refreshLayout();
        int ox = getOffsetX();
        int oy = getOffsetY();
        int bh = getBodyHeight();
        if (row >= 0) {
            int top = (int) Math.min(Integer.MAX_VALUE - rowH, (long) row * rowH);
            if (top < oy) {
                oy = top;
            } else if (top + rowH > oy + bh) {
                oy = top + rowH - bh;
            }
        }
        if (column >= frozenCount && column < vis.length) {
            int bw = getBodyWidth();
            int l = colX[column];
            int r = l + colW[column];
            if (l < ox) {
                ox = l;
            } else if (r > ox + bw) {
                ox = Math.min(l, r - bw);
            }
        }
        scrollTo(ox, oy);
    }

    // ---------------------------------------------------------- hit test

    // The visible column at a point of the component, or -1.
    private int columnAt(int x) {
        boolean inFrozen = x < getHeaderWidth();
        int cx = inFrozen ? x : x - getHeaderWidth() + getOffsetX();
        int from = inFrozen ? 0 : frozenCount;
        int to = inFrozen ? frozenCount : vis.length;
        for (int i = from; i < to; i++) {
            if (cx >= colX[i] && cx < colX[i] + colW[i]) {
                return i;
            }
        }
        return -1;
    }

    private int rowAt(int y) {
        if (y < headerH) {
            return -1;
        }
        int r = (y - headerH + getOffsetY()) / rowH;
        return r >= 0 && r < rows.size() ? r : -1;
    }

    // The column whose right edge is near a point of the header, or -1.
    private int edgeAt(int x, int y) {
        if (y >= headerH) {
            return -1;
        }
        int slop = GridLook.mm(1.2f);
        for (int i = 0; i < vis.length; i++) {
            int e = columnScreenX(i, true);
            if (Math.abs(x - e) <= slop && vis[i].isResizable()
                    && (i < frozenCount || e > getHeaderWidth())) {
                return i;
            }
        }
        return -1;
    }

    private boolean overArrow(int row, int col, int x) {
        if (arrowW == 0 || col < 0 || vis[col] != effectiveTreeColumn() || !rows.isExpandable(row)) { //NOPMD CompareObjectsWithEquals - a column is an identity
            return false;
        }
        int left = columnScreenX(col, false) + rows.depth(row) * indentW;
        return x >= left - GridLook.mm(1) && x < left + arrowW + GridLook.mm(1);
    }

    // ------------------------------------------------------------- input

    @Override
    protected void pressStarted(int x, int y) {
        if (editor != null) {
            commitEdit();
        }
    }

    @Override
    protected void tapped(int x, int y, boolean doubleTap) {
        refreshLayout();
        requestFocus();
        int col = columnAt(x);
        if (y < headerH) {
            if (col >= 0 && vis[col].isSortable()) {
                headerTapped(vis[col]);
            }
            return;
        }
        int row = rowAt(y);
        if (row < 0) {
            return;
        }
        if (overArrow(row, col, x) || (rows.isGroup(row) && !doubleTap)) {
            setExpanded(row, !rows.isExpanded(row));
            return;
        }
        Display d = Display.getInstance();
        boolean toggle = d.isControlKeyDown() || d.isMetaKeyDown();
        boolean extend = d.isShiftKeyDown();
        int old = lead;
        choose(row, toggle, extend);
        if (col >= 0) {
            leadColumn = col;
        }
        selectionListeners.fireSelectionEvent(old, lead);
        if (col >= 0 && isCellEditable(row, col)) {
            GridColumn c = vis[col];
            if (c.resolveKind(rows.value(row, c.getModelColumn())) == GridColumn.Kind.BOOLEAN) {
                toggleBoolean(row, c);
                return;
            }
            if (doubleTap) {
                editCellAt(row, col);
                return;
            }
        } else if (doubleTap) {
            if (rows.isExpandable(row)) {
                setExpanded(row, !rows.isExpanded(row));
            }
            actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.PointerPressed, x, y));
        }
        repaint();
    }

    private void headerTapped(GridColumn c) {
        int dir = getSortDirection(c);
        if (Display.getInstance().isShiftKeyDown() && !sortColumns.isEmpty()) {
            if (dir == 0) {
                addSort(c, true);
            } else if (dir > 0) {
                addSort(c, false);
            } else {
                int i = sortColumns.indexOf(c);
                sortColumns.remove(i);
                sortAscending.remove(i);
                resort();
            }
            return;
        }
        if (dir == 0) {
            sortBy(c, true);
        } else if (dir > 0) {
            sortBy(c, false);
        } else {
            clearSort();
        }
    }

    private void choose(int row, boolean toggle, boolean extend) {
        if (selectionMode == SelectionMode.MULTIPLE_ROWS) {
            if (extend && anchor >= 0) {
                if (toggle) {
                    selectedAll = false;
                    selection.add(anchor, row);
                } else {
                    selectedAll = false;
                    selection.set(anchor, row);
                }
            } else if (toggle) {
                selectedAll = false;
                selection.toggle(row);
                anchor = row;
            } else {
                selectedAll = false;
                selection.set(row, row);
                anchor = row;
            }
        } else if (selectionMode != SelectionMode.NONE) {
            selectedAll = false;
            selection.set(row, row);
            anchor = row;
        }
        lead = row;
    }

    private boolean isCellEditable(int row, int col) {
        if (row < 0 || row >= rows.size() || col < 0 || col >= vis.length || rows.isGroup(row)) {
            return false;
        }
        GridColumn c = vis[col];
        return c.isEditable() && rows.isEditable(row, c.getModelColumn());
    }

    private void toggleBoolean(int row, GridColumn c) {
        Object v = rows.value(row, c.getModelColumn());
        boolean on = v instanceof Boolean && ((Boolean) v).booleanValue();
        rows.setValue(row, c.getModelColumn(), Boolean.valueOf(!on));
        repaint();
    }

    @Override
    protected boolean editStarted(int x, int y, EditTrigger trigger) {
        refreshLayout();
        if (editor != null) {
            commitEdit();
        }
        int edge = edgeAt(x, y);
        if (edge >= 0) {
            gesture = GESTURE_RESIZE;
            dragColumn = edge;
            dragStartX = x;
            dragStartW = colW[edge];
            return true;
        }
        if (y < headerH) {
            int col = columnAt(x);
            if (col >= 0 && vis[col].isMovable() && trigger != EditTrigger.HANDLE_DRAG) {
                gesture = GESTURE_MOVE;
                dragColumn = col;
                dragX = x;
                moveTarget = col;
                return true;
            }
            return false;
        }
        if (trigger == EditTrigger.DRAG && selectionMode == SelectionMode.MULTIPLE_ROWS) {
            int row = rowAt(y);
            if (row >= 0) {
                gesture = GESTURE_SELECT;
                int old = lead;
                if (!Display.getInstance().isShiftKeyDown() || anchor < 0) {
                    anchor = row;
                }
                selectedAll = false;
                selection.set(anchor, row);
                lead = row;
                selectionListeners.fireSelectionEvent(old, lead);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void editMoved(int x, int y) {
        if (gesture == GESTURE_RESIZE) {
            GridColumn c = vis[dragColumn];
            int w = Math.max(GridLook.mm(c.getMinWidthMM()), dragStartW + (x - dragStartX));
            c.width(w);
            dirty = true;
        } else if (gesture == GESTURE_MOVE) {
            dragX = x;
            int over = columnAt(Math.max(0, Math.min(getWidth() - 1, x)));
            if (over >= 0) {
                // Columns move within their own region: the frozen ones
                // among themselves, the others among themselves.
                boolean same = (over < frozenCount) == (dragColumn < frozenCount);
                if (same) {
                    moveTarget = over;
                }
            }
        } else if (gesture == GESTURE_SELECT) {
            int row = rowAt(Math.max(headerH, Math.min(getHeight() - 1, y)));
            if (row < 0 && y >= headerH) {
                row = rows.size() - 1;
            }
            if (row >= 0 && row != lead) {
                int old = lead;
                selectedAll = false;
                selection.set(anchor, row);
                lead = row;
                selectionListeners.fireSelectionEvent(old, lead);
            }
        }
        repaint();
    }

    @Override
    protected void editEnded(boolean committed) {
        if (gesture == GESTURE_MOVE && committed && moveTarget >= 0 && moveTarget != dragColumn) {
            GridColumn moving = vis[dragColumn];
            GridColumn target = vis[moveTarget];
            moveColumn(columns.indexOf(moving), columns.indexOf(target));
            if (leadColumn == dragColumn) {
                leadColumn = moveTarget;
            }
        } else if (gesture == GESTURE_RESIZE && committed && dragColumn >= 0) {
            // The width is kept in millimetres, like one that was set, and the
            // exact pixels are kept for this density.
            GridColumn c = vis[dragColumn];
            int px = c.width();
            float ppmm = Math.max(0.01f, Display.getInstance().convertToPixels(100f) / 100f);
            c.setWidthMM(px / ppmm);
            c.width(px);
            dirty = true;
        }
        gesture = GESTURE_NONE;
        dragColumn = -1;
        moveTarget = -1;
        repaint();
    }

    @Override
    protected boolean isEditAutoScroll(int x, int y) {
        return gesture == GESTURE_SELECT || gesture == GESTURE_MOVE;
    }

    @Override
    protected void hoverAt(int x, int y) {
        refreshLayout();
        int edge = edgeAt(x, y);
        if (edge >= 0) {
            setHoverTarget(vis[edge], Component.E_RESIZE_CURSOR);
        } else {
            setHoverTarget(null, Component.DEFAULT_CURSOR);
        }
    }

    @Override
    protected void scrollChanged() {
        if (editor != null && !committing) {
            commitEdit();
        }
    }

    @Override
    public boolean handlesInput() {
        // The arrows move between cells, not to the next component.
        return rows.size() > 0;
    }

    @Override
    public void keyPressed(int keyCode) {
        if (editor != null || rows.size() == 0) {
            super.keyPressed(keyCode);
            return;
        }
        refreshLayout();
        int ga = Display.getInstance().getGameAction(keyCode);
        boolean extend = Display.getInstance().isShiftKeyDown() && selectionMode == SelectionMode.MULTIPLE_ROWS;
        int row = lead < 0 ? 0 : lead;
        int col = leadColumn;
        switch (ga) {
            case Display.GAME_UP:
                moveLead(Math.max(0, row - (lead < 0 ? 0 : 1)), col, extend);
                return;
            case Display.GAME_DOWN:
                moveLead(Math.min(rows.size() - 1, row + (lead < 0 ? 0 : 1)), col, extend);
                return;
            case Display.GAME_LEFT:
                if (lead >= 0 && rows.isExpandable(lead) && rows.isExpanded(lead) && arrowW > 0) {
                    setExpanded(lead, false);
                } else if (lead >= 0 && tree != null && tree.depth(lead) > 0 && col <= 0) {
                    int d = tree.depth(lead);
                    int p = lead - 1;
                    while (p > 0 && tree.depth(p) >= d) {
                        p--;
                    }
                    moveLead(p, col, false);
                } else {
                    moveLead(row, Math.max(0, col - 1), false);
                }
                return;
            case Display.GAME_RIGHT:
                if (lead >= 0 && rows.isExpandable(lead) && !rows.isExpanded(lead) && arrowW > 0) {
                    setExpanded(lead, true);
                } else {
                    moveLead(row, Math.min(vis.length - 1, col + 1), false);
                }
                return;
            case Display.GAME_FIRE:
                if (lead < 0) {
                    return;
                }
                if (isCellEditable(lead, col)) {
                    GridColumn c = vis[col];
                    if (c.resolveKind(rows.value(lead, c.getModelColumn())) == GridColumn.Kind.BOOLEAN) {
                        toggleBoolean(lead, c);
                    } else {
                        editCellAt(lead, col);
                    }
                } else if (rows.isExpandable(lead)) {
                    setExpanded(lead, !rows.isExpanded(lead));
                } else {
                    actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.KeyPress, keyCode));
                }
                return;
            default:
                super.keyPressed(keyCode);
        }
    }

    private void moveLead(int row, int col, boolean extend) {
        int old = lead;
        if (extend && anchor >= 0) {
            selectedAll = false;
            selection.set(anchor, row);
            lead = row;
        } else {
            choose(row, false, false);
        }
        leadColumn = Math.max(0, col);
        scrollToCell(row, leadColumn);
        repaint();
        selectionListeners.fireSelectionEvent(old, lead);
    }

    // ------------------------------------------------------------ editing

    /// Whether a cell is being edited.
    @Override
    public boolean isEditing() {
        return editor != null;
    }

    /// The view row being edited, or -1.
    public int getEditingRow() {
        return editor == null ? -1 : editRow;
    }

    /// The column being edited, or null.
    public GridColumn getEditingColumn() {
        return editor == null ? null : editColumn;
    }

    /// Edits a cell, when it may be edited. `column` is an index among the
    /// visible columns. A boolean is toggled at once; anything else gets its
    /// column's editor over the cell.
    ///
    /// #### Returns
    ///
    /// true when the cell is being edited, or was toggled
    public boolean editCellAt(int row, int column) {
        refreshLayout();
        if (!isCellEditable(row, column)) {
            return false;
        }
        commitEdit();
        GridColumn c = vis[column];
        if (c.resolveKind(rows.value(row, c.getModelColumn())) == GridColumn.Kind.BOOLEAN) {
            toggleBoolean(row, c);
            return true;
        }
        TopLevelContainer top = getTopLevelContainer();
        if (top == null) {
            return false;
        }
        scrollToCell(row, column);
        stopFling();
        fillCell(row, c, GridLook.selected("DataGridRow"));
        GridCellEditor source = c.getEditor() != null ? c.getEditor() : defaultEditor;
        Component cmp = source.createEditor(this, cell);
        if (cmp == null) {
            return false;
        }
        editRow = row;
        editColumn = c;
        editKey = rows.key(row);
        editorSource = source;
        editor = cmp;
        leadColumn = column;
        final int col = column;
        final Component ed = cmp;
        Container layer = top.getFormLayeredPane(DataGrid.class, true);
        editorHolder = new EditorLayer(new Layout() {
            @Override
            public void layoutContainer(Container parent) {
                int[] b = new int[4];
                getCellBounds(editRow, col, b);
                ed.setX(getAbsoluteX() + b[0] - parent.getAbsoluteX());
                ed.setY(getAbsoluteY() + b[1] - parent.getAbsoluteY());
                ed.setWidth(b[2]);
                ed.setHeight(b[3]);
            }

            @Override
            public Dimension getPreferredSize(Container parent) {
                return new Dimension(0, 0);
            }
        }, ed);
        editorHolder.add(cmp);
        layer.add(editorHolder);
        layer.revalidate();
        repaint();
        if (cmp.isEditable()) {
            cmp.startEditingAsync();
        } else {
            cmp.requestFocus();
        }
        return true;
    }

    /// Ends the edit, writing the editor's value to the model.
    public void commitEdit() {
        if (editor == null || committing) {
            return;
        }
        committing = true;
        try {
            int row = rows.find(editKey);
            if (row >= 0) {
                fillCell(row, editColumn, GridLook.style("DataGridRow"));
                Object v = editorSource.getEditorValue(editor, cell);
                Object before = cell.getValue();
                if (v == null ? before != null : !v.equals(before)) {
                    rows.setValue(row, editColumn.getModelColumn(), v);
                }
            }
        } finally {
            closeEditor();
            committing = false;
        }
    }

    /// Ends the edit without changing the model, and any drag in progress.
    @Override
    public final void cancelEdit() {
        super.cancelEdit();
        if (editor != null && !committing) {
            committing = true;
            try {
                closeEditor();
            } finally {
                committing = false;
            }
        }
    }

    private void closeEditor() {
        Component ed = editor;
        Container holder = editorHolder;
        editor = null;
        editorHolder = null;
        editorSource = null;
        editKey = null;
        editRow = -1;
        editColumn = null;
        if (ed != null && ed.isEditing()) {
            ed.stopEditing(null);
        }
        if (holder != null) {
            Container layer = holder.getParent();
            holder.remove();
            if (layer != null) {
                layer.revalidate();
            }
        }
        if (isInitialized()) {
            requestFocus();
        }
        repaint();
    }

    // The layer the editor is laid in. It covers the form, so only the
    // editor itself takes the pointer: a tap anywhere else reaches what is
    // under it.
    private static final class EditorLayer extends Container {
        private final Component editor;

        EditorLayer(Layout layout, Component editor) {
            super(layout);
            this.editor = editor;
        }

        @Override
        public boolean contains(int x, int y) {
            return editor.contains(x, y);
        }
    }
}
