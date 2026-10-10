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
package javafx.scene.control.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;

/// A table cell that shows a check box, with a text beside it when the
/// cell has a converter.
///
/// The box follows a boolean value: the one the selected-state callback
/// answers for the row, or without a callback the value of the cell's
/// column. When that value is a `BooleanProperty` the two are bound both
/// ways, so a click on the box writes the property; no edit is started or
/// committed. The box is disabled while the table, the column or the cell
/// is not editable.
public class CheckBoxTableCell<S, T> extends TableCell<S, T> {

    private final CheckBox checkBox = new CheckBox();
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");
    private final ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallback =
            new SimpleObjectProperty<Callback<Integer, ObservableValue<Boolean>>>(this, "selectedStateCallback");
    private BooleanProperty bound;

    /// Creates a cell that follows the value of its column.
    public CheckBoxTableCell() {
        this(null, null);
    }

    /// Creates a cell that follows what a callback answers for its row.
    public CheckBoxTableCell(Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
        this(getSelectedProperty, null);
    }

    /// Creates a cell that follows what a callback answers for its row and
    /// shows its item as text through a converter.
    public CheckBoxTableCell(Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
            StringConverter<T> converter) {
        getStyleClass().add("check-box-table-cell");
        setSelectedStateCallback(getSelectedProperty);
        setConverter(converter);
        setAlignment(Pos.CENTER);
    }

    /// Returns a cell factory for a column of booleans.
    public static <S> Callback<TableColumn<S, Boolean>, TableCell<S, Boolean>> forTableColumn(
            TableColumn<S, Boolean> column) {
        return forTableColumn((Callback<Integer, ObservableValue<Boolean>>) null, null);
    }

    /// Returns a cell factory whose boxes follow a callback.
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
            Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
        return forTableColumn(getSelectedProperty, null);
    }

    /// Returns a cell factory whose boxes follow a callback, with the item
    /// as text beside the box when `showLabel` is true.
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
            Callback<Integer, ObservableValue<Boolean>> getSelectedProperty, boolean showLabel) {
        StringConverter<T> text = !showLabel ? null : new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
        return forTableColumn(getSelectedProperty, text);
    }

    /// Returns a cell factory whose boxes follow a callback, with the item
    /// as text through a converter.
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
            final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
            final StringConverter<T> converter) {
        return new Callback<TableColumn<S, T>, TableCell<S, T>>() {
            @Override
            public TableCell<S, T> call(TableColumn<S, T> column) {
                return new CheckBoxTableCell<S, T>(getSelectedProperty, converter);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private ObservableValue<Boolean> selectedValue() {
        Callback<Integer, ObservableValue<Boolean>> callback = getSelectedStateCallback();
        if (callback != null) {
            return callback.call(Integer.valueOf(getIndex()));
        }
        TableColumn<S, T> column = getTableColumn();
        // The column's value is read as a boolean; only the type argument
        // is taken on trust, and no class is checked for it at run time.
        ObservableValue<?> value = column == null ? null : column.getCellObservableValue(getIndex());
        return (ObservableValue<Boolean>) value;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (bound != null) {
            checkBox.selectedProperty().unbindBidirectional(bound);
            bound = null;
        }
        if (empty) {
            setText(null);
            setGraphic(null);
            return;
        }
        StringConverter<T> c = getConverter();
        setText(c == null ? null : c.toString(item));
        setGraphic(checkBox);
        ObservableValue<Boolean> value = selectedValue();
        if (value instanceof BooleanProperty) {
            bound = (BooleanProperty) value;
            checkBox.selectedProperty().bindBidirectional(bound);
        } else {
            Object now = value == null ? item : value.getValue();
            checkBox.setSelected(Boolean.TRUE.equals(now));
        }
        TableView<S> table = getTableView();
        TableColumn<S, T> column = getTableColumn();
        checkBox.setDisable(table == null || column == null || !table.isEditable() || !column.isEditable()
                || !isEditable());
    }

    /// The converter that makes the text beside the box.
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /// Sets the converter that makes the text beside the box.
    public final void setConverter(StringConverter<T> value) {
        converter.set(value);
    }

    /// Returns the converter, or `null` for a box without a text.
    public final StringConverter<T> getConverter() {
        return converter.get();
    }

    /// What answers the value the box of a row follows.
    public final ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
        return selectedStateCallback;
    }

    /// Sets what answers the value the box of a row follows.
    public final void setSelectedStateCallback(Callback<Integer, ObservableValue<Boolean>> value) {
        selectedStateCallback.set(value);
    }

    /// Returns what answers the value the box of a row follows.
    public final Callback<Integer, ObservableValue<Boolean>> getSelectedStateCallback() {
        return selectedStateCallback.get();
    }
}
