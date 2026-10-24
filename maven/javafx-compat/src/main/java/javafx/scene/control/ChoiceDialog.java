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

import java.util.Collection;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.util.Callback;

/// A dialog that asks the user to pick one of a list of items. It shows
/// the items in a [ComboBox], has an OK and a Cancel button, and answers
/// with the selected item when OK is chosen, with nothing otherwise.
///
/// The content text is shown as a label left of the combo box.
public class ChoiceDialog<T> extends Dialog<T> {

    private final T defaultChoice;
    private final ComboBox<T> comboBox = new ComboBox<T>();

    /// Creates a dialog with no items.
    public ChoiceDialog() {
        this((T) null, (T[]) null);
    }

    /// Creates a dialog over items, one of them selected to start with.
    @SuppressWarnings("unchecked")
    public ChoiceDialog(T defaultChoice, T... choices) {
        this(defaultChoice, choices == null ? null : java.util.Arrays.asList(choices));
    }

    /// Creates a dialog over items, one of them selected to start with.
    /// A default choice that is not among the items is ignored, and the
    /// first item is selected.
    public ChoiceDialog(T defaultChoice, Collection<T> choices) {
        DialogRow row = new DialogRow(comboBox);
        DialogPane pane = getDialogPane();
        pane.contentTextProperty().addListener(row);
        pane.getStyleClass().add("choice-dialog");
        pane.setContent(row);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setTitle("Confirmation");
        pane.setHeaderText("Confirmation");
        ObservableList<T> items = comboBox.getItems();
        if (choices != null) {
            items.addAll(choices);
        }
        this.defaultChoice = defaultChoice != null && items.contains(defaultChoice) ? defaultChoice : null;
        SingleSelectionModel<T> model = comboBox.getSelectionModel();
        if (this.defaultChoice == null) {
            model.selectFirst();
        } else {
            model.select(this.defaultChoice);
        }
        setResultConverter(new Callback<ButtonType, T>() {
            @Override
            public T call(ButtonType chosen) {
                ButtonData data = chosen == null ? null : chosen.getButtonData();
                return data == ButtonData.OK_DONE ? getSelectedItem() : null;
            }
        });
    }

    /// Returns the items to pick from.
    public final ObservableList<T> getItems() {
        return comboBox.getItems();
    }

    /// Returns the selected item.
    public final T getSelectedItem() {
        return comboBox.getSelectionModel().getSelectedItem();
    }

    /// The selected item.
    public final ReadOnlyObjectProperty<T> selectedItemProperty() {
        return comboBox.getSelectionModel().selectedItemProperty();
    }

    /// Selects an item.
    public final void setSelectedItem(T item) {
        comboBox.getSelectionModel().select(item);
    }

    /// Returns the item selected to start with, or `null`.
    public final T getDefaultChoice() {
        return defaultChoice;
    }
}
