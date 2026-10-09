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
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.KeyAdapter;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.KeyListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.event.CellEditorListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeExpansionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeModelEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeModelListener;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionListener;
import com.codename1.desktopcompat.javax.swing.event.TreeWillExpandListener;
import com.codename1.desktopcompat.javax.swing.text.JTextComponent;
import com.codename1.desktopcompat.javax.swing.tree.DefaultMutableTreeNode;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellEditor;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeModel;
import com.codename1.desktopcompat.javax.swing.tree.DefaultTreeSelectionModel;
import com.codename1.desktopcompat.javax.swing.tree.ExpandVetoException;
import com.codename1.desktopcompat.javax.swing.tree.RowMapper;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellEditor;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreeModel;
import com.codename1.desktopcompat.javax.swing.tree.TreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.javax.swing.tree.TreeSelectionModel;
import com.codename1.desktopcompat.rt.CellPainter;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.ListEnumeration;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/// A tree of rows over a [TreeModel], with the JDK's paths, expansion,
/// selection, renderers and editors.
///
/// ## How it is drawn
///
/// The tree paints itself. Every row that meets the clip is painted by
/// the component its [TreeCellRenderer] answers -- by default a label,
/// and so a Codename One label in the theme's colors -- at the row's
/// indent, with an arrow before it for a node that has children. A
/// selected row is tinted across the whole width of the tree.
///
/// ## What the model is asked
///
/// Only the children of expanded nodes. A collapsed node is asked
/// whether it is a leaf, to decide whether it gets an arrow, and for
/// nothing else, so a model that makes its children on demand stays
/// lazy. Expansion is remembered by path and survives changes of the
/// model around it.
///
/// ## What differs from a desktop
///
///  - All rows have one height: the one set with [#setRowHeight(int)],
///    or one that fits the font and, on a touch screen, a finger.
///    [#getRowHeight()] answers the height in use.
///  - On a touch screen a tap selects and a drag scrolls the enclosing
///    scroll pane; the arrow's zone is the whole indent step, which is
///    wider there. With a mouse a press selects.
///  - A press anywhere from the start of a row's text to the right edge
///    of the tree counts as on the row, so [#getPathForLocation] answers
///    the path there too. [#getPathBounds] is the JDK's rectangle: the
///    text's own width.
///  - There are no lines between nodes and no stock icons.
///  - Editing is the plain kind: a text field over the row, started by
///    [#startEditingAtPath], F2 or a third click.
///  - No drag starts from the tree (`setDragEnabled` is kept as a property);
///    tool tips per row and type-ahead search are absent.
public class JTree extends JComponent implements Scrollable {

    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";
    public static final String TREE_MODEL_PROPERTY = "model";
    public static final String ROOT_VISIBLE_PROPERTY = "rootVisible";
    public static final String SHOWS_ROOT_HANDLES_PROPERTY = "showsRootHandles";
    public static final String ROW_HEIGHT_PROPERTY = "rowHeight";
    public static final String CELL_EDITOR_PROPERTY = "cellEditor";
    public static final String EDITABLE_PROPERTY = "editable";
    public static final String LARGE_MODEL_PROPERTY = "largeModel";
    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";
    public static final String VISIBLE_ROW_COUNT_PROPERTY = "visibleRowCount";
    public static final String INVOKES_STOP_CELL_EDITING_PROPERTY = "invokesStopCellEditing";
    public static final String SCROLLS_ON_EXPAND_PROPERTY = "scrollsOnExpand";
    public static final String TOGGLE_CLICK_COUNT_PROPERTY = "toggleClickCount";
    public static final String LEAD_SELECTION_PATH_PROPERTY = "leadSelectionPath";
    public static final String ANCHOR_SELECTION_PATH_PROPERTY = "anchorSelectionPath";
    public static final String EXPANDS_SELECTED_PATHS_PROPERTY = "expandsSelectedPaths";

    protected transient TreeModel treeModel;
    protected transient TreeSelectionModel selectionModel;
    protected boolean rootVisible;
    protected transient TreeCellRenderer cellRenderer;
    protected int rowHeight;
    protected boolean showsRootHandles;
    protected transient TreeCellEditor cellEditor;
    protected boolean editable;
    protected boolean largeModel;
    protected int visibleRowCount;
    protected boolean invokesStopCellEditing;
    protected boolean scrollsOnExpand;
    protected int toggleClickCount;
    protected transient TreeModelListener treeModelListener;

    private final HashMap<TreePath, Boolean> expandedState = new HashMap<TreePath, Boolean>();
    private final TreeSelectionListener selectionRelay = new SelectionRelay();
    private final RowMapper rowMapper = new Rows();
    private ArrayList<TreePath> rows;
    private HashMap<TreePath, Integer> rowIndex;
    private int preferredWidth = -1;
    private boolean rowHeightSet;
    private boolean expandsSelectedPaths = true;
    private TreePath anchorPath;
    private TreePath leadPath;
    private boolean pressTouch;
    private TreePath clickPath;
    private int clickBase;
    private TreePath editingPath;
    private Component editorComp;
    private TreeCellEditor activeEditor;
    private CellEditorListener editorListener;
    private KeyListener editorKeys;

    /// A tree over a small sample model.
    public JTree() {
        this(getDefaultTreeModel());
    }

    public JTree(Object[] value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
        expandRoot();
    }

    public JTree(Vector<?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
        expandRoot();
    }

    public JTree(Hashtable<?, ?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
        expandRoot();
    }

    public JTree(TreeNode root) {
        this(root, false);
    }

    public JTree(TreeNode root, boolean asksAllowsChildren) {
        this(new DefaultTreeModel(root, asksAllowsChildren));
    }

    public JTree(TreeModel newModel) {
        setLayout(null);
        rowHeight = 16;
        visibleRowCount = 20;
        rootVisible = true;
        toggleClickCount = 2;
        scrollsOnExpand = true;
        setOpaque(true);
        setBackground(CellTheme.background("Tree.background"));
        setForeground(CellTheme.foreground("Tree.foreground"));
        setFont(CellTheme.font());
        cellRenderer = new DefaultTreeCellRenderer();
        setSelectionModel(new DefaultTreeSelectionModel());
        setModel(newModel);
        setFocusable(true);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    // ------------------------------------------------------------ models

    private static DefaultMutableTreeNode branch(String name, String[] leaves) {
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(name);
        for (int i = 0; i < leaves.length; i++) {
            parent.add(new DefaultMutableTreeNode(leaves[i]));
        }
        return parent;
    }

    /// A small sample model, for a tree made without one.
    protected static TreeModel getDefaultTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        root.add(branch("colors", new String[]{"blue", "violet", "red", "yellow"}));
        root.add(branch("sports", new String[]{"basketball", "soccer", "football", "hockey"}));
        root.add(branch("food", new String[]{"hot dogs", "pizza", "ravioli", "bananas"}));
        return new DefaultTreeModel(root);
    }

    /// A model over an array, a vector or a hash table: one node per
    /// element, under a root named "root".
    protected static TreeModel createTreeModel(Object value) {
        DefaultMutableTreeNode root;
        if (value instanceof Object[] || value instanceof Hashtable || value instanceof Vector) {
            root = new DefaultMutableTreeNode("root");
            DynamicUtilTreeNode.createChildren(root, value);
        } else {
            root = new DynamicUtilTreeNode("root", value);
        }
        return new DefaultTreeModel(root, false);
    }

    private void expandRoot() {
        TreeModel m = getModel();
        if (m != null && m.getRoot() != null) {
            expandPath(new TreePath(m.getRoot()));
        }
    }

    public TreeModel getModel() {
        return treeModel;
    }

    public void setModel(TreeModel newModel) {
        cancelEditing();
        clearSelection();
        TreeModel old = treeModel;
        if (old != null && treeModelListener != null) {
            old.removeTreeModelListener(treeModelListener);
        }
        treeModel = newModel;
        clearToggledPaths();
        if (newModel != null) {
            if (treeModelListener == null) {
                treeModelListener = createTreeModelListener();
            }
            newModel.addTreeModelListener(treeModelListener);
            Object root = newModel.getRoot();
            if (root != null && !newModel.isLeaf(root)) {
                expandedState.put(new TreePath(root), Boolean.TRUE);
            }
        }
        firePropertyChange(TREE_MODEL_PROPERTY, old, newModel);
        treeDidChange();
    }

    protected TreeModelListener createTreeModelListener() {
        return new ModelChanges();
    }

    public TreeCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(TreeCellRenderer x) {
        TreeCellRenderer old = cellRenderer;
        cellRenderer = x;
        firePropertyChange(CELL_RENDERER_PROPERTY, old, x);
        treeDidChange();
    }

    public void setEditable(boolean flag) {
        boolean old = editable;
        editable = flag;
        firePropertyChange(EDITABLE_PROPERTY, old, flag);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setCellEditor(TreeCellEditor cellEditor) {
        TreeCellEditor old = this.cellEditor;
        cancelEditing();
        this.cellEditor = cellEditor;
        firePropertyChange(CELL_EDITOR_PROPERTY, old, cellEditor);
    }

    public TreeCellEditor getCellEditor() {
        return cellEditor;
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    /// Shows or hides the root's own row. A hidden root is expanded, so
    /// that its children show.
    public void setRootVisible(boolean rootVisible) {
        boolean old = this.rootVisible;
        this.rootVisible = rootVisible;
        if (!rootVisible && treeModel != null && treeModel.getRoot() != null) {
            expandedState.put(new TreePath(treeModel.getRoot()), Boolean.TRUE);
        }
        firePropertyChange(ROOT_VISIBLE_PROPERTY, old, rootVisible);
        treeDidChange();
    }

    /// Whether the nodes of the first level get an arrow and the indent
    /// it needs.
    public void setShowsRootHandles(boolean newValue) {
        boolean old = showsRootHandles;
        showsRootHandles = newValue;
        firePropertyChange(SHOWS_ROOT_HANDLES_PROPERTY, old, newValue);
        treeDidChange();
    }

    public boolean getShowsRootHandles() {
        return showsRootHandles;
    }

    /// Sets the height of every row in logical pixels and stops the tree
    /// from sizing its rows for the device. A height below 1 gives the
    /// sizing back to the tree.
    public void setRowHeight(int rowHeight) {
        int old = this.rowHeight;
        this.rowHeight = rowHeight;
        rowHeightSet = rowHeight > 0;
        firePropertyChange(ROW_HEIGHT_PROPERTY, old, rowHeight);
        treeDidChange();
    }

    /// The height of a row: the one that was set, or else one that fits
    /// the font, and a finger on a touch screen.
    public int getRowHeight() {
        if (rowHeightSet) {
            return rowHeight;
        }
        return Math.max(1, CellTheme.rowHeight(getFont(), 18, 32));
    }

    public boolean isFixedRowHeight() {
        return true;
    }

    public void setLargeModel(boolean newValue) {
        boolean old = largeModel;
        largeModel = newValue;
        firePropertyChange(LARGE_MODEL_PROPERTY, old, newValue);
    }

    public boolean isLargeModel() {
        return largeModel;
    }

    public void setInvokesStopCellEditing(boolean newValue) {
        boolean old = invokesStopCellEditing;
        invokesStopCellEditing = newValue;
        firePropertyChange(INVOKES_STOP_CELL_EDITING_PROPERTY, old, newValue);
    }

    public boolean getInvokesStopCellEditing() {
        return invokesStopCellEditing;
    }

    public void setScrollsOnExpand(boolean newValue) {
        boolean old = scrollsOnExpand;
        scrollsOnExpand = newValue;
        firePropertyChange(SCROLLS_ON_EXPAND_PROPERTY, old, newValue);
    }

    public boolean getScrollsOnExpand() {
        return scrollsOnExpand;
    }

    public void setToggleClickCount(int clickCount) {
        int old = toggleClickCount;
        toggleClickCount = clickCount;
        firePropertyChange(TOGGLE_CLICK_COUNT_PROPERTY, old, clickCount);
    }

    public int getToggleClickCount() {
        return toggleClickCount;
    }

    public void setExpandsSelectedPaths(boolean newValue) {
        boolean old = expandsSelectedPaths;
        expandsSelectedPaths = newValue;
        firePropertyChange(EXPANDS_SELECTED_PATHS_PROPERTY, old, newValue);
    }

    public boolean getExpandsSelectedPaths() {
        return expandsSelectedPaths;
    }

    public void setVisibleRowCount(int newCount) {
        int old = visibleRowCount;
        visibleRowCount = newCount;
        firePropertyChange(VISIBLE_ROW_COUNT_PROPERTY, old, newCount);
    }

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public boolean isPathEditable(TreePath path) {
        return isEditable();
    }

    /// The text shown for a node: its `toString`.
    public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        return value == null ? "" : value.toString();
    }

    // ------------------------------------------------------------ rows

    /// The paths that have a row, top to bottom. Only expanded nodes are
    /// asked for their children.
    private ArrayList<TreePath> rows() {
        if (rows == null) {
            ArrayList<TreePath> out = new ArrayList<TreePath>();
            TreeModel m = treeModel;
            Object root = m == null ? null : m.getRoot();
            if (root != null) {
                TreePath rootPath = new TreePath(root);
                if (rootVisible) {
                    out.add(rootPath);
                }
                if (Boolean.TRUE.equals(expandedState.get(rootPath))) {
                    addChildren(m, rootPath, out);
                }
            }
            rows = out;
            rowIndex = null;
            preferredWidth = -1;
        }
        return rows;
    }

    private void addChildren(TreeModel m, TreePath parent, ArrayList<TreePath> out) {
        Object node = parent.getLastPathComponent();
        int n = m.getChildCount(node);
        for (int i = 0; i < n; i++) {
            Object child = m.getChild(node, i);
            if (child == null) {
                continue;
            }
            TreePath path = parent.pathByAddingChild(child);
            out.add(path);
            if (Boolean.TRUE.equals(expandedState.get(path))) {
                addChildren(m, path, out);
            }
        }
    }

    private HashMap<TreePath, Integer> rowIndex() {
        ArrayList<TreePath> all = rows();
        if (rowIndex == null) {
            HashMap<TreePath, Integer> map = new HashMap<TreePath, Integer>();
            for (int i = 0; i < all.size(); i++) {
                map.put(all.get(i), Integer.valueOf(i));
            }
            rowIndex = map;
        }
        return rowIndex;
    }

    /// Called when the rows may have changed: they are worked out again
    /// when next needed, and the tree is laid out and painted again.
    public void treeDidChange() {
        rows = null;
        rowIndex = null;
        preferredWidth = -1;
        if (selectionModel != null) {
            selectionModel.resetRowSelection();
        }
        if (editorComp != null && editingPath != null) {
            Rectangle r = editorBounds(editingPath);
            if (r == null) {
                cancelEditing();
            } else {
                editorComp.setBounds(r);
            }
        }
        revalidate();
        repaint();
    }

    public int getRowCount() {
        return rows().size();
    }

    public TreePath getPathForRow(int row) {
        ArrayList<TreePath> all = rows();
        return row >= 0 && row < all.size() ? all.get(row) : null;
    }

    public int getRowForPath(TreePath path) {
        if (path == null) {
            return -1;
        }
        Integer at = rowIndex().get(path);
        return at == null ? -1 : at.intValue();
    }

    private int indent() {
        return CellTheme.touch() ? 28 : 20;
    }

    /// How many steps a path's row is indented.
    private int depth(TreePath path) {
        return path.getPathCount() - 1 - (rootVisible ? 0 : 1) + (showsRootHandles ? 1 : 0);
    }

    private int labelX(TreePath path) {
        return Math.max(0, depth(path)) * indent();
    }

    private boolean leaf(TreePath path) {
        return treeModel == null || treeModel.isLeaf(path.getLastPathComponent());
    }

    private Component rendererFor(TreePath path, int row) {
        TreeCellRenderer r = cellRenderer;
        if (r == null) {
            return null;
        }
        boolean lead = isFocusOwner() && path.equals(getLeadSelectionPath());
        return r.getTreeCellRendererComponent(this, path.getLastPathComponent(), isPathSelected(path),
                isExpanded(path), leaf(path), row, lead);
    }

    private int labelWidth(TreePath path, int row) {
        Component c = rendererFor(path, row);
        if (c == null) {
            return 0;
        }
        Dimension d = c.getPreferredSize();
        return d == null ? 0 : d.width;
    }

    /// The rectangle of a path's text in logical pixels: from its indent,
    /// as wide as the renderer wants. `null` for a path without a row.
    public Rectangle getPathBounds(TreePath path) {
        int row = getRowForPath(path);
        if (row < 0) {
            return null;
        }
        int rh = getRowHeight();
        return new Rectangle(labelX(path), row * rh, labelWidth(path, row), rh);
    }

    public Rectangle getRowBounds(int row) {
        return getPathBounds(getPathForRow(row));
    }

    /// Where an editor goes: from the text's start to the tree's right
    /// edge, or the text's width if that is more.
    private Rectangle editorBounds(TreePath path) {
        Rectangle r = getPathBounds(path);
        if (r != null) {
            r.width = Math.max(Math.max(r.width, getWidth() - r.x), 40);
        }
        return r;
    }

    private int rowAtY(int y) {
        int rh = getRowHeight();
        if (y < 0 || rh <= 0) {
            return -1;
        }
        int row = y / rh;
        return row < getRowCount() ? row : -1;
    }

    /// The row at a point in logical pixels, or -1 when the point is
    /// below the last row or before the row's text.
    public int getRowForLocation(int x, int y) {
        int row = rowAtY(y);
        if (row < 0) {
            return -1;
        }
        return x >= labelX(getPathForRow(row)) ? row : -1;
    }

    public TreePath getPathForLocation(int x, int y) {
        return getPathForRow(getRowForLocation(x, y));
    }

    /// The row nearest to a point, whatever its x; -1 for an empty tree.
    public int getClosestRowForLocation(int x, int y) {
        int n = getRowCount();
        int rh = getRowHeight();
        if (n == 0 || rh <= 0) {
            return -1;
        }
        return Math.max(0, Math.min(n - 1, y / rh));
    }

    public TreePath getClosestPathForLocation(int x, int y) {
        return getPathForRow(getClosestRowForLocation(x, y));
    }

    /// Whether a point is on the arrow of a path's row: in the indent
    /// step before the text of a node that has children.
    private boolean onHandle(TreePath path, int x) {
        int lx = labelX(path);
        return lx > 0 && x < lx && x >= lx - indent() && !leaf(path);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        ArrayList<TreePath> all = rows();
        if (preferredWidth < 0) {
            int w = 0;
            for (int i = 0; i < all.size(); i++) {
                TreePath p = all.get(i);
                w = Math.max(w, labelX(p) + labelWidth(p, i));
            }
            preferredWidth = w;
        }
        return new Dimension(preferredWidth, all.size() * getRowHeight());
    }

    // ------------------------------------------------------------ expansion

    public boolean isExpanded(TreePath path) {
        if (path == null) {
            return false;
        }
        for (TreePath p = path; p != null; p = p.getParentPath()) {
            if (!Boolean.TRUE.equals(expandedState.get(p))) {
                return false;
            }
        }
        return true;
    }

    public boolean isExpanded(int row) {
        TreePath path = getPathForRow(row);
        return path != null && Boolean.TRUE.equals(expandedState.get(path));
    }

    public boolean isCollapsed(TreePath path) {
        return !isExpanded(path);
    }

    public boolean isCollapsed(int row) {
        return !isExpanded(row);
    }

    public boolean hasBeenExpanded(TreePath path) {
        return path != null && expandedState.get(path) != null;
    }

    /// The expanded paths at and below `parent`, if it is expanded.
    public Enumeration<TreePath> getExpandedDescendants(TreePath parent) {
        if (!isExpanded(parent)) {
            return null;
        }
        ArrayList<TreePath> out = new ArrayList<TreePath>();
        for (Iterator<TreePath> it = expandedState.keySet().iterator(); it.hasNext();) {
            TreePath p = it.next();
            if (!p.equals(parent) && parent.isDescendant(p) && isExpanded(p)) {
                out.add(p);
            }
        }
        out.add(parent);
        return new ListEnumeration<TreePath>(out, false);
    }

    /// Expands the nodes above a path so that the path has a row.
    public void makeVisible(TreePath path) {
        if (path != null) {
            TreePath parent = path.getParentPath();
            if (parent != null) {
                expandPath(parent);
            }
        }
    }

    public boolean isVisible(TreePath path) {
        if (path != null) {
            TreePath parent = path.getParentPath();
            return parent == null || isExpanded(parent);
        }
        return false;
    }

    public void expandPath(TreePath path) {
        TreeModel m = getModel();
        if (path != null && m != null && !m.isLeaf(path.getLastPathComponent())) {
            setExpandedState(path, true);
        }
    }

    public void expandRow(int row) {
        expandPath(getPathForRow(row));
    }

    public void collapsePath(TreePath path) {
        setExpandedState(path, false);
    }

    public void collapseRow(int row) {
        collapsePath(getPathForRow(row));
    }

    /// Expands or collapses a node, asking the will-expand listeners
    /// first; one that objects stops it. Expanding also expands the nodes
    /// above. Collapsing a node moves a selection below it onto it.
    protected void setExpandedState(TreePath path, boolean state) {
        if (path == null) {
            return;
        }
        boolean opened = false;
        ArrayList<TreePath> above = new ArrayList<TreePath>();
        for (TreePath p = path.getParentPath(); p != null; p = p.getParentPath()) {
            above.add(p);
        }
        for (int i = above.size() - 1; i >= 0; i--) {
            TreePath p = above.get(i);
            if (!Boolean.TRUE.equals(expandedState.get(p))) {
                if (!state) {
                    // Collapsing below a collapsed node: nothing shows it,
                    // the state alone changes.
                    break;
                }
                try {
                    fireTreeWillExpand(p);
                } catch (ExpandVetoException e) {
                    return;
                }
                expandedState.put(p, Boolean.TRUE);
                opened = true;
                fireTreeExpanded(p);
            }
        }
        boolean was = Boolean.TRUE.equals(expandedState.get(path));
        if (state) {
            if (!was) {
                try {
                    fireTreeWillExpand(path);
                } catch (ExpandVetoException e) {
                    treeDidChange();
                    return;
                }
                expandedState.put(path, Boolean.TRUE);
                treeDidChange();
                fireTreeExpanded(path);
            } else if (opened) {
                treeDidChange();
            }
        } else if (was) {
            try {
                fireTreeWillCollapse(path);
            } catch (ExpandVetoException e) {
                return;
            }
            expandedState.put(path, Boolean.FALSE);
            if (editingPath != null && path.isDescendant(editingPath) && !path.equals(editingPath)) {
                cancelEditing();
            }
            treeDidChange();
            if (removeDescendantSelectedPaths(path, false) && !isPathSelected(path)) {
                addSelectionPath(path);
            }
            fireTreeCollapsed(path);
        }
    }

    /// Forgets which nodes are, or ever were, expanded.
    protected void clearToggledPaths() {
        expandedState.clear();
        rows = null;
        rowIndex = null;
    }

    private void forgetExpandedBelow(TreePath parent, boolean includeParent) {
        for (Iterator<TreePath> it = expandedState.keySet().iterator(); it.hasNext();) {
            TreePath p = it.next();
            if (parent.isDescendant(p) && (includeParent || !parent.equals(p))) {
                it.remove();
            }
        }
    }

    /// Takes the selected paths below `path`, and `path` itself if asked,
    /// out of the selection. Answers whether there were any.
    protected boolean removeDescendantSelectedPaths(TreePath path, boolean includePath) {
        TreePath[] selected = getSelectionPaths();
        if (selected == null) {
            return false;
        }
        ArrayList<TreePath> gone = new ArrayList<TreePath>();
        for (int i = 0; i < selected.length; i++) {
            if (path.isDescendant(selected[i]) && (includePath || !path.equals(selected[i]))) {
                gone.add(selected[i]);
            }
        }
        if (gone.isEmpty()) {
            return false;
        }
        getSelectionModel().removeSelectionPaths(gone.toArray(new TreePath[gone.size()]));
        return true;
    }

    public void addTreeExpansionListener(TreeExpansionListener tel) {
        listenerList.add(TreeExpansionListener.class, tel);
    }

    public void removeTreeExpansionListener(TreeExpansionListener tel) {
        listenerList.remove(TreeExpansionListener.class, tel);
    }

    public TreeExpansionListener[] getTreeExpansionListeners() {
        return listenerList.getListeners(TreeExpansionListener.class);
    }

    public void addTreeWillExpandListener(TreeWillExpandListener tel) {
        listenerList.add(TreeWillExpandListener.class, tel);
    }

    public void removeTreeWillExpandListener(TreeWillExpandListener tel) {
        listenerList.remove(TreeWillExpandListener.class, tel);
    }

    public TreeWillExpandListener[] getTreeWillExpandListeners() {
        return listenerList.getListeners(TreeWillExpandListener.class);
    }

    public void fireTreeExpanded(TreePath path) {
        TreeExpansionListener[] ls = getTreeExpansionListeners();
        TreeExpansionEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeExpansionEvent(this, path);
            }
            ls[i].treeExpanded(e);
        }
    }

    public void fireTreeCollapsed(TreePath path) {
        TreeExpansionListener[] ls = getTreeExpansionListeners();
        TreeExpansionEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeExpansionEvent(this, path);
            }
            ls[i].treeCollapsed(e);
        }
    }

    public void fireTreeWillExpand(TreePath path) throws ExpandVetoException {
        TreeWillExpandListener[] ls = getTreeWillExpandListeners();
        TreeExpansionEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeExpansionEvent(this, path);
            }
            ls[i].treeWillExpand(e);
        }
    }

    public void fireTreeWillCollapse(TreePath path) throws ExpandVetoException {
        TreeWillExpandListener[] ls = getTreeWillExpandListeners();
        TreeExpansionEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new TreeExpansionEvent(this, path);
            }
            ls[i].treeWillCollapse(e);
        }
    }

    // ------------------------------------------------------------ selection

    public void setSelectionModel(TreeSelectionModel selectionModel) {
        TreeSelectionModel next = selectionModel != null ? selectionModel : new DefaultTreeSelectionModel();
        TreeSelectionModel old = this.selectionModel;
        if (old != null) {
            old.removeTreeSelectionListener(selectionRelay);
            old.setRowMapper(null);
        }
        this.selectionModel = next;
        next.setRowMapper(rowMapper);
        next.addTreeSelectionListener(selectionRelay);
        firePropertyChange(SELECTION_MODEL_PROPERTY, old, next);
        repaint();
    }

    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionPath(TreePath path) {
        getSelectionModel().setSelectionPath(path);
    }

    public void setSelectionPaths(TreePath[] paths) {
        getSelectionModel().setSelectionPaths(paths);
    }

    public void setLeadSelectionPath(TreePath newPath) {
        TreePath old = leadPath;
        leadPath = newPath;
        firePropertyChange(LEAD_SELECTION_PATH_PROPERTY, old, newPath);
        repaint();
    }

    public void setAnchorSelectionPath(TreePath newPath) {
        TreePath old = anchorPath;
        anchorPath = newPath;
        firePropertyChange(ANCHOR_SELECTION_PATH_PROPERTY, old, newPath);
    }

    public void setSelectionRow(int row) {
        setSelectionRows(new int[]{row});
    }

    private TreePath[] pathsOf(int[] rowsWanted) {
        ArrayList<TreePath> out = new ArrayList<TreePath>();
        if (rowsWanted != null) {
            for (int i = 0; i < rowsWanted.length; i++) {
                TreePath p = getPathForRow(rowsWanted[i]);
                if (p != null) {
                    out.add(p);
                }
            }
        }
        return out.toArray(new TreePath[out.size()]);
    }

    public void setSelectionRows(int[] rows) {
        setSelectionPaths(pathsOf(rows));
    }

    public void addSelectionPath(TreePath path) {
        getSelectionModel().addSelectionPath(path);
    }

    public void addSelectionPaths(TreePath[] paths) {
        getSelectionModel().addSelectionPaths(paths);
    }

    public void addSelectionRow(int row) {
        addSelectionRows(new int[]{row});
    }

    public void addSelectionRows(int[] rows) {
        addSelectionPaths(pathsOf(rows));
    }

    public Object getLastSelectedPathComponent() {
        TreePath path = getSelectionModel().getSelectionPath();
        return path == null ? null : path.getLastPathComponent();
    }

    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    public TreePath getAnchorSelectionPath() {
        return anchorPath;
    }

    public TreePath getSelectionPath() {
        return getSelectionModel().getSelectionPath();
    }

    public TreePath[] getSelectionPaths() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        return paths != null && paths.length > 0 ? paths : null;
    }

    public int[] getSelectionRows() {
        return getSelectionModel().getSelectionRows();
    }

    public int getSelectionCount() {
        return getSelectionModel().getSelectionCount();
    }

    public int getMinSelectionRow() {
        return getSelectionModel().getMinSelectionRow();
    }

    public int getMaxSelectionRow() {
        return getSelectionModel().getMaxSelectionRow();
    }

    public int getLeadSelectionRow() {
        return getRowForPath(getLeadSelectionPath());
    }

    public boolean isPathSelected(TreePath path) {
        return getSelectionModel().isPathSelected(path);
    }

    public boolean isRowSelected(int row) {
        return getSelectionModel().isRowSelected(row);
    }

    /// The paths of the rows from `index0` to `index1`, both included and
    /// in either order.
    protected TreePath[] getPathBetweenRows(int index0, int index1) {
        int n = getRowCount();
        if (n == 0) {
            return null;
        }
        int a = Math.max(0, Math.min(index0, index1));
        int b = Math.min(n - 1, Math.max(index0, index1));
        if (a > b) {
            return null;
        }
        TreePath[] out = new TreePath[b - a + 1];
        for (int i = a; i <= b; i++) {
            out[i - a] = getPathForRow(i);
        }
        return out;
    }

    public void setSelectionInterval(int index0, int index1) {
        getSelectionModel().setSelectionPaths(getPathBetweenRows(index0, index1));
    }

    public void addSelectionInterval(int index0, int index1) {
        TreePath[] paths = getPathBetweenRows(index0, index1);
        if (paths != null && paths.length > 0) {
            getSelectionModel().addSelectionPaths(paths);
        }
    }

    public void removeSelectionInterval(int index0, int index1) {
        TreePath[] paths = getPathBetweenRows(index0, index1);
        if (paths != null && paths.length > 0) {
            getSelectionModel().removeSelectionPaths(paths);
        }
    }

    public void removeSelectionPath(TreePath path) {
        getSelectionModel().removeSelectionPath(path);
    }

    public void removeSelectionPaths(TreePath[] paths) {
        getSelectionModel().removeSelectionPaths(paths);
    }

    public void removeSelectionRow(int row) {
        removeSelectionRows(new int[]{row});
    }

    public void removeSelectionRows(int[] rows) {
        removeSelectionPaths(pathsOf(rows));
    }

    public void clearSelection() {
        if (selectionModel != null) {
            selectionModel.clearSelection();
        }
    }

    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    public void addTreeSelectionListener(TreeSelectionListener tsl) {
        listenerList.add(TreeSelectionListener.class, tsl);
    }

    public void removeTreeSelectionListener(TreeSelectionListener tsl) {
        listenerList.remove(TreeSelectionListener.class, tsl);
    }

    public TreeSelectionListener[] getTreeSelectionListeners() {
        return listenerList.getListeners(TreeSelectionListener.class);
    }

    protected void fireValueChanged(TreeSelectionEvent e) {
        TreeSelectionListener[] ls = getTreeSelectionListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].valueChanged(e);
        }
    }

    /// Passes the selection model's events on as the tree's own, shows
    /// newly selected paths and follows the lead.
    private final class SelectionRelay implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath[] paths = e.getPaths();
            boolean[] added = new boolean[paths.length];
            for (int i = 0; i < paths.length; i++) {
                added[i] = e.isAddedPath(i);
                if (added[i] && expandsSelectedPaths) {
                    makeVisible(paths[i]);
                }
            }
            TreePath lead = e.getNewLeadSelectionPath();
            if (lead == null ? leadPath != null : !lead.equals(leadPath)) {
                setLeadSelectionPath(lead);
            }
            if (isSelectionEmpty()) {
                anchorPath = null;
            } else if (anchorPath == null || getSelectionCount() == 1) {
                anchorPath = lead;
            }
            repaint();
            fireValueChanged(new TreeSelectionEvent(JTree.this, paths, added, e.getOldLeadSelectionPath(), lead));
        }
    }

    private final class Rows implements RowMapper {
        @Override
        public int[] getRowsForPaths(TreePath[] path) {
            if (path == null) {
                return new int[0];
            }
            int[] out = new int[path.length];
            for (int i = 0; i < path.length; i++) {
                out[i] = getRowForPath(path[i]);
            }
            return out;
        }
    }

    // ------------------------------------------------------------ model events

    /// Keeps the rows, the expansion and the selection in step with the
    /// model. Expansion is kept by path, so whatever the change did not
    /// remove stays as it was.
    private final class ModelChanges implements TreeModelListener {

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            preferredWidth = -1;
            revalidate();
            repaint();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            treeDidChange();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            TreePath parent = e.getTreePath();
            Object[] children = e.getChildren();
            if (parent != null && children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (children[i] == null) {
                        continue;
                    }
                    TreePath gone = parent.pathByAddingChild(children[i]);
                    forgetExpandedBelow(gone, true);
                    removeDescendantSelectedPaths(gone, true);
                    if (editingPath != null && gone.isDescendant(editingPath)) {
                        cancelEditing();
                    }
                }
            }
            treeDidChange();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            TreePath parent = e.getTreePath();
            cancelEditing();
            Object root = treeModel == null ? null : treeModel.getRoot();
            if (parent == null || parent.getPathCount() == 1) {
                clearToggledPaths();
                clearSelection();
                if (root != null && (!rootVisible || !treeModel.isLeaf(root))) {
                    expandedState.put(new TreePath(root), Boolean.TRUE);
                }
            } else {
                forgetExpandedBelow(parent, false);
                removeDescendantSelectedPaths(parent, false);
            }
            treeDidChange();
        }
    }

    // ------------------------------------------------------------ scrolling

    /// Scrolls the viewport this tree is in so that the rectangle shows.
    public void scrollRectToVisible(Rectangle aRect) {
        ScrollDelegate.reveal(this, aRect);
    }

    /// Expands what is above a path and scrolls its row into view.
    public void scrollPathToVisible(TreePath path) {
        if (path != null) {
            makeVisible(path);
            Rectangle bounds = getPathBounds(path);
            if (bounds != null) {
                scrollRectToVisible(bounds);
            }
        }
    }

    public void scrollRowToVisible(int row) {
        scrollPathToVisible(getPathForRow(row));
    }

    /// As wide as the rows want and as high as the visible row count.
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(getPreferredSize().width, Math.max(0, visibleRowCount) * getRowHeight());
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? getRowHeight() : 4;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(1, orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        return parent instanceof JViewport && parent.getWidth() > getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        return parent instanceof JViewport && parent.getHeight() > getPreferredSize().height;
    }

    // ------------------------------------------------------------ paint

    private Color rowSelectionColor() {
        if (cellRenderer instanceof DefaultTreeCellRenderer) {
            Color c = ((DefaultTreeCellRenderer) cellRenderer).getBackgroundSelectionColor();
            if (c != null) {
                return c;
            }
        }
        return CellTheme.selectionBackground("Tree.selectionBackground");
    }

    /// Paints the rows that meet the clip: the tint of a selected row,
    /// the arrow of a node with children, and the renderer's component.
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
        ArrayList<TreePath> all = rows();
        int rh = getRowHeight();
        if (all.isEmpty() || rh <= 0 || clip.height <= 0) {
            return;
        }
        int first = Math.max(0, clip.y / rh);
        int last = Math.min(all.size() - 1, (clip.y + clip.height - 1) / rh);
        int step = indent();
        int width = getWidth();
        Color tint = null;
        for (int row = first; row <= last; row++) {
            TreePath path = all.get(row);
            int y = row * rh;
            int lx = labelX(path);
            boolean selected = isPathSelected(path);
            if (selected) {
                if (tint == null) {
                    tint = rowSelectionColor();
                }
                g.setColor(tint);
                g.fillRect(0, y, width, rh);
            }
            if (lx > 0 && !leaf(path)) {
                paintHandle(g, lx - step / 2, y + rh / 2, isExpanded(path), selected);
            }
            if (path.equals(editingPath) && editorComp != null) {
                continue;
            }
            Component c = rendererFor(path, row);
            if (c != null) {
                int w = Math.max(width - lx, c.getPreferredSize().width);
                CellPainter.paint(g, c, lx, y, w, rh);
            }
        }
    }

    /// The arrow of a node with children: pointing down when it is
    /// expanded, towards the text when it is not.
    private void paintHandle(Graphics g, int cx, int cy, boolean expanded, boolean selected) {
        Color fg = getForeground();
        Color bg = getBackground();
        Color c = fg == null ? Color.GRAY : bg == null ? fg : CellTheme.mix(bg, fg, 0.65f);
        g.setColor(c);
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

    // ------------------------------------------------------------ pointer

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (isEnabled()) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    pressTouch = CellTheme.touch();
                    if (overEditor(e)) {
                        break;
                    }
                    if (pressTouch) {
                        ScrollDelegate.forward(this, e);
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        pointed(e);
                    }
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (pressTouch) {
                        ScrollDelegate.forward(this, e);
                    }
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    if (pressTouch && !overEditor(e)) {
                        pointed(e);
                    }
                    break;
                default:
                    break;
            }
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (isEnabled() && pressTouch && e.getID() == MouseEvent.MOUSE_DRAGGED) {
            ScrollDelegate.forward(this, e);
        }
        super.processMouseMotionEvent(e);
    }

    private boolean overEditor(MouseEvent e) {
        return editorComp != null && editorComp.getBounds().contains(e.getX(), e.getY());
    }

    private void toggle(TreePath path) {
        if (Boolean.TRUE.equals(expandedState.get(path))) {
            collapsePath(path);
        } else {
            expandPath(path);
            if (scrollsOnExpand && isExpanded(path)) {
                int row = getRowForPath(path);
                int n = treeModel == null ? 0 : treeModel.getChildCount(path.getLastPathComponent());
                if (row >= 0) {
                    int rh = getRowHeight();
                    scrollRectToVisible(new Rectangle(labelX(path), row * rh, 1, rh * (n + 1)));
                }
            }
        }
    }

    /// What a press with a mouse, or a tap with a finger, does: on an
    /// arrow it expands or collapses, on a row it selects, starts an edit
    /// or -- on the click the toggle count names -- expands or collapses.
    private void pointed(MouseEvent e) {
        int row = rowAtY(e.getY());
        if (isEditing() && !stopEditing()) {
            cancelEditing();
        }
        if (row < 0) {
            requestFocusInWindow();
            return;
        }
        TreePath path = getPathForRow(row);
        if (onHandle(path, e.getX())) {
            toggle(path);
            return;
        }
        if (e.getX() < labelX(path)) {
            return;
        }
        requestFocusInWindow();
        // Clicks are counted by time alone, so a quick press on another
        // row arrives as a second click: count from the row's own first.
        if (!path.equals(clickPath) || e.getClickCount() <= 1) {
            clickBase = e.getClickCount() - 1;
            clickPath = path;
        }
        int clicks = e.getClickCount() - clickBase;
        if (e.isControlDown() || e.isMetaDown()) {
            if (isPathSelected(path)) {
                removeSelectionPath(path);
            } else {
                addSelectionPath(path);
            }
            anchorPath = path;
        } else if (e.isShiftDown() && anchorPath != null && getRowForPath(anchorPath) >= 0) {
            TreePath anchor = anchorPath;
            setSelectionInterval(getRowForPath(anchor), row);
            anchorPath = anchor;
            setLeadSelectionPath(path);
        } else {
            setSelectionPath(path);
            anchorPath = path;
        }
        if (isEditable() && isPathEditable(path)) {
            TreeCellEditor editor = editorInUse();
            MouseEvent counted = clicks == e.getClickCount() ? e : new MouseEvent(this, e.getID(), e.getWhen(),
                    e.getModifiers(), e.getX(), e.getY(), clicks, e.isPopupTrigger(), e.getButton());
            if (editor.isCellEditable(counted)) {
                startEditingAtPath(path);
                return;
            }
        }
        if (toggleClickCount > 0 && clicks == toggleClickCount && !leaf(path)) {
            toggle(path);
        }
    }

    // ------------------------------------------------------------ keys

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (isEnabled() && !e.isConsumed() && e.getID() == KeyEvent.KEY_PRESSED) {
            keyPressed(e);
        }
        super.processKeyEvent(e);
    }

    private void lead(int row, boolean extend) {
        int n = getRowCount();
        if (n == 0) {
            return;
        }
        int at = Math.max(0, Math.min(row, n - 1));
        TreePath path = getPathForRow(at);
        if (extend && anchorPath != null && getRowForPath(anchorPath) >= 0) {
            TreePath anchor = anchorPath;
            setSelectionInterval(getRowForPath(anchor), at);
            anchorPath = anchor;
            setLeadSelectionPath(path);
        } else {
            setSelectionPath(path);
            anchorPath = path;
        }
        scrollPathToVisible(path);
    }

    private void keyPressed(KeyEvent e) {
        int row = getLeadSelectionRow();
        TreePath path = getPathForRow(row);
        boolean shift = e.isShiftDown();
        int page = 1;
        Container p = getParent();
        int rh = getRowHeight();
        if (rh > 0) {
            page = Math.max(1, (p instanceof JViewport ? p.getHeight() : getHeight()) / rh - 1);
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                lead(row < 0 ? 0 : row - 1, shift);
                break;
            case KeyEvent.VK_DOWN:
                lead(row + 1, shift);
                break;
            case KeyEvent.VK_HOME:
                lead(0, shift);
                break;
            case KeyEvent.VK_END:
                lead(getRowCount() - 1, shift);
                break;
            case KeyEvent.VK_PAGE_UP:
                lead(Math.max(0, row - page), shift);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                lead(row + page, shift);
                break;
            case KeyEvent.VK_RIGHT:
                if (path != null && !leaf(path)) {
                    if (Boolean.TRUE.equals(expandedState.get(path))) {
                        lead(row + 1, false);
                    } else {
                        expandPath(path);
                    }
                }
                break;
            case KeyEvent.VK_LEFT:
                if (path != null) {
                    if (!leaf(path) && Boolean.TRUE.equals(expandedState.get(path))) {
                        collapsePath(path);
                    } else {
                        int parent = getRowForPath(path.getParentPath());
                        if (parent >= 0) {
                            lead(parent, false);
                        }
                    }
                }
                break;
            case KeyEvent.VK_ENTER:
                if (path != null && !leaf(path)) {
                    toggle(path);
                }
                break;
            case KeyEvent.VK_F2:
                if (path != null) {
                    startEditingAtPath(path);
                }
                break;
            default:
                return;
        }
        e.consume();
    }

    // ------------------------------------------------------------ editing

    private TreeCellEditor editorInUse() {
        if (cellEditor == null) {
            DefaultTreeCellRenderer r = cellRenderer instanceof DefaultTreeCellRenderer
                    ? (DefaultTreeCellRenderer) cellRenderer : null;
            cellEditor = new DefaultTreeCellEditor(this, r);
        }
        return cellEditor;
    }

    public boolean isEditing() {
        return editingPath != null;
    }

    public TreePath getEditingPath() {
        return editingPath;
    }

    /// Ends an edit and stores its value in the model. Answers whether
    /// there was an edit and it ended.
    public boolean stopEditing() {
        TreeCellEditor editor = activeEditor;
        return editor != null && editor.stopCellEditing();
    }

    /// Ends an edit without storing anything.
    public void cancelEditing() {
        TreeCellEditor editor = activeEditor;
        if (editor != null) {
            editor.cancelCellEditing();
        }
        // An editor that does not answer still must not stay on the tree.
        finishEditing(false);
    }

    /// Starts editing a node, if the tree is editable and the node has a
    /// row: the editor's component is placed over the row as a child of
    /// the tree.
    public void startEditingAtPath(TreePath path) {
        if (path == null || !isEditable() || !isPathEditable(path)) {
            return;
        }
        if (isEditing() && !stopEditing()) {
            cancelEditing();
        }
        makeVisible(path);
        int row = getRowForPath(path);
        TreeCellEditor editor = editorInUse();
        if (row < 0) {
            return;
        }
        Component c = editor.getTreeCellEditorComponent(this, path.getLastPathComponent(), isPathSelected(path),
                isExpanded(path), leaf(path), row);
        Rectangle bounds = editorBounds(path);
        if (c == null || bounds == null) {
            return;
        }
        editingPath = path;
        editorComp = c;
        activeEditor = editor;
        c.setBounds(bounds);
        add(c);
        if (editorListener == null) {
            editorListener = new EditorEnds();
        }
        editor.addCellEditorListener(editorListener);
        if (editorKeys == null) {
            editorKeys = new EditorKeys();
        }
        c.addKeyListener(editorKeys);
        revalidate();
        repaint();
        c.requestFocus();
        com.codename1.ui.Component peer = c.cn1PeerOrNull();
        if (c instanceof JTextComponent && peer instanceof com.codename1.ui.TextArea
                && peer.getComponentForm() != null && CellTheme.touch()) {
            ((com.codename1.ui.TextArea) peer).startEditingAsync();
        }
    }

    private void finishEditing(boolean store) {
        TreeCellEditor editor = activeEditor;
        TreePath path = editingPath;
        Component c = editorComp;
        if (editor == null && c == null) {
            return;
        }
        Object value = store && editor != null ? editor.getCellEditorValue() : null;
        activeEditor = null;
        editingPath = null;
        editorComp = null;
        if (editor != null && editorListener != null) {
            editor.removeCellEditorListener(editorListener);
        }
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
        if (store && path != null && treeModel != null) {
            treeModel.valueForPathChanged(path, value);
        }
        preferredWidth = -1;
        revalidate();
        repaint();
    }

    private final class EditorEnds implements CellEditorListener {
        @Override
        public void editingStopped(ChangeEvent e) {
            finishEditing(true);
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
            finishEditing(false);
        }
    }

    /// Escape in the editor's component cancels; Enter is the text
    /// field's own action and stops the edit through the editor.
    private final class EditorKeys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!isEditing()) {
                return;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                cancelEditing();
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                stopEditing();
                e.consume();
            }
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",rootVisible=" + rootVisible + ",rowHeight=" + getRowHeight()
                + ",showsRootHandles=" + showsRootHandles + ",scale=" + Units.scale();
    }

    // ------------------------------------------------------------ nodes

    /// A node over an array, a vector or a hash table that makes its
    /// children from the elements the first time it is asked for them.
    public static class DynamicUtilTreeNode extends DefaultMutableTreeNode {

        protected Object childValue;
        protected boolean loadedChildren;

        /// Adds a node per element of `children` to `parent`, if it is an
        /// array, a vector or a hash table; the keys of a table name its
        /// nodes and the values are their children.
        public static void createChildren(DefaultMutableTreeNode parent, Object children) {
            if (children instanceof Vector) {
                Vector<?> v = (Vector<?>) children;
                for (int i = 0; i < v.size(); i++) {
                    parent.add(new DynamicUtilTreeNode(v.elementAt(i), v.elementAt(i)));
                }
            } else if (children instanceof Hashtable) {
                Hashtable<?, ?> t = (Hashtable<?, ?>) children;
                for (Enumeration<?> keys = t.keys(); keys.hasMoreElements();) {
                    Object key = keys.nextElement();
                    parent.add(new DynamicUtilTreeNode(key, t.get(key)));
                }
            } else if (children instanceof Object[]) {
                Object[] a = (Object[]) children;
                for (int i = 0; i < a.length; i++) {
                    parent.add(new DynamicUtilTreeNode(a[i], a[i]));
                }
            }
        }

        public DynamicUtilTreeNode(Object value, Object children) {
            super(value);
            loadedChildren = false;
            childValue = children;
            boolean container = children instanceof Vector || children instanceof Hashtable
                    || children instanceof Object[];
            setAllowsChildren(container);
        }

        @Override
        public boolean isLeaf() {
            return !getAllowsChildren();
        }

        @Override
        public int getChildCount() {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.getChildCount();
        }

        protected void loadChildren() {
            loadedChildren = true;
            createChildren(this, childValue);
        }

        @Override
        public TreeNode getChildAt(int index) {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.getChildAt(index);
        }

        @Override
        public Enumeration children() {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.children();
        }
    }

    // ------------------------------------------------------------ drag

    /// Records whether dragging out of the component is wanted. The layer
    /// starts no drag of its own, so this is a property and nothing more.
    public void setDragEnabled(boolean b) {
        cn1DragEnabled = b;
    }

    public boolean getDragEnabled() {
        return cn1DragEnabled;
    }

    private boolean cn1DragEnabled;
}
