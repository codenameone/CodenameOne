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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/// A table cell that shows its value as text and, while it is edited, as a
/// text field. The action of the field commits what was typed, converted
/// by the cell's converter, and Escape cancels.
///
/// A cell is edited by a double click in an editable table, or by
/// `TableView.edit`.
public class TextFieldTableCell<S, T> extends TableCell<S, T> {

    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");
    private TextField textField;

    /// Creates a cell with no converter: it can show a value and cannot
    /// commit one.
    public TextFieldTableCell() {
        this(null);
    }

    /// Creates a cell that converts between its values and text.
    public TextFieldTableCell(StringConverter<T> converter) {
        getStyleClass().add("text-field-table-cell");
        setConverter(converter);
    }

    /// Returns a cell factory for a column of strings.
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    /// Returns a cell factory for a column of values a converter reads
    /// from text and writes as text.
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
            final StringConverter<T> converter) {
        return new Callback<TableColumn<S, T>, TableCell<S, T>>() {
            @Override
            public TableCell<S, T> call(TableColumn<S, T> column) {
                return new TextFieldTableCell<S, T>(converter);
            }
        };
    }

    /// The converter between the values of this cell and text.
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /// Sets the converter.
    public final void setConverter(StringConverter<T> value) {
        converter.set(value);
    }

    /// Returns the converter, or `null`.
    public final StringConverter<T> getConverter() {
        return converter.get();
    }

    private String itemText() {
        T item = getItem();
        StringConverter<T> c = getConverter();
        if (c != null) {
            return c.toString(item);
        }
        return item == null ? "" : item.toString();
    }

    private TextField field() {
        if (textField == null) {
            textField = new TextField();
            textField.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    StringConverter<T> c = getConverter();
                    if (c == null) {
                        throw new IllegalStateException("Attempting to convert text input into Object, but provided "
                                + "StringConverter is null. Be sure to set a StringConverter in your cell factory.");
                    }
                    commitEdit(c.fromString(textField.getText()));
                    event.consume();
                }
            });
            textField.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                        event.consume();
                    }
                }
            });
        }
        return textField;
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (!isEditing()) {
            return;
        }
        TextField field = field();
        field.setText(itemText());
        setText(null);
        setGraphic(field);
        field.selectAll();
        field.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(itemText());
        setGraphic(null);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            field().setText(itemText());
            setText(null);
            setGraphic(textField);
        } else {
            setText(itemText());
            setGraphic(null);
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (textField != null && getGraphic() == textField) {
            // The field takes the whole cell, not its preferred width.
            Insets in = getInsets();
            textField.resizeRelocate(in.getLeft(), in.getTop(), Math.max(0, getWidth() - in.getLeft() - in.getRight()),
                    Math.max(0, getHeight() - in.getTop() - in.getBottom()));
        }
    }
}
