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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
/// Cell selection (`TablePosition`, `TableSelectionModel`), row nodes
/// (`TableRow`, `rowFactory`), sorting, column resize policies, editing
/// and the focus model of JavaFX are not part of this layer.
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
    private int anchor = -1;

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
    }

    /// The columns, their widths or what they show changed: the headers
    /// and every cell are made again.
    private void columnsChanged() {
        for (int i = 0; i < headerLabels.size(); i++) {
            cn1Children().remove(headerLabels.get(i));
        }
        headerLabels.clear();
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<S, ?> column = columns.get(i);
            Label label = new Label(column.getText());
            label.getStyleClass().add("column-header");
            label.setManaged(false);
            label.setVisible(column.isVisible());
            headerLabels.add(label);
            cn1Children().add(label);
        }
        flow.rebuild();
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
