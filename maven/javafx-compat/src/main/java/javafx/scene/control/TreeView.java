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

import java.util.ArrayList;
import java.util.List;

import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Callback;

/// A tree of [TreeItem]s shown as rows: the root, and under every open
/// item its children, each indented by its depth.
///
/// A press on the arrow of a row opens or closes its item, and so does a
/// double click anywhere on the row; any other press selects the row. The
/// rows are the items that can be seen -- an item is one while every item
/// above it is open -- and the selection model counts in those rows, so
/// an index is a row, as it is in JavaFX. Closing an item deselects what
/// was selected under it.
///
/// A cell factory makes the [TreeCell]s; without one a cell shows the
/// value's `toString()` after the graphic of its item, or the value
/// itself when it is a node. Rows are all one height, the height of the
/// first cell or [#setFixedCellSize(double)].
///
/// There is no focus model and no editing by the tree itself: a cell
/// that edits commits into its item.
public class TreeView<T> extends Control {

    private final ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<TreeItem<T>>(this, "root");
    private final BooleanProperty showRoot = new SimpleBooleanProperty(this, "showRoot", true);
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    private final ObjectProperty<Callback<TreeView<T>, TreeCell<T>>> cellFactory =
            new SimpleObjectProperty<Callback<TreeView<T>, TreeCell<T>>>(this, "cellFactory");
    private final ObjectProperty<MultipleSelectionModel<TreeItem<T>>> selectionModel =
            new SimpleObjectProperty<MultipleSelectionModel<TreeItem<T>>>(this, "selectionModel");
    private final DoubleProperty fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize",
            Region.USE_COMPUTED_SIZE);
    private final ReadOnlyIntegerWrapper expandedItemCount = new ReadOnlyIntegerWrapper(this, "expandedItemCount",
            0);
    private final ObservableList<TreeItem<T>> rows = FXCollections.observableArrayList();
    private final RowFlow flow;
    private int anchor = -1;
    private final EventHandler<TreeItem.TreeModificationEvent<T>> treeChanged = event -> {
        if (event.getEventType() == TreeItem.valueChangedEvent()
                || event.getEventType() == TreeItem.graphicChangedEvent()) {
            refresh();
        } else {
            rowsChanged();
        }
    };
    private final ListChangeListener<Integer> selectionListener = change -> syncSelection();

    /// Creates a tree with no root.
    public TreeView() {
        this(null);
    }

    /// Creates a tree of a root item.
    public TreeView(TreeItem<T> root) {
        getStyleClass().add("tree-view");
        flow = new RowFlow(new FlowRows());
        cn1Children().add(flow);
        this.root.addListener((observable, was, now) -> {
            if (was != null) {
                was.removeEventHandler(TreeItem.<T>treeNotificationEvent(), treeChanged);
            }
            if (now != null) {
                now.addEventHandler(TreeItem.<T>treeNotificationEvent(), treeChanged);
            }
            flow.scrollTo(0);
            rowsChanged();
        });
        showRoot.addListener((observable, was, now) -> rowsChanged());
        cellFactory.addListener((observable, was, now) -> flow.rebuild());
        fixedCellSize.addListener((observable, was, now) -> flow.rowsChanged());
        selectionModel.addListener((observable, was, now) -> {
            if (was != null) {
                was.getSelectedIndices().removeListener(selectionListener);
            }
            if (now != null) {
                now.getSelectedIndices().addListener(selectionListener);
            }
            syncSelection();
        });
        flow.addEventHandler(MouseEvent.MOUSE_PRESSED, this::pressed);
        selectionModel.set(new Model<T>(this));
        this.root.set(root);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public String cn1DefaultStyle() {
        return "-fx-background-color: -fx-box-border, -fx-control-inner-background; -fx-background-insets: 0, 1;"
                + " -fx-padding: 1;";
    }

    private void collect(TreeItem<T> item, List<TreeItem<T>> into, int depth) {
        // A tree that holds itself must not hang the walk.
        if (item == null || depth > 1000) {
            return;
        }
        into.add(item);
        if (item.isExpanded()) {
            List<TreeItem<T>> under = item.getChildren();
            for (int i = 0; i < under.size(); i++) {
                collect(under.get(i), into, depth + 1);
            }
        }
    }

    /// Works out the rows again and keeps selected what is still a row.
    private void rowsChanged() {
        ArrayList<TreeItem<T>> next = new ArrayList<TreeItem<T>>();
        TreeItem<T> top = getRoot();
        if (top != null) {
            if (isShowRoot()) {
                collect(top, next, 0);
            } else {
                List<TreeItem<T>> under = top.getChildren();
                for (int i = 0; i < under.size(); i++) {
                    collect(under.get(i), next, 1);
                }
            }
        }
        MultipleSelectionModel<TreeItem<T>> model = getSelectionModel();
        ArrayList<TreeItem<T>> kept = new ArrayList<TreeItem<T>>();
        if (model != null) {
            kept.addAll(model.getSelectedItems());
        }
        rows.setAll(next);
        if (model instanceof Model) {
            ((Model<T>) model).selection.rowsReplaced();
        }
        if (model != null) {
            for (int i = 0; i < kept.size(); i++) {
                int at = rows.indexOf(kept.get(i));
                if (at >= 0) {
                    model.select(at);
                }
            }
        }
        anchor = -1;
        expandedItemCount.set(rows.size());
        flow.rowsChanged();
        flow.refresh();
        requestLayout();
    }

    private void syncSelection() {
        MultipleSelectionModel<TreeItem<T>> model = getSelectionModel();
        ArrayList<IndexedCell<?>[]> slots = flow.slots();
        for (int i = 0; i < slots.size(); i++) {
            IndexedCell<?>[] slot = slots.get(i);
            for (int c = 0; c < slot.length; c++) {
                int row = slot[c].getIndex();
                slot[c].updateSelected(model != null && row >= 0 && model.isSelected(row));
            }
        }
    }

    private void pressed(MouseEvent event) {
        int row = flow.rowAt(event);
        TreeItem<T> item = getTreeItem(row);
        if (item == null) {
            return;
        }
        IndexedCell<?> cell = flow.cell(row, 0);
        if (!item.isLeaf() && cell instanceof TreeCell) {
            TreeCell<?> shown = (TreeCell<?>) cell;
            Point2D p = shown.sceneToLocal(event.getSceneX(), event.getSceneY());
            double from = shown.arrowStart();
            boolean onArrow = p.getX() >= from && p.getX() < from + TreeCell.INDENT;
            if (onArrow || event.getClickCount() == 2) {
                item.setExpanded(!item.isExpanded());
                if (onArrow) {
                    return;
                }
            }
        }
        MultipleSelectionModel<TreeItem<T>> model = getSelectionModel();
        // The rows may have changed under the press.
        row = rows.indexOf(item);
        if (model != null && row >= 0) {
            anchor = RowPress.apply(model, event.getButton() == MouseButton.PRIMARY, event.isShortcutDown(),
                    event.isShiftDown(), row, anchor);
        }
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        flow.resizeRelocate(in.getLeft(), in.getTop(), Math.max(0, getWidth() - in.getLeft() - in.getRight()),
                Math.max(0, getHeight() - in.getTop() - in.getBottom()));
    }

    @Override
    protected double computePrefWidth(double height) {
        return 248;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 400;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /// Returns the item at a row, or `null` when there is no such row.
    public TreeItem<T> getTreeItem(int row) {
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    /// Returns the row of an item, -1 when it cannot be seen.
    public int getRow(TreeItem<T> item) {
        return rows.indexOf(item);
    }

    /// Returns how many items are above an item, none for the root.
    public int getTreeItemLevel(TreeItem<?> node) {
        int level = 0;
        TreeItem<?> at = node == null ? null : node.getParent();
        while (at != null && level < 1000) {
            level++;
            at = at.getParent();
        }
        return level;
    }

    /// Returns how many rows the tree has.
    public final int getExpandedItemCount() {
        return expandedItemCount.get();
    }

    /// How many rows the tree has.
    public final ReadOnlyIntegerProperty expandedItemCountProperty() {
        return expandedItemCount.getReadOnlyProperty();
    }

    /// Returns the item at the top of the tree.
    public final TreeItem<T> getRoot() {
        return root.get();
    }

    /// Sets the item at the top of the tree.
    public final void setRoot(TreeItem<T> value) {
        root.set(value);
    }

    /// The item at the top of the tree.
    public final ObjectProperty<TreeItem<T>> rootProperty() {
        return root;
    }

    /// Returns whether the root has a row of its own.
    public final boolean isShowRoot() {
        return showRoot.get();
    }

    /// Sets whether the root has a row of its own; without one its
    /// children are the first level, whether it is open or not.
    public final void setShowRoot(boolean value) {
        showRoot.set(value);
    }

    /// Whether the root has a row of its own.
    public final BooleanProperty showRootProperty() {
        return showRoot;
    }

    /// Returns whether cells may be edited.
    public final boolean isEditable() {
        return editable.get();
    }

    /// Sets whether cells may be edited.
    public final void setEditable(boolean value) {
        editable.set(value);
    }

    /// Whether cells may be edited.
    public final BooleanProperty editableProperty() {
        return editable;
    }

    /// Sets what creates the cells.
    public final void setCellFactory(Callback<TreeView<T>, TreeCell<T>> value) {
        cellFactory.set(value);
    }

    /// Returns what creates the cells, or `null` for the default.
    public final Callback<TreeView<T>, TreeCell<T>> getCellFactory() {
        return cellFactory.get();
    }

    /// What creates the cells.
    public final ObjectProperty<Callback<TreeView<T>, TreeCell<T>>> cellFactoryProperty() {
        return cellFactory;
    }

    /// Sets the selection model.
    public final void setSelectionModel(MultipleSelectionModel<TreeItem<T>> value) {
        selectionModel.set(value);
    }

    /// Returns the selection model, whose indices are rows.
    public final MultipleSelectionModel<TreeItem<T>> getSelectionModel() {
        return selectionModel.get();
    }

    /// The selection model.
    public final ObjectProperty<MultipleSelectionModel<TreeItem<T>>> selectionModelProperty() {
        return selectionModel;
    }

    /// Sets the height of every row; `Region.USE_COMPUTED_SIZE` takes it
    /// from the first cell.
    public final void setFixedCellSize(double value) {
        fixedCellSize.set(value);
    }

    /// Returns the height of every row, or `Region.USE_COMPUTED_SIZE`.
    public final double getFixedCellSize() {
        return fixedCellSize.get();
    }

    /// The height of every row.
    public final DoubleProperty fixedCellSizeProperty() {
        return fixedCellSize;
    }

    /// Scrolls so that a row is the first one shown, as far as the rows
    /// reach.
    public void scrollTo(int index) {
        flow.show(index);
    }

    /// Shows every visible row again, for items that changed in a way
    /// the tree could not observe.
    public void refresh() {
        flow.refresh();
    }

    private static final class DefaultCell<T> extends TreeCell<T> {
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            TreeItem<T> of = getTreeItem();
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof Node) {
                setText(null);
                setGraphic((Node) item);
            } else {
                setText(item == null ? "null" : item.toString());
                setGraphic(of == null ? null : of.getGraphic());
            }
        }
    }

    private final class FlowRows implements RowFlow.Rows {
        @Override
        public int rowCount() {
            return rows.size();
        }

        @Override
        public int columnCount() {
            return 1;
        }

        @Override
        public void columns(double width, double[] x, double[] w) {
            x[0] = 0;
            w[0] = width;
        }

        @Override
        public IndexedCell<?> createCell(int column) {
            Callback<TreeView<T>, TreeCell<T>> factory = getCellFactory();
            TreeCell<T> cell = factory == null ? null : factory.call(TreeView.this);
            if (cell == null) {
                cell = new DefaultCell<T>();
            }
            cell.updateTreeView(TreeView.this);
            return cell;
        }

        @Override
        public void updateCell(IndexedCell<?> cell, int column, int row, boolean force) {
            if (force && cell instanceof TreeCell) {
                ((TreeCell<?>) cell).markStale();
            }
            cell.updateIndex(row);
        }

        @Override
        public double fixedCellSize() {
            return getFixedCellSize();
        }
    }

    private static final class Model<T> extends MultipleSelectionModel<TreeItem<T>>
            implements RowSelection.Owner<TreeItem<T>> {

        private final TreeView<T> view;
        private final RowSelection<TreeItem<T>> selection;

        Model(TreeView<T> view) {
            this.view = view;
            this.selection = new RowSelection<TreeItem<T>>(this);
        }

        @Override
        public ObservableList<TreeItem<T>> rows() {
            return view.rows;
        }

        @Override
        public boolean multiple() {
            return getSelectionMode() == SelectionMode.MULTIPLE;
        }

        @Override
        public void lead(int index, TreeItem<T> item) {
            setSelectedIndex(index);
            setSelectedItem(item);
        }

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return selection.indices();
        }

        @Override
        public ObservableList<TreeItem<T>> getSelectedItems() {
            return selection.items();
        }

        @Override
        public void selectIndices(int index, int... indices) {
            selection.selectIndices(index, indices);
        }

        @Override
        public void selectAll() {
            selection.selectAll();
        }

        @Override
        public void selectFirst() {
            if (selection.rowCount() > 0) {
                select(0);
            }
        }

        @Override
        public void selectLast() {
            int n = selection.rowCount();
            if (n > 0) {
                select(n - 1);
            }
        }

        @Override
        public void clearAndSelect(int index) {
            selection.clearAndSelect(index);
        }

        @Override
        public void select(int index) {
            selection.select(index);
        }

        @Override
        public void select(TreeItem<T> obj) {
            selection.select(obj);
        }

        @Override
        public void clearSelection(int index) {
            selection.clear(index);
        }

        @Override
        public void clearSelection() {
            selection.clear();
        }

        @Override
        public boolean isSelected(int index) {
            return selection.isSelected(index);
        }

        @Override
        public boolean isEmpty() {
            return selection.isEmpty();
        }

        @Override
        public void selectPrevious() {
            int lead = selection.lead();
            if (lead > 0) {
                select(lead - 1);
            }
        }

        @Override
        public void selectNext() {
            int lead = selection.lead();
            if (lead < selection.rowCount() - 1) {
                select(lead + 1);
            }
        }
    }
}
