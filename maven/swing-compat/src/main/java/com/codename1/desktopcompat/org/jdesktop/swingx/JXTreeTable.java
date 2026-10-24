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
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.RowSorter;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.event.TreeWillExpandListener;
import com.codename1.desktopcompat.javax.swing.table.AbstractTableModel;
import com.codename1.desktopcompat.javax.swing.table.TableCellRenderer;
import com.codename1.desktopcompat.javax.swing.table.TableModel;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreeModel;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultTreeRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.table.TableColumnExt;
import com.codename1.desktopcompat.org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.treetable.TreeTableModel;
import com.codename1.desktopcompat.rt.CellPainter;
import com.codename1.desktopcompat.rt.CellTheme;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

/// A table whose rows are the nodes of a tree: one column, the
/// hierarchical one, indents each row by its depth and has the arrow
/// that expands and collapses a node, and the others show the node's
/// values.
///
/// ## Model
///
/// The model is a
/// [com.codename1.desktopcompat.org.jdesktop.swingx.treetable.TreeTableModel],
/// set with [#setTreeTableModel(TreeTableModel)]. The table model is an
/// adapter the tree table makes itself and [#setModel(TableModel)] takes
/// nothing else. A row is a node that shows: the children of the
/// expanded nodes, from the root down. The root has no row unless
/// [#setRootVisible(boolean)] gives it one, and a hidden root is always
/// expanded.
///
/// ## The hierarchical column
///
/// The cell is drawn by a tree cell renderer, which is given the node
/// and by default shows the model's value for the node in that column.
/// A press on the arrow, or as many clicks on the cell as
/// [#setToggleClickCount(int)] says when they do not start an edit,
/// expands or collapses the node. A press on the arrow selects the row
/// as well. An editor in this column is placed after the indent.
///
/// ## Highlighters
///
/// Highlighters see every cell through an adapter that knows the node:
/// whether it is a leaf, whether it is expanded and how deep it is, the
/// root being at depth 0. In the hierarchical column they decorate the
/// tree cell renderer's component, and its background fills the indent.
///
/// ## Not here
///
/// A tree table is not sortable and not filterable. The tree selection
/// model and its listeners, the icon setters, the large model and
/// expands-selected-paths switches, tool tips of the hierarchical column
/// and drag and drop are absent.
public class JXTreeTable extends JXTable {

    // None of these has an initializer: the superclass constructor
    // reaches methods overridden here before one would run.
    private Rows cn1Tree;
    private TreeTableModelAdapter cn1Adapter;
    private HierarchicalRenderer cn1Renderer;
    private HierarchicalCell cn1Cell;
    private List<TreeExpansionListener> cn1ExpansionListeners;
    private boolean cn1LostSelection;
    private TreePath cn1PressPath;
    private int cn1ClickBase;

    /// A tree table over an empty model without columns.
    public JXTreeTable() {
        this(new DefaultTreeTableModel());
    }

    /// A tree table over `treeModel`, which must not be `null`.
    public JXTreeTable(TreeTableModel treeModel) {
        this(new TreeTableModelAdapter(new Rows(cn1NotNull(treeModel)), treeModel));
    }

    private JXTreeTable(TreeTableModelAdapter adapter) {
        super(adapter);
        cn1Adapter = adapter;
        cn1Tree = adapter.tree;
        adapter.table = this;
        cn1Renderer = new HierarchicalRenderer(this);
        cn1Tree.addTreeExpansionListener(new ExpansionRelay());
        super.setSortable(false);
    }

    private static TreeTableModel cn1NotNull(TreeTableModel treeModel) {
        if (treeModel == null) {
            throw new NullPointerException("treeModel must not be null");
        }
        return treeModel;
    }

    // ------------------------------------------------------------ sorting

    /// Does nothing: a tree table is not sortable.
    @Override
    public void setSortable(boolean sortable) {
    }

    /// Does nothing: a tree table is not sortable.
    @Override
    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
    }

    /// Does nothing: a tree table is not sortable.
    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
    }

    /// The table's own [#setAutoCreateRowSorter(boolean)], for a
    /// subclass that sorts the rows in a way that keeps the tree.
    protected void superSetAutoCreateRowSorter(boolean autoCreateRowSorter) {
        super.setAutoCreateRowSorter(autoCreateRowSorter);
    }

    /// The table's own [#setSortable(boolean)].
    protected void superSetSortable(boolean sortable) {
        super.setSortable(sortable);
    }

    /// The table's own [#setRowSorter(RowSorter)].
    protected void superSetRowSorter(RowSorter<? extends TableModel> sorter) {
        super.setRowSorter(sorter);
    }

    // ------------------------------------------------------------ model

    /// Replaces the tree table model, which must not be `null`. The
    /// columns are made again and the expanded nodes forgotten.
    public void setTreeTableModel(TreeTableModel treeModel) {
        TreeTableModel old = getTreeTableModel();
        cn1Adapter.setModel(cn1NotNull(treeModel));
        firePropertyChange("treeTableModel", old, getTreeTableModel());
    }

    public TreeTableModel getTreeTableModel() {
        return cn1Adapter != null ? cn1Adapter.model : null;
    }

    /// Takes only the adapter the tree table made over its tree table
    /// model: the model is changed with
    /// [#setTreeTableModel(TreeTableModel)].
    @Override
    public final void setModel(TableModel tableModel) {
        if (!(tableModel instanceof TreeTableModelAdapter)) {
            throw new IllegalArgumentException("unsupported model type");
        }
        super.setModel(tableModel);
    }

    // ------------------------------------------------------------ rows and paths

    /// The path of a view row, or `null`.
    private TreePath cn1Path(int row) {
        if (cn1Tree == null || row < 0 || row >= getRowCount()) {
            return null;
        }
        return cn1Tree.getPathForRow(convertRowIndexToModel(row));
    }

    /// The path of the node in a row, or `null` for no such row.
    public TreePath getPathForRow(int row) {
        return cn1Path(row);
    }

    /// The row of the node at the end of a path, or -1 when it has none.
    public int getRowForPath(TreePath path) {
        int row = cn1Tree.getRowForPath(path);
        return row < 0 ? -1 : convertRowIndexToView(row);
    }

    /// The path of the node in the row at a point, or `null`.
    public TreePath getPathForLocation(int x, int y) {
        return cn1Path(rowAtPoint(new Point(x, y)));
    }

    public void expandAll() {
        cn1Tree.expandAll();
    }

    /// Collapses every row, from the last up.
    public void collapseAll() {
        cn1Tree.collapseAll();
    }

    /// Expands the node at the end of the path and the nodes above it.
    /// A leaf is left alone.
    public void expandPath(TreePath path) {
        cn1Tree.expandPath(path);
    }

    public void collapsePath(TreePath path) {
        cn1Tree.collapsePath(path);
    }

    public void expandRow(int row) {
        cn1Tree.expandPath(cn1Path(row));
    }

    public void collapseRow(int row) {
        cn1Tree.collapsePath(cn1Path(row));
    }

    /// Whether the node at the end of the path has a row or would get
    /// one from its parent: every node above it is expanded.
    public boolean isVisible(TreePath path) {
        return cn1Tree.isVisible(path);
    }

    /// Whether the node at the end of the path and every node above it
    /// is expanded.
    public boolean isExpanded(TreePath path) {
        return cn1Tree.isExpanded(path);
    }

    public boolean isExpanded(int row) {
        TreePath path = cn1Path(row);
        return path != null && cn1Tree.isExpanded(path);
    }

    public boolean isCollapsed(TreePath path) {
        return cn1Tree.isCollapsed(path);
    }

    public boolean isCollapsed(int row) {
        return !isExpanded(row);
    }

    /// The expanded paths at and below `path`, or `null` when it is not
    /// expanded itself.
    public Enumeration<?> getExpandedDescendants(TreePath path) {
        return cn1Tree.getExpandedDescendants(path);
    }

    /// Expands what is above the path and scrolls its row into view.
    public void scrollPathToVisible(TreePath path) {
        if (path == null) {
            return;
        }
        cn1Tree.makeVisible(path);
        int row = getRowForPath(path);
        if (row >= 0) {
            scrollRowToVisible(row);
        }
    }

    /// Gives the root a row of its own or takes it away. A hidden root
    /// is expanded.
    public void setRootVisible(boolean visible) {
        cn1Tree.setRootVisible(visible);
    }

    public boolean isRootVisible() {
        return cn1Tree.isRootVisible();
    }

    /// Whether the nodes of the first level get an arrow and the indent
    /// it needs. On by default.
    public void setShowsRootHandles(boolean visible) {
        cn1Tree.setShowsRootHandles(visible);
    }

    public boolean getShowsRootHandles() {
        return cn1Tree.getShowsRootHandles();
    }

    /// Whether expanding a node scrolls the table to show its children.
    /// On by default.
    public void setScrollsOnExpand(boolean scroll) {
        cn1Tree.setScrollsOnExpand(scroll);
    }

    public boolean getScrollsOnExpand() {
        return cn1Tree.getScrollsOnExpand();
    }

    public int getToggleClickCount() {
        return cn1Tree.getToggleClickCount();
    }

    /// Sets how many clicks on a node's hierarchical cell expand or
    /// collapse it; 2 by default, and less than 1 for none.
    public void setToggleClickCount(int clickCount) {
        cn1Tree.setToggleClickCount(clickCount);
    }

    // ------------------------------------------------------------ listeners

    /// Adds a listener that is told, with this table as the source,
    /// after a node was expanded or collapsed.
    public void addTreeExpansionListener(TreeExpansionListener tel) {
        if (tel == null) {
            return;
        }
        if (cn1ExpansionListeners == null) {
            cn1ExpansionListeners = new ArrayList<TreeExpansionListener>();
        }
        cn1ExpansionListeners.add(tel);
    }

    public void removeTreeExpansionListener(TreeExpansionListener tel) {
        if (cn1ExpansionListeners != null) {
            cn1ExpansionListeners.remove(tel);
        }
    }

    /// Adds a listener that is asked before a node is expanded or
    /// collapsed and can refuse. The source of its events is the tree
    /// inside the table, not the table.
    public void addTreeWillExpandListener(TreeWillExpandListener tel) {
        cn1Tree.addTreeWillExpandListener(tel);
    }

    public void removeTreeWillExpandListener(TreeWillExpandListener tel) {
        cn1Tree.removeTreeWillExpandListener(tel);
    }

    private final class ExpansionRelay implements TreeExpansionListener {

        private TreeExpansionListener[] listeners() {
            List<TreeExpansionListener> all = cn1ExpansionListeners;
            if (all == null) {
                return new TreeExpansionListener[0];
            }
            return all.toArray(new TreeExpansionListener[all.size()]);
        }

        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            if (cn1Tree.getScrollsOnExpand()) {
                cn1ShowChildren(path);
            }
            TreeExpansionListener[] ls = listeners();
            if (ls.length > 0) {
                TreeExpansionEvent own = new TreeExpansionEvent(JXTreeTable.this, path);
                for (int i = ls.length - 1; i >= 0; i--) {
                    ls[i].treeExpanded(own);
                }
            }
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            if (cn1LostSelection) {
                // A selected row went with the collapse: the selection
                // moves up to the node that was collapsed.
                cn1LostSelection = false;
                int row = getRowForPath(path);
                if (row >= 0) {
                    addRowSelectionInterval(row, row);
                }
            }
            TreeExpansionListener[] ls = listeners();
            if (ls.length > 0) {
                TreeExpansionEvent own = new TreeExpansionEvent(JXTreeTable.this, path);
                for (int i = ls.length - 1; i >= 0; i--) {
                    ls[i].treeCollapsed(own);
                }
            }
        }
    }

    /// Scrolls so that the children of a node that was just expanded
    /// show, as far as they fit below the node itself.
    private void cn1ShowChildren(TreePath path) {
        if (getParent() == null) {
            return;
        }
        int row = getRowForPath(path);
        if (row < 0) {
            return;
        }
        int children = cn1Adapter.model.getChildCount(path.getLastPathComponent());
        int last = Math.min(getRowCount() - 1, row + children);
        if (last > row) {
            scrollRowToVisible(last);
            scrollRowToVisible(row);
        }
    }

    // ------------------------------------------------------------ hierarchical column

    /// Whether the column at a view index is the one that shows the
    /// tree.
    public boolean isHierarchical(int column) {
        if (cn1Adapter == null || column < 0 || column >= getColumnCount()) {
            return false;
        }
        return convertColumnIndexToModel(column) == cn1Adapter.model.getHierarchicalColumn();
    }

    /// The view index of the column that shows the tree, or -1 when the
    /// model has none or it is hidden.
    public int getHierarchicalColumn() {
        return convertColumnIndexToView(cn1Adapter.model.getHierarchicalColumn());
    }

    /// Sets the renderer of the hierarchical column's cells; `null` is
    /// the default one, which shows the model's value for the node.
    public void setTreeCellRenderer(TreeCellRenderer cellRenderer) {
        cn1Tree.setCellRenderer(cellRenderer);
    }

    /// The renderer the hierarchical column is drawn with: a wrapper
    /// around the one that was set.
    public TreeCellRenderer getTreeCellRenderer() {
        return cn1Tree.getCellRenderer();
    }

    /// The renderer of a cell: in the hierarchical column the tree
    /// table's own, whatever the column has.
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (cn1Renderer != null && isHierarchical(column)) {
            return cn1Renderer;
        }
        return super.getCellRenderer(row, column);
    }

    /// The text the table shows for a cell: in the hierarchical column
    /// what the tree cell renderer says for the node.
    @Override
    public String getStringAt(int row, int column) {
        if (isHierarchical(column)) {
            TreePath path = cn1Path(row);
            if (path != null) {
                return cn1Tree.getStringAt(path);
            }
        }
        return super.getStringAt(row, column);
    }

    private int cn1Step() {
        return CellTheme.touch() ? 28 : 20;
    }

    /// Where a node's own component starts in its hierarchical cell.
    private int cn1LabelX(TreePath path) {
        int depth = path.getPathCount() - 1 - (cn1Tree.isRootVisible() ? 0 : 1)
                + (cn1Tree.getShowsRootHandles() ? 1 : 0);
        return Math.max(0, depth) * cn1Step();
    }

    private boolean cn1Leaf(TreePath path) {
        return cn1Adapter.model.isLeaf(path.getLastPathComponent());
    }

    /// Whether `x`, counted from the left of the hierarchical cell, is
    /// on the node's arrow.
    private boolean cn1OnHandle(TreePath path, int x) {
        int lx = cn1LabelX(path);
        return lx > 0 && x < lx && x >= lx - cn1Step() && !cn1Leaf(path);
    }

    private static boolean cn1Same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /// Makes the component of a hierarchical cell, without highlights.
    private Component cn1Stamp(Object value, boolean selected, boolean focus, int row, int column) {
        TreePath path = cn1Path(row);
        if (path == null) {
            // A prototype, measured outside any row.
            TableCellRenderer plain = getDefaultRenderer(Object.class);
            return plain == null ? null
                    : plain.getTableCellRendererComponent(this, value, selected, focus, row, column);
        }
        Rows tree = cn1Tree;
        if (!cn1Same(tree.getBackground(), getBackground())) {
            tree.setBackground(getBackground());
        }
        if (!cn1Same(tree.getForeground(), getForeground())) {
            tree.setForeground(getForeground());
        }
        if (!cn1Same(tree.getFont(), getFont())) {
            tree.setFont(getFont());
        }
        Object node = path.getLastPathComponent();
        boolean leaf = cn1Leaf(path);
        boolean expanded = tree.isExpanded(path);
        TreeCellRenderer renderer = tree.getCellRenderer();
        Component inner = renderer == null ? null : renderer.getTreeCellRendererComponent(tree, node, selected,
                expanded, leaf, tree.getRowForPath(path), focus);
        if (inner != null && selected) {
            // The row is a table row: it is selected in the table's
            // colors, not the tree's.
            if (getSelectionBackground() != null) {
                inner.setBackground(getSelectionBackground());
            }
            if (getSelectionForeground() != null) {
                inner.setForeground(getSelectionForeground());
            }
        }
        if (cn1Cell == null) {
            cn1Cell = new HierarchicalCell();
        }
        HierarchicalCell cell = cn1Cell;
        int lx = cn1LabelX(path);
        cell.inner = inner;
        cell.labelX = lx;
        cell.step = cn1Step();
        cell.handle = lx > 0 && !leaf;
        cell.expanded = expanded;
        cell.fallback = selected && getSelectionBackground() != null ? getSelectionBackground() : getBackground();
        cell.ink = getForeground();
        return cell;
    }

    /// Asks the renderer for the cell's component and passes it through
    /// the highlighters. In the hierarchical column that is the tree
    /// cell renderer's component, inside the one that draws the indent
    /// and the arrow.
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        if (cn1Renderer == null || renderer != cn1Renderer) {
            return super.prepareRenderer(renderer, row, column);
        }
        boolean focus = isFocusOwner() && getSelectionModel().getLeadSelectionIndex() == row
                && getColumnModel().getSelectionModel().getLeadSelectionIndex() == column;
        Component stamp = cn1Stamp(getValueAt(row, column), isCellSelected(row, column), focus, row, column);
        ComponentAdapter adapter = getComponentAdapter(row, column);
        if (stamp instanceof HierarchicalCell) {
            HierarchicalCell cell = (HierarchicalCell) stamp;
            cell.inner = applyRenderer(cell.inner, adapter);
            return cell;
        }
        return applyRenderer(stamp, adapter);
    }

    /// Passes a component through the table's highlighters and then
    /// those of the adapter's column, and answers what they made of it.
    protected Component applyRenderer(Component componentToUse, ComponentAdapter adapter) {
        Component stamp = componentToUse;
        if (stamp == null || adapter == null) {
            return stamp;
        }
        if (compoundHighlighter != null) {
            stamp = compoundHighlighter.highlight(stamp, adapter);
        }
        TableColumnExt ext = getColumnExt(adapter.column);
        if (ext != null) {
            stamp = ext.cn1Highlight(stamp, adapter);
        }
        return stamp;
    }

    /// The renderer of the hierarchical column, for the code that asks
    /// a renderer directly, as packing a column does.
    private static final class HierarchicalRenderer implements TableCellRenderer {

        private final JXTreeTable table;

        HierarchicalRenderer(JXTreeTable table) {
            this.table = table;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return table.cn1Stamp(value, isSelected, hasFocus, row, column);
        }
    }

    /// Draws a hierarchical cell: the background over the whole cell,
    /// the arrow of a node with children, and the tree cell renderer's
    /// component after the indent.
    private static final class HierarchicalCell extends JComponent {

        private Component inner;
        private int labelX;
        private int step;
        private boolean handle;
        private boolean expanded;
        private Color fallback;
        private Color ink;

        @Override
        public Dimension getPreferredSize() {
            Dimension d = inner == null ? null : inner.getPreferredSize();
            return d == null ? new Dimension(labelX, 0) : new Dimension(d.width + labelX, d.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            Color fill = fallback;
            Color fg = ink;
            if (inner != null) {
                if (inner.isOpaque() && inner.getBackground() != null) {
                    fill = inner.getBackground();
                }
                if (inner.getForeground() != null) {
                    fg = inner.getForeground();
                }
            }
            if (fill != null) {
                g.setColor(fill);
                g.fillRect(0, 0, w, h);
            }
            if (handle) {
                g.setColor(fg == null ? Color.GRAY : fill == null ? fg : CellTheme.mix(fill, fg, 0.65f));
                int cx = labelX - step / 2;
                int cy = h / 2;
                int s = CellTheme.touch() ? 5 : 4;
                int[] xs;
                int[] ys;
                if (expanded) {
                    xs = new int[]{cx - s, cx + s, cx};
                    ys = new int[]{cy - s / 2, cy - s / 2, cy + s - s / 2};
                } else {
                    xs = new int[]{cx - s / 2, cx - s / 2, cx + s - s / 2};
                    ys = new int[]{cy - s, cy + s, cy};
                }
                g.fillPolygon(xs, ys, 3);
            }
            if (inner != null && w > labelX) {
                CellPainter.paint(g, inner, labelX, 0, w - labelX, h);
            }
        }
    }

    // ------------------------------------------------------------ pointer and editing

    /// A press on a node's arrow, or the toggle click count on its
    /// hierarchical cell, expands or collapses the node once the table
    /// has done with the event what it does with any.
    @Override
    protected void processMouseEvent(MouseEvent e) {
        TreePath toggle = null;
        if (cn1Tree != null && isEnabled()) {
            boolean touch = CellTheme.touch();
            if (e.getID() == (touch ? MouseEvent.MOUSE_CLICKED : MouseEvent.MOUSE_PRESSED)
                    && (touch || e.getButton() == MouseEvent.BUTTON1)) {
                toggle = cn1ToggleTarget(e);
            }
        }
        super.processMouseEvent(e);
        if (toggle != null && !isEditing()) {
            if (cn1Tree.isExpanded(toggle)) {
                cn1Tree.collapsePath(toggle);
            } else {
                cn1Tree.expandPath(toggle);
            }
        }
    }

    /// The node a pointer event asks to expand or collapse, or `null`.
    private TreePath cn1ToggleTarget(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int column = columnAtPoint(e.getPoint());
        TreePath path = row >= 0 && column >= 0 && isHierarchical(column) ? cn1Path(row) : null;
        if (path == null) {
            cn1PressPath = null;
            return null;
        }
        // Clicks are counted by time alone, so a quick press on another
        // node arrives as a second click: count from the node's own.
        int clicks = e.getClickCount();
        if (!path.equals(cn1PressPath) || clicks <= 1) {
            cn1ClickBase = clicks - 1;
        }
        cn1PressPath = path;
        if (cn1Leaf(path)) {
            return null;
        }
        Rectangle cell = getCellRect(row, column, false);
        if (cn1OnHandle(path, e.getX() - cell.x)) {
            return path;
        }
        int wanted = cn1Tree.getToggleClickCount();
        return wanted > 0 && clicks - cn1ClickBase == wanted ? path : null;
    }

    /// Starts editing a cell as the table does, except that a press on
    /// a node's arrow never starts an edit and that the editor of a
    /// hierarchical cell is placed after the indent.
    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (cn1Tree != null && e instanceof MouseEvent && row >= 0 && isHierarchical(column)) {
            TreePath path = cn1Path(row);
            if (path != null) {
                Rectangle cell = getCellRect(row, column, false);
                if (cn1OnHandle(path, ((MouseEvent) e).getX() - cell.x)) {
                    return false;
                }
            }
        }
        boolean editing = super.editCellAt(row, column, e);
        if (editing) {
            cn1PlaceEditor();
        }
        return editing;
    }

    /// Moves the editor of a hierarchical cell to after the indent.
    private void cn1PlaceEditor() {
        if (cn1Tree == null || !isEditing()) {
            return;
        }
        int row = getEditingRow();
        int column = getEditingColumn();
        Component editor = getEditorComponent();
        TreePath path = editor != null && isHierarchical(column) ? cn1Path(row) : null;
        if (path == null) {
            return;
        }
        Rectangle r = getCellRect(row, column, false);
        int lx = Math.min(cn1LabelX(path), Math.max(0, r.width - 1));
        editor.setBounds(r.x + lx, r.y, r.width - lx, r.height);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        cn1PlaceEditor();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        super.columnMarginChanged(e);
        cn1PlaceEditor();
    }

    // ------------------------------------------------------------ the tree

    /// The text of a node in the default renderer: the model's value for
    /// it in the hierarchical column.
    private static final class HierarchicalText implements StringValue {

        private final Rows tree;

        HierarchicalText(Rows tree) {
            this.tree = tree;
        }

        @Override
        public String getString(Object value) {
            TreeModel m = tree.getModel();
            if (m instanceof TreeTableModel) {
                TreeTableModel model = (TreeTableModel) m;
                int column = model.getHierarchicalColumn();
                if (column >= 0 && column < model.getColumnCount()) {
                    Object shown = model.getValueAt(value, column);
                    return shown == null ? "" : shown.toString();
                }
            }
            return value == null ? "" : value.toString();
        }
    }

    /// The tree that keeps which nodes are expanded and so which have a
    /// row. It is never shown: the table draws its rows.
    private static final class Rows extends JXTree {

        private TreeTableModelAdapter adapter;
        private boolean live;
        private boolean fixing;

        Rows(TreeTableModel model) {
            super(model);
            super.setRootVisible(false);
            super.setShowsRootHandles(true);
            live = true;
        }

        @Override
        protected TreeCellRenderer createDefaultCellRenderer() {
            return new DefaultTreeRenderer(new HierarchicalText(this));
        }

        /// The rows may have changed. A hidden root that got its first
        /// children is expanded, or they would never show; then the
        /// table is told.
        @Override
        public void treeDidChange() {
            super.treeDidChange();
            if (live && !fixing && !isRootVisible()) {
                TreeModel m = getModel();
                Object root = m == null ? null : m.getRoot();
                if (root != null && !m.isLeaf(root)) {
                    TreePath rootPath = new TreePath(root);
                    if (!isExpanded(rootPath)) {
                        fixing = true;
                        try {
                            expandPath(rootPath);
                        } finally {
                            fixing = false;
                        }
                    }
                }
            }
            if (adapter != null) {
                adapter.rowsChanged();
            }
        }
    }

    // ------------------------------------------------------------ the table model

    /// The table model over the tree: a row for every node that shows.
    /// It follows the tree's rows and tells the table what went and what
    /// came, so that the selection stays with its nodes.
    private static final class TreeTableModelAdapter extends AbstractTableModel implements TreeModelListener {

        private final Rows tree;
        private TreeTableModel model;
        private JXTreeTable table;
        private ArrayList<TreePath> shown;
        private boolean muted;

        TreeTableModelAdapter(Rows tree, TreeTableModel model) {
            this.tree = tree;
            this.model = model;
            shown = snapshot();
            tree.adapter = this;
            model.addTreeModelListener(this);
        }

        private ArrayList<TreePath> snapshot() {
            int n = tree.getRowCount();
            ArrayList<TreePath> out = new ArrayList<TreePath>(n);
            for (int i = 0; i < n; i++) {
                out.add(tree.getPathForRow(i));
            }
            return out;
        }

        void setModel(TreeTableModel newModel) {
            model.removeTreeModelListener(this);
            model = newModel;
            muted = true;
            try {
                tree.setModel(newModel);
            } finally {
                muted = false;
            }
            newModel.addTreeModelListener(this);
            shown = snapshot();
            fireTableStructureChanged();
        }

        private Object node(int row) {
            TreePath path = tree.getPathForRow(row);
            return path == null ? null : path.getLastPathComponent();
        }

        @Override
        public int getRowCount() {
            return tree.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return model.getColumnCount();
        }

        @Override
        public String getColumnName(int column) {
            return model.getColumnName(column);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return model.getColumnClass(column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            Object node = node(row);
            return node == null ? null : model.getValueAt(node, column);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            Object node = node(row);
            return node != null && model.isCellEditable(node, column);
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            Object node = node(row);
            if (node != null) {
                model.setValueAt(value, node, column);
            }
        }

        /// The tree's rows may have changed: works out what went or came
        /// between the rows the table knows and the rows there are now.
        void rowsChanged() {
            if (muted) {
                return;
            }
            ArrayList<TreePath> old = shown;
            ArrayList<TreePath> now = snapshot();
            shown = now;
            if (table != null) {
                table.cn1LostSelection = false;
            }
            int on = old.size();
            int nn = now.size();
            int prefix = 0;
            while (prefix < on && prefix < nn && old.get(prefix).equals(now.get(prefix))) {
                prefix++;
            }
            int suffix = 0;
            while (suffix < on - prefix && suffix < nn - prefix
                    && old.get(on - 1 - suffix).equals(now.get(nn - 1 - suffix))) {
                suffix++;
            }
            if (on == nn && prefix == on) {
                if (nn > 0) {
                    fireTableRowsUpdated(0, nn - 1);
                }
            } else if (prefix + suffix == on) {
                fireTableRowsInserted(prefix, nn - suffix - 1);
            } else if (prefix + suffix == nn) {
                int last = on - suffix - 1;
                if (table != null) {
                    for (int row = prefix; row <= last; row++) {
                        if (table.isRowSelected(row)) {
                            table.cn1LostSelection = true;
                            break;
                        }
                    }
                }
                fireTableRowsDeleted(prefix, last);
            } else {
                fireTableDataChanged();
            }
        }

        /// A node's values changed; the tree's rows did not.
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            int n = tree.getRowCount();
            if (n > 0 && n == shown.size()) {
                fireTableRowsUpdated(0, n - 1);
            }
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
        }

        /// A change at the root may be a change of the columns: the
        /// table makes them again.
        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            TreePath path = e.getTreePath();
            if (path == null || path.getPathCount() == 1) {
                shown = snapshot();
                fireTableStructureChanged();
            }
        }
    }

    // ------------------------------------------------------------ highlighters

    /// The adapter highlighters see a cell through: the table's own,
    /// with what the tree knows of the cell's node.
    @Override
    protected ComponentAdapter getComponentAdapter() {
        if (!(dataAdapter instanceof TreeTableDataAdapter)) {
            dataAdapter = null;
            dataAdapter = new TreeTableDataAdapter(this, super.getComponentAdapter());
        }
        return dataAdapter;
    }

    private static final class TreeTableDataAdapter extends ComponentAdapter {

        private final JXTreeTable table;
        private final ComponentAdapter plain;

        TreeTableDataAdapter(JXTreeTable table, ComponentAdapter plain) {
            super(table);
            this.table = table;
            this.plain = plain;
        }

        /// The table's adapter, pointed at this one's cell.
        private ComponentAdapter cell() {
            plain.row = row;
            plain.column = column;
            return plain;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return plain.getColumnName(columnIndex);
        }

        @Override
        public Object getColumnIdentifierAt(int columnIndex) {
            return plain.getColumnIdentifierAt(columnIndex);
        }

        @Override
        public int getColumnIndex(Object identifier) {
            return plain.getColumnIndex(identifier);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return plain.getColumnClass(column);
        }

        @Override
        public int getColumnCount() {
            return plain.getColumnCount();
        }

        @Override
        public int getRowCount() {
            return plain.getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return plain.getValueAt(row, column);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return plain.isCellEditable(row, column);
        }

        /// The text at a model row and model column: in the hierarchical
        /// column what the tree cell renderer says for the node.
        @Override
        public String getStringAt(int row, int column) {
            TreeTableModel model = table.getTreeTableModel();
            if (model != null && column == model.getHierarchicalColumn()) {
                TreePath path = table.cn1Tree.getPathForRow(row);
                if (path != null) {
                    return table.cn1Tree.getStringAt(path);
                }
            }
            return plain.getStringAt(row, column);
        }

        @Override
        public Rectangle getCellBounds() {
            return cell().getCellBounds();
        }

        @Override
        public boolean hasFocus() {
            return cell().hasFocus();
        }

        @Override
        public boolean isSelected() {
            return cell().isSelected();
        }

        @Override
        public boolean isEditable() {
            return cell().isEditable();
        }

        @Override
        public boolean isExpanded() {
            return table.isExpanded(row);
        }

        @Override
        public boolean isLeaf() {
            TreePath path = table.cn1Path(row);
            return path != null && table.cn1Leaf(path);
        }

        @Override
        public boolean isHierarchical() {
            return table.isHierarchical(column);
        }

        /// How far below the root the cell's node is; the root is at 0.
        @Override
        public int getDepth() {
            TreePath path = table.cn1Path(row);
            return path == null ? 0 : path.getPathCount() - 1;
        }

        @Override
        public int convertColumnIndexToView(int columnModelIndex) {
            return plain.convertColumnIndexToView(columnModelIndex);
        }

        @Override
        public int convertColumnIndexToModel(int columnViewIndex) {
            return plain.convertColumnIndexToModel(columnViewIndex);
        }

        @Override
        public int convertRowIndexToView(int rowModelIndex) {
            return plain.convertRowIndexToView(rowModelIndex);
        }

        @Override
        public int convertRowIndexToModel(int rowViewIndex) {
            return plain.convertRowIndexToModel(rowViewIndex);
        }
    }
}
