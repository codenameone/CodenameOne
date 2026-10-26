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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.StringConverter;

/// A control that shows one of a short list of items and opens a chooser
/// to pick another.
///
/// It is shown by a Codename One `Picker`, as a [ComboBox] is: a button
/// with the text of the value, which opens the picker of the platform
/// when it is pressed. The text of an item comes from the converter, or
/// from `toString()` without one.
///
/// The value and the selection model follow each other, and a value the
/// items no longer hold after they changed is cleared. A change of the
/// value sends an `ActionEvent`. Codename One does not report a chooser the user
/// dismissed without choosing, so `showing` can stay `true` after that.
///
/// The showing and hiding events of JavaFX are not part of this layer.
///
/// ## Style
///
/// The style class is `choice-box`. The region properties apply around
/// the picker; the picker itself is drawn by the Codename One theme.
public class ChoiceBox<T> extends Control {

    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<ObservableList<T>>(this,
            "items");
    private final ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");
    private final ObjectProperty<SingleSelectionModel<T>> selectionModel =
            new SimpleObjectProperty<SingleSelectionModel<T>>(this, "selectionModel");
    private final ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper(this, "showing", false);
    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction");
    private final ChoiceCore<T> core;

    /// Creates a choice box with no items.
    public ChoiceBox() {
        this(FXCollections.<T>observableArrayList());
    }

    /// Creates a choice box over a list of items.
    public ChoiceBox(ObservableList<T> items) {
        getStyleClass().add("choice-box");
        addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                EventHandler<ActionEvent> handler = onAction.get();
                if (handler != null) {
                    handler.handle(event);
                }
            }
        });
        core = new ChoiceCore<T>(this, this.items, value, selectionModel, converter, null,
                showing.getReadOnlyProperty(), new ChoiceCore.Host() {
                    @Override
                    public void shown(boolean open) {
                        showing.set(open);
                    }
                }, true);
        value.addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                fireEvent(new ActionEvent(ChoiceBox.this, ChoiceBox.this));
            }
        });
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

    /// The chosen item.
    public ObjectProperty<T> valueProperty() {
        return value;
    }

    /// Sets the chosen item.
    public final void setValue(T value) {
        this.value.set(value);
    }

    /// Returns the chosen item.
    public final T getValue() {
        return value.get();
    }

    /// Makes the text of an item.
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

    /// Whether the chooser is open.
    public final ReadOnlyBooleanProperty showingProperty() {
        return showing.getReadOnlyProperty();
    }

    /// Returns whether the chooser is open.
    public final boolean isShowing() {
        return showing.get();
    }

    /// Opens the chooser; a disabled control stays closed.
    public void show() {
        if (isDisabled()) {
            return;
        }
        if (showing.get()) {
            // The chooser may have been dismissed unnoticed.
            core.reopen();
        } else {
            showing.set(true);
        }
    }

    /// Closes the chooser.
    public void hide() {
        showing.set(false);
    }

    /// The handler called when the value changed.
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /// Sets the handler called when the value changed.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        onAction.set(value);
    }

    /// Returns the handler called when the value changed.
    public final EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
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
