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

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;

/// A cell of a [TableView]: the value of one column for one row.
///
/// While it shows a row the cell listens to the observable value the
/// column's cell value factory answered, and shows the new value when it
/// changes.
///
/// There are no row nodes in this layer, so `tableRow` is absent.
public class TableCell<S, T> extends IndexedCell<T> {

    private final ReadOnlyObjectWrapper<TableColumn<S, T>> tableColumn = new ReadOnlyObjectWrapper<TableColumn<S, T>>(
            this, "tableColumn");
    private final ReadOnlyObjectWrapper<TableView<S>> tableView = new ReadOnlyObjectWrapper<TableView<S>>(this,
            "tableView");
    private final ChangeListener<T> valueListener = new ChangeListener<T>() {
        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            if (observable == observed) {
                updateItem(newValue, false);
            }
        }
    };
    private ObservableValue<T> observed;
    private boolean stale = true;

    /// Creates a cell that belongs to no table yet.
    public TableCell() {
        getStyleClass().add("table-cell");
    }

    /// The column this cell is in.
    public final ReadOnlyObjectProperty<TableColumn<S, T>> tableColumnProperty() {
        return tableColumn.getReadOnlyProperty();
    }

    /// Returns the column this cell is in.
    public final TableColumn<S, T> getTableColumn() {
        return tableColumn.get();
    }

    /// The table this cell is in.
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() {
        return tableView.getReadOnlyProperty();
    }

    /// Returns the table this cell is in.
    public final TableView<S> getTableView() {
        return tableView.get();
    }

    /// Tells the cell which table it is in; the table does.
    public final void updateTableView(TableView<S> tv) {
        tableView.set(tv);
    }

    /// Tells the cell which column it is in; the table does.
    public final void updateTableColumn(TableColumn<S, T> col) {
        tableColumn.set(col);
    }

    /// Makes the next update show the value again even if it is the same.
    final void markStale() {
        stale = true;
    }

    private void observe(ObservableValue<T> value) {
        if (observed == value) {
            return;
        }
        if (observed != null) {
            observed.removeListener(valueListener);
        }
        observed = value;
        if (value != null) {
            value.addListener(valueListener);
        }
    }

    private TablePosition<S, T> position() {
        return new TablePosition<S, T>(getTableView(), getIndex(), getTableColumn());
    }

    /// Starts editing this cell, when the table, the column and the cell
    /// are all editable, and tells the column.
    @Override
    public void startEdit() {
        TableView<S> table = getTableView();
        TableColumn<S, T> column = getTableColumn();
        if (table == null || column == null || !table.isEditable() || !column.isEditable()) {
            return;
        }
        super.startEdit();
        if (!isEditing()) {
            return;
        }
        TablePosition<S, T> at = position();
        table.editing(at);
        Event.fireEvent(column, new TableColumn.CellEditEvent<S, T>(table, at, TableColumn.<S, T>editStartEvent(),
                null));
    }

    /// Ends the edit with a value: the column is told, which is where the
    /// value is stored, and the cell then shows it.
    @Override
    public void commitEdit(T newValue) {
        if (!isEditing()) {
            return;
        }
        TableView<S> table = getTableView();
        TableColumn<S, T> column = getTableColumn();
        TablePosition<S, T> at = position();
        super.commitEdit(newValue);
        if (table != null) {
            table.editing(null);
        }
        if (table != null && column != null) {
            Event.fireEvent(column, new TableColumn.CellEditEvent<S, T>(table, at,
                    TableColumn.<S, T>editCommitEvent(), newValue));
        }
        updateItem(newValue, false);
    }

    /// Ends the edit without a value and tells the column.
    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }
        TableView<S> table = getTableView();
        TableColumn<S, T> column = getTableColumn();
        TablePosition<S, T> at = position();
        super.cancelEdit();
        if (table != null) {
            table.editing(null);
        }
        if (table != null && column != null) {
            Event.fireEvent(column, new TableColumn.CellEditEvent<S, T>(table, at,
                    TableColumn.<S, T>editCancelEvent(), null));
        }
    }

    @Override
    void indexChanged(int oldIndex, int newIndex) {
        if (isEditing() && oldIndex != newIndex) {
            // The cell is given another row: what was typed is dropped.
            cancelEdit();
        }
        TableView<S> table = getTableView();
        TableColumn<S, T> column = getTableColumn();
        ObservableList<S> items = table == null ? null : table.getItems();
        boolean valid = column != null && items != null && newIndex >= 0 && newIndex < items.size();
        if (valid) {
            ObservableValue<T> source = column.getCellObservableValue(newIndex);
            boolean moved = observed != source;
            observe(source);
            T value = source == null ? null : source.getValue();
            if (stale || moved || isEmpty() || oldIndex != newIndex || isItemChanged(getItem(), value)) {
                updateItem(value, false);
            }
        } else {
            observe(null);
            if (stale || !isEmpty()) {
                updateItem(null, true);
            }
        }
        stale = false;
        TableView.TableViewSelectionModel<S> model = table == null ? null : table.getSelectionModel();
        updateSelected(valid && model != null && model.isSelected(newIndex));
    }
}
