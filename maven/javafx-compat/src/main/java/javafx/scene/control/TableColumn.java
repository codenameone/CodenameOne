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
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
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
/// A table is sorted by the columns of its sort order, each in the
/// direction of its sort type and by its comparator. A cell that was
/// edited reports through the edit events of its column; until the
/// application sets a handler of its own, a committed value is written
/// to the observable value of the cell when that is writable.
///
/// Nested columns and `sortNode` are not part of this layer.
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

    private static final EventType<Event> EDIT_ANY_EVENT = new EventType<Event>(Event.ANY, "TABLE_COLUMN_EDIT");
    private static final EventType<Event> EDIT_START_EVENT = new EventType<Event>(EDIT_ANY_EVENT, "EDIT_START");
    private static final EventType<Event> EDIT_CANCEL_EVENT = new EventType<Event>(EDIT_ANY_EVENT, "EDIT_CANCEL");
    private static final EventType<Event> EDIT_COMMIT_EVENT = new EventType<Event>(EDIT_ANY_EVENT, "EDIT_COMMIT");

    private final ObjectProperty<SortType> sortType = new SimpleObjectProperty<SortType>(this, "sortType",
            SortType.ASCENDING);
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

    /// The type every edit event of a table column descends from.
    @SuppressWarnings("unchecked")
    public static <S, T> EventType<CellEditEvent<S, T>> editAnyEvent() {
        return (EventType<CellEditEvent<S, T>>) (EventType<?>) EDIT_ANY_EVENT;
    }

    /// The event of a cell that began to be edited.
    @SuppressWarnings("unchecked")
    public static <S, T> EventType<CellEditEvent<S, T>> editStartEvent() {
        return (EventType<CellEditEvent<S, T>>) (EventType<?>) EDIT_START_EVENT;
    }

    /// The event of an edit that ended without a value.
    @SuppressWarnings("unchecked")
    public static <S, T> EventType<CellEditEvent<S, T>> editCancelEvent() {
        return (EventType<CellEditEvent<S, T>>) (EventType<?>) EDIT_CANCEL_EVENT;
    }

    /// The event of an edit that ended with a new value.
    @SuppressWarnings("unchecked")
    public static <S, T> EventType<CellEditEvent<S, T>> editCommitEvent() {
        return (EventType<CellEditEvent<S, T>>) (EventType<?>) EDIT_COMMIT_EVENT;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void init() {
        styleClasses().add("table-column");
        // What a commit does until the application says otherwise.
        setOnEditCommit(new EventHandler<CellEditEvent<S, T>>() {
            @Override
            public void handle(CellEditEvent<S, T> event) {
                TablePosition<S, T> at = event.getTablePosition();
                ObservableValue<T> value = at == null ? null : getCellObservableValue(at.getRow());
                if (value instanceof WritableValue) {
                    ((WritableValue<T>) value).setValue(event.getNewValue());
                }
            }
        });
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

    /// The direction this column sorts in.
    public final ObjectProperty<SortType> sortTypeProperty() {
        return sortType;
    }

    /// Sets the direction this column sorts in.
    public final void setSortType(SortType value) {
        sortType.set(value);
    }

    /// Returns the direction this column sorts in.
    public final SortType getSortType() {
        return sortType.get();
    }

    /// Sets what is told that a cell of this column began to be edited.
    public final void setOnEditStart(EventHandler<CellEditEvent<S, T>> value) {
        events().setSlot(TableColumn.<S, T>editStartEvent(), "onEditStart", value);
    }

    /// Returns what is told that a cell began to be edited.
    @SuppressWarnings("unchecked")
    public final EventHandler<CellEditEvent<S, T>> getOnEditStart() {
        return (EventHandler<CellEditEvent<S, T>>) events().getSlot(TableColumn.<S, T>editStartEvent());
    }

    /// Sets what takes the value of a committed edit. It replaces the
    /// handler that writes the value to the cell's observable value, so
    /// it stores the value itself.
    public final void setOnEditCommit(EventHandler<CellEditEvent<S, T>> value) {
        events().setSlot(TableColumn.<S, T>editCommitEvent(), "onEditCommit", value);
    }

    /// Returns what takes the value of a committed edit.
    @SuppressWarnings("unchecked")
    public final EventHandler<CellEditEvent<S, T>> getOnEditCommit() {
        return (EventHandler<CellEditEvent<S, T>>) events().getSlot(TableColumn.<S, T>editCommitEvent());
    }

    /// Sets what is told that an edit ended without a value.
    public final void setOnEditCancel(EventHandler<CellEditEvent<S, T>> value) {
        events().setSlot(TableColumn.<S, T>editCancelEvent(), "onEditCancel", value);
    }

    /// Returns what is told that an edit ended without a value.
    @SuppressWarnings("unchecked")
    public final EventHandler<CellEditEvent<S, T>> getOnEditCancel() {
        return (EventHandler<CellEditEvent<S, T>>) events().getSlot(TableColumn.<S, T>editCancelEvent());
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

    /// The direction a column sorts in.
    public enum SortType {
        /// Smallest first.
        ASCENDING,
        /// Largest first.
        DESCENDING
    }

    /// What the edit handlers of a column are told: the cell, and for a
    /// commit the value the edit ended with.
    public static class CellEditEvent<S, T> extends Event {

        private static final long serialVersionUID = 1L;

        // Transient as in JavaFX: a position names a live table, and is
        // where the event's table is read from.
        private final transient TablePosition<S, T> pos;
        private final transient T newValue;

        /// Creates the event of a cell.
        public CellEditEvent(TableView<S> table, TablePosition<S, T> pos, EventType<CellEditEvent<S, T>> eventType,
                T newValue) {
            super(table, Event.NULL_SOURCE_TARGET, eventType);
            if (table == null) {
                throw new NullPointerException("TableView can not be null");
            }
            this.pos = pos;
            this.newValue = newValue;
        }

        /// Returns the table.
        public TableView<S> getTableView() {
            return pos == null ? null : pos.getTableView();
        }

        /// Returns the column.
        public TableColumn<S, T> getTableColumn() {
            return pos == null ? null : pos.getTableColumn();
        }

        /// Returns the cell that was edited.
        public TablePosition<S, T> getTablePosition() {
            return pos;
        }

        /// Returns the value the edit ended with; `null` unless this is
        /// a commit.
        public T getNewValue() {
            return newValue;
        }

        /// Returns the value the cell has now.
        public T getOldValue() {
            TableColumn<S, T> column = getTableColumn();
            return pos == null || column == null ? null : column.getCellData(pos.getRow());
        }

        /// Returns the item of the row that was edited.
        public S getRowValue() {
            TableView<S> table = getTableView();
            ObservableList<S> items = table == null ? null : table.getItems();
            int row = pos == null ? -1 : pos.getRow();
            return items == null || row < 0 || row >= items.size() ? null : items.get(row);
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
