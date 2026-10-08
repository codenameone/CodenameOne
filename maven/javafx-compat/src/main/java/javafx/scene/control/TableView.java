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
import java.util.Comparator;
import java.util.List;

import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/// A table: one row per item, one [TableCell] per row and column, under
/// a row of column headers.
///
/// The columns are laid side by side from the leading edge, each at the
/// width its [TableColumnBase] says; a column that does not fit is cut at
/// the edge, there is no horizontal scrolling. The rows work as in
/// [ListView]: one height for all of them, cells only for the rows in
/// view, vertical scrolling by wheel and by dragging.
///
/// Selection is by row. A press selects the row under it, and with
/// `SelectionMode.MULTIPLE` the shortcut key and Shift work as in a list.
///
/// A press on the header of a sortable column sorts by it: ascending,
/// then descending, then not at all, and with Shift held the column is
/// added to the ones already sorted by. The order is a comparator of
/// the items, [#comparatorProperty()]. Items in a `SortedList` are
/// sorted by binding its comparator to the table's; any other list is
/// sorted in place. `sortPolicy` and `onSort` are absent.
///
/// In an editable table a double click on a cell edits it, if its
/// column is editable and its cell factory makes cells that edit.
///
/// Cell selection (`TableSelectionModel`), row nodes (`TableRow`,
/// `rowFactory`), column resize policies and the focus model of JavaFX
/// are not part of this layer.
///
/// The control has no native component. It starts with a white
/// background and a thin grey border, which the `Region` style names
/// replace; the headers are labels with the style class `column-header`.
public class TableView<S> extends Control {

    private final ObjectProperty<ObservableList<S>> items = new SimpleObjectProperty<ObservableList<S>>(this,
            "items");
    private final ObservableList<TableColumn<S, ?>> columns = FXCollections.observableArrayList();
    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<Node>(this, "placeholder");
    private final ObjectProperty<TableViewSelectionModel<S>> selectionModel =
            new SimpleObjectProperty<TableViewSelectionModel<S>>(this, "selectionModel");
    private final DoubleProperty fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize", USE_COMPUTED_SIZE);
    private final RowFlow flow;
    private final Region header = new Region();
    private final ArrayList<Label> headerLabels = new ArrayList<Label>();
    private final ArrayList<Polygon> headerArrows = new ArrayList<Polygon>();
    private final ObservableList<TableColumn<S, ?>> sortOrder = FXCollections.observableArrayList();
    private final ReadOnlyObjectWrapper<Comparator<S>> comparator = new ReadOnlyObjectWrapper<Comparator<S>>(this,
            "comparator");
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    private final ReadOnlyObjectWrapper<TablePosition<S, ?>> editingCell =
            new ReadOnlyObjectWrapper<TablePosition<S, ?>>(this, "editingCell");
    private boolean sorting;
    private int anchor = -1;

    private final ChangeListener<Object> sortListener = new ChangeListener<Object>() {
        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            sort();
        }
    };

    private final ListChangeListener<S> itemsListener = new ListChangeListener<S>() {
        @Override
        public void onChanged(Change<? extends S> change) {
            RowSelection<S> own = ownSelection();
            if (own != null) {
                own.rowsChanged(change);
            }
            contentChanged();
        }
    };

    private final ListChangeListener<Integer> selectionListener = new ListChangeListener<Integer>() {
        @Override
        public void onChanged(Change<? extends Integer> change) {
            syncSelection();
        }
    };

    private final ChangeListener<Object> columnListener = new ChangeListener<Object>() {
        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            columnsChanged();
        }
    };

    private final ChangeListener<Object> widthListener = new ChangeListener<Object>() {
        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            // The cells stay; they are placed again.
            flow.requestLayout();
            requestLayout();
        }
    };

    /// Creates a table with no items.
    public TableView() {
        this(FXCollections.<S>observableArrayList());
    }

    /// Creates a table of some items.
    public TableView(ObservableList<S> items) {
        getStyleClass().add("table-view");
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(1))));
        header.setBackground(new Background(new BackgroundFill(Color.rgb(232, 232, 232), CornerRadii.EMPTY,
                Insets.EMPTY)));
        flow = new RowFlow(new FlowRows());
        cn1Children().add(flow);
        cn1Children().add(header);
        this.items.addListener(new ChangeListener<ObservableList<S>>() {
            @Override
            public void changed(ObservableValue<? extends ObservableList<S>> observable, ObservableList<S> oldValue,
                    ObservableList<S> newValue) {
                if (oldValue != null) {
                    oldValue.removeListener(itemsListener);
                }
                if (newValue != null) {
                    newValue.addListener(itemsListener);
                }
                RowSelection<S> own = ownSelection();
                if (own != null) {
                    own.rowsReplaced();
                }
                flow.scrollTo(0);
                contentChanged();
            }
        });
        columns.addListener(new ListChangeListener<TableColumn<S, ?>>() {
            @Override
            public void onChanged(Change<? extends TableColumn<S, ?>> change) {
                while (change.next()) {
                    List<? extends TableColumn<S, ?>> removed = change.getRemoved();
                    for (int i = 0; i < removed.size(); i++) {
                        detach(removed.get(i));
                    }
                    List<? extends TableColumn<S, ?>> added = change.getAddedSubList();
                    for (int i = 0; i < added.size(); i++) {
                        attach(added.get(i));
                    }
                }
                columnsChanged();
            }
        });
        selectionModel.addListener(new ChangeListener<TableViewSelectionModel<S>>() {
            @Override
            public void changed(ObservableValue<? extends TableViewSelectionModel<S>> observable,
                    TableViewSelectionModel<S> oldValue, TableViewSelectionModel<S> newValue) {
                if (oldValue != null) {
                    oldValue.getSelectedIndices().removeListener(selectionListener);
                }
                if (newValue != null) {
                    newValue.getSelectedIndices().addListener(selectionListener);
                }
                syncSelection();
            }
        });
        placeholder.addListener(new ChangeListener<Node>() {
            @Override
            public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
                if (oldValue != null) {
                    cn1Children().remove(oldValue);
                }
                if (newValue != null && !cn1Children().contains(newValue)) {
                    cn1Children().add(newValue);
                }
                requestLayout();
            }
        });
        fixedCellSize.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                flow.rowsChanged();
            }
        });
        flow.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pressed(event);
            }
        });
        flow.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                clicked(event);
            }
        });
        sortOrder.addListener(new ListChangeListener<TableColumn<S, ?>>() {
            @Override
            public void onChanged(Change<? extends TableColumn<S, ?>> change) {
                sort();
            }
        });
        selectionModel.set(new Model<S>(this));
        this.items.set(items);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private void attach(TableColumn<S, ?> column) {
        if (column.getTableView() == this) {
            return;
        }
        column.setTableView(this);
        column.textProperty().addListener(columnListener);
        column.visibleProperty().addListener(columnListener);
        column.widthProperty().addListener(widthListener);
        column.cellFactoryProperty().addListener(columnListener);
        column.cellValueFactoryProperty().addListener(columnListener);
        column.sortTypeProperty().addListener(sortListener);
        column.sortableProperty().addListener(sortListener);
        column.comparatorProperty().addListener(sortListener);
    }

    private void detach(TableColumn<S, ?> column) {
        if (columns.contains(column)) {
            return;
        }
        column.setTableView(null);
        column.textProperty().removeListener(columnListener);
        column.visibleProperty().removeListener(columnListener);
        column.widthProperty().removeListener(widthListener);
        column.cellFactoryProperty().removeListener(columnListener);
        column.cellValueFactoryProperty().removeListener(columnListener);
        column.sortTypeProperty().removeListener(sortListener);
        column.sortableProperty().removeListener(sortListener);
        column.comparatorProperty().removeListener(sortListener);
        sortOrder.remove(column);
    }

    /// The columns, their widths or what they show changed: the headers
    /// and every cell are made again.
    private void columnsChanged() {
        for (int i = 0; i < headerLabels.size(); i++) {
            cn1Children().remove(headerLabels.get(i));
        }
        headerLabels.clear();
        for (int i = 0; i < headerArrows.size(); i++) {
            cn1Children().remove(headerArrows.get(i));
        }
        headerArrows.clear();
        for (int i = 0; i < columns.size(); i++) {
            final TableColumn<S, ?> column = columns.get(i);
            Label label = new Label(column.getText());
            label.getStyleClass().add("column-header");
            label.setManaged(false);
            label.setVisible(column.isVisible());
            label.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        headerClicked(column, event.isShiftDown());
                    }
                }
            });
            headerLabels.add(label);
            cn1Children().add(label);
            Polygon arrow = new Polygon();
            arrow.setManaged(false);
            arrow.setMouseTransparent(true);
            arrow.setFill(Color.rgb(90, 90, 90));
            arrow.setVisible(false);
            headerArrows.add(arrow);
            cn1Children().add(arrow);
        }
        flow.rebuild();
        syncArrows();
        requestLayout();
    }

    @SuppressWarnings("unchecked")
    private RowSelection<S> ownSelection() {
        TableViewSelectionModel<S> model = getSelectionModel();
        return model instanceof Model ? ((Model<S>) model).selection : null;
    }

    private void contentChanged() {
        flow.rowsChanged();
        requestLayout();
    }

    private void syncSelection() {
        TableViewSelectionModel<S> model = getSelectionModel();
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
        TableViewSelectionModel<S> model = getSelectionModel();
        int row = flow.rowAt(event);
        if (model == null || row < 0) {
            return;
        }
        anchor = RowPress.apply(model, event.getButton() == MouseButton.PRIMARY, event.isShortcutDown(),
                event.isShiftDown(), row, anchor);
    }

    private void clicked(MouseEvent event) {
        if (!isEditable() || event.getButton() != MouseButton.PRIMARY || event.getClickCount() < 2) {
            return;
        }
        int row = flow.rowAt(event);
        if (row < 0) {
            return;
        }
        double x = flow.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
        double at = 0;
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<S, ?> column = columns.get(i);
            if (!column.isVisible()) {
                continue;
            }
            if (x >= at && x < at + column.getWidth()) {
                edit(row, column);
                return;
            }
            at += column.getWidth();
        }
    }

    /// A header was pressed: the column is sorted ascending, then
    /// descending, then not at all. Alone, unless it is added to the
    /// columns already sorted by.
    private void headerClicked(TableColumn<S, ?> column, boolean add) {
        if (!column.isSortable()) {
            return;
        }
        boolean sorted = sortOrder.contains(column);
        sorting = true;
        if (!sorted) {
            column.setSortType(TableColumn.SortType.ASCENDING);
            if (!add) {
                sortOrder.clear();
            }
            sortOrder.add(column);
        } else if (column.getSortType() == TableColumn.SortType.ASCENDING) {
            column.setSortType(TableColumn.SortType.DESCENDING);
            if (!add && sortOrder.size() > 1) {
                sortOrder.setAll(java.util.Collections.<TableColumn<S, ?>>singletonList(column));
            }
        } else {
            sortOrder.remove(column);
        }
        sorting = false;
        sort();
    }

    private static <S, T> int compareBy(TableColumn<S, T> column, S a, S b) {
        Comparator<T> by = column.getComparator();
        if (by == null) {
            return 0;
        }
        // Descending by exchanging the operands: negating a result fails for
        // a comparator that answers Integer.MIN_VALUE.
        return column.getSortType() == TableColumn.SortType.DESCENDING
                ? by.compare(column.getCellData(b), column.getCellData(a))
                : by.compare(column.getCellData(a), column.getCellData(b));
    }

    /// Sorts the items by the columns of the sort order. With none the
    /// comparator is `null`: a `SortedList` bound to it returns to the
    /// order of its source, any other list stays as it is.
    public void sort() {
        if (sorting) {
            return;
        }
        final ArrayList<TableColumn<S, ?>> by = new ArrayList<TableColumn<S, ?>>();
        for (int i = 0; i < sortOrder.size(); i++) {
            TableColumn<S, ?> column = sortOrder.get(i);
            if (column != null && column.isSortable() && !by.contains(column)) {
                by.add(column);
            }
        }
        Comparator<S> order = by.isEmpty() ? null : new Comparator<S>() {
            @Override
            public int compare(S a, S b) {
                for (int i = 0; i < by.size(); i++) {
                    int c = compareBy(by.get(i), a, b);
                    if (c != 0) {
                        return c;
                    }
                }
                return 0;
            }
        };
        sorting = true;
        comparator.set(order);
        ObservableList<S> list = getItems();
        // A sorted list takes the comparator through its own property.
        if (order != null && list != null && !(list instanceof SortedList)) {
            FXCollections.sort(list, order);
        }
        sorting = false;
        syncArrows();
        requestLayout();
    }

    private void syncArrows() {
        for (int i = 0; i < headerArrows.size() && i < columns.size(); i++) {
            TableColumn<S, ?> column = columns.get(i);
            Polygon arrow = headerArrows.get(i);
            boolean shown = column.isVisible() && column.isSortable() && sortOrder.contains(column);
            arrow.setVisible(shown);
            if (shown) {
                boolean up = column.getSortType() != TableColumn.SortType.DESCENDING;
                arrow.getPoints().setAll(0.0, up ? 6.0 : 0.0, 8.0, up ? 6.0 : 0.0, 4.0, up ? 0.0 : 6.0);
            }
        }
    }

    final void editing(TablePosition<S, ?> cell) {
        editingCell.set(cell);
    }

    /// Starts editing the cell of a row in a column, when the table and
    /// the column are editable and the row is in view; a row of -1 or a
    /// `null` column cancels the edit in progress.
    public void edit(int row, TableColumn<S, ?> column) {
        TablePosition<S, ?> now = editingCell.get();
        if (now != null) {
            IndexedCell<?> cell = flow.cell(now.getRow(), now.getColumn());
            if (cell != null && cell.isEditing()) {
                cell.cancelEdit();
            }
            editingCell.set(null);
        }
        int at = column == null ? -1 : columns.indexOf(column);
        if (row < 0 || at < 0 || !isEditable() || !column.isEditable()) {
            return;
        }
        IndexedCell<?> cell = flow.cell(row, at);
        if (cell != null) {
            cell.startEdit();
        }
    }

    /// Returns the cell being edited, or `null`.
    public final TablePosition<S, ?> getEditingCell() {
        return editingCell.get();
    }

    /// The cell being edited.
    public final ReadOnlyObjectProperty<TablePosition<S, ?>> editingCellProperty() {
        return editingCell.getReadOnlyProperty();
    }

    /// Sets whether the cells of this table may be edited.
    public final void setEditable(boolean value) {
        editable.set(value);
    }

    /// Returns whether the cells of this table may be edited.
    public final boolean isEditable() {
        return editable.get();
    }

    /// Whether the cells of this table may be edited; false by default.
    public final BooleanProperty editableProperty() {
        return editable;
    }

    /// Returns the columns the table is sorted by, the first one first.
    public final ObservableList<TableColumn<S, ?>> getSortOrder() {
        return sortOrder;
    }

    /// The order of the items the sort order asks for, `null` with no
    /// sort order. A `SortedList` shown by the table binds its own
    /// comparator to this one.
    public final ReadOnlyObjectProperty<Comparator<S>> comparatorProperty() {
        return comparator.getReadOnlyProperty();
    }

    /// Returns the order of the items the sort order asks for, or `null`.
    public final Comparator<S> getComparator() {
        return comparator.get();
    }

    private double headerHeight() {
        double h = 0;
        for (int i = 0; i < headerLabels.size(); i++) {
            h = Math.max(h, headerLabels.get(i).prefHeight(-1));
        }
        return h > 0 ? h : 24;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        double hh = Math.min(h, headerHeight());
        header.resizeRelocate(in.getLeft(), in.getTop(), w, hh);
        double x = 0;
        for (int i = 0; i < headerLabels.size() && i < columns.size(); i++) {
            TableColumn<S, ?> column = columns.get(i);
            Label label = headerLabels.get(i);
            if (!column.isVisible()) {
                continue;
            }
            double cw = Math.max(0, Math.min(column.getWidth(), w - x));
            label.setVisible(cw > 0);
            label.resizeRelocate(in.getLeft() + x, in.getTop(), cw, hh);
            if (i < headerArrows.size()) {
                Polygon arrow = headerArrows.get(i);
                if (cw < 16) {
                    arrow.setVisible(false);
                }
                arrow.relocate(in.getLeft() + x + cw - 12, in.getTop() + (hh - 6) / 2);
            }
            x += column.getWidth();
        }
        Node empty = getPlaceholder();
        ObservableList<S> list = getItems();
        boolean showEmpty = empty != null && (list == null || list.isEmpty());
        flow.setVisible(!showEmpty);
        flow.resizeRelocate(in.getLeft(), in.getTop() + hh, w, h - hh);
        if (empty != null) {
            empty.setVisible(showEmpty);
            if (showEmpty) {
                layoutInArea(empty, in.getLeft(), in.getTop() + hh, w, h - hh, 0, javafx.geometry.HPos.CENTER,
                        javafx.geometry.VPos.CENTER);
            }
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        double w = 0;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).isVisible()) {
                w += columns.get(i).getWidth();
            }
        }
        return Math.max(248, in.getLeft() + w + in.getRight());
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

    @Override
    public double getBaselineOffset() {
        return BASELINE_OFFSET_SAME_AS_HEIGHT;
    }

    /// The items shown, one per row.
    public final ObjectProperty<ObservableList<S>> itemsProperty() {
        return items;
    }

    /// Sets the items shown. Changes to an observable list show at once.
    public final void setItems(ObservableList<S> value) {
        items.set(value);
    }

    /// Returns the items shown.
    public final ObservableList<S> getItems() {
        return items.get();
    }

    /// Returns the columns, in the order they are shown.
    public final ObservableList<TableColumn<S, ?>> getColumns() {
        return columns;
    }

    /// The node shown instead of the rows while there are no items.
    public final ObjectProperty<Node> placeholderProperty() {
        return placeholder;
    }

    /// Sets the node shown while there are no items.
    public final void setPlaceholder(Node value) {
        placeholder.set(value);
    }

    /// Returns the node shown while there are no items.
    public final Node getPlaceholder() {
        return placeholder.get();
    }

    /// The selection model.
    public final ObjectProperty<TableViewSelectionModel<S>> selectionModelProperty() {
        return selectionModel;
    }

    /// Sets the selection model.
    public final void setSelectionModel(TableViewSelectionModel<S> value) {
        selectionModel.set(value);
    }

    /// Returns the selection model.
    public final TableViewSelectionModel<S> getSelectionModel() {
        return selectionModel.get();
    }

    /// Sets the height of every row; `Region.USE_COMPUTED_SIZE` takes it
    /// from the cells of the first row.
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

    /// Scrolls to an item of the table.
    public void scrollTo(S object) {
        ObservableList<S> list = getItems();
        int at = list == null ? -1 : list.indexOf(object);
        if (at >= 0) {
            scrollTo(at);
        }
    }

    /// Shows every visible row again, for items that changed in a way
    /// the table could not observe.
    public void refresh() {
        flow.refresh();
    }

    private <T> TableCell<S, T> createCell(TableColumn<S, T> column) {
        TableCell<S, T> cell = column.getCellFactory() == null ? null : column.getCellFactory().call(column);
        if (cell == null) {
            @SuppressWarnings("unchecked")
            TableCell<S, T> made = (TableCell<S, T>) TableColumn.DEFAULT_CELL_FACTORY.call(column);
            cell = made;
        }
        cell.updateTableView(this);
        cell.updateTableColumn(column);
        return cell;
    }

    private final class FlowRows implements RowFlow.Rows {
        @Override
        public int rowCount() {
            ObservableList<S> list = getItems();
            return list == null ? 0 : list.size();
        }

        @Override
        public int columnCount() {
            return columns.size();
        }

        @Override
        public void columns(double width, double[] x, double[] w) {
            double at = 0;
            for (int i = 0; i < x.length && i < columns.size(); i++) {
                TableColumn<S, ?> column = columns.get(i);
                x[i] = at;
                w[i] = column.isVisible() ? Math.max(0, Math.min(column.getWidth(), width - at)) : 0;
                if (column.isVisible()) {
                    at += column.getWidth();
                }
            }
        }

        @Override
        public IndexedCell<?> createCell(int column) {
            return TableView.this.createCell(columns.get(column));
        }

        @Override
        public void updateCell(IndexedCell<?> cell, int column, int row, boolean force) {
            if (force && cell instanceof TableCell) {
                ((TableCell<?, ?>) cell).markStale();
            }
            cell.updateIndex(row);
        }

        @Override
        public double fixedCellSize() {
            return getFixedCellSize();
        }
    }

    /// The selection of a table: whole rows.
    ///
    /// Unlike in JavaFX this class extends [MultipleSelectionModel]
    /// directly; the cell selection members of `TableSelectionModel` are
    /// not part of this layer.
    public abstract static class TableViewSelectionModel<S> extends MultipleSelectionModel<S> {

        private final TableView<S> tableView;

        /// Creates the selection model of a table.
        public TableViewSelectionModel(final TableView<S> tableView) {
            if (tableView == null) {
                throw new NullPointerException("TableView can not be null");
            }
            this.tableView = tableView;
        }

        /// Returns the table this model selects in.
        public TableView<S> getTableView() {
            return tableView;
        }
    }

    private static final class Model<S> extends TableViewSelectionModel<S> implements RowSelection.Owner<S> {

        private final RowSelection<S> selection;

        Model(TableView<S> view) {
            super(view);
            this.selection = new RowSelection<S>(this);
        }

        @Override
        public ObservableList<S> rows() {
            return getTableView().getItems();
        }

        @Override
        public boolean multiple() {
            return getSelectionMode() == SelectionMode.MULTIPLE;
        }

        @Override
        public void lead(int index, S item) {
            setSelectedIndex(index);
            setSelectedItem(item);
        }

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return selection.indices();
        }

        @Override
        public ObservableList<S> getSelectedItems() {
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
        public void select(S obj) {
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
