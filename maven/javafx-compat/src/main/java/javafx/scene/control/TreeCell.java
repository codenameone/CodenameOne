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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/// A cell of a [TreeView]. An application subclasses it and overrides
/// `updateItem` to decide what a row looks like; the item is the value of
/// the [TreeItem] of the row, which [#getTreeItem()] answers.
///
/// Before its graphic and text a cell keeps the indent of its depth and,
/// for an item with children, the disclosure node: an arrow that points
/// right while the item is closed and down while it is open. The
/// pseudo-class states `expanded` and `collapsed` follow the item.
public class TreeCell<T> extends IndexedCell<T> {

    private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass COLLAPSED = PseudoClass.getPseudoClass("collapsed");

    /// The width kept for one level of depth, and for the arrow.
    static final double INDENT = 18;

    private final ReadOnlyObjectWrapper<TreeItem<T>> treeItem = new ReadOnlyObjectWrapper<TreeItem<T>>(this,
            "treeItem");
    private final ReadOnlyObjectWrapper<TreeView<T>> treeView = new ReadOnlyObjectWrapper<TreeView<T>>(this,
            "treeView");
    private final ObjectProperty<Node> disclosureNode = new SimpleObjectProperty<Node>(this, "disclosureNode");
    private final Polygon arrow = new Polygon();
    private boolean stale = true;
    private boolean open;
    private boolean branch;
    private int level;

    /// Creates a cell that belongs to no tree yet.
    public TreeCell() {
        getStyleClass().add("tree-cell");
        disclosureNode.addListener((observable, was, now) -> {
            if (was != null) {
                cn1Children().remove(was);
            }
            if (now != null && !cn1Children().contains(now)) {
                cn1Children().add(now);
            }
            requestLayout();
        });
        arrow.setFill(Color.gray(0.3));
        arrow.getStyleClass().add("arrow");
        StackPane node = new StackPane(arrow);
        node.getStyleClass().add("tree-disclosure-node");
        node.setMouseTransparent(true);
        setDisclosureNode(node);
        shape();
    }

    private void shape() {
        if (open) {
            arrow.getPoints().setAll(Double.valueOf(0), Double.valueOf(0), Double.valueOf(8), Double.valueOf(0),
                    Double.valueOf(4), Double.valueOf(5));
        } else {
            arrow.getPoints().setAll(Double.valueOf(0), Double.valueOf(0), Double.valueOf(5), Double.valueOf(4),
                    Double.valueOf(0), Double.valueOf(8));
        }
    }

    @Override
    double leading() {
        return (level + 1) * INDENT;
    }

    /// Where the arrow area of this cell starts, from its left edge.
    final double arrowStart() {
        return getInsets().getLeft() + level * INDENT;
    }

    /// Whether the cell shows an item that has children.
    final boolean isBranch() {
        return branch;
    }

    /// Makes the next update show the item again even if it is the same.
    final void markStale() {
        stale = true;
    }

    @Override
    void indexChanged(int oldIndex, int newIndex) {
        TreeView<T> view = getTreeView();
        TreeItem<T> item = view == null ? null : view.getTreeItem(newIndex);
        TreeItem<T> before = getTreeItem();
        if (item != null) {
            boolean other = before != item;
            updateTreeItem(item);
            T value = item.getValue();
            if (stale || other || isEmpty() || isItemChanged(getItem(), value)) {
                updateItem(value, false);
            }
        } else {
            updateTreeItem(null);
            if (stale || !isEmpty()) {
                updateItem(null, true);
            }
        }
        stale = false;
        boolean nowBranch = item != null && !item.isLeaf();
        boolean nowOpen = nowBranch && item.isExpanded();
        int nowLevel = item == null || view == null ? 0 : Math.max(0, view.getTreeItemLevel(item)
                - (view.isShowRoot() ? 0 : 1));
        if (nowBranch != branch || nowOpen != open || nowLevel != level) {
            branch = nowBranch;
            open = nowOpen;
            level = nowLevel;
            shape();
            cn1Invalidated(Dirty.NATIVE);
            requestLayout();
        }
        pseudoClassStateChanged(EXPANDED, nowOpen);
        pseudoClassStateChanged(COLLAPSED, nowBranch && !nowOpen);
        MultipleSelectionModel<TreeItem<T>> model = view == null ? null : view.getSelectionModel();
        updateSelected(item != null && model != null && model.isSelected(newIndex));
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        Node node = getDisclosureNode();
        if (node != null) {
            node.setVisible(branch);
            if (branch) {
                Insets in = getInsets();
                double h = getHeight() - in.getTop() - in.getBottom();
                double w = node.prefWidth(-1);
                double nh = node.prefHeight(-1);
                if (node.isResizable()) {
                    node.resize(w, nh);
                }
                node.relocate(arrowStart() + (INDENT - w) / 2 - node.getLayoutBounds().getMinX(),
                        in.getTop() + (h - nh) / 2 - node.getLayoutBounds().getMinY());
            }
        }
    }

    /// Returns the item of the tree this cell shows, or `null`.
    public final TreeItem<T> getTreeItem() {
        return treeItem.get();
    }

    /// The item of the tree this cell shows.
    public final ReadOnlyObjectProperty<TreeItem<T>> treeItemProperty() {
        return treeItem.getReadOnlyProperty();
    }

    /// Tells the cell which item it shows; the tree does.
    public final void updateTreeItem(TreeItem<T> item) {
        treeItem.set(item);
    }

    /// Returns the tree this cell belongs to.
    public final TreeView<T> getTreeView() {
        return treeView.get();
    }

    /// The tree this cell belongs to.
    public final ReadOnlyObjectProperty<TreeView<T>> treeViewProperty() {
        return treeView.getReadOnlyProperty();
    }

    /// Tells the cell which tree it belongs to; the tree does.
    public final void updateTreeView(TreeView<T> tree) {
        treeView.set(tree);
    }

    /// Returns the node that opens and closes an item with children.
    public final Node getDisclosureNode() {
        return disclosureNode.get();
    }

    /// Sets the node that opens and closes an item with children.
    public final void setDisclosureNode(Node value) {
        disclosureNode.set(value);
    }

    /// The node that opens and closes an item with children.
    public final ObjectProperty<Node> disclosureNodeProperty() {
        return disclosureNode;
    }

    /// Ends the edit with a new value, which becomes the value of the
    /// item of the row.
    @Override
    public void commitEdit(T newValue) {
        boolean was = isEditing();
        super.commitEdit(newValue);
        TreeItem<T> item = getTreeItem();
        if (was && item != null) {
            item.setValue(newValue);
        }
    }
}
