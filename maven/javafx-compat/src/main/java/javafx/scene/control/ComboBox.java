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

import com.codename1.ui.Component;
import com.codename1.ui.spinner.Picker;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

/// A control that shows one of a list of items and opens a chooser to
/// pick another.
///
/// It is shown by a Codename One `Picker`: a button with the text of the
/// value, which opens the picker of the platform, a wheel or a list, when
/// it is pressed. The text of an item comes from the converter, or from
/// `toString()` without one; the prompt text shows while there is no
/// value.
///
/// The value and the selection model follow each other. Selecting a row
/// makes its item the value; setting a value selects its row, and a value
/// that is not among the items stays the value with no row selected. A
/// change of the value sends an `ActionEvent`.
///
/// There are no cells here: the cell factory, the button cell, the
/// placeholder and the editor of JavaFX are absent. `visibleRowCount` is
/// recorded; the platform decides how many rows its picker shows.
///
/// ## Style
///
/// The style classes are `combo-box` and `combo-box-base`. The region
/// properties (`-fx-background-color`, `-fx-border-color`, `-fx-padding`)
/// apply around the picker; the picker itself is drawn by the Codename
/// One theme.
public class ComboBox<T> extends ComboBoxBase<T> {

    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<ObservableList<T>>(this,
            "items");
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");
    private final ObjectProperty<SingleSelectionModel<T>> selectionModel =
            new SimpleObjectProperty<SingleSelectionModel<T>>(this, "selectionModel");
    private final IntegerProperty visibleRowCount = new SimpleIntegerProperty(this, "visibleRowCount", 10);
    private final ChoiceCore<T> core;

    /// Creates a combo box with no items.
    public ComboBox() {
        this(FXCollections.<T>observableArrayList());
    }

    /// Creates a combo box over a list of items.
    public ComboBox(ObservableList<T> items) {
        getStyleClass().add("combo-box");
        core = new ChoiceCore<T>(this, this.items, valueRef(), selectionModel, converter, promptTextProperty(),
                showingRef(), new ChoiceCore.Host() {
                    @Override
                    public void shown(boolean showing) {
                        chooserShown(showing);
                    }
                }, false);
        selectionModel.set(new ChoiceModel<T>(this.items));
        this.items.set(items);
    }

    /// The items to choose from.
    public final ObjectProperty<ObservableList<T>> itemsProperty() {
        return items;
    }

    /// Sets the items to choose from.
    public final void setItems(ObservableList<T> value) {
        items.set(value);
    }

    /// Returns the items to choose from.
    public final ObservableList<T> getItems() {
        return items.get();
    }

    /// Makes the text of an item. `fromString` is never called: the user
    /// cannot type a value.
    public ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /// Sets what makes the text of an item.
    public final void setConverter(StringConverter<T> value) {
        converter.set(value);
    }

    /// Returns what makes the text of an item.
    public final StringConverter<T> getConverter() {
        return converter.get();
    }

    /// The model of the selected row.
    public final ObjectProperty<SingleSelectionModel<T>> selectionModelProperty() {
        return selectionModel;
    }

    /// Sets the model of the selected row.
    public final void setSelectionModel(SingleSelectionModel<T> value) {
        selectionModel.set(value);
    }

    /// Returns the model of the selected row.
    public final SingleSelectionModel<T> getSelectionModel() {
        return selectionModel.get();
    }

    /// The number of rows the chooser is asked to show; recorded.
    public final IntegerProperty visibleRowCountProperty() {
        return visibleRowCount;
    }

    /// Sets the number of rows the chooser is asked to show; recorded.
    public final void setVisibleRowCount(int value) {
        visibleRowCount.set(value);
    }

    /// Returns the number of rows the chooser is asked to show.
    public final int getVisibleRowCount() {
        return visibleRowCount.get();
    }

    @Override
    void chooserReopen() {
        core.reopen();
    }

    @Override
    protected Component cn1CreateNative() {
        return core.create();
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof Picker && core != null) {
            core.sync((Picker) c);
        }
    }
}
