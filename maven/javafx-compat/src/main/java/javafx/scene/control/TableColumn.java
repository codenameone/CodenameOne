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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.util.Callback;

/// A column of a [TableView]: `S` is the type of the table's items and
/// `T` the type of the value this column shows for one.
///
/// The cell value factory answers, for an item, the observable value the
/// cell shows; the cell follows that value while it shows the row.
/// `PropertyValueFactory` is not part of this layer because it reads
/// properties by reflection, which a device build does not have: write
/// the callback out, `f -> f.getValue().nameProperty()`.
///
/// Sorting (`sortType`, `comparator`), nested columns and the edit events
/// are not part of this layer.
public class TableColumn<S, T> extends TableColumnBase<S, T> {

    /// Creates the cell a column without a cell factory uses: it shows
    /// the value's `toString()`, or the value itself when it is a node.
    public static final Callback<TableColumn<?, ?>, TableCell<?, ?>> DEFAULT_CELL_FACTORY =
            new Callback<TableColumn<?, ?>, TableCell<?, ?>>() {
                @Override
                public TableCell<?, ?> call(TableColumn<?, ?> column) {
                    return new DefaultCell<Object, Object>();
                }
            };

    private final ReadOnlyObjectWrapper<TableView<S>> tableView = new ReadOnlyObjectWrapper<TableView<S>>(this,
            "tableView");
    private final ObjectProperty<Callback<CellDataFeatures<S, T>, ObservableValue<T>>> cellValueFactory =
            new SimpleObjectProperty<Callback<CellDataFeatures<S, T>, ObservableValue<T>>>(this, "cellValueFactory");
    private final ObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>> cellFactory =
            new SimpleObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>>(this, "cellFactory");

    /// Creates a column with no header text.
    public TableColumn() {
        init();
    }

    /// Creates a column with a header text.
    public TableColumn(String text) {
        super(text);
        init();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void init() {
        styleClasses().add("table-column");
        // The default factory makes cells of any type.
        cellFactory.set((Callback) DEFAULT_CELL_FACTORY);
    }

    /// The table this column is in.
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() {
        return tableView.getReadOnlyProperty();
    }

    final void setTableView(TableView<S> value) {
        tableView.set(value);
    }

    /// Returns the table this column is in, or `null`.
    public final TableView<S> getTableView() {
        return tableView.get();
    }

    /// Sets what answers the value of this column for an item.
    public final void setCellValueFactory(Callback<CellDataFeatures<S, T>, ObservableValue<T>> value) {
        cellValueFactory.set(value);
    }

    /// Returns what answers the value of this column for an item.
    public final Callback<CellDataFeatures<S, T>, ObservableValue<T>> getCellValueFactory() {
        return cellValueFactory.get();
    }

    /// What answers the value of this column for an item.
    public final ObjectProperty<Callback<CellDataFeatures<S, T>, ObservableValue<T>>> cellValueFactoryProperty() {
        return cellValueFactory;
    }

    /// Sets what creates the cells of this column.
    public final void setCellFactory(Callback<TableColumn<S, T>, TableCell<S, T>> value) {
        cellFactory.set(value);
    }

    /// Returns what creates the cells of this column.
    public final Callback<TableColumn<S, T>, TableCell<S, T>> getCellFactory() {
        return cellFactory.get();
    }

    /// What creates the cells of this column.
    public final ObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>> cellFactoryProperty() {
        return cellFactory;
    }

    @Override
    public ObservableValue<T> getCellObservableValue(int index) {
        TableView<S> table = getTableView();
        ObservableList<S> items = table == null ? null : table.getItems();
        if (items == null || index < 0 || index >= items.size()) {
            return null;
        }
        return getCellObservableValue(items.get(index));
    }

    @Override
    public ObservableValue<T> getCellObservableValue(S item) {
        Callback<CellDataFeatures<S, T>, ObservableValue<T>> factory = getCellValueFactory();
        TableView<S> table = getTableView();
        if (factory == null || table == null) {
            return null;
        }
        return factory.call(new CellDataFeatures<S, T>(table, this, item));
    }

    @Override
    public Styleable getStyleableParent() {
        return getTableView();
    }

    /// What a cell value factory is asked with: the item of the row, and
    /// the table and column the value is for.
    public static class CellDataFeatures<S, T> {

        private final TableView<S> tableView;
        private final TableColumn<S, T> tableColumn;
        private final S value;

        /// Creates the question for one item.
        public CellDataFeatures(TableView<S> tableView, TableColumn<S, T> tableColumn, S value) {
            this.tableView = tableView;
            this.tableColumn = tableColumn;
            this.value = value;
        }

        /// Returns the item of the row.
        public S getValue() {
            return value;
        }

        /// Returns the column the value is for.
        public TableColumn<S, T> getTableColumn() {
            return tableColumn;
        }

        /// Returns the table the value is for.
        public TableView<S> getTableView() {
            return tableView;
        }
    }

    private static final class DefaultCell<S, T> extends TableCell<S, T> {
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof Node) {
                setText(null);
                setGraphic((Node) item);
            } else {
                setText(item.toString());
                setGraphic(null);
            }
        }
    }
}
