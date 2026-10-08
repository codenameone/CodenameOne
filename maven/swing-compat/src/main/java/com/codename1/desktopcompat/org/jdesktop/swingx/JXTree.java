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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellRenderer;
import com.codename1.desktopcompat.javax.swing.tree.TreeModel;
import com.codename1.desktopcompat.javax.swing.tree.TreeNode;
import com.codename1.desktopcompat.javax.swing.tree.TreePath;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.ComponentAdapter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.CompoundHighlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.decorator.Highlighter;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.DefaultTreeRenderer;
import com.codename1.desktopcompat.org.jdesktop.swingx.renderer.StringValue;
import com.codename1.desktopcompat.org.jdesktop.swingx.rollover.RolloverProducer;
import java.util.Hashtable;
import java.util.Vector;

/// A tree with highlighters, rollover, and expanding or collapsing
/// everything at once.
///
/// The renderer set is wrapped: [#getCellRenderer()] answers the wrapper
/// that applies the highlighters and [#getWrappedCellRenderer()] the
/// renderer that was set. The default is a SwingX renderer, whose label
/// takes any color a highlighter gives it. In a highlight predicate a
/// node has a depth, the root's being 0, and is a leaf or a folder.
///
/// Searching, the icon setters, the selection colors and the look and
/// feel hooks are absent.
public class JXTree extends JTree {

    protected CompoundHighlighter compoundHighlighter;
    protected ComponentAdapter dataAdapter;

    private boolean cn1Ready;
    private DelegatingRenderer cn1Renderer;
    private ChangeListener cn1HighlighterListener;
    private boolean cn1Rollover;
    private RolloverProducer cn1RolloverProducer;

    public JXTree() {
        super();
        cn1Init();
    }

    public JXTree(Object[] value) {
        super(value);
        cn1Init();
    }

    public JXTree(Vector<?> value) {
        super(value);
        cn1Init();
    }

    public JXTree(Hashtable<?, ?> value) {
        super(value);
        cn1Init();
    }

    public JXTree(TreeNode root) {
        super(root);
        cn1Init();
    }

    public JXTree(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
        cn1Init();
    }

    public JXTree(TreeModel newModel) {
        super(newModel);
        cn1Init();
    }

    private void cn1Init() {
        cn1Ready = true;
        setCellRenderer(createDefaultCellRenderer());
    }

    // ------------------------------------------------------------ rows

    /// The text the tree shows for a row, or `null` for no such row.
    public String getStringAt(int row) {
        return getStringAt(getPathForRow(row));
    }

    /// The text the tree shows for the last node of a path.
    public String getStringAt(TreePath path) {
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        TreeCellRenderer renderer = getWrappedCellRenderer();
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(node);
        }
        TreeModel m = getModel();
        return convertValueToText(node, isPathSelected(path), isExpanded(path), m != null && m.isLeaf(node),
                getRowForPath(path), false);
    }

    /// Collapses every row, from the last up.
    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0; i--) {
            collapseRow(i);
        }
    }

    /// Expands every row, including those that expanding brings up.
    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    // ------------------------------------------------------------ rollover

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
                int row = getClosestRowForLocation(mousePoint.x, mousePoint.y);
                if (row >= 0) {
                    Rectangle bounds = getRowBounds(row);
                    if (bounds == null || mousePoint.y < bounds.y || mousePoint.y >= bounds.y + bounds.height) {
                        row = -1;
                    }
                }
                rollover.setLocation(row >= 0 ? 0 : -1, row);
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

    // ------------------------------------------------------------ highlighters

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

    protected ChangeListener createHighlighterChangeListener() {
        return new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repaint();
            }
        };
    }

    // ------------------------------------------------------------ renderer

    protected TreeCellRenderer createDefaultCellRenderer() {
        return new DefaultTreeRenderer();
    }

    /// The renderer the tree paints with: a wrapper that applies the
    /// highlighters to what the renderer that was set answers.
    @Override
    public TreeCellRenderer getCellRenderer() {
        return cn1Renderer != null ? cn1Renderer : super.getCellRenderer();
    }

    /// The renderer that was set.
    public TreeCellRenderer getWrappedCellRenderer() {
        return cn1Renderer != null ? cn1Renderer.delegate : super.getCellRenderer();
    }

    /// Sets the renderer; `null` is the default one.
    @Override
    public void setCellRenderer(TreeCellRenderer renderer) {
        if (!cn1Ready) {
            super.setCellRenderer(renderer);
            return;
        }
        TreeCellRenderer delegate = renderer != null ? renderer : createDefaultCellRenderer();
        DelegatingRenderer wrapper = new DelegatingRenderer();
        wrapper.delegate = delegate;
        cn1Renderer = wrapper;
        super.setCellRenderer(wrapper);
    }

    private final class DelegatingRenderer implements TreeCellRenderer {
        private TreeCellRenderer delegate;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            Component stamp = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row,
                    hasFocus);
            if (stamp != null && compoundHighlighter != null && row >= 0) {
                stamp = compoundHighlighter.highlight(stamp, getComponentAdapter(row));
            }
            return stamp;
        }
    }

    // ------------------------------------------------------------ adapter

    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new TreeAdapter(this);
        }
        return dataAdapter;
    }

    /// The adapter, pointed at a row.
    protected ComponentAdapter getComponentAdapter(int index) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.column = 0;
        adapter.row = index;
        return adapter;
    }

    private static final class TreeAdapter extends ComponentAdapter {

        private final JXTree tree;

        TreeAdapter(JXTree tree) {
            super(tree);
            this.tree = tree;
        }

        private TreePath path(int row) {
            return row >= 0 && row < tree.getRowCount() ? tree.getPathForRow(row) : null;
        }

        @Override
        public int getRowCount() {
            return tree.getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column) {
            TreePath path = path(row);
            return path != null ? path.getLastPathComponent() : null;
        }

        @Override
        public String getStringAt(int row, int column) {
            String s = tree.getStringAt(row);
            return s != null ? s : "";
        }

        @Override
        public Rectangle getCellBounds() {
            return tree.getRowBounds(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            TreePath path = path(row);
            return path != null && tree.isPathEditable(path);
        }

        @Override
        public boolean isEditable() {
            return isCellEditable(row, column);
        }

        @Override
        public boolean isSelected() {
            return tree.isRowSelected(row);
        }

        @Override
        public boolean hasFocus() {
            return tree.isFocusOwner() && tree.getLeadSelectionRow() == row;
        }

        @Override
        public boolean isExpanded() {
            return tree.isExpanded(row);
        }

        @Override
        public int getDepth() {
            TreePath path = path(row);
            return path != null ? path.getPathCount() - 1 : 0;
        }

        @Override
        public boolean isHierarchical() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            TreePath path = path(row);
            TreeModel m = tree.getModel();
            return path != null && m != null && m.isLeaf(path.getLastPathComponent());
        }
    }
}
